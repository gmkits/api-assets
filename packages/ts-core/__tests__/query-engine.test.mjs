import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { installCalendarAsset } from '../../ts-lunar/dist/esm/index.js';

import {
  isLeapYear,
  dayOfYear,
  parseDate,
  formatDate,
  monthDayFromIndex,
  queryDay,
  resolveNames,
  resolveLabels,
} from '../dist/esm/index.js';

import { NO_INDEX } from '../../ts-spec/dist/esm/index.js';

const here = dirname(fileURLToPath(import.meta.url));
const calendarBytes = readFileSync(resolve(
  here,
  '../../../data/date-assets/calendar/calendar.cdat',
));
installCalendarAsset(calendarBytes.buffer.slice(
  calendarBytes.byteOffset,
  calendarBytes.byteOffset + calendarBytes.byteLength,
));

function workdayTable(dayCount) {
  const words = Math.ceil(dayCount / 32);
  const workdayBits = new Uint32Array(words);
  workdayBits.fill(0xffffffff);
  const nameListIndexes = new Int16Array(dayCount);
  const labelListIndexes = new Int16Array(dayCount);
  nameListIndexes.fill(-1);
  labelListIndexes.fill(-1);
  return {
    length: dayCount,
    holidayBits: new Uint32Array(words),
    workdayBits,
    weekendBits: new Uint32Array(words),
    statutoryBits: new Uint32Array(words),
    adjustedBits: new Uint32Array(words),
    nameListIndexes,
    labelListIndexes,
  };
}

describe('isLeapYear', () => {
  it('should identify leap years', () => {
    assert.equal(isLeapYear(2000), true);
    assert.equal(isLeapYear(2024), true);
    assert.equal(isLeapYear(2400), true);
  });

  it('should identify non-leap years', () => {
    assert.equal(isLeapYear(2001), false);
    assert.equal(isLeapYear(2023), false);
    assert.equal(isLeapYear(1900), false);
    assert.equal(isLeapYear(2100), false);
  });
});

describe('dayOfYear', () => {
  it('should return 0 for Jan 1', () => {
    assert.equal(dayOfYear(2025, 1, 1), 0);
  });

  it('should return 364 for Dec 31 (non-leap)', () => {
    assert.equal(dayOfYear(2025, 12, 31), 364);
  });

  it('should return 365 for Dec 31 (leap)', () => {
    assert.equal(dayOfYear(2024, 12, 31), 365);
  });

  it('should handle Feb 29 in leap year', () => {
    assert.equal(dayOfYear(2024, 2, 29), 59);
  });

  it('should handle March 1 differently for leap/non-leap', () => {
    assert.equal(dayOfYear(2024, 3, 1), 60);
    assert.equal(dayOfYear(2025, 3, 1), 59);
  });
});

describe('parseDate', () => {
  it('should parse YYYY-MM-DD format', () => {
    assert.deepEqual(parseDate('2025-01-01'), [2025, 1, 1]);
    assert.deepEqual(parseDate('2025-12-31'), [2025, 12, 31]);
  });

  it('should throw on invalid format', () => {
    assert.throws(() => parseDate('2025/01/01'));
    assert.throws(() => parseDate('25-01-01'));
    assert.throws(() => parseDate('invalid'));
    assert.throws(() => parseDate(''));
  });
});

describe('formatDate', () => {
  it('should format to YYYY-MM-DD', () => {
    assert.equal(formatDate(2025, 1, 1), '2025-01-01');
    assert.equal(formatDate(2025, 12, 31), '2025-12-31');
  });

  it('should zero-pad month and day', () => {
    assert.equal(formatDate(2025, 3, 5), '2025-03-05');
  });
});

describe('monthDayFromIndex', () => {
  it('should return [1, 1] for index 0', () => {
    assert.deepEqual(monthDayFromIndex(2025, 0), [1, 1]);
  });

  it('should return [12, 31] for last index (non-leap)', () => {
    assert.deepEqual(monthDayFromIndex(2025, 364), [12, 31]);
  });

  it('should return [12, 31] for last index (leap)', () => {
    assert.deepEqual(monthDayFromIndex(2024, 365), [12, 31]);
  });

  it('should handle Feb 29 in leap year', () => {
    assert.deepEqual(monthDayFromIndex(2024, 59), [2, 29]);
  });

  it('should throw on out-of-range index', () => {
    assert.throws(() => monthDayFromIndex(2025, 365), RangeError);
    assert.throws(() => monthDayFromIndex(2025, -1), RangeError);
  });

  it('should roundtrip with dayOfYear', () => {
    for (let idx = 0; idx < 365; idx++) {
      const [m, d] = monthDayFromIndex(2025, idx);
      assert.equal(dayOfYear(2025, m, d), idx, `Roundtrip failed for index ${idx}`);
    }
  });
});

describe('resolveNames', () => {
  const strings = ['zh-CN', '元旦', 'en-US', "New Year's Day"];

  it('should return empty object for undefined entry', () => {
    assert.deepEqual(resolveNames(undefined, strings), {});
  });

  it('should resolve locale to name arrays', () => {
    const entry = {
      pairs: [
        { keyIndex: 0, valueIndex: 1 },
        { keyIndex: 2, valueIndex: 3 },
      ],
    };
    const result = resolveNames(entry, strings);
    assert.deepEqual(result['zh-CN'], ['元旦']);
    assert.deepEqual(result['en-US'], ["New Year's Day"]);
  });

  it('should skip NO_INDEX keys', () => {
    const entry = {
      pairs: [{ keyIndex: NO_INDEX, valueIndex: 1 }],
    };
    const result = resolveNames(entry, strings);
    assert.deepEqual(result, {});
  });
});

describe('resolveLabels', () => {
  const strings = ['NEW_YEAR', 'STATUTORY', 'zh-CN', '元旦'];

  it('should return empty array for undefined entry', () => {
    assert.deepEqual(resolveLabels(undefined, strings), []);
  });

  it('should resolve labels (entries where keyIndex is NO_INDEX)', () => {
    const entry = {
      pairs: [
        { keyIndex: NO_INDEX, valueIndex: 0 },
        { keyIndex: NO_INDEX, valueIndex: 1 },
      ],
    };
    const result = resolveLabels(entry, strings);
    assert.deepEqual(result, ['NEW_YEAR', 'STATUTORY']);
  });

  it('should skip entries where keyIndex is not NO_INDEX', () => {
    const entry = {
      pairs: [
        { keyIndex: 2, valueIndex: 3 },
        { keyIndex: NO_INDEX, valueIndex: 0 },
      ],
    };
    const result = resolveLabels(entry, strings);
    assert.deepEqual(result, ['NEW_YEAR']);
  });
});

describe('queryDay', () => {
  it('should return null lunar fields outside supported range', () => {
    const bundle = {
      header: {
        magic: 'HDAY',
        majorVersion: 2,
        minorVersion: 0,
        flags: 0,
        year: 1900,
        regionCode: 'CN',
        calendarSystem: 0,
        dayCount: 1,
        sectionCount: 0,
      },
      days: workdayTable(1),
      names: [],
      labels: [],
      metadata: {},
    };

    const info = queryDay(bundle, '1900-01-01');
    assert.ok(info);
    assert.equal(info.lunar, null);
    assert.equal(info.ganZhi, null);
  });

  it('should keep lunar fields on the last supported solar day', () => {
    const bundle = {
      header: {
        magic: 'HDAY',
        majorVersion: 2,
        minorVersion: 0,
        flags: 0,
        year: 2101,
        regionCode: 'CN',
        calendarSystem: 0,
        dayCount: 29,
        sectionCount: 0,
      },
      days: workdayTable(29),
      names: [],
      labels: [],
      metadata: {},
    };

    const lastSupported = queryDay(bundle, '2101-01-28');
    const nextDay = queryDay(bundle, '2101-01-29');

    assert.ok(lastSupported);
    assert.ok(lastSupported.lunar);
    assert.ok(lastSupported.ganZhi);
    assert.ok(nextDay);
    assert.equal(nextDay.lunar, null);
    assert.equal(nextDay.ganZhi, null);
  });
});
