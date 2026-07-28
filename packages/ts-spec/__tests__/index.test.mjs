import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

import {
  DAY_FLAGS,
  SECTION_TYPES,
  CALENDAR_SYSTEM_CODES,
  HDAY_MAGIC,
  HDAY_HEADER_SIZE,
  HDAY_SECTION_ENTRY_SIZE,
  HDAY_DAY_OVERRIDE_SIZE,
  HDAY_VERSION_MAJOR,
  NO_INDEX,
} from '../dist/esm/index.js';

describe('DAY_FLAGS', () => {
  it('should have correct bit positions', () => {
    assert.equal(DAY_FLAGS.IS_HOLIDAY, 1);
    assert.equal(DAY_FLAGS.IS_WORKDAY, 2);
    assert.equal(DAY_FLAGS.IS_WEEKEND, 4);
    assert.equal(DAY_FLAGS.IS_STATUTORY_HOLIDAY, 8);
    assert.equal(DAY_FLAGS.IS_ADJUSTED_WORKDAY, 16);
    assert.equal(DAY_FLAGS.HAS_NAME, 32);
    assert.equal(DAY_FLAGS.HAS_LABEL, 64);
  });

  it('should be powers of two (non-overlapping bits)', () => {
    const allFlags = Object.values(DAY_FLAGS);
    for (let i = 0; i < allFlags.length; i++) {
      for (let j = i + 1; j < allFlags.length; j++) {
        assert.equal(allFlags[i] & allFlags[j], 0, `Flags at index ${i} and ${j} overlap`);
      }
    }
  });

  it('should support combining multiple flags', () => {
    const combined = DAY_FLAGS.IS_HOLIDAY | DAY_FLAGS.IS_STATUTORY_HOLIDAY | DAY_FLAGS.HAS_NAME;
    assert.notEqual(combined & DAY_FLAGS.IS_HOLIDAY, 0);
    assert.notEqual(combined & DAY_FLAGS.IS_STATUTORY_HOLIDAY, 0);
    assert.notEqual(combined & DAY_FLAGS.HAS_NAME, 0);
    assert.equal(combined & DAY_FLAGS.IS_WORKDAY, 0);
  });
});

describe('SECTION_TYPES', () => {
  it('should have distinct values', () => {
    const values = Object.values(SECTION_TYPES);
    assert.equal(new Set(values).size, values.length);
  });

  it('should have expected section types', () => {
    assert.equal(SECTION_TYPES.DAY_OVERRIDES, 0x0001);
    assert.equal(SECTION_TYPES.STRING_TABLE, 0x0002);
    assert.equal(SECTION_TYPES.NAME_LIST_TABLE, 0x0003);
    assert.equal(SECTION_TYPES.META_TABLE, 0x0004);
  });
});

describe('CALENDAR_SYSTEM_CODES', () => {
  it('should have GREGORIAN as 0', () => {
    assert.equal(CALENDAR_SYSTEM_CODES.GREGORIAN, 0);
  });

  it('should have CHINESE_LUNAR as 1', () => {
    assert.equal(CALENDAR_SYSTEM_CODES.CHINESE_LUNAR, 1);
  });
});

describe('Binary format constants', () => {
  it('should have correct magic string', () => {
    assert.equal(HDAY_MAGIC, 'HDAY');
    assert.equal(HDAY_MAGIC.length, 4);
  });

  it('should have correct header size', () => {
    assert.equal(HDAY_HEADER_SIZE, 32);
  });

  it('should have correct section entry size', () => {
    assert.equal(HDAY_SECTION_ENTRY_SIZE, 12);
  });

  it('should have correct day entry size', () => {
    assert.equal(HDAY_DAY_OVERRIDE_SIZE, 8);
    assert.equal(HDAY_VERSION_MAJOR, 2);
  });

  it('should have correct NO_INDEX sentinel', () => {
    assert.equal(NO_INDEX, 0xFFFF);
  });
});
