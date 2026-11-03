# Integracje — konfiguracja

Zakres na teraz: **tylko Notion**. Monday — poza zakresem (nie konfigurować).

## Vocative → Notion

| Todoisto | Notion |
|---|---|
| Projekt **Vocative** | Baza **📋 Vocative do ogarnięcia moje** (kanban, widok „Kanban Board” grupowany po Status) |
| Link do wklejenia w aplikacji | `https://app.notion.com/p/2f0c0eb791dd80f284bffed6d28485f5` |
| id bazy | `2f0c0eb791dd80f284bffed6d28485f5` (źródło danych: `2f0c0eb7-91dd-81b9-b6c2-000b911542d6`) |

Mapowanie pól (Edge Function `route-task`, wykrywane automatycznie ze schematu):

| Zadanie Todoisto | Kolumna Notion | Uwagi |
|---|---|---|
| tytuł | **Feature** (title) | |
| — | **Status** = **To Do** | ustawienie „To do” w aplikacji pasuje bez względu na wielkość liter |
| termin (dueDate, godzina) | **Target Date** | z godziną jako czas Europe/Warsaw |
| priorytet P1 / P2 / P3 | **Priority** = High / Medium / Low | P4 (brak priorytetu) → pusto |
| notatki | treść strony | ślad `activity:<id>` pomijany |
| id zadania | **Todoisto ID** (tekst) | techniczna, do deduplikacji — dodana 3.11.2026; nie usuwać ani nie edytować |
| — | Sprint, Summary, Text | nie są wypełniane |

Kierunek jednokierunkowy: zmiany statusu w Notion nie wracają do Todoisto (link do strony jest w szczegółach zadania).

## Uruchomienie (jednorazowo)

1. **Notion → integracja**: notion.so/profile/integrations → *New integration* (Internal), uprawnienia: Read content, Insert content. Skopiuj token (`ntn_…` / `secret_…`).
2. **Udostępnij bazę integracji**: otwórz bazę → `•••` → *Connections* → dodaj integrację. Bez tego Notion odpowiada 404.
3. **Supabase** (wg `supabase/README.md`): kopia → migracje `…000001`, `…000002` → `verify_security.sql` →
   `supabase secrets set NOTION_TOKEN=…` → `supabase functions deploy route-task`.
4. **Aplikacja** (v120+): Ustawienia → *Przekazywanie do Notion* → **Vocative** → wklej link, status `To do` (kolumny zostaw puste — wykrywane) → Zapisz.
5. **Test**: dodaj w Vocative zadanie „TEST Todoisto — usuń” z terminem i P2 → po chwili karta w kolumnie *To Do*; w szczegółach zadania link „Otwórz stronę w Notion”. Kartę testową usuń w Notion ręcznie.

Przekazywane są tylko zadania główne utworzone **po** zapisaniu konfiguracji (stare zadania i podzadania — nie).
