#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="${1:-$ROOT/java/cn-holiday-kit/target/cn-holiday-kit-1.0.0-SNAPSHOT.jar}"
SOURCE="$ROOT/java/cn-holiday-kit/src/standalone-test/java/com/github/gmkits/holiday/standalone/StandaloneSmoke.java"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

if [[ ! -f "$JAR" ]]; then
  echo "Missing cn-holiday-kit artifact: $JAR" >&2
  exit 1
fi

for entry in \
  com/github/gmkits/holiday/CnHolidayKit.class \
  com/github/gmkits/holiday/core/HolidayService.class \
  com/github/gmkits/holiday/lunar/LunarCalendar.class \
  com/github/gmkits/holiday/spec/DayInfo.class \
  cn-holiday-kit/assets/manifest.json; do
  if ! jar tf "$JAR" | grep -Fqx "$entry"; then
    echo "Single JAR is missing $entry" >&2
    exit 1
  fi
done

if ! unzip -p "$JAR" META-INF/MANIFEST.MF \
    | tr -d '\r' \
    | grep -Fqx 'Automatic-Module-Name: com.github.gmkits.holiday'; then
  echo "Stable Automatic-Module-Name is missing" >&2
  exit 1
fi

javac -encoding UTF-8 -cp "$JAR" -d "$TMP" "$SOURCE"
java -cp "$JAR:$TMP" com.github.gmkits.holiday.standalone.StandaloneSmoke
