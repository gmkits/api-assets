#!/usr/bin/env node

import { copyFileSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { buildCalendarAsset } from './lib/calendar-asset.mjs';

const root = fileURLToPath(new URL('..', import.meta.url));
const assets = join(root, 'data/date-assets');
const calendarDir = join(assets, 'calendar');
const holidayBundleDir = join(assets, 'holidays/bundles/CN');
const calendarAsset = join(calendarDir, 'calendar.cdat');
const holidayManifest = JSON.parse(readFileSync(join(root, 'data/manifest.json'), 'utf8'));

mkdirSync(calendarDir, { recursive: true });
mkdirSync(holidayBundleDir, { recursive: true });

buildCalendarAsset({
  lunarSource: join(root, 'data/source/calendar/lunar-years.hex'),
  solarSource: join(root, 'tests/solar-terms.csv'),
  output: calendarAsset,
});
copyFileSync(join(root, 'data/manifest.json'), join(assets, 'holidays/manifest.json'));

for (const name of readdirSync(join(root, 'data/bundles/CN')).filter((file) => file.endsWith('.hday'))) {
  copyFileSync(join(root, 'data/bundles/CN', name), join(holidayBundleDir, name));
}

const holidayYears = Object.keys(holidayManifest.bundles.CN ?? {}).map(Number).sort((a, b) => a - b);
if (holidayYears.length === 0) {
  throw new Error('No CN holiday bundles found');
}

const manifest = {
  formatVersion: 2,
  generatedAt: holidayManifest.publishedAt,
  calendar: {
    data: asset('calendar/calendar.cdat', 1900, 2100),
  },
  holidays: {
    region: 'CN',
    manifest: asset(
      'holidays/manifest.json',
      holidayYears[0],
      holidayYears[holidayYears.length - 1],
    ),
    bundleRoot: 'holidays/bundles',
  },
};

writeFileSync(join(assets, 'manifest.json'), `${JSON.stringify(manifest, null, 2)}\n`);
console.log('✓ Unified offline date assets built under data/date-assets');

function asset(path, startYear, endYear) {
  const data = readFileSync(join(assets, path));
  return {
    path,
    startYear,
    endYear,
    bytes: data.length,
    sha256: createHash('sha256').update(data).digest('hex'),
  };
}
