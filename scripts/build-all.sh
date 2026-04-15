#!/bin/bash
set -euo pipefail
echo "=== Building TypeScript packages ==="
pnpm -r run build
echo "=== Building Java modules ==="
cd java && mvn -B clean verify && cd ..
echo "=== All builds complete ==="
