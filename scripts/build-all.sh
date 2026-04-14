#!/bin/bash
set -euo pipefail
echo "=== Building TypeScript packages ==="
pnpm -r run build
echo "=== Building Java modules ==="
cd java && gradle build && cd ..
echo "=== All builds complete ==="
