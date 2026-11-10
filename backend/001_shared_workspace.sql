-- Objekt All-In Services: gemeinsamer Arbeitsraum fuer genau autorisierte Benutzer.
-- Ausfuehren im SQL Editor eines EIGENEN Supabase-Projekts.
-- Keine Passwoerter oder service_role Keys in Apps eintragen.

create extension if not exists pgcrypto;
create schema if not exists private;

create table if not exists public.company_members (
  user_id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null check (length(trim(display_name)) between 2 and 100),
  role text not null check (role in ('admin','manager')),
  active boolean not null default true,
  created_at timestamptz not null default now()
);

-- SECURITY DEFINER vermeidet zirkulaere RLS-Policies bei der Mitgliedschaftspruefung.
create or replace function private.is_member() returns boolean
language sql stable security definer set search_path = ''
as $$
  select exists (
    select 1 from public.company_members m
    where m.user_id = (select auth.uid()) and m.active = true
  );
$$;

create or replace function private.is_admin() returns boolean
language sql stable security definer set search_path = ''
as $$
  select exists (
    select 1 from public.company_members m
    where m.user_id = (select auth.uid()) and m.active = true and m.role = 'admin'
  );
$$;

revoke all on schema private from public;
grant usage on schema private to authenticated;
grant execute on function private.is_member() to authenticated;
grant execute on function private.is_admin() to authenticated;
revoke all on function private.is_member() from anon;
revoke all on function private.is_admin() from anon;

create table if not exists public.records (
  id uuid primary key default gen_random_uuid(),
  kind text not null check (kind in ('Auftrag','Aufgabe','Termin','Kunde','Angebot','Rechnung','Einnahme','Fahrzeug','Gerät')),
  title text not null check (length(trim(title)) between 1 and 240),
  details text not null default '',
  customer text not null default '',
  address text not null default '',
  phone text not null default '',
  status text not null default 'Offen',
  amount_cents bigint not null default 0,
  paid_cents bigint not null default 0,
  due_date date,
  assigned_to uuid references public.company_members(user_id) on delete set null,
  created_by uuid not null references auth.users(id),
  updated_by uuid not null references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists records_kind_idx on public.records(kind);
create index if not exists records_updated_at_idx on public.records(updated_at desc);
create index if not exists records_assigned_to_idx on public.records(assigned_to);

create table if not exists public.audit_events (
  id bigint generated always as identity primary key,
  record_id uuid not null,
  record_title text not null,
  record_kind text not null,
  actor_id uuid not null references auth.users(id),
  action text not null check(action in ('created','updated','deleted')),
  old_data jsonb,
  new_data jsonb,
  changed_at timestamptz not null default now()
);
create index if not exists audit_events_changed_idx on public.audit_events(changed_at desc);
create index if not exists audit_events_record_idx on public.audit_events(record_id, changed_at desc);

create or replace function private.stamp_and_audit_record() returns trigger
language plpgsql security definer set search_path = ''
as $$
declare
  actor uuid := auth.uid();
begin
  if actor is null then
    raise exception 'Benutzeranmeldung erforderlich';
  end if;
  if tg_op = 'INSERT' then
    new.created_by := actor;
    new.updated_by := actor;
    new.created_at := now();
    new.updated_at := now();
    insert into public.audit_events(record_id,record_title,record_kind,actor_id,action,old_data,new_data)
      values (new.id,new.title,new.kind,actor,'created',null,to_jsonb(new));
    return new;
  elsif tg_op = 'UPDATE' then
    new.created_by := old.created_by;
    new.created_at := old.created_at;
    new.updated_by := actor;
    new.updated_at := now();
    if to_jsonb(new) - 'updated_at' - 'updated_by' is distinct from to_jsonb(old) - 'updated_at' - 'updated_by' then
      insert into public.audit_events(record_id,record_title,record_kind,actor_id,action,old_data,new_data)
        values (new.id,new.title,new.kind,actor,'updated',to_jsonb(old),to_jsonb(new));
    end if;
    return new;
  else
    insert into public.audit_events(record_id,record_title,record_kind,actor_id,action,old_data,new_data)
      values (old.id,old.title,old.kind,actor,'deleted',to_jsonb(old),null);
    return old;
  end if;
end;
$$;

-- BEFORE-Trigger loggt nach erfolgreichem Statement in derselben Transaktion.
drop trigger if exists records_audit_trigger on public.records;
create trigger records_audit_trigger
before insert or update or delete on public.records
for each row execute function private.stamp_and_audit_record();

alter table public.company_members enable row level security;
alter table public.records enable row level security;
alter table public.audit_events enable row level security;

-- Auch bei versehentlich aktiviertem Public-API-Zugriff nicht anonym lesen.
revoke all on public.company_members, public.records, public.audit_events from anon;
grant select on public.company_members to authenticated;
grant select,insert,update,delete on public.records to authenticated;
grant select on public.audit_events to authenticated;

-- Mitgliedschaften werden AUSSCHLIESSLICH von den Projektadmins im SQL Editor verwaltet.
-- Keine Policies fuer insert/update/delete von public.company_members.
drop policy if exists company_members_read on public.company_members;
create policy company_members_read on public.company_members for select to authenticated
using ((select private.is_member()));

drop policy if exists records_read on public.records;
create policy records_read on public.records for select to authenticated
using ((select private.is_member()));
drop policy if exists records_insert on public.records;
create policy records_insert on public.records for insert to authenticated
with check ((select private.is_member()));
drop policy if exists records_update on public.records;
create policy records_update on public.records for update to authenticated
using ((select private.is_member())) with check ((select private.is_member()));
drop policy if exists records_delete on public.records;
create policy records_delete on public.records for delete to authenticated
using ((select private.is_admin()));

drop policy if exists audit_events_read on public.audit_events;
create policy audit_events_read on public.audit_events for select to authenticated
using ((select private.is_member()));
-- KEINE Schreibpolicies fuer audit_events. Der SECURITY-DEFINER-Trigger schreibt manipulationsgeschuetzt.

comment on table public.audit_events is 'Unveraenderliche Protokolle: wer wann welchen Datensatz erstellte, veraenderte oder loeschte';

-- Native Android HTTP-Verbindungen unterstuetzen PATCH nicht zuverlaessig.
-- Daher gibt es eine explizite POST-RPC-Aktualisierung; SECURITY INVOKER respektiert die RLS-Policies.
create or replace function public.update_record(p_id uuid, p_values jsonb)
returns setof public.records
language plpgsql security invoker set search_path = ''
as $$
begin
  return query
    update public.records as r set
      kind = coalesce(p_values->>'kind', r.kind),
      title = coalesce(p_values->>'title', r.title),
      details = coalesce(p_values->>'details', r.details),
      customer = coalesce(p_values->>'customer', r.customer),
      address = coalesce(p_values->>'address', r.address),
      phone = coalesce(p_values->>'phone', r.phone),
      status = coalesce(p_values->>'status', r.status),
      amount_cents = coalesce((p_values->>'amount_cents')::bigint, r.amount_cents),
      paid_cents = coalesce((p_values->>'paid_cents')::bigint, r.paid_cents),
      due_date = case when p_values ? 'due_date' then nullif(p_values->>'due_date','')::date else r.due_date end,
      assigned_to = case when p_values ? 'assigned_to' then nullif(p_values->>'assigned_to','')::uuid else r.assigned_to end
    where r.id = p_id
    returning r.*;
end;
$$;
revoke all on function public.update_record(uuid,jsonb) from public, anon;
grant execute on function public.update_record(uuid,jsonb) to authenticated;
