# Todoisto na Macu — start w 5 krokach

## A. Uruchom aplikację na Macu
1. Zainstaluj **JDK 17** (raz): otwórz Terminal i wklej
   `brew install --cask temurin@17`
   (jeśli nie masz Homebrew: https://brew.sh).
2. Pobierz kod: w Terminalu
   `git clone -b claude/android-todoist-app-ea5o7s https://github.com/lzyla/todoisto.git`
3. W Finderze wejdź do folderu `todoisto` i **kliknij dwukrotnie `Todoisto.command`**.
   - Pierwsze uruchomienie: macOS może zapytać o pozwolenie („nie można otworzyć,
     bo pochodzi od niezidentyfikowanego dewelopera") → **kliknij prawym → Otwórz → Otwórz**.
   - Okno aplikacji pojawi się po chwili (za pierwszym razem pobiera biblioteki).

## B. Załóż darmową chmurę (raz, wspólna dla telefonu i Maca)
Bez tego urządzenia się nie zsynchronizują.
1. Wejdź na **supabase.com** → **New project** (zapamiętaj hasło do bazy).
2. **SQL Editor → New query** → wklej i **Run**:
   ```sql
   create table if not exists public.todoisto_backups (
     user_id uuid primary key references auth.users(id) on delete cascade,
     data jsonb not null,
     updated_at timestamptz default now()
   );
   alter table public.todoisto_backups enable row level security;
   create policy "own select" on public.todoisto_backups
     for select using (auth.uid() = user_id);
   create policy "own insert" on public.todoisto_backups
     for insert with check (auth.uid() = user_id);
   create policy "own update" on public.todoisto_backups
     for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
   ```
3. (Ułatwienie) **Authentication → Providers → Email** → wyłącz **„Confirm email"** → Save.
4. **Project Settings → API** → skopiuj **Project URL** i klucz **anon public**.

## C. Połącz oba urządzenia
- **Telefon** (aktualizacja do v116 najpierw): Konto → Chmura → wklej URL + anon →
  Zapisz → zarejestruj/zaloguj e-mailem i hasłem.
- **Mac**: ⚙ Ustawienia → wpisz **te same** URL, anon, **ten sam e-mail i hasło** →
  Synchronizuj.

Gotowe — zadania latają Mac ↔ telefon (dodania, edycje, usunięcia, automatycznie w tle).

---
Pełne szczegóły i budowanie `.dmg`: patrz `DESKTOP.md`.
