create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  name text not null default 'Player' check (char_length(name) between 1 and 80),
  phone text check (
    phone is null or (
      phone ~ '^[0-9+() .-]{7,24}$'
      and char_length(regexp_replace(phone, '[^0-9]', '', 'g')) between 7 and 15
    )
  ),
  balance numeric(12, 2) not null default 0 check (balance >= 0),
  referred_by uuid references public.profiles(id) on delete set null,
  referral_count integer not null default 0 check (referral_count >= 0),
  free_tickets_earned integer not null default 0 check (free_tickets_earned >= 0),
  referral_qualified_at timestamptz,
  created_at timestamptz not null default now()
);

create table public.draws (
  id uuid primary key default gen_random_uuid(),
  draw_number integer not null unique check (draw_number between 1 and 50),
  title text not null check (char_length(trim(title)) between 1 and 100),
  description text not null default '' check (char_length(description) <= 4000),
  prize_details text not null default '' check (char_length(prize_details) <= 2000),
  ticket_price numeric(12, 2) not null check (ticket_price > 0),
  total_tickets integer not null check (total_tickets between 1 and 1000000),
  sold_tickets integer not null default 0 check (sold_tickets between 0 and total_tickets),
  profit_margin numeric(5, 2) not null default 0 check (profit_margin between 0 and 100),
  banner_image_url text,
  status text not null default 'active' check (status in ('active', 'completed')),
  winner_ticket integer,
  winner_user_id uuid references public.profiles(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  completed_at timestamptz,
  check (
    (status = 'active' and winner_ticket is null and winner_user_id is null and completed_at is null)
    or
    (status = 'completed' and winner_ticket is not null and winner_user_id is not null and completed_at is not null)
  )
);

create table public.user_tickets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  draw_id uuid not null references public.draws(id) on delete restrict,
  ticket_number integer not null check (ticket_number > 0),
  purchase_date timestamptz not null default now(),
  status text not null default 'active' check (status in ('active', 'won', 'lost')),
  source text not null default 'paid' check (source in ('paid', 'referral')),
  unique (draw_id, ticket_number)
);

create index user_tickets_user_purchase_idx on public.user_tickets (user_id, purchase_date desc);
create index user_tickets_draw_status_idx on public.user_tickets (draw_id, status);

create table public.site_content (
  id uuid primary key default gen_random_uuid(),
  section_name text not null check (section_name in ('hero_banner', 'guidelines', 'trust', 'videos')),
  content_text text not null default '' check (char_length(content_text) <= 10000),
  media_url text,
  display_order integer not null default 0,
  updated_at timestamptz not null default now()
);

create index site_content_section_order_idx on public.site_content (section_name, display_order, updated_at desc);

create table public.wallet_transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  amount numeric(12, 2) not null check (amount <> 0),
  transaction_type text not null check (transaction_type in ('ticket_purchase', 'admin_credit', 'admin_debit')),
  reference_id uuid,
  description text not null default '' check (char_length(description) <= 240),
  created_at timestamptz not null default now()
);

create index wallet_transactions_user_created_idx on public.wallet_transactions (user_id, created_at desc);

create table public.free_ticket_claims (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  draw_id uuid not null references public.draws(id) on delete restrict,
  claim_number integer not null check (claim_number > 0),
  user_ticket_id uuid not null unique references public.user_tickets(id) on delete restrict,
  claimed_at timestamptz not null default now(),
  unique (user_id, claim_number)
);

create table public.admin_users (
  user_id uuid primary key references auth.users(id) on delete cascade,
  assigned_at timestamptz not null default now()
);

create table public.admin_invites (
  email text primary key check (email = lower(trim(email))),
  created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.draws enable row level security;
alter table public.user_tickets enable row level security;
alter table public.site_content enable row level security;
alter table public.wallet_transactions enable row level security;
alter table public.free_ticket_claims enable row level security;
alter table public.admin_users enable row level security;
alter table public.admin_invites enable row level security;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.admin_users as admins
    where admins.user_id = auth.uid()
  );
$$;

create or replace function public.count_verified_referral(p_user_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_referrer_id uuid;
begin
  update public.profiles as referred
  set referral_qualified_at = now()
  where referred.id = p_user_id
    and referred.referred_by is not null
    and referred.referral_qualified_at is null
  returning referred.referred_by into v_referrer_id;

  if v_referrer_id is not null then
    update public.profiles as referrer
    set referral_count = referrer.referral_count + 1,
        free_tickets_earned = (referrer.referral_count + 1) / 10
    where referrer.id = v_referrer_id;
  end if;
end;
$$;

create or replace function public.handle_auth_user_created()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_referrer_id uuid;
  v_referrer_text text;
begin
  v_referrer_text := nullif(trim(new.raw_user_meta_data ->> 'referred_by'), '');

  if v_referrer_text ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' then
    v_referrer_id := v_referrer_text::uuid;
  end if;

  if v_referrer_id = new.id or not exists (
    select 1 from public.profiles as referrer where referrer.id = v_referrer_id
  ) then
    v_referrer_id := null;
  end if;

  insert into public.profiles (id, name, phone, referred_by)
  values (
    new.id,
    left(coalesce(nullif(trim(new.raw_user_meta_data ->> 'name'), ''), 'Player'), 80),
    nullif(trim(new.raw_user_meta_data ->> 'phone'), ''),
    v_referrer_id
  )
  on conflict (id) do nothing;

  if new.email_confirmed_at is not null then
    perform public.count_verified_referral(new.id);

    if new.email is not null and exists (
      select 1
      from public.admin_invites as invites
      where invites.email = lower(trim(new.email))
    ) then
      insert into public.admin_users (user_id)
      values (new.id)
      on conflict (user_id) do nothing;
    end if;
  end if;

  return new;
end;
$$;

create or replace function public.handle_auth_user_updated()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if old.email_confirmed_at is null and new.email_confirmed_at is not null then
    perform public.count_verified_referral(new.id);
  end if;

  if new.email_confirmed_at is not null and new.email is not null and exists (
    select 1
    from public.admin_invites as invites
    where invites.email = lower(trim(new.email))
  ) then
    insert into public.admin_users (user_id)
    values (new.id)
    on conflict (user_id) do nothing;
  end if;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_auth_user_created();

drop trigger if exists on_auth_user_updated on auth.users;
create trigger on_auth_user_updated
after update of email, email_confirmed_at on auth.users
for each row execute function public.handle_auth_user_updated();

create or replace function public.purchase_tickets(p_draw_id uuid, p_quantity integer)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_draw public.draws%rowtype;
  v_balance numeric(12, 2);
  v_amount numeric(12, 2);
  v_first_ticket integer;
  v_ticket_numbers integer[];
begin
  if v_user_id is null then
    raise exception using errcode = '28000', message = 'Sign in to buy tickets.';
  end if;

  if p_quantity is null or p_quantity < 1 or p_quantity > 5 then
    raise exception using errcode = '22023', message = 'Choose between 1 and 5 tickets.';
  end if;

  if not exists (
    select 1 from auth.users as users
    where users.id = v_user_id and users.email_confirmed_at is not null
  ) then
    raise exception using errcode = '42501', message = 'Confirm your email before buying tickets.';
  end if;

  select draws.* into v_draw
  from public.draws as draws
  where draws.id = p_draw_id
  for update;

  if not found or v_draw.status <> 'active' then
    raise exception using errcode = '55000', message = 'This draw is not open.';
  end if;

  if v_draw.sold_tickets + p_quantity > v_draw.total_tickets then
    raise exception using errcode = '55000', message = 'There are not enough tickets remaining.';
  end if;

  select profiles.balance into v_balance
  from public.profiles as profiles
  where profiles.id = v_user_id
  for update;

  if not found then
    raise exception using errcode = 'P0002', message = 'Profile not found.';
  end if;

  v_amount := v_draw.ticket_price * p_quantity;
  if v_balance < v_amount then
    raise exception using errcode = '22023', message = 'Your wallet balance is too low.';
  end if;

  update public.profiles as profiles
  set balance = profiles.balance - v_amount
  where profiles.id = v_user_id
  returning profiles.balance into v_balance;

  v_first_ticket := v_draw.sold_tickets + 1;
  select array_agg(v_first_ticket + series.number - 1 order by series.number)
  into v_ticket_numbers
  from generate_series(1, p_quantity) as series(number);

  insert into public.user_tickets (user_id, draw_id, ticket_number, source)
  select v_user_id, p_draw_id, v_first_ticket + series.number - 1, 'paid'
  from generate_series(1, p_quantity) as series(number);

  update public.draws as draws
  set sold_tickets = draws.sold_tickets + p_quantity,
      updated_at = now()
  where draws.id = p_draw_id;

  insert into public.wallet_transactions (user_id, amount, transaction_type, reference_id, description)
  values (v_user_id, -v_amount, 'ticket_purchase', p_draw_id, 'Ticket purchase');

  return jsonb_build_object(
    'ticket_numbers', v_ticket_numbers,
    'balance', v_balance,
    'amount', v_amount
  );
end;
$$;

create or replace function public.claim_referral_ticket(p_draw_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user_id uuid := auth.uid();
  v_draw public.draws%rowtype;
  v_profile public.profiles%rowtype;
  v_claimed_count integer;
  v_claim_number integer;
  v_ticket_number integer;
  v_ticket_id uuid;
begin
  if v_user_id is null then
    raise exception using errcode = '28000', message = 'Sign in to claim a free ticket.';
  end if;

  if not exists (
    select 1 from auth.users as users
    where users.id = v_user_id and users.email_confirmed_at is not null
  ) then
    raise exception using errcode = '42501', message = 'Confirm your email before claiming a free ticket.';
  end if;

  select draws.* into v_draw
  from public.draws as draws
  where draws.id = p_draw_id
  for update;

  if not found or v_draw.status <> 'active' then
    raise exception using errcode = '55000', message = 'Choose an open draw.';
  end if;

  if v_draw.sold_tickets >= v_draw.total_tickets then
    raise exception using errcode = '55000', message = 'This draw has no tickets remaining.';
  end if;

  select profiles.* into v_profile
  from public.profiles as profiles
  where profiles.id = v_user_id
  for update;

  if not found then
    raise exception using errcode = 'P0002', message = 'Profile not found.';
  end if;

  select count(*)::integer into v_claimed_count
  from public.free_ticket_claims as claims
  where claims.user_id = v_user_id;

  v_claim_number := v_claimed_count + 1;
  if v_profile.free_tickets_earned < v_claim_number then
    raise exception using errcode = '22023', message = 'Reach 10 verified referrals to unlock a free ticket.';
  end if;

  v_ticket_number := v_draw.sold_tickets + 1;
  insert into public.user_tickets (user_id, draw_id, ticket_number, source)
  values (v_user_id, p_draw_id, v_ticket_number, 'referral')
  returning id into v_ticket_id;

  insert into public.free_ticket_claims (user_id, draw_id, claim_number, user_ticket_id)
  values (v_user_id, p_draw_id, v_claim_number, v_ticket_id);

  update public.draws as draws
  set sold_tickets = draws.sold_tickets + 1,
      updated_at = now()
  where draws.id = p_draw_id;

  return jsonb_build_object(
    'ticket_number', v_ticket_number,
    'claim_number', v_claim_number
  );
end;
$$;

create or replace function public.admin_save_draw(
  p_draw_id uuid,
  p_draw_number integer,
  p_title text,
  p_description text,
  p_prize_details text,
  p_ticket_price numeric,
  p_total_tickets integer,
  p_profit_margin numeric,
  p_banner_image_url text
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_draw_id uuid;
  v_current_draw public.draws%rowtype;
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access required.';
  end if;

  if p_draw_number is null or p_draw_number not between 1 and 50
    or p_title is null or char_length(trim(p_title)) not between 1 and 100
    or char_length(coalesce(p_description, '')) > 4000
    or char_length(coalesce(p_prize_details, '')) > 2000
    or p_ticket_price is null or p_ticket_price <= 0
    or p_total_tickets is null or p_total_tickets not between 1 and 1000000
    or p_profit_margin is null or p_profit_margin not between 0 and 100
    or char_length(coalesce(p_banner_image_url, '')) > 2048 then
    raise exception using errcode = '22023', message = 'Check the draw details and try again.';
  end if;

  if p_draw_id is null then
    insert into public.draws (
      draw_number, title, description, prize_details, ticket_price,
      total_tickets, profit_margin, banner_image_url
    )
    values (
      p_draw_number, trim(p_title), coalesce(p_description, ''), coalesce(p_prize_details, ''),
      p_ticket_price, p_total_tickets, p_profit_margin, nullif(trim(coalesce(p_banner_image_url, '')), '')
    )
    returning id into v_draw_id;
  else
    select draws.* into v_current_draw
    from public.draws as draws
    where draws.id = p_draw_id
    for update;

    if not found or v_current_draw.status <> 'active' then
      raise exception using errcode = '55000', message = 'Only open draws can be edited.';
    end if;

    if p_total_tickets < v_current_draw.sold_tickets then
      raise exception using errcode = '22023', message = 'The ticket limit cannot be lower than tickets already sold.';
    end if;

    update public.draws as draws
    set draw_number = p_draw_number,
        title = trim(p_title),
        description = coalesce(p_description, ''),
        prize_details = coalesce(p_prize_details, ''),
        ticket_price = p_ticket_price,
        total_tickets = p_total_tickets,
        profit_margin = p_profit_margin,
        banner_image_url = nullif(trim(coalesce(p_banner_image_url, '')), ''),
        updated_at = now()
    where draws.id = p_draw_id
    returning id into v_draw_id;
  end if;

  return v_draw_id;
end;
$$;

create or replace function public.admin_complete_draw(p_draw_id uuid, p_winner_ticket integer default null)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_draw public.draws%rowtype;
  v_winner_ticket integer;
  v_winner_user_id uuid;
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access required.';
  end if;

  select draws.* into v_draw
  from public.draws as draws
  where draws.id = p_draw_id
  for update;

  if not found or v_draw.status <> 'active' then
    raise exception using errcode = '55000', message = 'Only open draws can be completed.';
  end if;

  if v_draw.sold_tickets = 0 then
    raise exception using errcode = '22023', message = 'A draw needs at least one sold ticket before a winner can be selected.';
  end if;

  if p_winner_ticket is null then
    select tickets.ticket_number, tickets.user_id
    into v_winner_ticket, v_winner_user_id
    from public.user_tickets as tickets
    where tickets.draw_id = p_draw_id and tickets.status = 'active'
    order by gen_random_uuid()
    limit 1;
  else
    select tickets.ticket_number, tickets.user_id
    into v_winner_ticket, v_winner_user_id
    from public.user_tickets as tickets
    where tickets.draw_id = p_draw_id
      and tickets.ticket_number = p_winner_ticket
      and tickets.status = 'active';
  end if;

  if v_winner_ticket is null or v_winner_user_id is null then
    raise exception using errcode = '22023', message = 'Choose a sold ticket from this draw.';
  end if;

  update public.user_tickets as tickets
  set status = case when tickets.ticket_number = v_winner_ticket then 'won' else 'lost' end
  where tickets.draw_id = p_draw_id and tickets.status = 'active';

  update public.draws as draws
  set status = 'completed',
      winner_ticket = v_winner_ticket,
      winner_user_id = v_winner_user_id,
      completed_at = now(),
      updated_at = now()
  where draws.id = p_draw_id;

  return jsonb_build_object(
    'winner_ticket', v_winner_ticket,
    'winner_user_id', v_winner_user_id
  );
end;
$$;

create or replace function public.admin_save_site_content(
  p_content_id uuid,
  p_section_name text,
  p_content_text text,
  p_media_url text,
  p_display_order integer
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_content_id uuid;
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access required.';
  end if;

  if p_section_name not in ('hero_banner', 'guidelines', 'trust', 'videos')
    or char_length(coalesce(p_content_text, '')) > 10000
    or char_length(coalesce(p_media_url, '')) > 2048
    or p_display_order is null then
    raise exception using errcode = '22023', message = 'Check the content details and try again.';
  end if;

  if p_content_id is null then
    insert into public.site_content (section_name, content_text, media_url, display_order, updated_at)
    values (
      p_section_name,
      coalesce(p_content_text, ''),
      nullif(trim(coalesce(p_media_url, '')), ''),
      p_display_order,
      now()
    )
    returning id into v_content_id;
  else
    update public.site_content as content
    set section_name = p_section_name,
        content_text = coalesce(p_content_text, ''),
        media_url = nullif(trim(coalesce(p_media_url, '')), ''),
        display_order = p_display_order,
        updated_at = now()
    where content.id = p_content_id
    returning id into v_content_id;

    if v_content_id is null then
      raise exception using errcode = 'P0002', message = 'Content item not found.';
    end if;
  end if;

  return v_content_id;
end;
$$;

create or replace function public.admin_delete_site_content(p_content_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access required.';
  end if;

  delete from public.site_content where id = p_content_id;
end;
$$;

create or replace function public.admin_adjust_wallet(p_user_id uuid, p_amount numeric, p_description text)
returns numeric
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_balance numeric(12, 2);
  v_transaction_type text;
  v_description text;
begin
  if not public.is_admin() then
    raise exception using errcode = '42501', message = 'Administrator access required.';
  end if;

  if p_amount is null or p_amount = 0 or abs(p_amount) > 10000000
    or char_length(coalesce(p_description, '')) > 240 then
    raise exception using errcode = '22023', message = 'Enter a valid adjustment amount and reason.';
  end if;

  select profiles.balance into v_balance
  from public.profiles as profiles
  where profiles.id = p_user_id
  for update;

  if not found then
    raise exception using errcode = 'P0002', message = 'User profile not found.';
  end if;

  if v_balance + p_amount < 0 then
    raise exception using errcode = '22023', message = 'The adjustment cannot make a wallet balance negative.';
  end if;

  update public.profiles as profiles
  set balance = profiles.balance + p_amount
  where profiles.id = p_user_id
  returning profiles.balance into v_balance;

  v_transaction_type := case when p_amount > 0 then 'admin_credit' else 'admin_debit' end;
  v_description := nullif(trim(coalesce(p_description, '')), '');

  insert into public.wallet_transactions (user_id, amount, transaction_type, description)
  values (p_user_id, p_amount, v_transaction_type, coalesce(v_description, 'Admin wallet adjustment'));

  return v_balance;
end;
$$;

create or replace view public.winner_history as
select
  draws.id,
  draws.draw_number,
  draws.title,
  draws.prize_details,
  draws.winner_ticket,
  profiles.name as winner_name,
  draws.completed_at
from public.draws as draws
join public.profiles as profiles on profiles.id = draws.winner_user_id
where draws.status = 'completed';

create policy profiles_read_own_or_admin
on public.profiles for select to authenticated
using (id = (select auth.uid()) or public.is_admin());

create policy profiles_update_own
on public.profiles for update to authenticated
using (id = (select auth.uid()))
with check (id = (select auth.uid()));

create policy draws_read_public
on public.draws for select to anon, authenticated
using (status in ('active', 'completed'));

create policy user_tickets_read_own_or_admin
on public.user_tickets for select to authenticated
using (user_id = (select auth.uid()) or public.is_admin());

create policy site_content_read_public
on public.site_content for select to anon, authenticated
using (true);

create policy wallet_transactions_read_own_or_admin
on public.wallet_transactions for select to authenticated
using (user_id = (select auth.uid()) or public.is_admin());

create policy free_ticket_claims_read_own_or_admin
on public.free_ticket_claims for select to authenticated
using (user_id = (select auth.uid()) or public.is_admin());

create policy admin_users_read_own_or_admin
on public.admin_users for select to authenticated
using (user_id = (select auth.uid()) or public.is_admin());

revoke all on public.profiles, public.draws, public.user_tickets, public.site_content,
  public.wallet_transactions, public.free_ticket_claims, public.admin_users, public.admin_invites
from anon, authenticated;

grant select on public.draws, public.site_content to anon, authenticated;
grant select on public.winner_history to anon, authenticated;
grant select on public.profiles, public.user_tickets, public.wallet_transactions,
  public.free_ticket_claims, public.admin_users to authenticated;
grant update (name, phone) on public.profiles to authenticated;

revoke all on function public.is_admin() from public, anon;
grant execute on function public.is_admin() to authenticated;
revoke all on function public.count_verified_referral(uuid) from public, anon, authenticated;
revoke all on function public.handle_auth_user_created() from public, anon, authenticated;
revoke all on function public.handle_auth_user_updated() from public, anon, authenticated;

revoke all on function public.purchase_tickets(uuid, integer) from public, anon;
grant execute on function public.purchase_tickets(uuid, integer) to authenticated;
revoke all on function public.claim_referral_ticket(uuid) from public, anon;
grant execute on function public.claim_referral_ticket(uuid) to authenticated;
revoke all on function public.admin_save_draw(uuid, integer, text, text, text, numeric, integer, numeric, text) from public, anon;
grant execute on function public.admin_save_draw(uuid, integer, text, text, text, numeric, integer, numeric, text) to authenticated;
revoke all on function public.admin_complete_draw(uuid, integer) from public, anon;
grant execute on function public.admin_complete_draw(uuid, integer) to authenticated;
revoke all on function public.admin_save_site_content(uuid, text, text, text, integer) from public, anon;
grant execute on function public.admin_save_site_content(uuid, text, text, text, integer) to authenticated;
revoke all on function public.admin_delete_site_content(uuid) from public, anon;
grant execute on function public.admin_delete_site_content(uuid) to authenticated;
revoke all on function public.admin_adjust_wallet(uuid, numeric, text) from public, anon;
grant execute on function public.admin_adjust_wallet(uuid, numeric, text) to authenticated;
