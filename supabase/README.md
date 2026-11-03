# Supabase — wdrożenie (ręcznie, świadomie)

Nic z tego katalogu nie jest wgrywane automatycznie. Kolejność:

1. **Kopia**: Dashboard → Database → Backups (albo `pg_dump`) — przed każdą migracją.
2. **Migracje** (SQL Editor albo `supabase db push`), w kolejności nazw:
   - `migrations/20261103000001_backups_rls_and_history.sql` — RLS wymuszone, historia migawek (30 ostatnich).
   - `migrations/20261103000002_routing_notion.sql` — konfiguracja przekazywania i rejestr przekazań.
3. **Kontrola**: `verify_security.sql` (tylko odczyt) — RLS na każdej tabeli `todoisto_*`, brak uprawnień `anon`.
4. **Edge Function `route-task`** (Notion):
   ```bash
   supabase secrets set NOTION_TOKEN=secret_…        # token integracji wewnętrznej Notion
   supabase functions deploy route-task              # weryfikacja JWT włączona (domyślnie)
   ```
   W Notion: integracja z uprawnieniem „Insert content” + „Read content”; każdą bazę
   udostępnij integracji (••• → Connections) i dodaj kolumnę tekstową **Todoisto ID**.
5. **Test** (konto testowe / baza testowa w Notion): w aplikacji Ustawienia → Przekazywanie do Notion,
   przypisz projekt testowy, dodaj zadanie, sprawdź stronę w Notion i link w szczegółach zadania.

Testy logiki funkcji (bez sieci): `node --experimental-strip-types --test supabase/functions/route-task/notion.test.ts`.
Wersja API Notion jest przypięta w `functions/route-task/notion.ts` (`NOTION_VERSION`) — zmiana tylko z testami.
