#!/usr/bin/env node

import { copyFileSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('..', import.meta.url));
const assets = join(root, 'data/date-assets');
const calendarDir = join(assets, 'calendar');
const holidayBundleDir = join(assets, 'holidays/bundles/CN');
const lunarAsset = join(calendarDir, 'lunar-years.hex');
const solarAsset = join(calendarDir, 'solar-terms.csv');
const holidayManifest = JSON.parse(readFileSync(join(root, 'data/manifest.json'), 'utf8'));

mkdirSync(calendarDir, { recursive: true });
mkdirSync(holidayBundleDir, { recursive: true });

if (!readFileSync(lunarAsset, 'utf8').match(/0x[0-9a-f]+/gi)) {
  throw new Error(`Invalid lunar asset: ${lunarAsset}`);
}

writeSolarTermsAsset();
copyFileSync(join(root, 'data/manifest.json'), join(assets, 'holidays/manifest.json'));

for (const name of readdirSync(join(root, 'data/bundles/CN')).filter((file) => file.endsWith('.hday'))) {
  copyFileSync(join(root, 'data/bundles/CN', name), join(holidayBundleDir, name));
}

const holidayYears = Object.keys(holidayManifest.bundles.CN ?? {}).map(Number).sort((a, b) => a - b);
if (holidayYears.length === 0) {
  throw new Error('No CN holiday bundles found');
}

const manifest = {
  formatVersion: 1,
  generatedAt: holidayManifest.publishedAt,
  calendar: {
    lunarYears: asset('calendar/lunar-years.hex', 1900, 2100),
    solarTerms: asset('calendar/solar-terms.csv', 1900, 2100),
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

function writeSolarTermsAsset() {
  const authoritative = readFileSync(join(root, 'tests/solar-terms.csv'), 'utf8')
    .trimEnd()
    .split(/\r?\n/);
  const golden = readFileSync(
    join(root, 'java/holiday-lunar-java/src/test/resources/solar-terms-golden.csv'),
    'utf8',
  ).split(/\r?\n/);
  const year1900 = golden
    .slice(1)
    .filter((line) => line.startsWith('1900,'))
    .map((line) => {
      const [year, index, name, month, day] = line.split(',');
      return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')},${index},${name}`;
    });

  if (year1900.length !== 24) {
    throw new Error(`Expected 24 solar terms for 1900, got ${year1900.length}`);
  }
  writeFileSync(solarAsset, `${authoritative[0]}\n${year1900.join('\n')}\n${authoritative.slice(1).join('\n')}\n`);
}
