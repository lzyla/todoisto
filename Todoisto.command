#!/bin/bash
# Todoisto na macOS — kliknij dwukrotnie ten plik, żeby uruchomić aplikację.
# (Za pierwszym razem pobierze zależności i chwilę potrwa; potem jest szybko.)

cd "$(dirname "$0")" || exit 1

echo "──────────────────────────────────────────────"
echo "  Todoisto — uruchamianie na macOS"
echo "──────────────────────────────────────────────"

# Wskaż JDK 17+ jeśli jest zainstalowany.
if /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
  export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
fi

if ! command -v java >/dev/null 2>&1 && [ -z "$JAVA_HOME" ]; then
  echo
  echo "  Brak Javy (JDK 17). Zainstaluj ją raz:"
  echo "    1) Homebrew (jeśli nie masz):  https://brew.sh"
  echo "    2) brew install --cask temurin@17"
  echo
  read -r -p "  Naciśnij Enter, aby zamknąć."
  exit 1
fi

echo "  Startuję…  (okno aplikacji zaraz się pojawi)"
./gradlew :desktop:run

echo
read -r -p "  Aplikacja zamknięta. Naciśnij Enter, aby zamknąć to okno."
