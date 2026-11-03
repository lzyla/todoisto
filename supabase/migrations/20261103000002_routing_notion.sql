-- Todoisto: konfiguracja przekazywania zadań do Notion/Monday + rejestr przekazań.
-- Idempotentne. Uruchamiać PO 20261103000001. Sekrety (token Notion) NIE trafiają
-- do bazy — żyją w sekretach Edge Function (`supabase secrets set NOTION_TOKEN=…`).

-- Gdzie trafiają zadania z danego projektu Todoisto.
create table if not exists public.todoisto_destinations (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  project_id bigint not null,                      -- id projektu w Todoisto (Ids, < 2^53)
  kind text not null default 'notion' check (kind in ('notion', 'monday')),
  notion_database_id text,                         -- 32 znaki hex z linku do bazy
  status_property text,                            -- null = wykryj kolumnę typu status/select
  status_value text not null default 'To do',      -- kolumna kanbanu „do zrobienia"
  date_property text,                              -- null = pierwsza kolumna typu date
  id_property text not null default 'Todoisto ID', -- ukryta kolumna rich_text do deduplikacji
  enabled boolean not null default true,
  enabled_at timestamptz not null default now(),   -- przekazujemy tylko zadania utworzone później
  updated_at timestamptz not null default now(),
  unique (user_id, project_id),
  check (kind <> 'notion' or notion_database_id ~ '^[0-9a-f]{32}$')
);

alter table public.todoisto_destinations enable row level security;
alter table public.todoisto_destinations force row level security;
revoke all on public.todoisto_destinations from anon;
grant select, insert, update, delete on public.todoisto_destinations to authenticated;

drop policy if exists "own dest select" on public.todoisto_destinations;
drop policy if exists "own dest insert" on public.todoisto_destinations;
drop policy if exists "own dest update" on public.todoisto_destinations;
drop policy if exists "own dest delete" on public.todoisto_destinations;
create policy "own dest select" on public.todoisto_destinations for select to authenticated using (auth.uid() = user_id);
create policy "own dest insert" on public.todoisto_destinations for insert to authenticated with check (auth.uid() = user_id);
create policy "own dest update" on public.todoisto_destinations for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own dest delete" on public.todoisto_destinations for delete to authenticated using (auth.uid() = user_id);

-- Rejestr przekazań: idempotencja (to samo zadanie → ta sama strona w Notion).
create table if not exists public.todoisto_routed (
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  task_id text not null,                           -- id zadania jako tekst (bez utraty precyzji w JS)
  destination_id uuid not null references public.todoisto_destinations(id) on delete cascade,
  remote_id text not null,
  remote_url text,
  created_at timestamptz not null default now(),
  primary key (user_id, task_id, destination_id)
);

alter table public.todoisto_routed enable row level security;
alter table public.todoisto_routed force row level security;
revoke all on public.todoisto_routed from anon;
grant select, insert on public.todoisto_routed to authenticated;

drop policy if exists "own routed select" on public.todoisto_routed;
drop policy if exists "own routed insert" on public.todoisto_routed;
create policy "own routed select" on public.todoisto_routed for select to authenticated using (auth.uid() = user_id);
create policy "own routed insert" on public.todoisto_routed for insert to authenticated with check (auth.uid() = user_id);
-- Brak UPDATE/DELETE dla klienta: rejestr jest tylko dopisywany.
