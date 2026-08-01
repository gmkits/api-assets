#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
exec make -C "$ROOT" verify
