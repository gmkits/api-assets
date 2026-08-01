import { describe, it, before } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { readFileSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  HdayFormatError,
  parseHdayBundle,
  queryDay,
  queryYear,
  queryRange,
  dayOfYear,
} from '../dist/esm/index.js';
import { crc32 } from '../../ts-spec/dist/esm/index.js';
import { installCalendarAsset } from '../../ts-lunar/dist/esm/index.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const calendarBytes = readFileSync(resolve(
  __dirname,
  '../../../data/date-assets/calendar/calendar.cdat',
));
installCalendarAsset(calendarBytes.buffer.slice(
  calendarBytes.byteOffset,
  calendarBytes.byteOffset + calendarBytes.byteLength,
));
const BUNDLE_2025 = resolve(__dirname, '../../../data/date-assets/holidays/bundles/CN/2025.hday');
const BUNDLE_2026 = resolve(__dirname, '../../../data/date-assets/holidays/bundles/CN/2026.hday');
const BUNDLE_2000 = resolve(__dirname, '../../../data/date-assets/holidays/bundles/CN/2000.hday');

let bundle2025 = null;
let bundle2026 = null;

/** Convert a Node.js Buffer to an ArrayBuffer suitable for parseHdayBundle. */
function toArrayBuffer(buf) {
  return buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength);
}

function refreshCrc(buf) {
  buf.writeUInt32LE(crc32(buf.subarray(0, -4)), buf.length - 4);
  return buf;
}

function assertFormatCode(buf, code) {
  assert.throws(
    () => parseHdayBundle(toArrayBuffer(buf)),
    (error) => error instanceof HdayFormatError && error.code === code,
  );
}

async function loadBundle(path) {
  try {
    const buf = await readFile(path);
    return parseHdayBundle(toArrayBuffer(buf));
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
    assert.equal(bundle2025.header.majorVersion, 2);
    assert.equal(bundle2025.header.year, 2025);
    assert.equal(bundle2025.header.regionCode, 'CN');
    assert.equal(bundle2025.header.dayCount, 365);
  });

  it('should have 365 day entries', () => {
    assert.ok(bundle2025);
    assert.equal(bundle2025.days.length, 365);
  });

  it('should retain resolved names instead of the raw string table', () => {
    assert.ok(bundle2025);
    assert.ok(bundle2025.names.length > 0);
    assert.equal('strings' in bundle2025, false);
  });

  it('should use typed status and annotation arrays', () => {
    assert.ok(bundle2025);
    assert.ok(bundle2025.days.holidayBits instanceof Uint32Array);
    assert.ok(bundle2025.days.nameListIndexes instanceof Int16Array);
    assert.equal('nameLists' in bundle2025, false);
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

  it('should expose unified lunar and gan-zhi fields', () => {
    assert.ok(bundle2025);
    const info = queryDay(bundle2025, '2025-01-01');
    assert.ok(info);
    assert.deepEqual(info.lunar, {
      year: 2024,
      month: 12,
      day: 2,
      isLeapMonth: false,
      monthName: '腊月',
      dayName: '初二',
    });
    assert.deepEqual(info.ganZhi, {
      yearName: '甲辰',
      heavenlyStem: '甲',
      earthlyBranch: '辰',
      zodiac: '龙',
    });
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
    assert.equal(info.isHoliday, true);
    assert.equal(info.isOfficialHoliday, false);
    assert.equal(info.isWorkday, false);
  });

  it('should combine official status, lunar date, festival and source version', () => {
    assert.ok(bundle2025);
    const info = queryDay(bundle2025, '2025-10-06');
    assert.ok(info);
    assert.equal(info.isOfficialHoliday, true);
    assert.ok(info.festivals.some((festival) => festival.code === 'MID_AUTUMN'));
    assert.ok(info.lunar);
    assert.ok(info.ganZhi);
    assert.ok(info.sourceVersion.length > 0);
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

describe('parseHdayBundle — CN 2000', () => {
  it('should keep the first supported year fully offline', async () => {
    const bundle = await loadBundle(BUNDLE_2000);
    assert.ok(bundle);
    assert.equal(bundle.header.dayCount, 366);
    const info = queryDay(bundle, '2000-10-01');
    assert.ok(info);
    assert.equal(info.isOfficialHoliday, true);
    assert.equal(info.isStatutoryHoliday, true);
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

  it('should keep lunar fields on year view entries', () => {
    assert.ok(bundle2025);
    const days = queryYear(bundle2025);
    assert.equal(days[0].lunar.monthName, '腊月');
    assert.equal(days[0].lunar.dayName, '初二');
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
    const buf = new ArrayBuffer(36);
    const view = new DataView(buf);
    view.setUint8(0, 0x42); // 'B'
    view.setUint8(1, 0x41); // 'A'
    view.setUint8(2, 0x44); // 'D'
    view.setUint8(3, 0x21); // '!'
    assert.throws(() => parseHdayBundle(buf), /魔数/i);
  });

  it('should reject CRC corruption', async () => {
    const buf = await readFile(BUNDLE_2025);
    buf[buf.length - 5] ^= 0x01;
    assertFormatCode(buf, 'BAD_CRC');
  });

  it('should reject unsupported major versions before CRC validation', async () => {
    const buf = await readFile(BUNDLE_2025);
    buf[4] = 3;
    assertFormatCode(buf, 'UNSUPPORTED_VERSION');
  });

  it('should reject malformed UTF-8 and non-zero region padding', async () => {
    const malformed = await readFile(BUNDLE_2025);
    malformed[11] = 0xc3;
    malformed[12] = 0x28;
    assertFormatCode(refreshCrc(malformed), 'BAD_UTF8');

    const padding = await readFile(BUNDLE_2025);
    padding[13] = 1;
    assertFormatCode(refreshCrc(padding), 'BAD_HEADER');
  });

  it('should reject a wrong leap-year day count', async () => {
    const buf = await readFile(BUNDLE_2025);
    buf.writeUInt16LE(366, 28);
    assertFormatCode(refreshCrc(buf), 'BAD_HEADER');
  });

  it('should reject duplicate and overlapping sections', async () => {
    const duplicate = await readFile(BUNDLE_2025);
    duplicate.writeUInt16LE(1, 68);
    assertFormatCode(refreshCrc(duplicate), 'BAD_SECTION_TABLE');

    const overlapping = await readFile(BUNDLE_2025);
    overlapping.writeUInt32LE(overlapping.readUInt32LE(36), 72);
    assertFormatCode(refreshCrc(overlapping), 'BAD_SECTION_TABLE');
  });

  it('should reject unknown critical sections and skip unknown optional ones', async () => {
    const critical = await readFile(BUNDLE_2025);
    critical.writeUInt16LE(0x7ffe, 68);
    critical.writeUInt16LE(1, 70);
    assertFormatCode(refreshCrc(critical), 'UNKNOWN_CRITICAL_SECTION');

    const optional = await readFile(BUNDLE_2025);
    optional.writeUInt16LE(0x7ffe, 68);
    optional.writeUInt16LE(0, 70);
    const bundle = parseHdayBundle(toArrayBuffer(refreshCrc(optional)));
    assert.equal(bundle.header.year, 2025);
    assert.deepEqual(bundle.metadata, {});
  });

  it('should reject illegal override indexes and state combinations', async () => {
    const badDay = await readFile(BUNDLE_2025);
    const daySectionOffset = badDay.readUInt32LE(36);
    badDay.writeUInt16LE(365, daySectionOffset + 2);
    assertFormatCode(refreshCrc(badDay), 'BAD_DAY_OVERRIDE');

    const badState = await readFile(BUNDLE_2025);
    const overrideOffset = badState.readUInt32LE(36);
    badState[overrideOffset + 4] = 0x03;
    assertFormatCode(refreshCrc(badState), 'BAD_DAY_OVERRIDE');

    const badIndex = await readFile(BUNDLE_2025);
    const firstOverride = badIndex.readUInt32LE(36);
    badIndex.writeUInt16LE(0xfffe, firstOverride + 6);
    assertFormatCode(refreshCrc(badIndex), 'BAD_INDEX');
  });
});
