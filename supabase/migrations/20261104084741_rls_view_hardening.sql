grant usage on schema private to authenticated;

create or replace function private.is_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.admin_users as admins
    where admins.user_id = (select auth.uid())
  );
$$;
revoke all on function private.is_admin() from public, anon, authenticated;
grant execute on function private.is_admin() to authenticated;

create or replace function public.is_admin()
returns boolean
language sql
stable
security invoker
set search_path = ''
as $$
  select private.is_admin();
$$;
revoke all on function public.is_admin() from public, anon;
grant execute on function public.is_admin() to authenticated;

drop policy if exists payment_accounts_read_active_or_admin on public.payment_accounts;
drop policy if exists payment_accounts_read_active on public.payment_accounts;
drop policy if exists payment_accounts_read_admin on public.payment_accounts;
create policy payment_accounts_read_active
  on public.payment_accounts for select to anon, authenticated
  using (is_active);
create policy payment_accounts_read_admin
  on public.payment_accounts for select to authenticated
  using (public.is_admin());

drop policy if exists draws_read_public on public.draws;
drop policy if exists draws_read_admin on public.draws;
create policy draws_read_public
  on public.draws for select to anon, authenticated
  using (
    status = 'completed'
    or (status = 'active' and (closed_at is null or end_condition = 'sold_out'))
  );
create policy draws_read_admin
  on public.draws for select to authenticated
  using (public.is_admin());

revoke all on public.draw_prizes from public, anon, authenticated;
grant select (draw_id, tier_order, prize_name, prize_description, prize_amount, winner_ticket_number, winner_public_name)
  on public.draw_prizes to anon, authenticated;
drop policy if exists draw_prizes_read_public on public.draw_prizes;
create policy draw_prizes_read_public
  on public.draw_prizes for select to anon, authenticated
  using (
    exists (
      select 1
      from public.draws as draws
      where draws.id = draw_prizes.draw_id
        and (
          draws.status = 'completed'
          or (draws.status = 'active' and (draws.closed_at is null or draws.end_condition = 'sold_out'))
        )
    )
  );

create or replace view public.draw_prizes_public
with (security_invoker = true)
as
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
where draws.status = 'completed'
  or (draws.status = 'active' and (draws.closed_at is null or draws.end_condition = 'sold_out'));

create or replace view public.draw_verification_public
with (security_invoker = true)
as
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
  coalesce(prizes.winner_public_name, 'Member•••') as winner_name,
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
left join public.draw_prizes as prizes on prizes.draw_id = draws.id and prizes.tier_order = 1
where draws.status = 'completed';

create or replace view public.winner_history
with (security_invoker = true)
as
select
  draws.id,
  draws.draw_number,
  draws.title,
  draws.prize_details,
  draws.winner_ticket,
  coalesce(prizes.winner_public_name, 'Member•••') as winner_name,
  draws.completed_at
from public.draws as draws
left join public.draw_prizes as prizes on prizes.draw_id = draws.id and prizes.tier_order = 1
where draws.status = 'completed';

revoke all on public.draw_prizes_public from public;
grant select on public.draw_prizes_public to anon, authenticated;
revoke all on public.draw_verification_public from public;
grant select on public.draw_verification_public to anon, authenticated;
revoke all on public.winner_history from public;
grant select on public.winner_history to anon, authenticated;

create index if not exists admin_audit_logs_actor_user_id_idx
  on private.admin_audit_logs (actor_user_id) where actor_user_id is not null;
create index if not exists draw_prizes_winner_user_id_idx
  on public.draw_prizes (winner_user_id) where winner_user_id is not null;
create index if not exists draws_winner_user_id_idx
  on public.draws (winner_user_id) where winner_user_id is not null;
create index if not exists free_ticket_claims_draw_id_idx
  on public.free_ticket_claims (draw_id);
create index if not exists profiles_referred_by_idx
  on public.profiles (referred_by) where referred_by is not null;
create index if not exists ticket_payment_submissions_draw_id_idx
  on public.ticket_payment_submissions (draw_id);
create index if not exists ticket_payment_submissions_reviewed_by_idx
  on public.ticket_payment_submissions (reviewed_by) where reviewed_by is not null;

create or replace function private.can_upload_ticket_payment_proof()
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
revoke all on function private.can_upload_ticket_payment_proof() from public, anon, authenticated;
grant execute on function private.can_upload_ticket_payment_proof() to authenticated;

drop policy if exists ticket_payment_proofs_upload_own on storage.objects;
create policy ticket_payment_proofs_upload_own
  on storage.objects for insert to authenticated
  with check (
    bucket_id = 'ticket-payment-proofs'
    and (storage.foldername(name))[1] = (select auth.uid())::text
    and private.can_upload_ticket_payment_proof()
  );
revoke all on function public.can_upload_ticket_payment_proof() from public, anon, authenticated;
drop function if exists public.can_upload_ticket_payment_proof();
