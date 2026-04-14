#!/bin/bash
set -euo pipefail
# Parse .hday via Java SDK and output golden-format JSON for specified dates
BUNDLE_DIR="${1:-../../data/bundles}"
OUTPUT_DIR="${2:-./output/java}"
mkdir -p "$OUTPUT_DIR"

# Build Java modules first
cd ../../java && gradle build -q && cd ../tests/cross-lang

# Compile and run a small test harness
javac -cp "../../java/holiday-core-java/build/libs/*:../../java/holiday-spec-java/build/libs/*" \
  -d /tmp/cross-lang-java CrossLangTest.java 2>/dev/null || true

echo "NOTE: Java cross-lang test requires CrossLangTest.java harness (TODO)"
echo "For now, compare golden files manually."
