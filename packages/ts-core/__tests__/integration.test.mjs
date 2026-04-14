import { describe, it, before } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

import { parseHdayBundle, queryDay, queryYear, queryRange, dayOfYear } from '../dist/esm/index.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const BUNDLE_2025 = resolve(__dirname, '../../../data/bundles/CN/2025.hday');
const BUNDLE_2026 = resolve(__dirname, '../../../data/bundles/CN/2026.hday');

let bundle2025 = null;
let bundle2026 = null;

async function loadBundle(path) {
  try {
    const buf = await readFile(path);
    return parseHdayBundle(buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength));
  } catch {
    return null;
  }
}

describe('parseHdayBundle — CN 2025', () => {
  before(async () => {
    bundle2025 = await loadBundle(BUNDLE_2025);
  });

  it('should parse header correctly', () => {
    assert.ok(bundle2025, 'Bundle 2025 must be available');
    assert.equal(bundle2025.header.magic, 'HDAY');
    assert.equal(bundle2025.header.majorVersion, 1);
    assert.equal(bundle2025.header.year, 2025);
    assert.equal(bundle2025.header.regionCode, 'CN');
    assert.equal(bundle2025.header.dayCount, 365);
  });

  it('should have 365 day entries', () => {
    assert.ok(bundle2025);
    assert.equal(bundle2025.days.length, 365);
  });

  it('should have non-empty string table', () => {
    assert.ok(bundle2025);
    assert.ok(bundle2025.strings.length > 0);
  });

  it('should have non-empty name lists', () => {
    assert.ok(bundle2025);
    assert.ok(bundle2025.nameLists.length > 0);
  });
});

describe('queryDay — CN 2025 holidays', () => {
  before(async () => {
    bundle2025 = bundle2025 ?? await loadBundle(BUNDLE_2025);
  });

  it('should return New Year 2025 as statutory holiday', () => {
    assert.ok(bundle2025);
    const info = queryDay(bundle2025, '2025-01-01');
    assert.ok(info);
    assert.equal(info.date, '2025-01-01');
    assert.equal(info.regionCode, 'CN');
    assert.equal(info.isHoliday, true);
    assert.equal(info.isStatutoryHoliday, true);
    assert.equal(info.isWorkday, false);
  });

  it('should return Jan 2 as normal workday', () => {
    assert.ok(bundle2025);
    const info = queryDay(bundle2025, '2025-01-02');
    assert.ok(info);
    assert.equal(info.isWorkday, true);
    assert.equal(info.isHoliday, false);
    assert.equal(info.isWeekend, false);
  });

  it('should return Jan 4 (Saturday) as weekend', () => {
    assert.ok(bundle2025);
    const info = queryDay(bundle2025, '2025-01-04');
    assert.ok(info);
    assert.equal(info.isWeekend, true);
    assert.equal(info.isWorkday, false);
  });

  it('should return Jan 26 as adjusted workday (Spring Festival makeup)', () => {
    assert.ok(bundle2025);
    const info = queryDay(bundle2025, '2025-01-26');
    assert.ok(info);
    assert.equal(info.isAdjustedWorkday, true);
    assert.equal(info.isWorkday, true);
    assert.equal(info.isWeekend, true);
    assert.equal(info.isHoliday, false);
  });

  it('should return Spring Festival (Jan 28) with correct labels', () => {
    assert.ok(bundle2025);
    const info = queryDay(bundle2025, '2025-01-28');
    assert.ok(info);
    assert.equal(info.isHoliday, true);
    assert.ok(info.labels.includes('SPRING_FESTIVAL'));
  });

  it('should return National Day (Oct 1) as statutory holiday', () => {
    assert.ok(bundle2025);
    const info = queryDay(bundle2025, '2025-10-01');
    assert.ok(info);
    assert.equal(info.isHoliday, true);
    assert.equal(info.isStatutoryHoliday, true);
    assert.ok(info.labels.includes('NATIONAL_DAY'));
  });

  it('should have holiday names for statutory holidays', () => {
    assert.ok(bundle2025);
    const info = queryDay(bundle2025, '2025-01-01');
    assert.ok(info);
    assert.ok(Object.keys(info.holidayNames).length > 0);
  });

  it('should return null for wrong year', () => {
    assert.ok(bundle2025);
    assert.equal(queryDay(bundle2025, '2026-01-01'), null);
  });
});

describe('queryYear — CN 2025', () => {
  before(async () => {
    bundle2025 = bundle2025 ?? await loadBundle(BUNDLE_2025);
  });

  it('should return 365 days', () => {
    assert.ok(bundle2025);
    const days = queryYear(bundle2025);
    assert.equal(days.length, 365);
  });

  it('should start with Jan 1 and end with Dec 31', () => {
    assert.ok(bundle2025);
    const days = queryYear(bundle2025);
    assert.equal(days[0].date, '2025-01-01');
    assert.equal(days[364].date, '2025-12-31');
  });

  it('should return a new array each time', () => {
    assert.ok(bundle2025);
    const a = queryYear(bundle2025);
    const b = queryYear(bundle2025);
    assert.notEqual(a, b);
    assert.deepEqual(a[0], b[0]);
  });
});

describe('queryRange — CN 2025', () => {
  before(async () => {
    bundle2025 = bundle2025 ?? await loadBundle(BUNDLE_2025);
  });

  it('should return correct slice for first week', () => {
    assert.ok(bundle2025);
    const startIdx = dayOfYear(2025, 1, 1);
    const endIdx = dayOfYear(2025, 1, 7);
    const range = queryRange(bundle2025, startIdx, endIdx);
    assert.equal(range.length, 7);
    assert.equal(range[0].date, '2025-01-01');
    assert.equal(range[6].date, '2025-01-07');
  });

  it('should return empty for reversed range', () => {
    assert.ok(bundle2025);
    const range = queryRange(bundle2025, 10, 5);
    assert.equal(range.length, 0);
  });

  it('should clamp to valid bounds', () => {
    assert.ok(bundle2025);
    const range = queryRange(bundle2025, -5, 2);
    assert.equal(range.length, 3);
    assert.equal(range[0].date, '2025-01-01');
  });
});

describe('parseHdayBundle — CN 2026', () => {
  before(async () => {
    bundle2026 = await loadBundle(BUNDLE_2026);
  });

  it('should parse 2026 header', () => {
    assert.ok(bundle2026);
    assert.equal(bundle2026.header.year, 2026);
    assert.equal(bundle2026.header.regionCode, 'CN');
    assert.equal(bundle2026.header.dayCount, 365);
  });

  it('should identify 2026 New Year as holiday', () => {
    assert.ok(bundle2026);
    const info = queryDay(bundle2026, '2026-01-01');
    assert.ok(info);
    assert.equal(info.isHoliday, true);
  });

  it('should identify 2026 Spring Festival (Feb 17)', () => {
    assert.ok(bundle2026);
    const info = queryDay(bundle2026, '2026-02-17');
    assert.ok(info);
    assert.equal(info.isHoliday, true);
    assert.ok(info.labels.includes('SPRING_FESTIVAL'));
  });
});

describe('parseHdayBundle — error cases', () => {
  it('should throw on empty buffer', () => {
    assert.throws(() => parseHdayBundle(new ArrayBuffer(0)));
  });

  it('should throw on too-small buffer', () => {
    assert.throws(() => parseHdayBundle(new ArrayBuffer(16)));
  });

  it('should throw on wrong magic', () => {
    const buf = new ArrayBuffer(32);
    const view = new DataView(buf);
    view.setUint8(0, 0x42); // 'B'
    view.setUint8(1, 0x41); // 'A'
    view.setUint8(2, 0x44); // 'D'
    view.setUint8(3, 0x21); // '!'
    assert.throws(() => parseHdayBundle(buf), /magic/i);
  });
});
