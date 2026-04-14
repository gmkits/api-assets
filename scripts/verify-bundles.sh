#!/bin/bash
set -euo pipefail
BUNDLES_DIR="${1:-data/bundles}"
MANIFEST="${2:-data/manifest.json}"

echo "=== Verifying bundles ==="

# Check manifest exists
if [ ! -f "$MANIFEST" ]; then
  echo "ERROR: manifest.json not found at $MANIFEST"
  exit 1
fi

# Check each .hday file has valid magic bytes
for hday in $(find "$BUNDLES_DIR" -name "*.hday"); do
  magic=$(head -c 4 "$hday")
  if [ "$magic" != "HDAY" ]; then
    echo "ERROR: Invalid magic in $hday"
    exit 1
  fi
  echo "✓ $hday — valid magic"
done

# Verify SHA256 from manifest using node
node -e "
const fs = require('fs');
const crypto = require('crypto');
const manifest = JSON.parse(fs.readFileSync('$MANIFEST', 'utf-8'));
let ok = true;
for (const [region, years] of Object.entries(manifest.bundles)) {
  for (const [year, entry] of Object.entries(years)) {
    const filePath = '$BUNDLES_DIR/' + entry.file;
    if (!fs.existsSync(filePath)) {
      console.error('ERROR: Missing bundle', filePath);
      ok = false;
      continue;
    }
    const buf = fs.readFileSync(filePath);
    const sha256 = crypto.createHash('sha256').update(buf).digest('hex');
    if (sha256 !== entry.sha256) {
      console.error('ERROR: SHA256 mismatch for', filePath);
      ok = false;
    } else {
      console.log('✓', filePath, '— SHA256 verified');
    }
  }
}
if (!ok) process.exit(1);
"
echo "=== All bundles verified ==="
