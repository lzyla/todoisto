-- Todoisto: utwardzenie tabeli migawek + historia wersji.
-- Idempotentne: można uruchomić wielokrotnie (SQL Editor → Run).
--
-- Co robi:
--  1. Upewnia się, że tabela istnieje i ma włączone (i WYMUSZONE) RLS.
--  2. Odtwarza polityki tylko dla roli `authenticated` (anon nie ma dostępu wcale).
--  3. Dodaje `updated_at` i historię poprzednich migawek (ostatnie 30 na użytkownika),
--     żeby nadpisanie bloba przez błędny sync dało się cofnąć.

create table if not exists public.todoisto_backups (
  user_id uuid primary key references auth.users(id) on delete cascade,
  data jsonb not null,
  updated_at timestamptz not null default now()
);

alter table public.todoisto_backups
  add column if not exists updated_at timestamptz not null default now();

alter table public.todoisto_backups enable row level security;
alter table public.todoisto_backups force row level security;

revoke all on public.todoisto_backups from anon;
grant select, insert, update on public.todoisto_backups to authenticated;

drop policy if exists "own select" on public.todoisto_backups;
drop policy if exists "own insert" on public.todoisto_backups;
drop policy if exists "own update" on public.todoisto_backups;
drop policy if exists "own delete" on public.todoisto_backups;

create policy "own select" on public.todoisto_backups
  for select to authenticated using (auth.uid() = user_id);
create policy "own insert" on public.todoisto_backups
  for insert to authenticated with check (auth.uid() = user_id);
create policy "own update" on public.todoisto_backups
  for update to authenticated using (auth.uid() = user_id) with check (auth.uid() = user_id);
-- Celowo brak polityki DELETE: klient nie może skasować swojej migawki.

-- ---------------------------------------------------------------------------
-- Historia migawek: każda aktualizacja odkłada poprzednią wersję.
-- ---------------------------------------------------------------------------
create table if not exists public.todoisto_backup_history (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  data jsonb not null,
  saved_at timestamptz not null default now()
);
create index if not exists todoisto_backup_history_user_idx
  on public.todoisto_backup_history (user_id, saved_at desc);

alter table public.todoisto_backup_history enable row level security;
-- Bez FORCE: trigger (security definer, właściciel tabeli) musi móc tu pisać.
-- Klient i tak nie ma uprawnień INSERT/UPDATE/DELETE.
revoke all on public.todoisto_backup_history from anon;
grant select on public.todoisto_backup_history to authenticated;

drop policy if exists "own history select" on public.todoisto_backup_history;
create policy "own history select" on public.todoisto_backup_history
  for select to authenticated using (auth.uid() = user_id);
-- Zapis do historii robi wyłącznie trigger (security definer), nie klient.

create or replace function public.todoisto_backups_archive()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  new.updated_at := now();
  if tg_op = 'UPDATE' and old.data is distinct from new.data then
    insert into public.todoisto_backup_history (user_id, data) values (old.user_id, old.data);
    delete from public.todoisto_backup_history h
     where h.user_id = old.user_id
       and h.id not in (
         select id from public.todoisto_backup_history
          where user_id = old.user_id
          order by saved_at desc
          limit 30
       );
  end if;
  return new;
end;
$$;

revoke all on function public.todoisto_backups_archive() from public, anon, authenticated;

drop trigger if exists todoisto_backups_archive on public.todoisto_backups;
create trigger todoisto_backups_archive
  before insert or update on public.todoisto_backups
  for each row execute function public.todoisto_backups_archive();
