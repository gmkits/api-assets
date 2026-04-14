#!/bin/bash
set -euo pipefail
# Parse .hday via Node SDK and output golden-format JSON for specified dates
BUNDLE_DIR="${1:-../../data/bundles}"
OUTPUT_DIR="${2:-./output/node}"
mkdir -p "$OUTPUT_DIR"

node -e "
const { createHolidayService } = require('../../packages/ts-core/dist/cjs/index.cjs');
const fs = require('fs');
const path = require('path');

const svc = createHolidayService({ dataPath: '$BUNDLE_DIR', defaultRegion: 'CN' });
const dates = ['2025-01-01','2025-01-26','2025-01-28','2025-05-01','2025-10-01','2026-01-01','2026-02-17','2026-10-01'];

(async () => {
  for (const d of dates) {
    const info = await svc.getDayInfo(d);
    const outPath = path.join('$OUTPUT_DIR', 'CN-' + d + '.day.json');
    fs.writeFileSync(outPath, JSON.stringify(info, null, 2) + '\n');
    console.log('✓ ' + outPath);
  }
})().catch(e => { console.error(e); process.exit(1); });
"
