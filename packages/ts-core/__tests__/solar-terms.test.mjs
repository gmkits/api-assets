import { before, describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  isLeapYear,
  parseHdayBundle,
  queryDay,
} from '../dist/esm/index.js';

import { DAY_FLAGS, NO_INDEX } from '../../ts-spec/dist/esm/index.js';

const TRAD_TO_SIMP = { '驚蟄': '惊蛰', '穀雨': '谷雨', '小滿': '小满', '芒種': '芒种', '處暑': '处暑' };
const toSimp = (n) => TRAD_TO_SIMP[n] || n;

const __dirname = dirname(fileURLToPath(import.meta.url));
const BUNDLE_2025 = resolve(__dirname, '../../../data/bundles/CN/2025.hday');
const SOLAR_TERMS_CSV = resolve(__dirname, '../../../tests/solar-terms.csv');
const DEFAULT_DAY_ENTRY = {
  flags: DAY_FLAGS.IS_WORKDAY,
  nameListIndex: NO_INDEX,
  labelListIndex: NO_INDEX,
  extIndex: NO_INDEX,
};

let bundle2025 = null;
let solarTermRows = [];

function toArrayBuffer(buf) {
  return buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength);
}

async function loadBundle(path) {
  const buf = await readFile(path);
  return parseHdayBundle(toArrayBuffer(buf));
}

function parseSolarTermCsv(text) {
  return text
    .trim()
    .split(/\r?\n/)
    .slice(1)
    .map((line) => {
      const [solarDate, solarTermIndexText, solarTermName] = line.split(',');
      return {
        solarDate,
        solarTermIndex: Number(solarTermIndexText),
        solarTermName,
      };
    });
}

function createSyntheticBundle(year) {
  const dayCount = isLeapYear(year) ? 366 : 365;
  return {
    header: {
      magic: 'HDAY',
      majorVersion: 1,
      minorVersion: 0,
      flags: 0,
      year,
      regionCode: 'CN',
      calendarSystem: 0,
      dayCount,
      sectionCount: 0,
    },
    days: Array.from({ length: dayCount }, () => DEFAULT_DAY_ENTRY),
    strings: [],
    nameLists: [],
  };
}

before(async () => {
  bundle2025 = await loadBundle(BUNDLE_2025);
  solarTermRows = parseSolarTermCsv(await readFile(SOLAR_TERMS_CSV, 'utf-8'));
});

describe('solar term fields', () => {
  it('should expose known solar terms from bundle queries', () => {
    assert.ok(bundle2025);

    const liChun = queryDay(bundle2025, '2025-02-03');
    const qingMing = queryDay(bundle2025, '2025-04-04');

    assert.ok(liChun);
    assert.ok(qingMing);
    assert.deepEqual(liChun.solarTerm, {
      index: 2,
      name: '立春',
    });
    assert.deepEqual(qingMing.solarTerm, {
      index: 6,
      name: '清明',
    });
    assert.ok(
      qingMing.festivals.some((festival) => festival.code === 'TOMB_SWEEPING'),
    );
  });

  it('should return null on non-solar-term dates', () => {
    assert.ok(bundle2025);
    const info = queryDay(bundle2025, '2025-02-04');
    assert.ok(info);
    assert.equal(info.solarTerm, null);
  });

  it('should match the full solar-term CSV with synthetic bundles', () => {
    const bundles = new Map();

    for (const row of solarTermRows) {
      const year = Number(row.solarDate.slice(0, 4));
      let bundle = bundles.get(year);
      if (!bundle) {
        bundle = createSyntheticBundle(year);
        bundles.set(year, bundle);
      }

      const info = queryDay(bundle, row.solarDate);
      assert.ok(info, row.solarDate);
      assert.deepEqual(
        info.solarTerm,
        {
          index: row.solarTermIndex,
          name: toSimp(row.solarTermName),
        },
        row.solarDate,
      );
    }
  });
});
