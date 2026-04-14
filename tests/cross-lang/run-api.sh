#!/bin/bash
set -euo pipefail
# Query API server and output golden-format JSON
API_URL="${1:-http://localhost:8080}"
OUTPUT_DIR="${2:-./output/api}"
mkdir -p "$OUTPUT_DIR"

DATES="2025-01-01 2025-01-26 2025-01-28 2025-05-01 2025-10-01 2026-01-01 2026-02-17 2026-10-01"

for d in $DATES; do
  curl -s "$API_URL/api/v1/day?date=$d&regionCode=CN" | python3 -m json.tool > "$OUTPUT_DIR/CN-$d.day.json"
  echo "✓ $OUTPUT_DIR/CN-$d.day.json"
done
