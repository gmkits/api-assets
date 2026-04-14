#!/bin/bash
set -euo pipefail
VERSION="${1:-$(date +%Y.%m.%d)}"
OUT="holiday-data-${VERSION}.zip"
echo "=== Creating release bundle $OUT ==="
zip -r "$OUT" data/manifest.json data/bundles/
echo "✓ Created $OUT"
