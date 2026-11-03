create or replace function public.create_wallet_request(
  p_request_type text,
  p_provider text,
  p_amount numeric,
  p_account_phone text,
  p_transaction_reference text,
  p_idempotency_key uuid
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := (select auth.uid());
  v_request_type text := lower(trim(coalesce(p_request_type, '')));
  v_provider text := lower(trim(coalesce(p_provider, '')));
  v_account_phone text := trim(coalesce(p_account_phone, ''));
  v_reference text := nullif(trim(coalesce(p_transaction_reference, '')), '');
  v_balance numeric(12, 2);
  v_request_id uuid;
  v_recent_request_count integer;
begin
  if v_user_id is null then
    raise exception using errcode = '42501', message = 'Sign in to create a wallet request.';
  end if;

  if not exists (
    select 1 from auth.users as users
    where users.id = v_user_id and users.email_confirmed_at is not null
  ) then
    raise exception using errcode = '42501', message = 'Confirm your email before creating a wallet request.';
  end if;

  if v_request_type not in ('deposit', 'withdrawal')
    or v_provider not in ('jazzcash', 'easypaisa')
    or p_amount is null
    or p_amount::text in ('NaN', 'Infinity', '-Infinity')
    or p_amount <= 0
    or p_amount > 1000000
    or p_amount <> round(p_amount, 2)
    or p_idempotency_key is null
    or char_length(v_account_phone) > 24
    or v_account_phone !~ '^[0-9+() .-]{7,24}$'
    or char_length(regexp_replace(v_account_phone, '[^0-9]', '', 'g')) not between 7 and 15 then
    raise exception using errcode = '22023', message = 'Check the wallet request details and try again.';
  end if;

  if (v_request_type = 'deposit' and (v_reference is null or char_length(v_reference) not between 4 and 120))
    or (v_request_type = 'withdrawal' and v_reference is not null) then
    raise exception using errcode = '22023', message = 'Enter a valid transfer reference for a deposit.';
  end if;

  select profiles.balance into v_balance
  from public.profiles as profiles
  where profiles.id = v_user_id
  for update;

  if not found then
    raise exception using errcode = 'P0002', message = 'Account profile not found.';
  end if;

  select requests.id into v_request_id
  from public.wallet_requests as requests
  where requests.user_id = v_user_id
    and requests.idempotency_key = p_idempotency_key;

  if v_request_id is not null then
    return v_request_id;
  end if;

  select count(*) into v_recent_request_count
  from public.wallet_requests as requests
  where requests.user_id = v_user_id
    and requests.created_at > now() - interval '1 hour';

  if v_recent_request_count >= 10 then
    raise exception using errcode = '22023', message = 'Too many wallet requests. Please wait before trying again.';
  end if;

  if v_request_type = 'withdrawal' and v_balance < p_amount then
    raise exception using errcode = '22023', message = 'Insufficient wallet balance for this withdrawal.';
  end if;

  insert into public.wallet_requests (
    user_id,
    request_type,
    provider,
    amount,
    account_phone,
    transaction_reference,
    idempotency_key
  ) values (
    v_user_id,
    v_request_type,
    v_provider,
    p_amount,
    v_account_phone,
    v_reference,
    p_idempotency_key
  ) returning id into v_request_id;

  if v_request_type = 'withdrawal' then
    update public.profiles as profiles
    set balance = profiles.balance - p_amount
    where profiles.id = v_user_id;

    insert into public.wallet_transactions (user_id, amount, transaction_type, reference_id, description)
    values (v_user_id, -p_amount, 'withdrawal_hold', v_request_id, 'Withdrawal request reserved for review');
  end if;

  return v_request_id;
end;
$$;

revoke all on function public.create_wallet_request(text, text, numeric, text, text, uuid) from public, anon;
grant execute on function public.create_wallet_request(text, text, numeric, text, text, uuid) to authenticated;
