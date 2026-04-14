#!/bin/bash
set -euo pipefail
# Parse .hday via Java SDK and output golden-format JSON for specified dates
BUNDLE_DIR="${1:-../../data/bundles}"
OUTPUT_DIR="${2:-./output/java}"
mkdir -p "$OUTPUT_DIR"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_DIR="$SCRIPT_DIR/../../java"

# Build Java modules first
echo "Building Java modules..."
cd "$JAVA_DIR" && ./gradlew :holiday-spec-java:jar :holiday-core-java:jar -q && cd "$SCRIPT_DIR"

# Locate JARs
SPEC_JAR=$(find "$JAVA_DIR/holiday-spec-java/build/libs" -name '*.jar' | head -1)
CORE_JAR=$(find "$JAVA_DIR/holiday-core-java/build/libs" -name '*.jar' | head -1)

if [ -z "$SPEC_JAR" ] || [ -z "$CORE_JAR" ]; then
  echo "ERROR: Could not find compiled JARs"
  exit 1
fi

# Compile the test harness
COMPILE_DIR="/tmp/cross-lang-java"
mkdir -p "$COMPILE_DIR"
echo "Compiling CrossLangTest.java..."
javac -cp "$SPEC_JAR:$CORE_JAR" -d "$COMPILE_DIR" CrossLangTest.java

# Run the harness
echo "Running cross-language tests..."
java -cp "$COMPILE_DIR:$SPEC_JAR:$CORE_JAR" CrossLangTest "$BUNDLE_DIR" "$OUTPUT_DIR"

echo "Java cross-lang test output written to $OUTPUT_DIR"
