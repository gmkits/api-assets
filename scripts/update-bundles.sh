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

for resources in \
  "$ROOT/java/holiday-core-java/src/test/resources" \
  "$ROOT/java/holiday-api-j8/src/main/resources" \
  "$ROOT/java/holiday-api-j8/src/test/resources" \
  "$ROOT/java/holiday-api-j25/src/main/resources"; do
  mkdir -p "$resources/bundles/CN"
  cp "$ROOT"/data/bundles/CN/*.hday "$resources/bundles/CN/"
done
cp "$ROOT/data/manifest.json" "$ROOT/java/holiday-api-j25/src/main/resources/manifest.json"

node "$ROOT/scripts/verify-holiday-sources.mjs"
bash "$ROOT/scripts/verify-bundles.sh"
echo "✓ Bundles, manifest, and API resources are synchronized"
