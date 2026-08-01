#!/usr/bin/env node

import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { buildCalendarAsset } from './lib/calendar-asset.mjs';

const root = fileURLToPath(new URL('../..', import.meta.url));
const assets = join(root, 'assets/runtime');
const calendarDir = join(assets, 'calendar');
const holidayBundleDir = join(assets, 'holidays/bundles/CN');
const calendarAsset = join(calendarDir, 'calendar.cdat');
const holidayManifestPath = join(assets, 'holidays/manifest.json');
const holidayManifest = JSON.parse(readFileSync(holidayManifestPath, 'utf8'));
const releaseVersion = JSON.parse(readFileSync(join(root, 'api-asset.json'), 'utf8')).version;

mkdirSync(calendarDir, { recursive: true });
mkdirSync(holidayBundleDir, { recursive: true });

buildCalendarAsset({
  lunarSource: join(root, 'assets/source/calendar/lunar-years.hex'),
  solarSource: join(root, 'tests/golden/solar-terms.csv'),
  output: calendarAsset,
});
const holidayYears = Object.keys(holidayManifest.bundles.CN ?? {}).map(Number).sort((a, b) => a - b);
if (holidayYears.length === 0) {
  throw new Error('No CN holiday bundles found');
}

const manifest = {
  formatVersion: 2,
  releaseVersion,
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
console.log('✓ Unified offline date assets built under assets/runtime');

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
