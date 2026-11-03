# Jak zbudować plik APK (Todoisto)

Instrukcja krok po kroku, jak z kodu źródłowego stworzyć plik `.apk`,
który zainstalujesz na telefonie z Androidem.

Masz dwie drogi:
- **A. Android Studio** — klikanie, najproszczej dla początkujących.
- **B. Linia poleceń** — `./gradlew`, szybkie i powtarzalne.

---

## 0. Czego potrzebujesz (raz)

| Narzędzie | Wersja | Po co |
|-----------|--------|-------|
| **JDK** | 17 | kompilacja Kotlina |
| **Android SDK** | API 34 (compileSdk), minSdk 24 | biblioteki Androida |
| **Android Studio** | Ladybug lub nowszy | IDE + SDK w jednym (zalecane) |

Najprościej: zainstaluj **Android Studio** — pociągnie za sobą JDK i Android SDK.
Pobierz z https://developer.android.com/studio

---

## A. Android Studio (klikanie)

1. **Otwórz projekt**: *File → Open…* i wskaż katalog `todoisto`
   (ten z plikiem `settings.gradle.kts`). Poczekaj, aż Gradle zsynchronizuje
   projekt (pasek na dole).
2. **Zbuduj APK**: *Build → Build App Bundle(s) / APK(s) → Build APK(s)*.
3. Po chwili w prawym dolnym rogu pojawi się dymek **„APK(s) generated…"**
   — kliknij **locate**, żeby otworzyć folder z plikiem.
4. Gotowy plik:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

To jest wersja **debug** — w sam raz do testów na własnym telefonie.

---

## B. Linia poleceń (`gradlew`)

W katalogu projektu (`todoisto`):

### Wersja debug (do testów)
```bash
./gradlew assembleDebug
```
Plik wyjściowy:
```
app/build/outputs/apk/debug/app-debug.apk
```

> Na Windows użyj `gradlew.bat assembleDebug`.
> Pierwszy build pobiera zależności z internetu i trwa dłużej.

### Wyczyszczenie i ponowny build (gdy coś się „zacięło")
```bash
./gradlew clean assembleDebug
```

---

## Instalacja APK na telefonie

**Sposób 1 — przez kabel (adb):**
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
(`-r` = zainstaluj ponownie z zachowaniem danych)

**Sposób 2 — bez kabla:**
1. Skopiuj `app-debug.apk` na telefon (np. przez chmurę / e-mail / USB).
2. Otwórz plik w telefonie.
3. Zezwól na **instalację z nieznanych źródeł** (system o to poprosi).

---

## Podpis aplikacji — dlaczego „Nie zainstalowano" i jak tego uniknąć

Android pozwala **zaktualizować** aplikację tylko plikiem podpisanym **tym samym
kluczem** co wersja już zainstalowana. Inny klucz = komunikat „Nie zainstalowano" /
„Aplikacja nie została zainstalowana", nawet jeśli plik jest poprawny.

Od v118 repozytorium zawiera stały klucz `app/keystore/todoisto.jks` (hasło i alias
`todoisto`), a `app/build.gradle.kts` podpisuje nim **każdy** build, debug i release.
Dzięki temu APK zbudowany na Macu, na innym komputerze czy w sesji Claude ma zawsze
ten sam podpis i aktualizuje się bez pytań.

**Jednorazowe przejście ze starszej wersji (podpisanej innym kluczem):**
1. W starej aplikacji: Konto → Chmura → *Wyloguj z chmury* → zaloguj ponownie →
   **Wyślij kopię do chmury** (świeże logowanie daje ważny token, więc wysyłka działa).
2. Odinstaluj starą aplikację.
3. Zainstaluj nowy APK, zaloguj się w chmurze → **Pobierz z chmury**. Dane wracają
   (scalanie po id, nic nie ginie).

To klucz aplikacji prywatnej — do publikacji w Google Play użyj osobnego, tajnego
klucza (sekcja niżej).

---

## Wersja RELEASE (podpisana, do dystrybucji)

Wersja debug jest podpisana kluczem testowym. Do „prawdziwej" dystrybucji
(np. sklep, dłuższe użytkowanie) zrób podpisany build release.

### 1. Utwórz keystore (raz)
```bash
keytool -genkey -v -keystore todoisto.keystore \
  -alias todoisto -keyalg RSA -keysize 2048 -validity 10000
```
Zapamiętaj **hasło** i **alias** — bez nich nie zaktualizujesz aplikacji.
Trzymaj plik `todoisto.keystore` bezpiecznie i **poza repozytorium**.

### 2. Podłącz podpis w `app/build.gradle.kts`
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../todoisto.keystore")
            storePassword = "TWOJE_HASŁO"
            keyAlias = "todoisto"
            keyPassword = "TWOJE_HASŁO"
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}
```
> Nie wpisuj haseł na stałe w pliku, jeśli repo jest publiczne —
> użyj zmiennych środowiskowych albo `local.properties` (nieśledzony przez git).

### 3. Zbuduj release
```bash
./gradlew assembleRelease
```
Plik wyjściowy:
```
app/build/outputs/apk/release/app-release.apk
```

---

## Zmiana numeru wersji

Przed każdą nową paczką podnieś wersję w `app/build.gradle.kts`:
```kotlin
defaultConfig {
    versionCode = 111      // liczba całkowita, zawsze rosnąca
    versionName = "111"    // widoczna w apce (Ustawienia → Wersja)
}
```
`versionCode` musi rosnąć, żeby telefon uznał nowy plik za aktualizację.

---

## Sprawdzenie jakości (opcjonalnie, przed wydaniem)

```bash
./gradlew assembleDebug recordPaparazziDebug testDebugUnitTest
```
- `assembleDebug` — czy się kompiluje,
- `recordPaparazziDebug` — zrzuty ekranu UI (snapshoty),
- `testDebugUnitTest` — testy jednostkowe.

---

## Najczęstsze problemy

| Objaw | Rozwiązanie |
|-------|-------------|
| `SDK location not found` | utwórz `local.properties` z `sdk.dir=/ścieżka/do/Android/Sdk` (Android Studio robi to sam) |
| `Unsupported Java` / błąd JDK | ustaw JDK 17 (*Settings → Build Tools → Gradle → Gradle JDK*) |
| Build długo wisi / dziwne błędy | `./gradlew clean` i spróbuj ponownie |
| Telefon nie chce zainstalować | włącz „nieznane źródła" i podnieś `versionCode` |
| „App not installed" przy aktualizacji | odinstaluj starą wersję albo podnieś `versionCode` |

---

## Gdzie ląduje gotowy plik — ściąga

| Typ | Ścieżka |
|-----|---------|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release | `app/build/outputs/apk/release/app-release.apk` |
