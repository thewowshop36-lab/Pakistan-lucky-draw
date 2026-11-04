alter table public.wallet_transactions
  drop constraint if exists wallet_transactions_transaction_type_check;

alter table public.wallet_transactions
  add constraint wallet_transactions_transaction_type_check
  check (transaction_type in (
    'ticket_purchase',
    'admin_credit',
    'admin_debit',
    'deposit_credit',
    'withdrawal_hold',
    'withdrawal_release'
  ));

create table public.wallet_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  request_type text not null check (request_type in ('deposit', 'withdrawal')),
  provider text not null check (provider in ('jazzcash', 'easypaisa')),
  amount numeric(12, 2) not null check (amount > 0),
  account_phone text not null check (
    account_phone ~ '^[0-9+() .-]{7,24}$'
    and char_length(regexp_replace(account_phone, '[^0-9]', '', 'g')) between 7 and 15
  ),
  transaction_reference text,
  status text not null default 'pending',
  admin_note text not null default '' check (char_length(admin_note) <= 240),
  idempotency_key uuid not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, idempotency_key),
  check (
    (request_type = 'deposit' and transaction_reference is not null and char_length(trim(transaction_reference)) between 4 and 120)
    or (request_type = 'withdrawal' and transaction_reference is null)
  ),
  check (
    (request_type = 'deposit' and status in ('pending', 'approved', 'rejected'))
    or (request_type = 'withdrawal' and status in ('pending', 'paid', 'rejected'))
  )
);

create unique index wallet_requests_deposit_reference_idx
  on public.wallet_requests (lower(transaction_reference))
  where request_type = 'deposit';

create index wallet_requests_user_created_idx
  on public.wallet_requests (user_id, created_at desc);

create index wallet_requests_status_created_idx
  on public.wallet_requests (status, created_at desc);

alter table public.wallet_requests enable row level security;

create policy wallet_requests_read_own_or_admin
  on public.wallet_requests for select to authenticated
  using (user_id = (select auth.uid()) or public.is_admin());

revoke all on public.wallet_requests from anon, authenticated;
grant select on public.wallet_requests to authenticated;

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

create or replace function public.admin_review_wallet_request(
  p_request_id uuid,
  p_decision text,
  p_admin_note text default ''
)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_request public.wallet_requests%rowtype;
  v_decision text := lower(trim(coalesce(p_decision, '')));
  v_admin_note text := trim(coalesce(p_admin_note, ''));
  v_next_status text;
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access required.';
  end if;

  if p_request_id is null or v_admin_note = '' or char_length(v_admin_note) > 240 then
    raise exception using errcode = '22023', message = 'Add a short review note before processing the request.';
  end if;

  select requests.* into v_request
  from public.wallet_requests as requests
  where requests.id = p_request_id
  for update;

  if not found then
    raise exception using errcode = 'P0002', message = 'Wallet request not found.';
  end if;

  if v_request.status <> 'pending' then
    raise exception using errcode = '22023', message = 'This wallet request has already been reviewed.';
  end if;

  if v_request.request_type = 'deposit' and v_decision = 'approve' then
    update public.profiles as profiles
    set balance = profiles.balance + v_request.amount
    where profiles.id = v_request.user_id;

    insert into public.wallet_transactions (user_id, amount, transaction_type, reference_id, description)
    values (v_request.user_id, v_request.amount, 'deposit_credit', v_request.id, 'Manual deposit verified by admin');

    v_next_status := 'approved';
  elsif v_request.request_type = 'withdrawal' and v_decision = 'paid' then
    v_next_status := 'paid';
  elsif v_decision = 'reject' then
    if v_request.request_type = 'withdrawal' then
      update public.profiles as profiles
      set balance = profiles.balance + v_request.amount
      where profiles.id = v_request.user_id;

      insert into public.wallet_transactions (user_id, amount, transaction_type, reference_id, description)
      values (v_request.user_id, v_request.amount, 'withdrawal_release', v_request.id, 'Rejected withdrawal returned to wallet');
    end if;

    v_next_status := 'rejected';
  else
    raise exception using errcode = '22023', message = 'Choose a valid review action for this request.';
  end if;

  update public.wallet_requests as requests
  set status = v_next_status,
      admin_note = v_admin_note,
      updated_at = now()
  where requests.id = v_request.id;

  return v_next_status;
end;
$$;

revoke all on function public.create_wallet_request(text, text, numeric, text, text, uuid) from public, anon;
grant execute on function public.create_wallet_request(text, text, numeric, text, text, uuid) to authenticated;

revoke all on function public.admin_review_wallet_request(uuid, text, text) from public, anon;
grant execute on function public.admin_review_wallet_request(uuid, text, text) to authenticated;
