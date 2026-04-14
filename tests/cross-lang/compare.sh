#!/bin/bash
set -euo pipefail
echo "=== Cross-Language Parity Test ==="

DIRS=("output/node" "output/java" "output/api")
REFERENCE="${DIRS[0]}"

# Check all outputs exist
for dir in "${DIRS[@]}"; do
  if [ ! -d "$dir" ]; then
    echo "SKIP: $dir not found"
    continue
  fi
  if [ "$dir" = "$REFERENCE" ]; then continue; fi

  echo "--- Comparing $REFERENCE vs $dir ---"
  DIFF_FOUND=false
  for f in "$REFERENCE"/*.json; do
    fname=$(basename "$f")
    other="$dir/$fname"
    if [ ! -f "$other" ]; then
      echo "  MISSING: $other"
      DIFF_FOUND=true
      continue
    fi
    if ! diff -q "$f" "$other" > /dev/null 2>&1; then
      echo "  DIFF: $fname"
      diff "$f" "$other" || true
      DIFF_FOUND=true
    fi
  done
  if [ "$DIFF_FOUND" = false ]; then
    echo "  ✓ All files match"
  fi
done

echo "=== Done ==="
