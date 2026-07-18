#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLI="$ROOT/packages/ts-compiler/dist/esm/cli.js"

cd "$ROOT"
pnpm --filter @holiday/spec build
pnpm --filter @holiday/compiler build

for canonical in "$ROOT"/data/canonical/CN/*.canon.json; do
  year="$(basename "$canonical" .canon.json)"
  materialized="$ROOT/data/materialized/CN/$year.year.json"
  bundle="$ROOT/data/bundles/CN/$year.hday"

  node "$CLI" validate --input "$canonical"
  node "$CLI" materialize --input "$canonical" --output "$materialized"
  node "$CLI" compile --input "$materialized" --output "$bundle"
done

node "$CLI" build-manifest \
  --bundles-dir "$ROOT/data/bundles" \
  --output "$ROOT/data/manifest.json"

mkdir -p "$ROOT/java/holiday-core-java/src/test/resources/bundles/CN"
cp "$ROOT"/data/bundles/CN/*.hday "$ROOT/java/holiday-core-java/src/test/resources/bundles/CN/"
node "$ROOT/scripts/build-date-assets.mjs"

node "$ROOT/scripts/verify-holiday-sources.mjs"
bash "$ROOT/scripts/verify-bundles.sh"
echo "✓ Bundles, manifest, and unified offline date assets are synchronized"
