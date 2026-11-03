# Todoisto — zasady pracy

## Cel
Osobista aplikacja typu Todoist jako hub: wejście zadań, routing do Notion (kanban Vocative, doktorat) i Monday, widok „do zrobienia” z wagami. Rutyny tylko w Todoisto (nie w Notion).

## Architektura
- Android: Kotlin, Compose, MVVM, Room, offline-first.
- Sync: Supabase (baza, auth, Realtime). Klucze Notion, Monday, OpenAI wyłącznie w Edge Functions.
- Routing do Notion/Monday jednokierunkowy: zapisujemy remoteId i remoteUrl, status nie wraca do Todoisto.
- Każdy rekord: id UUID generowane po stronie klienta, version, updated_at, deleted_at (soft delete).

## Zasady bezpieczeństwa (nienegocjowalne)
- RLS włączone na każdej tabeli przed pierwszym zapisem danych.
- Żaden sekret w kodzie klienta ani w repozytorium. Sprawdzaj .gitignore.
- Wersja API Notion przypięta jawnie, nie zmieniana bez testów.
- Cykliczne zadania automatyczne nie używają produkcyjnych tokenów z prawem zapisu.

## Zasady pracy z modelami
- Domyślnie zadania wykonuje `inzynier`.
- Konfiguracja i testy: `mechanik`. Eskaluj do `inzynier`, jeśli `mechanik` nie rozwiąże zadania w dwóch próbach.
- `architekt` tylko na decyzje projektowe i przegląd bezpieczeństwa, maksymalnie raz na etap.
- Nie zmieniaj modelu bez wyraźnej potrzeby; krótkie sesje zamiast długich.

## Zasady zmian
- Każda zmiana w schemacie Supabase: migracja wersjonowana (`supabase/migrations/`), nigdy ręcznie w panelu.
- Przed migracją backup.
- Nie usuwaj kodu wspierającego przypomnienia bez testu.
- Zmiany w synchronizacji wymagają testu scenariusza konfliktu (`shared/src/test/`).

## Testy i kontrola
- Tygodniowy smoke test: utwórz i usuń testowe zadanie w Notion i Monday.
- Przypomnienia: test przy wygaszonym ekranie i z oszczędzaniem baterii.
- Przed wyłączeniem Todoist: checklista z docs/checklist.md.

## Zakres MVP
Dodawanie zadań, widok „dziś”, przypomnienia, routing do Notion. Reszta funkcji po MVP.

## Nie rób bez pytania
- Nie wyłączaj Todoist ani nie usuwaj danych z niego.
- Nie dodawaj nowych integracji poza Notion i Monday.
- Nie publikuj aplikacji.
