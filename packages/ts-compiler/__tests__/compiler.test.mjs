import { describe, it, before } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  validate,
  materialize,
  isLeapYear,
  getDaysInYear,
  dateToIndex,
  indexToDate,
  getWeekday,
  compile,
  crc32,
  readHday,
} from '../dist/esm/index.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const CANON_2025 = resolve(__dirname, '../../../data/canonical/CN/2025.canon.json');

async function loadCanon(path) {
  try {
    const text = await readFile(path, 'utf-8');
    return JSON.parse(text);
  } catch {
    return null;
  }
}

// ---- Materializer helpers ----

describe('materializer — isLeapYear', () => {
  it('should detect leap years', () => {
    assert.equal(isLeapYear(2000), true);
    assert.equal(isLeapYear(2024), true);
    assert.equal(isLeapYear(2100), false);
    assert.equal(isLeapYear(2025), false);
  });
});

describe('materializer — getDaysInYear', () => {
  it('should return 365 for non-leap', () => {
    assert.equal(getDaysInYear(2025), 365);
  });

  it('should return 366 for leap', () => {
    assert.equal(getDaysInYear(2024), 366);
  });
});

describe('materializer — dateToIndex / indexToDate roundtrip', () => {
  it('should roundtrip for Jan 1', () => {
    assert.equal(dateToIndex('2025-01-01'), 0);
    assert.equal(indexToDate(2025, 0), '2025-01-01');
  });

  it('should roundtrip for Dec 31', () => {
    assert.equal(dateToIndex('2025-12-31'), 364);
    assert.equal(indexToDate(2025, 364), '2025-12-31');
  });

  it('should roundtrip for all days in 2025', () => {
    for (let i = 0; i < 365; i++) {
      const date = indexToDate(2025, i);
      assert.equal(dateToIndex(date), i, `Roundtrip failed for index ${i} → ${date}`);
    }
  });

  it('should handle leap year Feb 29', () => {
    assert.equal(dateToIndex('2024-02-29'), 59);
    assert.equal(indexToDate(2024, 59), '2024-02-29');
  });
});

describe('materializer — getWeekday', () => {
  it('should return correct weekday for known dates', () => {
    assert.equal(getWeekday('2025-01-01'), 'WED');
    assert.equal(getWeekday('2025-01-04'), 'SAT');
    assert.equal(getWeekday('2025-01-05'), 'SUN');
    assert.equal(getWeekday('2025-01-06'), 'MON');
  });
});

// ---- Validator ----

describe('validator', () => {
  it('should reject document without meta', () => {
    const result = validate({});
    assert.equal(result.valid, false);
    assert.ok(result.errors.some(e => e.includes('meta')));
  });

  it('should reject document with missing meta fields', () => {
    const doc = {
      meta: { year: 2025 },
      sources: [],
      rules: [],
      overrides: [],
      extensions: {},
    };
    const result = validate(doc);
    assert.equal(result.valid, false);
    assert.ok(result.errors.length > 0);
  });
});

// ---- Validate real canonical data ----

describe('validator — CN 2025 canonical', () => {
  let canon;

  before(async () => {
    canon = await loadCanon(CANON_2025);
  });

  it('should validate CN 2025 canonical document successfully', () => {
    assert.ok(canon, 'Canon 2025 must be available');
    const result = validate(canon);
    assert.equal(result.valid, true, `Errors: ${result.errors.join(', ')}`);
  });
});

// ---- Materialize + Compile + Read roundtrip ----

describe('compile/read roundtrip — CN 2025', () => {
  let canon;

  before(async () => {
    canon = await loadCanon(CANON_2025);
  });

  it('should materialize to 365 days', () => {
    assert.ok(canon);
    const yearData = materialize(canon);
    assert.equal(Object.keys(yearData.days).length, 365);
  });

  it('should mark Jan 1 as statutory holiday', () => {
    assert.ok(canon);
    const yearData = materialize(canon);
    const day = yearData.days['2025-01-01'];
    assert.ok(day, 'Day 2025-01-01 should exist');
    assert.equal(day.isHoliday, true);
    assert.equal(day.isStatutoryHoliday, true);
  });

  it('should mark Jan 26 as adjusted workday', () => {
    assert.ok(canon);
    const yearData = materialize(canon);
    const day = yearData.days['2025-01-26'];
    assert.ok(day);
    assert.equal(day.isAdjustedWorkday, true);
    assert.equal(day.isWorkday, true);
  });

  it('should compile and read back to equivalent data', () => {
    assert.ok(canon);
    const yearData = materialize(canon);
    const compiled = compile(yearData);
    assert.ok(compiled.length > 0);

    const readBack = readHday(compiled);
    assert.equal(readBack.meta.year, 2025);
    assert.equal(readBack.meta.regionCode, 'CN');
    assert.equal(Object.keys(readBack.days).length, 365);
  });

  it('should preserve holiday flags across compile/read roundtrip', () => {
    assert.ok(canon);
    const yearData = materialize(canon);
    const compiled = compile(yearData);
    const readBack = readHday(compiled);

    const original = yearData.days['2025-01-01'];
    const restored = readBack.days['2025-01-01'];
    assert.equal(restored.isHoliday, original.isHoliday);
    assert.equal(restored.isStatutoryHoliday, original.isStatutoryHoliday);
    assert.equal(restored.isWorkday, original.isWorkday);
    assert.equal(restored.isWeekend, original.isWeekend);
    assert.equal(restored.isAdjustedWorkday, original.isAdjustedWorkday);
  });
});

describe('crc32', () => {
  it('should compute checksum for a known buffer', () => {
    const buf = Buffer.from('HDAY');
    const result = crc32(buf);
    assert.equal(typeof result, 'number');
    assert.ok(result >= 0);
  });

  it('should return different values for different data', () => {
    const a = crc32(Buffer.from('hello'));
    const b = crc32(Buffer.from('world'));
    assert.notEqual(a, b);
  });

  it('should return same value for same data', () => {
    const data = Buffer.from('test data');
    assert.equal(crc32(data), crc32(data));
  });
});
