# Todoisto na macOS (Compose Multiplatform — Desktop)

Natywna wersja Todoisto na Maca, zbudowana na **Compose for Desktop**
(to samo Compose UI i ten sam Kotlin co w wersji Android). Aplikacja
uruchamia się jako natywne okno macOS i pakuje do `.dmg` / `.pkg`.

> **Status: faza 1.** Reużywamy czysty kod domenowy z wersji mobilnej
> (`Priority`, `Recurrence`, `QuickAddParser`, `AutomationAdvisor`).
> Działa: lista „Dzisiaj", szybkie dodawanie z parserem języka
> naturalnego (np. `Zadzwonić jutro o 15 #Praca p1`), odhaczanie zadań,
> priorytety kolorem. Kolejne fazy: trwała baza, więcej ekranów, chmura.

## Wymagania (Mac)

- **JDK 17** — `brew install --cask temurin@17`
- Repo sklonowane (`git clone -b claude/android-todoist-app-ea5o7s …`)

Nie potrzebujesz Android Studio do wersji desktopowej — wystarczy JDK.

## Uruchomienie (tryb deweloperski)

W katalogu projektu:

```bash
./gradlew :desktop:run
```

Otworzy się okno aplikacji Todoisto.

> Jeśli `./gradlew` marudzi o Javie:
> `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`

## Zbudowanie natywnej paczki `.dmg`

```bash
./gradlew :desktop:packageDmg
# wynik: desktop/build/compose/binaries/main/dmg/Todoisto-1.0.0.dmg
```

Inne formaty:
```bash
./gradlew :desktop:packageDistributionForCurrentOS   # .dmg (na Macu)
./gradlew :desktop:createDistributable               # gotowy .app bez instalatora
# → desktop/build/compose/binaries/main/app/Todoisto.app
```

Otwierasz `.dmg`, przeciągasz Todoisto do Aplikacji — i gotowe, działa jak
zwykła aplikacja macOS.

> **Uwaga:** `.dmg`/`.pkg` da się zbudować **tylko na macOS** (tak działa
> pakowanie Compose Desktop). Sam kod kompiluje się na każdym systemie.
> Aby paczka nie była blokowana przez Gatekeeper u innych użytkowników,
> trzeba ją podpisać certyfikatem Apple Developer (osobny krok, opcjonalny).

## Struktura

```
desktop/
├── build.gradle.kts                 # plugin Compose Desktop + pakowanie .dmg/.pkg
└── src/main/kotlin/pl/media30/todoisto/
    ├── data/                        # REUŻYTY czysty kod z wersji mobilnej
    │   ├── Priority.kt · Recurrence.kt
    │   ├── QuickAddParser.kt · AutomationAdvisor.kt
    └── desktop/
        ├── Model.kt                 # lekki Task + repo pamięciowy + dane demo
        └── Main.kt                  # okno macOS + UI (lista, szybkie dodawanie)
```

## Synchronizacja z wersją Android (Supabase)

Desktop i Android **synchronizują się przez tę samą chmurę Supabase** —
wspólny moduł `shared` mówi tym samym protokołem i **tym samym formatem
danych** (tabela `todoisto_backups`, blob zgodny bajt-w-bajt z tym, co
zapisuje aplikacja mobilna). Dzięki temu zadania latają Mac ↔ telefon.

Jak włączyć na Macu:
1. W apce desktop kliknij **ⓘ Ustawienia chmury** (koło zębate na górze).
2. Wpisz dane **tego samego** projektu Supabase, którego używa Android:
   URL projektu (`https://…supabase.co`), klucz **anon (public)**, e-mail
   i hasło konta.
3. Kliknij **☁ Synchronizuj** — desktop pobiera dane z chmury (nadpisuje
   lokalne), a potem wysyła aktualny stan.

**Bezpieczne scalanie (nic nie ginie):** sync nie nadpisuje już całości —
łączy dane po `id`. Zadanie dodane po jednej stronie zostaje; przy
konflikcie (to samo `id`) wygrywa nowsza wersja (znacznik `updatedAt`,
z fallbackiem na `completedAt`/`createdAt`). Projekty/etykiety: suma.

**Automatyczny sync w tle:** przy starcie (gdy dane logowania są
uzupełnione), cyklicznie co ~2 min i wkrótce po każdej zmianie (debounce
~4 s). Można wyłączyć w Ustawieniach chmury; przycisk „☁ Synchronizuj"
zawsze wymusza sync ręcznie.

**Usuwanie z nagrobkami (tombstones):** skasowane zadanie nie znika od
razu z danych — zostaje jako ślad (`deleted`) z nowym znacznikiem czasu,
więc usunięcie **wygrywa przy scalaniu** i nie wraca z drugiego
urządzenia. Nagrobki starsze niż 30 dni są czyszczone. W interfejsie
usuniętych zadań nie widać (ikonka kosza na wierszu).

**Android też jest na module `shared`** (od v115): telefon używa tego
samego kodeka i logiki scalania, honoruje `deleted` i niesie nagrobki,
więc **usunięcia i scalanie działają end-to-end Mac ↔ telefon** (nic nie
ginie, delete się propaguje w obie strony). Room dostał pola `updatedAt`
i `deleted`; usuwanie zadania jest miękkie (nagrobek), a restore z chmury
SCALA zamiast nadpisywać.

Dane logowania i hasło zostają **tylko na tym Macu** (java.util.prefs).
Lokalnie zadania zapisują się do `~/.todoisto/data.json` (ten sam format
co chmura), więc przeżywają zamknięcie aplikacji.

## Wspólny moduł `shared`

`shared/` (czysty Kotlin/JVM, bez Androida i bez Compose) zawiera:
- `CloudModels` — neutralne modele (`CloudTask/Project/Label`),
- `CloudBackupCodec` — (de)serializacja blobu, zgodna z wersją Android,
- `SupabaseSync` — logowanie + push/pull przez REST Supabase.

Docelowo również aplikacja Android przełączy się na ten moduł (dziś ma
własną, ale **format identyczny**, więc interoperują już teraz).

## Roadmap wersji desktop

- ✅ Trwałe dane lokalne (`~/.todoisto/data.json`)
- ✅ Synchronizacja Mac ↔ Android przez Supabase
- ✅ Bezpieczne scalanie per-zadanie (nic nie ginie) + testy
- ✅ Automatyczny sync w tle (start / cyklicznie / po zmianie)
- ✅ Usuwanie z nagrobkami (tombstones) — desktop/chmura + testy
- ✅ Android na module `shared` (jedna kopia kodeka; scalanie i usunięcia
  działają end-to-end Mac ↔ telefon)
- ✅ Auto-sync w tle również w Androidzie (start / co 3 min / po zmianie)
- ✅ Ekrany desktopu: Dziś / Nadchodzące / Ukończone
- ✅ Szczegóły zadania (edycja tytułu, notatek, priorytetu, projektu,
  terminu) + usuwanie
- ✅ Projekty i etykiety (synchronizowane) — prawdziwe nazwy, wybór
  projektu w szczegółach
- ✅ Zadania cykliczne — po odhaczeniu przechodzą na kolejny termin
- ✅ Powiadomienia macOS o przypomnieniach (także z zadań z telefonu)
- ✅ Motywy (6 palet) — akcent jak w wersji mobilnej
- ✅ Asystent AI („Zapytaj AI: jak zrobić szybciej") w szczegółach zadania
- ✅ Deadline, czas trwania i przypomnienie w szczegółach
- ✅ Gmail — maile z gwiazdką → zadania (IMAP, hasło do aplikacji)
- ✅ Menu aplikacji macOS + skróty (⌘N nowe zadanie, ⌘S sync, ⌘, ustawienia)
- ✅ Pasek boczny jak szuflada w Androidzie / Todoist: Skrzynka, Dziś,
  Nadchodzące, Ukończone, ★ Ulubione, Projekty, Etykiety, Szukaj — pełna
  funkcjonalność dostępna z menu, liczniki przy widokach
- ✅ Dodawanie zadań naprawione: Enter (także numeryczny) i przycisk „+”;
  w widoku Dziś zadanie bez daty dostaje termin „dziś”, w widoku projektu
  trafia do tego projektu, inaczej do Skrzynki (z przełączeniem widoku)
- ✅ Sekcja ZALEGŁE z przyciskiem „Zmień termin → dziś” (jak w Todoist),
  czerwone daty zaległych zadań, nazwa projektu po prawej w wierszu
- Sekcje w projektach, załączniki (pełna parzystość z mobilną)

## Parytet z wersją Android — co jest, a czego nie

**Na Macu (przenośne, wdrożone):** zadania (Dziś/Nadchodzące/Ukończone),
szybkie dodawanie z parserem, szczegóły (tytuł, notatki, priorytet,
projekt, termin, deadline, czas trwania, przypomnienie), cykliczne,
projekty i etykiety, synchronizacja + auto-sync, powiadomienia macOS,
motywy, **AI („Zapytaj AI")**, **Gmail** (maile z gwiazdką).

**Nieprzenośne (API tylko Androida — świadomie pominięte):**
- **Przypomnienia lokalizacyjne / geofencing** — Mac nie ma GPS ani
  WorkManagera; przypomnienia czasowe działają (powiadomienia).
- **Dodawanie głosowe** — `SpeechRecognizer` jest androidowy.
- **Skan aparatem na żywo** — desktop nie ma aparatu (skan z pliku przez
  AI-wizję jest możliwy do dołożenia later — kod AI już jest przenośny).
- **Planer pogodowy** wg bieżącej lokalizacji — brak GPS na Macu.

Reszta różnic to kwestia dopisania UI, nie architektury — wspólny moduł
`shared` i przenośne klienty (AI, Gmail) są właśnie po to.
- Powiadomienia macOS, skróty klawiszowe, menu aplikacji
