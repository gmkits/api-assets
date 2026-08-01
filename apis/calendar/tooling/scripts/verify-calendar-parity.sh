#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

node "$ROOT/tooling/scripts/generate-hko-golden.mjs" --output "$TMP_DIR/lunar.csv"
cmp "$TMP_DIR/lunar.csv" "$ROOT/tests/golden/lunar-golden.csv"

node "$ROOT/tooling/scripts/generate-hko-solar-terms.mjs" \
  --output "$TMP_DIR/solar-terms.csv"
cmp "$TMP_DIR/solar-terms.csv" "$ROOT/tests/golden/solar-terms.csv"

echo "✓ Lunar dates and solar terms match freshly downloaded HKO data (1901-2100)"
