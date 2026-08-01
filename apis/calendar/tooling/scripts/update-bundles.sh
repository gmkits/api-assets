#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CLI="$ROOT/tooling/compiler/dist/esm/cli.js"

cd "$ROOT"
pnpm --filter @api-assets/calendar-compiler build

node "$ROOT/tooling/scripts/build-cn-holiday-canonical.mjs"

GENERATED="$ROOT/target/generated-data"
BUNDLES="$ROOT/assets/runtime/holidays/bundles"
HOLIDAY_MANIFEST="$ROOT/assets/runtime/holidays/manifest.json"
mkdir -p "$GENERATED/materialized/CN"
mkdir -p "$BUNDLES/CN"

for canonical in "$GENERATED"/canonical/CN/*.canon.json; do
  year="$(basename "$canonical" .canon.json)"
  materialized="$GENERATED/materialized/CN/$year.year.json"
  bundle="$BUNDLES/CN/$year.hday"

  node "$CLI" validate --input "$canonical"
  node "$CLI" materialize --input "$canonical" --output "$materialized"
  node "$CLI" compile --input "$materialized" --output "$bundle"
done

node "$CLI" build-manifest \
  --bundles-dir "$BUNDLES" \
  --output "$HOLIDAY_MANIFEST"

node "$ROOT/tooling/scripts/build-date-assets.mjs"

node "$ROOT/tooling/scripts/verify-cn-holiday-data.mjs"
bash "$ROOT/tooling/scripts/verify-bundles.sh"
echo "✓ Bundles, manifest, and unified offline date assets are synchronized"
