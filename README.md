# Todoisto

Aplikacja zadań (to-do) na Androida w stylu Todoista, z fioletową kolorystyką.
Napisana natywnie w **Kotlinie** z użyciem **Jetpack Compose**, **Material 3** i
**Room** do lokalnego przechowywania danych.

## Funkcje

- ⭐ **Szybkie dodawanie (Quick Add)** z parserem języka naturalnego (polski):
  daty (`jutro`, `w poniedziałek`, `za 3 dni`, `2026-07-20`, `20.07`), godzina
  (`o 15:00`), priorytet (`p1`–`p4`), projekt (`#projekt`), etykiety (`@etykieta`),
  rekurencja (`codziennie`, `co tydzień`, `co poniedziałek`), deadline (`do 2026-07-31`).
  Podgląd rozpoznanych tokenów na żywo.
- 📁 **Projekty** + **sekcje** wewnątrz projektu
- 🏷️ **Etykiety** (przekrojowa kategoryzacja niezależna od projektu)
- ✅ **Podzadania** — rozbijanie złożonych zadań, z postępem (np. 1/3)
- 🚩 **Priorytety P1–P4** z kolorowym oznaczeniem
- 📅 **Termin (due date)** oraz osobny **Deadline** (nieprzekraczalny)
- 🔁 **Rekurencja** — po ukończeniu zadanie samo przechodzi na kolejny termin
- 🗂️ **Widoki**: Dzisiaj · Nadchodzące · Skrzynka · Ukończone · per projekt · per etykieta
- 🧭 **Szuflada nawigacji** z licznikami (Dzisiaj / Skrzynka)
- 💾 **Trwałe dane lokalne, w pełni offline** (Room / SQLite)
- 🟣 Interfejs **liquid glass** — żywe fioletowe tło, oszronione półprzezroczyste panele

### Roadmap (kolejne fazy)

Funkcje wymagające dodatkowej infrastruktury, zaplanowane na następne iteracje:
przypomnienia czasowe i lokalizacyjne (powiadomienia / geofencing), własne filtry
ze składnią zapytań, komentarze i załączniki, widżety ekranu głównego, widok Kanban,
szablony projektów, karma / statystyki, a także funkcje wymagające konta/serwera:
synchronizacja wielourządzeniowa, współdzielone projekty z przypisywaniem oraz
dwukierunkowa integracja z kalendarzem.

## Architektura

Wzorzec **MVVM** z jednokierunkowym przepływem danych:

```
UI (Compose)  ─►  TodoViewModel  ─►  TaskRepository  ─►  Room (TaskDao / TodoDatabase)
     ▲                                                          │
     └───────────────  StateFlow<TodoUiState>  ◄───────────────┘
```

- `data/` – encja `Task`, `TaskDao`, baza `TodoDatabase`, `TaskRepository`, `Priority`
- `ui/` – `TodoViewModel`, motyw (`theme/`), ekrany (`screens/`), komponenty (`components/`)
- Nawigacja: Jetpack Navigation Compose (lista → dodawanie / edycja)

## Wymagania

- Android Studio (Ladybug lub nowszy)
- JDK 17
- Android SDK 34, minSdk 24 (Android 7.0+)

## Budowanie

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Lub otwórz projekt w Android Studio i uruchom konfigurację `app`.

## Struktura projektu

```
app/src/main/java/pl/media30/todoisto/
├── TodoApplication.kt          # wstrzyknięcie repozytorium
├── MainActivity.kt             # host nawigacji Compose
├── data/                       # warstwa danych (Room)
│   ├── Task.kt · Priority.kt
│   ├── TaskDao.kt · TodoDatabase.kt · Converters.kt
│   └── TaskRepository.kt
└── ui/
    ├── TodoViewModel.kt        # logika + filtry + StateFlow
    ├── theme/                  # paleta fioletowa, typografia
    ├── screens/                # TaskListScreen, AddEditTaskScreen
    └── components/             # TaskItem
```

## Dalsze pomysły (roadmap)

- Projekty / etykiety i sekcje
- Powiadomienia i przypomnienia
- Zadania cykliczne
- Przeciąganie w celu ukończenia / usunięcia (swipe)
- Synchronizacja w chmurze
