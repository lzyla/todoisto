-- Todoisto: token odświeżania Google Health (Fitbit). Idempotentne; uruchamiać PO 20261103000002.
-- Token to sekret: RLS włączone i ZERO polityk + brak uprawnień dla anon/authenticated.
-- Czyta/zapisuje go wyłącznie Edge Function `health-sync` (rola serwisowa, po weryfikacji sesji).
create table if not exists public.todoisto_health_tokens (
  user_id uuid primary key references auth.users(id) on delete cascade,
  refresh_token text not null,
  scope text,
  updated_at timestamptz not null default now()
);
alter table public.todoisto_health_tokens enable row level security;
alter table public.todoisto_health_tokens force row level security;
revoke all on public.todoisto_health_tokens from anon, authenticated;
grant select, insert, update, delete on public.todoisto_health_tokens to service_role;
