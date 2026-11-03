# Checklista przed wyłączeniem Todoist

Każdy punkt odhaczamy dopiero po faktycznym teście; przy punkcie wpisz datę i wynik.

- [ ] **Offline na obu urządzeniach** — dodaj zadania bez sieci na Androidzie i na Macu, potem włącz sieć: nic nie znika ani się nie dubluje.
- [ ] **Konflikt edycji** — edytuj to samo zadanie na dwóch urządzeniach przed synchronizacją: wynik jest przewidywalny i zgodny z przyjętym modelem konfliktów.
- [ ] **Przypomnienia przy zablokowanym ekranie przez tydzień** — telefon zablokowany, tryb oszczędzania baterii włączony; każde przypomnienie przychodzi na czas (zapisuj opóźnienia).
- [ ] **Awaria Notion z kolejką** — przy niedostępnym Notion (lub błędnym tokenie) zadanie zostaje w kolejce, status jest widoczny, a po przywróceniu wysyła się dokładnie raz.
- [ ] **Backup i przywracanie** — eksport, czysta instalacja (symulacja utraty telefonu), przywrócenie z chmury i z pliku: komplet danych.
- [ ] **RLS i klucze** — `supabase/verify_security.sql` pokazuje RLS na każdej tabeli, brak uprawnień dla `anon`; żadnych kluczy Notion/Monday/OpenAI w kliencie ani w repozytorium.
- [ ] **Pauza Supabase** — wiadomo, jak projekt free tier zachowuje się po okresie bezczynności, aplikacja pokazuje czytelny błąd, a wznowienie projektu przywraca synchronizację bez utraty danych.
