#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="${1:-$ROOT/java/cn-holiday-kit/target/cn-holiday-kit-1.0.0-rc1.jar}"
SOURCE_ROOT="$ROOT/java/cn-holiday-kit/src/module-test/java"
MAIN_CLASS="com.github.gmkits.holiday.moduletest.ModulePathSmoke"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

if [[ ! -f "$JAR" ]]; then
  echo "Missing cn-holiday-kit artifact: $JAR" >&2
  exit 1
fi

javac \
  -encoding UTF-8 \
  --module-path "$JAR" \
  -d "$TMP" \
  "$SOURCE_ROOT/module-info.java" \
  "$SOURCE_ROOT/com/github/gmkits/holiday/moduletest/ModulePathSmoke.java"

java \
  --module-path "$JAR:$TMP" \
  --module "com.github.gmkits.holiday.smoke/$MAIN_CLASS"
