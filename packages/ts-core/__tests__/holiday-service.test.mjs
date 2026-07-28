import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { createHolidayService } from '../dist/esm/index.js';

const here = dirname(fileURLToPath(import.meta.url));
const bundlePath = resolve(here, '../../../data/bundles/CN/2025.hday');
const manifestPath = resolve(here, '../../../data/manifest.json');
const calendarPath = resolve(
  here,
  '../../../data/date-assets/calendar/calendar.cdat',
);

function arrayBuffer(bytes) {
  return bytes.buffer.slice(
    bytes.byteOffset,
    bytes.byteOffset + bytes.byteLength,
  );
}

async function fixture() {
  const [bundle, manifestText, calendar] = await Promise.all([
    readFile(bundlePath),
    readFile(manifestPath, 'utf8'),
    readFile(calendarPath),
  ]);
  return {
    bundle: arrayBuffer(bundle),
    manifest: JSON.parse(manifestText),
    calendar: arrayBuffer(calendar),
  };
}

describe('HolidayService manifest and calendar assets', () => {
  it('verifies SHA-256 and answers status directly from bitmaps', async () => {
    const { bundle, manifest, calendar } = await fixture();
    const service = createHolidayService({
      manifest,
      calendarData: calendar,
      preloadedBundles: new Map([['CN-2025', bundle]]),
    });
    assert.equal(await service.isHoliday('2025-01-01'), true);
    assert.equal(await service.isWorkday('2025-01-02'), true);
  });

  it('rejects a manifest SHA-256 mismatch', async () => {
    const { bundle, manifest } = await fixture();
    manifest.bundles.CN['2025'].sha256 = '0'.repeat(64);
    const service = createHolidayService({
      manifest,
      preloadedBundles: new Map([['CN-2025', bundle]]),
    });
    await assert.rejects(
      () => service.isHoliday('2025-01-01'),
      /SHA-256 校验失败/,
    );
  });

  it('rejects region/year keys not declared by the manifest', async () => {
    const { bundle, manifest } = await fixture();
    const service = createHolidayService({
      manifest,
      preloadedBundles: new Map([['CN-2025', bundle]]),
    });
    await assert.rejects(
      () => service.isHoliday('2027-01-01'),
      /manifest 未声明/,
    );
  });
});
