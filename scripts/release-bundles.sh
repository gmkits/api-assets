#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="${1:-$(date +%Y.%m.%d)}"
OUT="$ROOT/holiday-data-${VERSION}.zip"
cd "$ROOT"
echo "=== Creating release bundle $OUT ==="
node scripts/build-date-assets.mjs
zip -r "$OUT" data/date-assets/
echo "✓ Created $OUT"
