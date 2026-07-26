#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLI="$ROOT/packages/ts-compiler/dist/esm/cli.js"

cd "$ROOT"
pnpm --filter @holiday/spec build
pnpm --filter @holiday/compiler build

node "$ROOT/scripts/build-cn-holiday-canonical.mjs"

GENERATED="$ROOT/target/generated-data"
mkdir -p "$GENERATED/materialized/CN"

for canonical in "$GENERATED"/canonical/CN/*.canon.json; do
  year="$(basename "$canonical" .canon.json)"
  materialized="$GENERATED/materialized/CN/$year.year.json"
  bundle="$ROOT/data/bundles/CN/$year.hday"

  node "$CLI" validate --input "$canonical"
  node "$CLI" materialize --input "$canonical" --output "$materialized"
  node "$CLI" compile --input "$materialized" --output "$bundle"
done

node "$CLI" build-manifest \
  --bundles-dir "$ROOT/data/bundles" \
  --output "$ROOT/data/manifest.json"

mkdir -p "$ROOT/java/holiday-core-java/src/test/resources/bundles/CN"
cp "$ROOT"/data/bundles/CN/2000.hday "$ROOT/java/holiday-core-java/src/test/resources/bundles/CN/"
cp "$ROOT"/data/bundles/CN/2025.hday "$ROOT/java/holiday-core-java/src/test/resources/bundles/CN/"
cp "$ROOT"/data/bundles/CN/2026.hday "$ROOT/java/holiday-core-java/src/test/resources/bundles/CN/"
node "$ROOT/scripts/build-date-assets.mjs"

node "$ROOT/scripts/verify-cn-holiday-data.mjs"
bash "$ROOT/scripts/verify-bundles.sh"
echo "✓ Bundles, manifest, and unified offline date assets are synchronized"
