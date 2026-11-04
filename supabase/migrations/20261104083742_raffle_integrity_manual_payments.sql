create schema if not exists private;
revoke all on schema private from public, anon, authenticated;
grant execute on function public.is_admin() to anon, authenticated;

alter table public.profiles
  add column if not exists email_verified_at timestamptz,
  add column if not exists phone_verified_at timestamptz,
  add column if not exists age_confirmed_at timestamptz,
  add column if not exists terms_accepted_at timestamptz,
  add column if not exists terms_version text,
  add column if not exists account_status text not null default 'active',
  add column if not exists suspended_at timestamptz,
  add column if not exists suspension_reason text;

alter table public.draws
  add column if not exists reserved_tickets integer not null default 0,
  add column if not exists end_condition text not null default 'sold_out',
  add column if not exists closed_at timestamptz,
  add column if not exists server_seed_commitment text,
  add column if not exists revealed_server_seed text,
  add column if not exists beacon_source text,
  add column if not exists beacon_pulse_index text,
  add column if not exists beacon_timestamp timestamptz,
  add column if not exists beacon_output_value text,
  add column if not exists beacon_pulse_uri text,
  add column if not exists beacon_signature text,
  add column if not exists draw_algorithm text not null default 'sha256-rejection-v1';

alter table public.user_tickets
  drop constraint if exists user_tickets_source_check;
alter table public.user_tickets
  add constraint user_tickets_source_check check (source in ('paid', 'referral', 'manual_payment'));

alter table public.draws
  add constraint draws_capacity_with_reservations_check
  check (sold_tickets + reserved_tickets <= total_tickets);

alter table public.draws
  add constraint draws_reserved_tickets_nonnegative_check
  check (reserved_tickets >= 0);

alter table public.draws
  add constraint draws_end_condition_check
  check (end_condition in ('sold_out', 'admin_close'));

alter table public.draws
  add constraint draws_seed_commitment_format_check
  check (server_seed_commitment is null or server_seed_commitment ~ '^[a-f0-9]{64}$');

alter table public.draws
  add constraint draws_seed_reveal_format_check
  check (revealed_server_seed is null or revealed_server_seed ~ '^[a-f0-9]{64}$');

alter table public.profiles
  add constraint profiles_account_status_check
  check (account_status in ('active', 'suspended'));

alter table public.profiles
  add constraint profiles_suspension_reason_length_check
  check (suspension_reason is null or char_length(suspension_reason) <= 500);

create table if not exists private.draw_seed_vault (
  draw_id uuid primary key references public.draws(id) on delete cascade,
  server_seed text not null check (server_seed ~ '^[a-f0-9]{64}$'),
  created_at timestamptz not null default now(),
  revealed_at timestamptz
);
alter table private.draw_seed_vault enable row level security;
revoke all on private.draw_seed_vault from public, anon, authenticated;

create table if not exists private.admin_audit_logs (
  id bigint generated always as identity primary key,
  actor_user_id uuid references auth.users(id) on delete set null,
  action text not null check (char_length(action) between 1 and 80),
  entity_type text not null check (char_length(entity_type) between 1 and 80),
  entity_id uuid,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
alter table private.admin_audit_logs enable row level security;
revoke all on private.admin_audit_logs from public, anon, authenticated;
create index if not exists admin_audit_logs_created_at_idx on private.admin_audit_logs (created_at desc);

create table if not exists public.payment_accounts (
  provider text primary key check (provider in ('jazzcash', 'easypaisa')),
  account_title text not null check (char_length(trim(account_title)) between 2 and 80),
  account_identifier text not null check (char_length(trim(account_identifier)) between 4 and 48),
  instructions text not null default '' check (char_length(instructions) <= 500),
  is_active boolean not null default true,
  updated_at timestamptz not null default now()
);
alter table public.payment_accounts enable row level security;
revoke all on public.payment_accounts from public, anon, authenticated;
grant select on public.payment_accounts to anon, authenticated;
drop policy if exists payment_accounts_read_active_or_admin on public.payment_accounts;
create policy payment_accounts_read_active_or_admin
  on public.payment_accounts for select to anon, authenticated
  using (is_active or public.is_admin());

create table if not exists public.ticket_payment_submissions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete restrict,
  draw_id uuid not null references public.draws(id) on delete restrict,
  provider text not null check (provider in ('jazzcash', 'easypaisa')),
  quantity integer not null check (quantity between 1 and 20),
  ticket_price numeric(12, 2) not null check (ticket_price > 0),
  amount numeric(12, 2) not null check (amount > 0),
  transaction_reference text not null check (char_length(trim(transaction_reference)) between 4 and 120),
  proof_path text not null check (char_length(proof_path) between 8 and 220),
  idempotency_key uuid not null,
  status text not null default 'pending' check (status in ('pending', 'approved', 'rejected')),
  admin_note text not null default '' check (char_length(admin_note) <= 500),
  reviewed_by uuid references auth.users(id) on delete set null,
  reviewed_at timestamptz,
  created_at timestamptz not null default now(),
  unique (user_id, idempotency_key)
);
alter table public.ticket_payment_submissions enable row level security;
revoke all on public.ticket_payment_submissions from public, anon, authenticated;
grant select on public.ticket_payment_submissions to authenticated;
drop policy if exists ticket_payment_submissions_read_own_or_admin on public.ticket_payment_submissions;
create policy ticket_payment_submissions_read_own_or_admin
  on public.ticket_payment_submissions for select to authenticated
  using (user_id = (select auth.uid()) or public.is_admin());
create unique index if not exists ticket_payment_submissions_reference_unique_idx
  on public.ticket_payment_submissions (lower(trim(transaction_reference)));
create index if not exists ticket_payment_submissions_status_created_idx
  on public.ticket_payment_submissions (status, created_at desc);
create index if not exists ticket_payment_submissions_user_created_idx
  on public.ticket_payment_submissions (user_id, created_at desc);

create table if not exists public.draw_prizes (
  id uuid primary key default gen_random_uuid(),
  draw_id uuid not null references public.draws(id) on delete restrict,
  tier_order integer not null check (tier_order between 1 and 20),
  prize_name text not null check (char_length(trim(prize_name)) between 1 and 80),
  prize_description text not null default '' check (char_length(prize_description) <= 500),
  prize_amount numeric(12, 2) not null default 0 check (prize_amount >= 0),
  winner_ticket_number integer,
  winner_user_id uuid references public.profiles(id) on delete set null,
  winner_public_name text,
  created_at timestamptz not null default now(),
  unique (draw_id, tier_order),
  unique (draw_id, winner_ticket_number)
);
alter table public.draw_prizes enable row level security;
revoke all on public.draw_prizes from public, anon, authenticated;
create index if not exists draw_prizes_draw_tier_idx on public.draw_prizes (draw_id, tier_order);

create index if not exists wallet_requests_reference_lookup_idx
  on public.wallet_requests (lower(trim(transaction_reference)))
  where transaction_reference is not null;

alter table public.wallet_transactions
  drop constraint if exists wallet_transactions_transaction_type_check;
alter table public.wallet_transactions
  add constraint wallet_transactions_transaction_type_check
  check (transaction_type in (
    'ticket_purchase', 'admin_credit', 'admin_debit', 'deposit_credit',
    'withdrawal_hold', 'withdrawal_release', 'draw_prize'
  ));

create or replace function private.user_is_eligible(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from auth.users as users
    join public.profiles as profiles on profiles.id = users.id
    where users.id = p_user_id
      and users.email_confirmed_at is not null
      and users.phone_confirmed_at is not null
      and profiles.age_confirmed_at is not null
      and profiles.terms_accepted_at is not null
      and profiles.terms_version = '2026-11-04'
      and profiles.account_status = 'active'
  );
$$;
revoke all on function private.user_is_eligible(uuid) from public, anon, authenticated;

create or replace function private.user_can_withdraw(p_user_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from auth.users as users
    join public.profiles as profiles on profiles.id = users.id
    where users.id = p_user_id
      and users.email_confirmed_at is not null
      and users.phone_confirmed_at is not null
      and profiles.account_status = 'active'
  );
$$;
revoke all on function private.user_can_withdraw(uuid) from public, anon, authenticated;

create or replace function private.write_admin_audit(
  p_action text,
  p_entity_type text,
  p_entity_id uuid,
  p_details jsonb default '{}'::jsonb
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into private.admin_audit_logs (actor_user_id, action, entity_type, entity_id, details)
  values ((select auth.uid()), p_action, p_entity_type, p_entity_id, coalesce(p_details, '{}'::jsonb));
end;
$$;
revoke all on function private.write_admin_audit(text, text, uuid, jsonb) from public, anon, authenticated;

create or replace function private.validate_new_user_metadata()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_metadata jsonb := coalesce(new.raw_user_meta_data, '{}'::jsonb);
  v_birth_date date;
  v_has_eligibility_fields boolean;
begin
  v_has_eligibility_fields := v_metadata ? 'birth_date'
    or v_metadata ? 'accepted_terms'
    or v_metadata ? 'terms_version';

  if v_has_eligibility_fields then
    begin
      v_birth_date := (v_metadata ->> 'birth_date')::date;
    exception when others then
      raise exception using errcode = '22023', message = 'Enter a valid date of birth.';
    end;

    if v_birth_date is null or v_birth_date > (current_date - interval '18 years')::date then
      raise exception using errcode = '22023', message = 'You must be at least 18 years old to create an account.';
    end if;

    if lower(coalesce(v_metadata ->> 'accepted_terms', 'false')) <> 'true'
      or coalesce(v_metadata ->> 'terms_version', '') <> '2026-11-04' then
      raise exception using errcode = '22023', message = 'Accept the current terms and privacy policy to continue.';
    end if;

    v_metadata := (v_metadata - array['birth_date', 'accepted_terms', 'terms_version', 'v0_eligibility_validated'])
      || jsonb_build_object('v0_eligibility_validated', true, 'v0_terms_version', '2026-11-04');
  else
    v_metadata := v_metadata - 'v0_eligibility_validated' - 'v0_terms_version';
  end if;

  new.raw_user_meta_data := v_metadata;
  return new;
end;
$$;
revoke all on function private.validate_new_user_metadata() from public, anon, authenticated;
drop trigger if exists v0_validate_new_user_metadata on auth.users;
create trigger v0_validate_new_user_metadata
  before insert on auth.users
  for each row execute function private.validate_new_user_metadata();

create or replace function private.sync_auth_profile()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_is_insert boolean := tg_op = 'INSERT';
  v_eligibility_validated boolean := coalesce(new.raw_user_meta_data ->> 'v0_eligibility_validated', 'false') = 'true';
begin
  insert into public.profiles (
    id, name, phone, email_verified_at, phone_verified_at,
    age_confirmed_at, terms_accepted_at, terms_version
  ) values (
    new.id,
    left(coalesce(nullif(trim(new.raw_user_meta_data ->> 'name'), ''), 'Player'), 80),
    coalesce(new.phone, nullif(trim(new.raw_user_meta_data ->> 'phone'), '')),
    new.email_confirmed_at,
    new.phone_confirmed_at,
    case when v_eligibility_validated then now() else null end,
    case when v_eligibility_validated then now() else null end,
    case when v_eligibility_validated then new.raw_user_meta_data ->> 'v0_terms_version' else null end
  )
  on conflict (id) do update set
    email_verified_at = coalesce(excluded.email_verified_at, public.profiles.email_verified_at),
    phone = case when new.phone_confirmed_at is not null then new.phone else coalesce(public.profiles.phone, excluded.phone) end,
    phone_verified_at = coalesce(excluded.phone_verified_at, public.profiles.phone_verified_at),
    age_confirmed_at = coalesce(public.profiles.age_confirmed_at, excluded.age_confirmed_at),
    terms_accepted_at = coalesce(public.profiles.terms_accepted_at, excluded.terms_accepted_at),
    terms_version = coalesce(public.profiles.terms_version, excluded.terms_version);

  if not v_is_insert and new.phone_confirmed_at is null and old.phone_confirmed_at is not null then
    update public.profiles set phone_verified_at = null where id = new.id;
  end if;

  return new;
end;
$$;
revoke all on function private.sync_auth_profile() from public, anon, authenticated;
drop trigger if exists zz_v0_sync_auth_profile_insert on auth.users;
create trigger zz_v0_sync_auth_profile_insert
  after insert on auth.users
  for each row execute function private.sync_auth_profile();
drop trigger if exists zz_v0_sync_auth_profile_update on auth.users;
create trigger zz_v0_sync_auth_profile_update
  after update of email, email_confirmed_at, phone, phone_confirmed_at on auth.users
  for each row execute function private.sync_auth_profile();

update public.profiles as profiles
set email_verified_at = users.email_confirmed_at,
    phone_verified_at = users.phone_confirmed_at,
    phone = case when users.phone_confirmed_at is not null then users.phone else profiles.phone end
from auth.users as users
where users.id = profiles.id
  and (
    profiles.email_verified_at is distinct from users.email_confirmed_at
    or profiles.phone_verified_at is distinct from users.phone_confirmed_at
    or (users.phone_confirmed_at is not null and profiles.phone is distinct from users.phone)
  );

create or replace function public.current_user_can_enter_draw()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select private.user_is_eligible((select auth.uid()));
$$;
revoke all on function public.current_user_can_enter_draw() from public, anon;
grant execute on function public.current_user_can_enter_draw() to authenticated;

create or replace function public.complete_user_eligibility(p_birth_date date, p_terms_version text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := (select auth.uid());
begin
  if v_user_id is null then
    raise exception using errcode = '42501', message = 'Sign in to complete account verification.';
  end if;

  if not exists (
    select 1 from auth.users as users
    where users.id = v_user_id and users.email_confirmed_at is not null
  ) then
    raise exception using errcode = '42501', message = 'Confirm your email before completing account verification.';
  end if;

  if p_birth_date is null or p_birth_date > (current_date - interval '18 years')::date then
    raise exception using errcode = '22023', message = 'You must be at least 18 years old to enter a draw.';
  end if;
  if p_terms_version is distinct from '2026-11-04' then
    raise exception using errcode = '22023', message = 'Review and accept the current terms and privacy policy.';
  end if;

  update public.profiles
  set age_confirmed_at = coalesce(age_confirmed_at, now()),
      terms_accepted_at = now(),
      terms_version = '2026-11-04'
  where id = v_user_id;

  if not found then
    raise exception using errcode = 'P0002', message = 'Account profile not found.';
  end if;
end;
$$;
revoke all on function public.complete_user_eligibility(date, text) from public, anon;
grant execute on function public.complete_user_eligibility(date, text) to authenticated;

create or replace function private.guard_user_ticket_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_draw_status text;
  v_draw_closed_at timestamptz;
begin
  if not private.user_is_eligible(new.user_id) then
    raise exception using errcode = '42501', message = 'Complete email, phone, age, and terms verification before entering a draw.';
  end if;

  select draws.status, draws.closed_at
  into v_draw_status, v_draw_closed_at
  from public.draws as draws
  where draws.id = new.draw_id
  for update;

  if not found or v_draw_status <> 'active' or v_draw_closed_at is not null then
    raise exception using errcode = '55000', message = 'This draw is closed for ticket entries.';
  end if;

  return new;
end;
$$;
revoke all on function private.guard_user_ticket_insert() from public, anon, authenticated;
drop trigger if exists guard_user_ticket_insert on public.user_tickets;
create trigger guard_user_ticket_insert
  before insert on public.user_tickets
  for each row execute function private.guard_user_ticket_insert();

revoke all on function public.admin_complete_draw(uuid, integer) from public, anon, authenticated;

create or replace function private.guard_wallet_request_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_reference text := nullif(lower(trim(coalesce(new.transaction_reference, ''))), '');
begin
  if new.request_type = 'deposit' then
    if not private.user_is_eligible(new.user_id) then
      raise exception using errcode = '42501', message = 'Complete email, phone, age, and terms verification before depositing.';
    end if;
    if v_reference is not null then
      perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(v_reference, 0));
      if exists (
        select 1 from public.ticket_payment_submissions as submissions
        where lower(trim(submissions.transaction_reference)) = v_reference
      ) then
        raise exception using errcode = '23505', message = 'That transfer reference has already been submitted.';
      end if;
    end if;
  elsif new.request_type = 'withdrawal' and not private.user_can_withdraw(new.user_id) then
    raise exception using errcode = '42501', message = 'Verify your email and phone before requesting a withdrawal.';
  end if;

  return new;
end;
$$;
revoke all on function private.guard_wallet_request_insert() from public, anon, authenticated;
drop trigger if exists guard_wallet_request_insert on public.wallet_requests;
create trigger guard_wallet_request_insert
  before insert on public.wallet_requests
  for each row execute function private.guard_wallet_request_insert();

create or replace function private.initialize_draw_seed()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_seed text := encode(extensions.gen_random_bytes(32), 'hex');
begin
  insert into private.draw_seed_vault (draw_id, server_seed)
  values (new.id, v_seed);

  update public.draws
  set server_seed_commitment = encode(extensions.digest(v_seed, 'sha256'), 'hex')
  where id = new.id;

  return new;
end;
$$;
revoke all on function private.initialize_draw_seed() from public, anon, authenticated;
drop trigger if exists a_initialize_draw_seed on public.draws;
create trigger a_initialize_draw_seed
  after insert on public.draws
  for each row execute function private.initialize_draw_seed();

create or replace function private.add_default_draw_prize()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.draw_prizes (draw_id, tier_order, prize_name, prize_description, prize_amount)
  values (new.id, 1, 'Grand prize', coalesce(new.prize_details, ''), 0)
  on conflict (draw_id, tier_order) do nothing;
  return new;
end;
$$;
revoke all on function private.add_default_draw_prize() from public, anon, authenticated;
drop trigger if exists b_add_default_draw_prize on public.draws;
create trigger b_add_default_draw_prize
  after insert on public.draws
  for each row execute function private.add_default_draw_prize();

insert into private.draw_seed_vault (draw_id, server_seed)
select draws.id, encode(extensions.gen_random_bytes(32), 'hex')
from public.draws as draws
where draws.status = 'active'
  and draws.server_seed_commitment is null
on conflict (draw_id) do nothing;

update public.draws as draws
set server_seed_commitment = encode(extensions.digest(vault.server_seed, 'sha256'), 'hex')
from private.draw_seed_vault as vault
where vault.draw_id = draws.id
  and draws.status = 'active'
  and draws.server_seed_commitment is null;

insert into public.draw_prizes (
  draw_id, tier_order, prize_name, prize_description, prize_amount,
  winner_ticket_number, winner_user_id, winner_public_name
)
select
  draws.id,
  1,
  'Grand prize',
  coalesce(draws.prize_details, ''),
  0,
  draws.winner_ticket,
  draws.winner_user_id,
  left(trim(coalesce(profiles.name, 'Member')), 1) || '•••'
from public.draws as draws
left join public.profiles as profiles on profiles.id = draws.winner_user_id
where draws.status = 'completed'
  and not exists (
    select 1 from public.draw_prizes as prizes where prizes.draw_id = draws.id
  )
on conflict (draw_id, tier_order) do nothing;

create or replace function private.capture_draw_close_time()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if new.status = 'active' and new.sold_tickets >= new.total_tickets and old.sold_tickets < old.total_tickets then
    new.closed_at := coalesce(new.closed_at, now());
  end if;
  return new;
end;
$$;
revoke all on function private.capture_draw_close_time() from public, anon, authenticated;
drop trigger if exists capture_draw_close_time on public.draws;
create trigger capture_draw_close_time
  before update of sold_tickets, total_tickets on public.draws
  for each row execute function private.capture_draw_close_time();

create or replace function private.guard_closed_draw_configuration()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if (old.closed_at is not null and new.closed_at is null) or (
    (old.closed_at is not null or old.sold_tickets > 0 or old.reserved_tickets > 0)
    and (
      new.draw_number is distinct from old.draw_number
      or new.title is distinct from old.title
      or new.description is distinct from old.description
      or new.prize_details is distinct from old.prize_details
      or new.ticket_price is distinct from old.ticket_price
      or new.total_tickets is distinct from old.total_tickets
      or new.profit_margin is distinct from old.profit_margin
      or new.banner_image_url is distinct from old.banner_image_url
      or new.end_condition is distinct from old.end_condition
    )
  ) then
    raise exception using errcode = '55000', message = 'Draw configuration is locked after ticket sales or closure.';
  end if;
  return new;
end;
$$;
revoke all on function private.guard_closed_draw_configuration() from public, anon, authenticated;
drop trigger if exists guard_closed_draw_configuration on public.draws;
create trigger guard_closed_draw_configuration
  before update of draw_number, title, description, prize_details, ticket_price, total_tickets, profit_margin, banner_image_url, end_condition, closed_at
  on public.draws
  for each row execute function private.guard_closed_draw_configuration();

create or replace function public.submit_ticket_payment(
  p_draw_id uuid,
  p_provider text,
  p_quantity integer,
  p_transaction_reference text,
  p_proof_path text,
  p_idempotency_key uuid
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := (select auth.uid());
  v_provider text := lower(trim(coalesce(p_provider, '')));
  v_reference text := nullif(trim(coalesce(p_transaction_reference, '')), '');
  v_reference_key text;
  v_proof_path text := trim(coalesce(p_proof_path, ''));
  v_existing public.ticket_payment_submissions%rowtype;
  v_draw public.draws%rowtype;
  v_submission_id uuid;
  v_recent_count integer;
  v_amount numeric(12, 2);
begin
  if v_user_id is null then
    raise exception using errcode = '42501', message = 'Sign in to submit a ticket payment.';
  end if;
  if not private.user_is_eligible(v_user_id) then
    raise exception using errcode = '42501', message = 'Complete email, phone, age, and terms verification before entering a draw.';
  end if;
  if v_provider not in ('jazzcash', 'easypaisa')
    or p_quantity is null or p_quantity not between 1 and 20
    or v_reference is null or char_length(v_reference) not between 4 and 120
    or p_idempotency_key is null
    or v_proof_path !~ ('^' || v_user_id::text || '/[A-Za-z0-9][A-Za-z0-9._-]{0,180}$') then
    raise exception using errcode = '22023', message = 'Check the payment details and upload a valid proof file.';
  end if;

  select * into v_existing
  from public.ticket_payment_submissions as submissions
  where submissions.user_id = v_user_id
    and submissions.idempotency_key = p_idempotency_key;
  if found then
    return jsonb_build_object(
      'submission_id', v_existing.id,
      'status', v_existing.status,
      'amount', v_existing.amount,
      'idempotent', true
    );
  end if;

  v_reference_key := lower(v_reference);
  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(v_reference_key, 0));
  if exists (
    select 1 from public.wallet_requests as requests
    where requests.request_type = 'deposit'
      and lower(trim(coalesce(requests.transaction_reference, ''))) = v_reference_key
  ) or exists (
    select 1 from public.ticket_payment_submissions as submissions
    where lower(trim(submissions.transaction_reference)) = v_reference_key
  ) then
    raise exception using errcode = '23505', message = 'That transfer reference has already been submitted.';
  end if;

  if not exists (
    select 1 from storage.objects as objects
    where objects.bucket_id = 'ticket-payment-proofs'
      and objects.name = v_proof_path
  ) then
    raise exception using errcode = '22023', message = 'Upload the payment proof before submitting the request.';
  end if;

  select count(*) into v_recent_count
  from public.ticket_payment_submissions as submissions
  where submissions.user_id = v_user_id
    and submissions.created_at > now() - interval '1 hour';
  if v_recent_count >= 10 then
    raise exception using errcode = '22023', message = 'Too many payment submissions. Please wait before trying again.';
  end if;

  select * into v_draw
  from public.draws as draws
  where draws.id = p_draw_id
  for update;
  if not found or v_draw.status <> 'active' or v_draw.closed_at is not null then
    raise exception using errcode = 'P0002', message = 'This draw is not open for ticket requests.';
  end if;

  if v_draw.total_tickets - v_draw.sold_tickets - v_draw.reserved_tickets < p_quantity then
    raise exception using errcode = '22023', message = 'There are not enough tickets remaining for this request.';
  end if;

  v_amount := round(v_draw.ticket_price * p_quantity, 2);
  insert into public.ticket_payment_submissions (
    user_id, draw_id, provider, quantity, ticket_price, amount,
    transaction_reference, proof_path, idempotency_key
  ) values (
    v_user_id, p_draw_id, v_provider, p_quantity, v_draw.ticket_price, v_amount,
    v_reference, v_proof_path, p_idempotency_key
  ) returning id into v_submission_id;

  update public.draws
  set reserved_tickets = reserved_tickets + p_quantity
  where id = p_draw_id;

  perform private.write_admin_audit(
    'ticket_payment_submitted', 'ticket_payment_submission', v_submission_id,
    jsonb_build_object('draw_id', p_draw_id, 'quantity', p_quantity, 'amount', v_amount, 'provider', v_provider)
  );

  return jsonb_build_object(
    'submission_id', v_submission_id,
    'status', 'pending',
    'amount', v_amount,
    'idempotent', false
  );
end;
$$;
revoke all on function public.submit_ticket_payment(uuid, text, integer, text, text, uuid) from public, anon;
grant execute on function public.submit_ticket_payment(uuid, text, integer, text, text, uuid) to authenticated;

create or replace function public.admin_review_ticket_payment(
  p_submission_id uuid,
  p_decision text,
  p_admin_note text default ''
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_submission public.ticket_payment_submissions%rowtype;
  v_draw public.draws%rowtype;
  v_note text := left(trim(coalesce(p_admin_note, '')), 500);
  v_first_ticket integer;
  v_ticket_numbers integer[];
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access is required.';
  end if;
  if p_decision is null or p_decision not in ('approve', 'reject') then
    raise exception using errcode = '22023', message = 'Choose approve or reject.';
  end if;

  select * into v_submission
  from public.ticket_payment_submissions as submissions
  where submissions.id = p_submission_id
  for update;
  if not found or v_submission.status <> 'pending' then
    raise exception using errcode = 'P0002', message = 'This payment request is no longer pending.';
  end if;

  select * into v_draw
  from public.draws as draws
  where draws.id = v_submission.draw_id
  for update;
  if not found or v_draw.reserved_tickets < v_submission.quantity then
    raise exception using errcode = 'P0002', message = 'The reserved ticket count is no longer available.';
  end if;

  if p_decision = 'approve' then
    if v_draw.status <> 'active' or v_draw.closed_at is not null or not private.user_is_eligible(v_submission.user_id) then
      raise exception using errcode = '42501', message = 'The account or draw is no longer eligible for approval.';
    end if;
    if v_draw.sold_tickets + v_submission.quantity > v_draw.total_tickets then
      raise exception using errcode = '22023', message = 'The draw no longer has enough available tickets.';
    end if;

    v_first_ticket := v_draw.sold_tickets + 1;
    select array_agg(ticket_number order by ticket_number) into v_ticket_numbers
    from generate_series(v_first_ticket, v_first_ticket + v_submission.quantity - 1) as series(ticket_number);

    insert into public.user_tickets (user_id, draw_id, ticket_number, status, source)
    select v_submission.user_id, v_draw.id, ticket_number, 'active', 'manual_payment'
    from generate_series(v_first_ticket, v_first_ticket + v_submission.quantity - 1) as series(ticket_number);

    update public.draws
    set sold_tickets = sold_tickets + v_submission.quantity,
        reserved_tickets = reserved_tickets - v_submission.quantity
    where id = v_draw.id;
  else
    update public.draws
    set reserved_tickets = reserved_tickets - v_submission.quantity
    where id = v_draw.id;
  end if;

  update public.ticket_payment_submissions
  set status = case when p_decision = 'approve' then 'approved' else 'rejected' end,
      admin_note = v_note,
      reviewed_by = (select auth.uid()),
      reviewed_at = now()
  where id = v_submission.id;

  perform private.write_admin_audit(
    case when p_decision = 'approve' then 'ticket_payment_approved' else 'ticket_payment_rejected' end,
    'ticket_payment_submission', v_submission.id,
    jsonb_build_object(
      'user_id', v_submission.user_id,
      'draw_id', v_submission.draw_id,
      'quantity', v_submission.quantity,
      'amount', v_submission.amount,
      'ticket_numbers', coalesce(to_jsonb(v_ticket_numbers), '[]'::jsonb),
      'note', v_note
    )
  );

  return jsonb_build_object(
    'submission_id', v_submission.id,
    'status', case when p_decision = 'approve' then 'approved' else 'rejected' end,
    'ticket_numbers', coalesce(to_jsonb(v_ticket_numbers), '[]'::jsonb)
  );
end;
$$;
revoke all on function public.admin_review_ticket_payment(uuid, text, text) from public, anon;
grant execute on function public.admin_review_ticket_payment(uuid, text, text) to authenticated;

create or replace function public.admin_save_payment_account(
  p_provider text,
  p_account_title text,
  p_account_identifier text,
  p_instructions text default '',
  p_is_active boolean default true
)
returns public.payment_accounts
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_provider text := lower(trim(coalesce(p_provider, '')));
  v_title text := trim(coalesce(p_account_title, ''));
  v_identifier text := trim(coalesce(p_account_identifier, ''));
  v_account public.payment_accounts%rowtype;
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access is required.';
  end if;
  if v_provider not in ('jazzcash', 'easypaisa')
    or char_length(v_title) not between 2 and 80
    or char_length(v_identifier) not between 4 and 48
    or v_identifier !~ '^[A-Za-z0-9+() ./-]{4,48}$'
    or char_length(coalesce(p_instructions, '')) > 500 then
    raise exception using errcode = '22023', message = 'Check the account details and try again.';
  end if;

  insert into public.payment_accounts (provider, account_title, account_identifier, instructions, is_active, updated_at)
  values (v_provider, v_title, v_identifier, trim(coalesce(p_instructions, '')), coalesce(p_is_active, false), now())
  on conflict (provider) do update set
    account_title = excluded.account_title,
    account_identifier = excluded.account_identifier,
    instructions = excluded.instructions,
    is_active = excluded.is_active,
    updated_at = now()
  returning * into v_account;

  perform private.write_admin_audit(
    'payment_account_saved', 'payment_account', null,
    jsonb_build_object('provider', v_provider, 'is_active', v_account.is_active)
  );
  return v_account;
end;
$$;
revoke all on function public.admin_save_payment_account(text, text, text, text, boolean) from public, anon;
grant execute on function public.admin_save_payment_account(text, text, text, text, boolean) to authenticated;

create or replace function public.admin_set_user_status(
  p_user_id uuid,
  p_status text,
  p_reason text default ''
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_reason text := left(trim(coalesce(p_reason, '')), 500);
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access is required.';
  end if;
  if p_status not in ('active', 'suspended') then
    raise exception using errcode = '22023', message = 'Choose a valid account status.';
  end if;
  if p_user_id = (select auth.uid()) and p_status = 'suspended' then
    raise exception using errcode = '22023', message = 'You cannot suspend your own administrator account.';
  end if;
  if exists (select 1 from public.admin_users as admins where admins.user_id = p_user_id) and p_status = 'suspended' then
    raise exception using errcode = '22023', message = 'Administrator accounts cannot be suspended here.';
  end if;

  update public.profiles
  set account_status = p_status,
      suspended_at = case when p_status = 'suspended' then coalesce(suspended_at, now()) else null end,
      suspension_reason = case when p_status = 'suspended' then nullif(v_reason, '') else null end
  where id = p_user_id;
  if not found then
    raise exception using errcode = 'P0002', message = 'Account profile not found.';
  end if;

  perform private.write_admin_audit(
    case when p_status = 'suspended' then 'user_suspended' else 'user_reactivated' end,
    'profile', p_user_id,
    jsonb_build_object('status', p_status, 'reason', v_reason)
  );
end;
$$;
revoke all on function public.admin_set_user_status(uuid, text, text) from public, anon;
grant execute on function public.admin_set_user_status(uuid, text, text) to authenticated;

create or replace function public.admin_set_draw_end_condition(p_draw_id uuid, p_end_condition text)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access is required.';
  end if;
  if p_end_condition is null or p_end_condition not in ('sold_out', 'admin_close') then
    raise exception using errcode = '22023', message = 'Choose a valid draw end condition.';
  end if;

  update public.draws
  set end_condition = p_end_condition,
      updated_at = now()
  where id = p_draw_id and status = 'active' and closed_at is null;
  if not found then
    raise exception using errcode = 'P0002', message = 'Active draw not found.';
  end if;

  perform private.write_admin_audit(
    'draw_end_condition_changed', 'draw', p_draw_id,
    jsonb_build_object('end_condition', p_end_condition)
  );
end;
$$;
revoke all on function public.admin_set_draw_end_condition(uuid, text) from public, anon;
grant execute on function public.admin_set_draw_end_condition(uuid, text) to authenticated;

create or replace function public.admin_save_draw_prizes(p_draw_id uuid, p_prizes jsonb)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_draw public.draws%rowtype;
  v_item jsonb;
  v_order integer;
  v_name text;
  v_description text;
  v_amount numeric(12, 2);
  v_total numeric(12, 2) := 0;
  v_max_pool numeric(14, 2);
  v_count integer := 0;
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access is required.';
  end if;
  if p_prizes is null or jsonb_typeof(p_prizes) <> 'array' or jsonb_array_length(p_prizes) not between 1 and 20 then
    raise exception using errcode = '22023', message = 'Add between 1 and 20 prize tiers.';
  end if;

  select * into v_draw from public.draws where id = p_draw_id for update;
  if not found or v_draw.status <> 'active' or v_draw.closed_at is not null then
    raise exception using errcode = 'P0002', message = 'Only open active draws can have prize tiers edited.';
  end if;
  if v_draw.sold_tickets > 0 or v_draw.reserved_tickets > 0 then
    raise exception using errcode = '55000', message = 'Prize tiers are locked after the first ticket is sold or reserved.';
  end if;

  v_max_pool := round(v_draw.ticket_price * v_draw.total_tickets * (100 - v_draw.profit_margin) / 100, 2);
  for v_item in select value from jsonb_array_elements(p_prizes)
  loop
    v_order := (v_item ->> 'tier_order')::integer;
    v_name := trim(coalesce(v_item ->> 'prize_name', ''));
    v_description := trim(coalesce(v_item ->> 'prize_description', ''));
    v_amount := (v_item ->> 'prize_amount')::numeric;
    if v_order not between 1 and 20
      or char_length(v_name) not between 1 and 80
      or char_length(v_description) > 500
      or v_amount is null
      or v_amount::text in ('NaN', 'Infinity', '-Infinity')
      or v_amount < 0
      or v_amount > 100000000
      or v_amount <> round(v_amount, 2) then
      raise exception using errcode = '22023', message = 'Check each prize tier name and amount.';
    end if;
    v_total := v_total + v_amount;
    v_count := v_count + 1;
  end loop;

  if v_total > v_max_pool then
    raise exception using errcode = '22023', message = 'Prize amounts cannot exceed the maximum available prize pool.';
  end if;

  if exists (select 1 from public.draw_prizes where draw_id = p_draw_id and winner_ticket_number is not null) then
    raise exception using errcode = '22023', message = 'Prize tiers are locked after a draw result is published.';
  end if;

  delete from public.draw_prizes where draw_id = p_draw_id;
  for v_item in select value from jsonb_array_elements(p_prizes)
  loop
    insert into public.draw_prizes (draw_id, tier_order, prize_name, prize_description, prize_amount)
    values (
      p_draw_id,
      (v_item ->> 'tier_order')::integer,
      trim(v_item ->> 'prize_name'),
      trim(coalesce(v_item ->> 'prize_description', '')),
      (v_item ->> 'prize_amount')::numeric
    );
  end loop;

  perform private.write_admin_audit(
    'draw_prizes_saved', 'draw', p_draw_id,
    jsonb_build_object('tier_count', v_count, 'committed_prize_amount', v_total, 'maximum_pool', v_max_pool)
  );
end;
$$;
revoke all on function public.admin_save_draw_prizes(uuid, jsonb) from public, anon;
grant execute on function public.admin_save_draw_prizes(uuid, jsonb) to authenticated;

create or replace function public.admin_get_draw_prizes()
returns table (
  id uuid,
  draw_id uuid,
  tier_order integer,
  prize_name text,
  prize_description text,
  prize_amount numeric,
  winner_ticket_number integer,
  winner_user_id uuid,
  winner_public_name text
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access is required.';
  end if;
  return query
  select prizes.id, prizes.draw_id, prizes.tier_order, prizes.prize_name,
    prizes.prize_description, prizes.prize_amount, prizes.winner_ticket_number,
    prizes.winner_user_id, prizes.winner_public_name
  from public.draw_prizes as prizes
  order by prizes.draw_id, prizes.tier_order;
end;
$$;
revoke all on function public.admin_get_draw_prizes() from public, anon;
grant execute on function public.admin_get_draw_prizes() to authenticated;

create or replace function public.admin_get_draw_financials()
returns table (
  draw_id uuid,
  draw_number integer,
  gross_revenue numeric,
  platform_profit numeric,
  prize_pool numeric,
  committed_prizes numeric,
  remaining_prize_pool numeric,
  paid_tickets bigint,
  referral_tickets bigint,
  pending_payment_amount numeric,
  pending_payment_tickets bigint,
  reserved_tickets integer
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access is required.';
  end if;

  return query
  select
    draws.id,
    draws.draw_number,
    round(coalesce(ticket_counts.paid_tickets, 0)::numeric * draws.ticket_price, 2),
    round(coalesce(ticket_counts.paid_tickets, 0)::numeric * draws.ticket_price * draws.profit_margin / 100, 2),
    round(coalesce(ticket_counts.paid_tickets, 0)::numeric * draws.ticket_price * (100 - draws.profit_margin) / 100, 2),
    coalesce(prize_totals.committed_prizes, 0)::numeric,
    round(coalesce(ticket_counts.paid_tickets, 0)::numeric * draws.ticket_price * (100 - draws.profit_margin) / 100, 2) - coalesce(prize_totals.committed_prizes, 0),
    coalesce(ticket_counts.paid_tickets, 0)::bigint,
    coalesce(ticket_counts.referral_tickets, 0)::bigint,
    coalesce(pending_totals.pending_payment_amount, 0)::numeric,
    coalesce(pending_totals.pending_payment_tickets, 0)::bigint,
    draws.reserved_tickets
  from public.draws as draws
  left join lateral (
    select
      count(*) filter (where tickets.source in ('paid', 'manual_payment')) as paid_tickets,
      count(*) filter (where tickets.source = 'referral') as referral_tickets
    from public.user_tickets as tickets
    where tickets.draw_id = draws.id
  ) as ticket_counts on true
  left join lateral (
    select sum(prizes.prize_amount) as committed_prizes
    from public.draw_prizes as prizes
    where prizes.draw_id = draws.id
  ) as prize_totals on true
  left join lateral (
    select sum(submissions.amount) as pending_payment_amount,
      sum(submissions.quantity) as pending_payment_tickets
    from public.ticket_payment_submissions as submissions
    where submissions.draw_id = draws.id and submissions.status = 'pending'
  ) as pending_totals on true
  order by draws.draw_number;
end;
$$;
revoke all on function public.admin_get_draw_financials() from public, anon;
grant execute on function public.admin_get_draw_financials() to authenticated;

create or replace function public.admin_get_recent_audit_logs(p_limit integer default 50)
returns table (
  id bigint,
  actor_user_id uuid,
  actor_name text,
  action text,
  entity_type text,
  entity_id uuid,
  details jsonb,
  created_at timestamptz
)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access is required.';
  end if;
  return query
  select logs.id, logs.actor_user_id, profiles.name, logs.action,
    logs.entity_type, logs.entity_id, logs.details, logs.created_at
  from private.admin_audit_logs as logs
  left join public.profiles as profiles on profiles.id = logs.actor_user_id
  order by logs.created_at desc
  limit greatest(1, least(coalesce(p_limit, 50), 100));
end;
$$;
revoke all on function public.admin_get_recent_audit_logs(integer) from public, anon;
grant execute on function public.admin_get_recent_audit_logs(integer) to authenticated;

create or replace function private.audit_wallet_request_status()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if old.status is distinct from new.status then
    perform private.write_admin_audit(
      'wallet_request_' || new.status, 'wallet_request', new.id,
      jsonb_build_object('user_id', new.user_id, 'request_type', new.request_type, 'amount', new.amount, 'provider', new.provider)
    );
  end if;
  return new;
end;
$$;
revoke all on function private.audit_wallet_request_status() from public, anon, authenticated;
drop trigger if exists audit_wallet_request_status on public.wallet_requests;
create trigger audit_wallet_request_status
  after update of status on public.wallet_requests
  for each row execute function private.audit_wallet_request_status();

create or replace function private.audit_wallet_transaction_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform private.write_admin_audit(
    'wallet_ledger_entry', 'wallet_transaction', new.id,
    jsonb_build_object('user_id', new.user_id, 'amount', new.amount, 'transaction_type', new.transaction_type, 'reference_id', new.reference_id)
  );
  return new;
end;
$$;
revoke all on function private.audit_wallet_transaction_insert() from public, anon, authenticated;
drop trigger if exists audit_wallet_transaction_insert on public.wallet_transactions;
create trigger audit_wallet_transaction_insert
  after insert on public.wallet_transactions
  for each row execute function private.audit_wallet_transaction_insert();

create or replace function private.audit_draw_configuration()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform private.write_admin_audit(
    case when tg_op = 'INSERT' then 'draw_created' else 'draw_configuration_updated' end,
    'draw', new.id,
    jsonb_build_object(
      'draw_number', new.draw_number,
      'title', new.title,
      'ticket_price', new.ticket_price,
      'total_tickets', new.total_tickets,
      'profit_margin', new.profit_margin
    )
  );
  return new;
end;
$$;
revoke all on function private.audit_draw_configuration() from public, anon, authenticated;
drop trigger if exists audit_draw_configuration on public.draws;
create trigger audit_draw_configuration
  after insert or update of draw_number, title, description, prize_details, ticket_price, total_tickets, profit_margin, banner_image_url
  on public.draws
  for each row execute function private.audit_draw_configuration();

create or replace function public.mark_draw_closed(p_draw_id uuid, p_allow_partial boolean)
returns timestamptz
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_draw public.draws%rowtype;
begin
  if coalesce((select auth.role()), '') <> 'service_role' then
    raise exception using errcode = '42501', message = 'Service authorization is required.';
  end if;

  select * into v_draw from public.draws where id = p_draw_id for update;
  if not found or v_draw.status <> 'active' then
    raise exception using errcode = 'P0002', message = 'This draw is not active.';
  end if;
  if v_draw.reserved_tickets > 0 then
    raise exception using errcode = '22023', message = 'Review or reject pending payment requests before drawing a result.';
  end if;
  if p_allow_partial then
    if v_draw.end_condition <> 'admin_close' then
      raise exception using errcode = '22023', message = 'This draw is configured to close only when sold out.';
    end if;
  elsif v_draw.end_condition <> 'sold_out' or v_draw.sold_tickets <> v_draw.total_tickets then
    raise exception using errcode = '22023', message = 'The draw must be sold out before publishing its result.';
  end if;
  if v_draw.sold_tickets < 1 then
    raise exception using errcode = '22023', message = 'At least one ticket must be sold before publishing a result.';
  end if;

  update public.draws
  set closed_at = coalesce(closed_at, now()), updated_at = now()
  where id = p_draw_id
  returning closed_at into v_draw.closed_at;
  return v_draw.closed_at;
end;
$$;
revoke all on function public.mark_draw_closed(uuid, boolean) from public, anon, authenticated;
grant execute on function public.mark_draw_closed(uuid, boolean) to service_role;

create or replace function public.finalize_draw_with_beacon(
  p_draw_id uuid,
  p_beacon_output_value text,
  p_beacon_pulse_index text,
  p_beacon_timestamp timestamptz,
  p_beacon_pulse_uri text,
  p_beacon_signature text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_draw public.draws%rowtype;
  v_seed text;
  v_ticket_count integer;
  v_paid_tickets bigint;
  v_gross_revenue numeric(14, 2);
  v_prize_pool numeric(14, 2);
  v_prize_total numeric(14, 2);
  v_hash bytea;
  v_random_value numeric;
  v_universe numeric := power(2::numeric, 256);
  v_limit numeric;
  v_candidate integer;
  v_counter integer;
  v_winner_numbers integer[] := '{}'::integer[];
  v_prize record;
  v_winner_user_id uuid;
  v_winner_name text;
  v_masked_name text;
  v_winners jsonb := '[]'::jsonb;
  v_existing_winners jsonb;
  v_i integer;
begin
  if coalesce((select auth.role()), '') <> 'service_role' then
    raise exception using errcode = '42501', message = 'Service authorization is required.';
  end if;
  if p_beacon_output_value is null
    or char_length(p_beacon_output_value) not between 64 and 512
    or mod(char_length(p_beacon_output_value), 2) <> 0
    or p_beacon_output_value !~ '^[a-fA-F0-9]+$'
    or nullif(trim(coalesce(p_beacon_pulse_index, '')), '') is null
    or p_beacon_timestamp is null
    or p_beacon_pulse_uri is null
    or p_beacon_pulse_uri !~ '^https://beacon[.]nist[.]gov/' then
    raise exception using errcode = '22023', message = 'The public randomness pulse is incomplete or invalid.';
  end if;

  select * into v_draw from public.draws where id = p_draw_id for update;
  if not found then
    raise exception using errcode = 'P0002', message = 'Draw not found.';
  end if;
  if v_draw.status = 'completed' then
    select jsonb_agg(jsonb_build_object(
      'tier_order', prizes.tier_order,
      'prize_name', prizes.prize_name,
      'prize_amount', prizes.prize_amount,
      'ticket_number', prizes.winner_ticket_number,
      'winner_name', prizes.winner_public_name
    ) order by prizes.tier_order) into v_existing_winners
    from public.draw_prizes as prizes where prizes.draw_id = v_draw.id;
    return jsonb_build_object('status', 'completed', 'draw_id', v_draw.id, 'winners', coalesce(v_existing_winners, '[]'::jsonb));
  end if;
  if v_draw.closed_at is null or p_beacon_timestamp < v_draw.closed_at then
    raise exception using errcode = '22023', message = 'The NIST pulse must be generated after the draw closes. Try again after the next pulse.';
  end if;
  if v_draw.reserved_tickets > 0 then
    raise exception using errcode = '22023', message = 'Review or reject pending payment requests before drawing a result.';
  end if;
  if v_draw.end_condition = 'sold_out' and v_draw.sold_tickets <> v_draw.total_tickets then
    raise exception using errcode = '22023', message = 'The draw must be sold out before publishing its result.';
  end if;

  select server_seed into v_seed
  from private.draw_seed_vault
  where draw_id = v_draw.id and revealed_at is null
  for update;
  if not found or encode(extensions.digest(v_seed, 'sha256'), 'hex') <> v_draw.server_seed_commitment then
    raise exception using errcode = 'P0001', message = 'The draw seed commitment could not be verified.';
  end if;

  select count(*)::integer into v_ticket_count
  from public.user_tickets as tickets
  where tickets.draw_id = v_draw.id and tickets.status = 'active';
  if v_ticket_count < 1 or v_ticket_count <> v_draw.sold_tickets then
    raise exception using errcode = 'P0001', message = 'The sold ticket ledger does not match the draw counter.';
  end if;
  if not exists (select 1 from public.draw_prizes where draw_id = v_draw.id) then
    insert into public.draw_prizes (draw_id, tier_order, prize_name, prize_description, prize_amount)
    values (v_draw.id, 1, 'Grand prize', coalesce(v_draw.prize_details, ''), 0);
  end if;

  select count(*)::bigint into v_paid_tickets
  from public.user_tickets as tickets
  where tickets.draw_id = v_draw.id and tickets.source in ('paid', 'manual_payment');
  select round(coalesce(sum(prizes.prize_amount), 0), 2) into v_prize_total
  from public.draw_prizes as prizes where prizes.draw_id = v_draw.id;

  v_gross_revenue := round(v_paid_tickets::numeric * v_draw.ticket_price, 2);
  v_prize_pool := round(v_gross_revenue * (100 - v_draw.profit_margin) / 100, 2);
  if v_prize_total > v_prize_pool then
    raise exception using errcode = '22023', message = 'Configured prize amounts exceed the realized prize pool.';
  end if;
  if (select count(*) from public.draw_prizes where draw_id = v_draw.id) > v_ticket_count then
    raise exception using errcode = '22023', message = 'There are more prize tiers than sold tickets.';
  end if;

  v_limit := v_universe - mod(v_universe, v_ticket_count::numeric);
  for v_prize in
    select prizes.id, prizes.tier_order, prizes.prize_name, prizes.prize_amount
    from public.draw_prizes as prizes
    where prizes.draw_id = v_draw.id
    order by prizes.tier_order
  loop
    v_counter := 0;
    loop
      if v_counter > 10000 then
        raise exception using errcode = 'P0001', message = 'The unbiased winner selection could not complete.';
      end if;
      v_hash := extensions.digest(
        v_seed || ':' || lower(p_beacon_output_value) || ':' || v_draw.id::text || ':' || v_prize.tier_order::text || ':' || v_counter::text,
        'sha256'
      );
      v_random_value := 0;
      for v_i in 0..31 loop
        v_random_value := v_random_value * 256 + get_byte(v_hash, v_i);
      end loop;
      if v_random_value < v_limit then
        v_candidate := mod(v_random_value, v_ticket_count::numeric)::integer + 1;
        if not (v_candidate = any(v_winner_numbers)) then
          exit;
        end if;
      end if;
      v_counter := v_counter + 1;
    end loop;

    select tickets.user_id, profiles.name into v_winner_user_id, v_winner_name
    from public.user_tickets as tickets
    join public.profiles as profiles on profiles.id = tickets.user_id
    where tickets.draw_id = v_draw.id
      and tickets.ticket_number = v_candidate
      and tickets.status = 'active'
    for update of tickets;
    if not found then
      raise exception using errcode = 'P0001', message = 'A selected ticket is not present in the sold ticket ledger.';
    end if;

    v_masked_name := left(trim(coalesce(v_winner_name, 'Member')), 1) || '•••';
    update public.draw_prizes
    set winner_ticket_number = v_candidate,
        winner_user_id = v_winner_user_id,
        winner_public_name = v_masked_name
    where id = v_prize.id;

    if v_prize.prize_amount > 0 then
      update public.profiles set balance = balance + v_prize.prize_amount where id = v_winner_user_id;
      if not found then
        raise exception using errcode = 'P0002', message = 'The prize winner wallet could not be credited.';
      end if;
      insert into public.wallet_transactions (user_id, amount, transaction_type, reference_id, description)
      values (
        v_winner_user_id,
        v_prize.prize_amount,
        'draw_prize',
        v_draw.id,
        left('Prize award · Draw ' || v_draw.draw_number::text || ' · ' || v_prize.prize_name, 240)
      );
    end if;

    v_winner_numbers := array_append(v_winner_numbers, v_candidate);
    v_winners := v_winners || jsonb_build_array(jsonb_build_object(
      'tier_order', v_prize.tier_order,
      'prize_name', v_prize.prize_name,
      'prize_amount', v_prize.prize_amount,
      'ticket_number', v_candidate,
      'winner_name', v_masked_name
    ));
  end loop;

  update public.user_tickets
  set status = 'lost'
  where draw_id = v_draw.id and status = 'active';
  update public.user_tickets
  set status = 'won'
  where draw_id = v_draw.id and ticket_number = any(v_winner_numbers);

  update public.draws
  set status = 'completed',
      winner_ticket = v_winner_numbers[1],
      winner_user_id = (select prizes.winner_user_id from public.draw_prizes as prizes where prizes.draw_id = v_draw.id order by prizes.tier_order limit 1),
      completed_at = now(),
      revealed_server_seed = v_seed,
      beacon_source = 'NIST Randomness Beacon 2.0',
      beacon_pulse_index = p_beacon_pulse_index,
      beacon_timestamp = p_beacon_timestamp,
      beacon_output_value = lower(p_beacon_output_value),
      beacon_pulse_uri = p_beacon_pulse_uri,
      beacon_signature = p_beacon_signature,
      draw_algorithm = 'sha256-rejection-v1',
      updated_at = now()
  where id = v_draw.id;

  update private.draw_seed_vault set revealed_at = now() where draw_id = v_draw.id;
  perform private.write_admin_audit(
    'draw_result_published', 'draw', v_draw.id,
    jsonb_build_object(
      'draw_number', v_draw.draw_number,
      'beacon_pulse_index', p_beacon_pulse_index,
      'beacon_timestamp', p_beacon_timestamp,
      'server_seed_commitment', v_draw.server_seed_commitment,
      'winners', v_winners,
      'gross_revenue', v_gross_revenue,
      'prize_pool', v_prize_pool
    )
  );

  return jsonb_build_object(
    'status', 'completed',
    'draw_id', v_draw.id,
    'server_seed_commitment', v_draw.server_seed_commitment,
    'revealed_server_seed', v_seed,
    'beacon_pulse_index', p_beacon_pulse_index,
    'beacon_timestamp', p_beacon_timestamp,
    'winners', v_winners
  );
end;
$$;
revoke all on function public.finalize_draw_with_beacon(uuid, text, text, timestamptz, text, text) from public, anon, authenticated;
grant execute on function public.finalize_draw_with_beacon(uuid, text, text, timestamptz, text, text) to service_role;

create or replace function private.prevent_wallet_ledger_mutation()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  raise exception using errcode = '42501', message = 'Wallet ledger entries are immutable.';
end;
$$;
revoke all on function private.prevent_wallet_ledger_mutation() from public, anon, authenticated;
drop trigger if exists prevent_wallet_ledger_mutation on public.wallet_transactions;
create trigger prevent_wallet_ledger_mutation
  before update or delete on public.wallet_transactions
  for each row execute function private.prevent_wallet_ledger_mutation();

create or replace view public.draw_prizes_public as
select
  prizes.draw_id,
  prizes.tier_order,
  prizes.prize_name,
  prizes.prize_description,
  prizes.prize_amount,
  prizes.winner_ticket_number,
  prizes.winner_public_name
from public.draw_prizes as prizes
join public.draws as draws on draws.id = prizes.draw_id
where draws.status in ('active', 'completed');
revoke all on public.draw_prizes_public from public;
grant select on public.draw_prizes_public to anon, authenticated;

create or replace view public.draw_verification_public as
select
  draws.id,
  draws.draw_number,
  draws.title,
  draws.prize_details,
  draws.status,
  draws.end_condition,
  draws.sold_tickets,
  draws.total_tickets,
  draws.profit_margin,
  draws.winner_ticket,
  left(trim(coalesce(profiles.name, 'Member')), 1) || '•••' as winner_name,
  draws.server_seed_commitment,
  draws.revealed_server_seed,
  draws.closed_at,
  draws.beacon_source,
  draws.beacon_pulse_index,
  draws.beacon_timestamp,
  draws.beacon_output_value,
  draws.beacon_pulse_uri,
  draws.beacon_signature,
  draws.draw_algorithm,
  draws.created_at,
  draws.completed_at
from public.draws as draws
left join public.profiles as profiles on profiles.id = draws.winner_user_id
where draws.status = 'completed';
revoke all on public.draw_verification_public from public;
grant select on public.draw_verification_public to anon, authenticated;

create or replace view public.winner_history as
select
  draws.id,
  draws.draw_number,
  draws.title,
  draws.prize_details,
  draws.winner_ticket,
  left(trim(coalesce(profiles.name, 'Member')), 1) || '•••' as winner_name,
  draws.completed_at
from public.draws as draws
left join public.profiles as profiles on profiles.id = draws.winner_user_id
where draws.status = 'completed';
revoke all on public.winner_history from public;
grant select on public.winner_history to anon, authenticated;

drop policy if exists draws_read_public on public.draws;
create policy draws_read_public
  on public.draws for select to anon, authenticated
  using (
    public.is_admin()
    or status = 'completed'
    or (status = 'active' and (closed_at is null or end_condition = 'sold_out'))
  );

revoke select on public.draws from anon, authenticated;
revoke select (winner_user_id) on public.draws from anon, authenticated;
grant select (
  id, draw_number, title, description, prize_details, ticket_price,
  total_tickets, sold_tickets, reserved_tickets, profit_margin,
  banner_image_url, status, winner_ticket, created_at, updated_at,
  completed_at, end_condition, closed_at, server_seed_commitment,
  revealed_server_seed, beacon_source, beacon_pulse_index,
  beacon_timestamp, beacon_output_value, beacon_pulse_uri,
  beacon_signature, draw_algorithm
) on public.draws to anon, authenticated;

revoke update on public.profiles from authenticated;
revoke update (phone, email_verified_at, phone_verified_at, age_confirmed_at, terms_accepted_at, terms_version, account_status, suspended_at, suspension_reason, balance, referral_count, free_tickets_earned, referral_qualified_at) on public.profiles from authenticated;
grant update (name) on public.profiles to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'ticket-payment-proofs',
  'ticket-payment-proofs',
  false,
  5242880,
  array['image/jpeg', 'image/png', 'image/webp', 'application/pdf']
)
on conflict (id) do update set
  public = false,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

create table if not exists private.ticket_proof_upload_events (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);
alter table private.ticket_proof_upload_events enable row level security;
revoke all on private.ticket_proof_upload_events from public, anon, authenticated;
create index if not exists ticket_proof_upload_events_user_created_idx
  on private.ticket_proof_upload_events (user_id, created_at desc);

create or replace function public.can_upload_ticket_payment_proof()
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := (select auth.uid());
  v_recent_count bigint;
begin
  if v_user_id is null then
    return false;
  end if;

  perform pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(v_user_id::text || ':ticket-proof-upload', 0));
  delete from private.ticket_proof_upload_events as events
  where events.user_id = v_user_id
    and events.created_at <= now() - interval '1 day';

  select count(*) into v_recent_count
  from private.ticket_proof_upload_events as events
  where events.user_id = v_user_id
    and events.created_at > now() - interval '1 hour';

  if v_recent_count >= 10 then
    return false;
  end if;

  insert into private.ticket_proof_upload_events (user_id) values (v_user_id);
  return true;
end;
$$;
revoke all on function public.can_upload_ticket_payment_proof() from public, anon;
grant execute on function public.can_upload_ticket_payment_proof() to authenticated;

drop policy if exists ticket_payment_proofs_upload_own on storage.objects;
create policy ticket_payment_proofs_upload_own
  on storage.objects for insert to authenticated
  with check (
    bucket_id = 'ticket-payment-proofs'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and public.can_upload_ticket_payment_proof()
  );
drop policy if exists ticket_payment_proofs_read_own_or_admin on storage.objects;
create policy ticket_payment_proofs_read_own_or_admin
  on storage.objects for select to authenticated
  using (
    bucket_id = 'ticket-payment-proofs'
    and ((storage.foldername(name))[1] = (select auth.uid())::text or public.is_admin())
  );
drop policy if exists ticket_payment_proofs_delete_own_pending on storage.objects;
create policy ticket_payment_proofs_delete_own_pending
  on storage.objects for delete to authenticated
  using (
    bucket_id = 'ticket-payment-proofs'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and not exists (
      select 1 from public.ticket_payment_submissions as submissions
      where submissions.proof_path = name
    )
  );
