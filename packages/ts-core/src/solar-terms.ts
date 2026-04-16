import type { SolarTermInfo } from '@holiday/spec';
import { isLeapYear } from '@holiday/spec';

import {
  SOLAR_TERM_DAY_INDEXES_BY_YEAR,
  SOLAR_TERM_END_YEAR,
  SOLAR_TERM_NAMES,
  SOLAR_TERM_NAMES_ZH_TW,
  SOLAR_TERM_START_YEAR,
} from './generated/solar-term-data.js';

type ChineseLocale = 'zh-CN' | 'zh-TW';

const YEAR_COUNT = SOLAR_TERM_END_YEAR - SOLAR_TERM_START_YEAR + 1;
const INFOS_BY_LOCALE = new Map<string, ReadonlyArray<SolarTermInfo>>();

function getInfos(locale: ChineseLocale): ReadonlyArray<SolarTermInfo> {
  let infos = INFOS_BY_LOCALE.get(locale);
  if (!infos) {
    const names = locale === 'zh-TW' ? SOLAR_TERM_NAMES_ZH_TW : SOLAR_TERM_NAMES;
    infos = names.map((name, index) => ({ index, name }));
    INFOS_BY_LOCALE.set(locale, infos);
  }
  return infos;
}

// Flat buffer: 366 bytes per year × 200 years = 73,200 bytes single allocation.
// Year offsets stored separately for O(1) lookup without cumulative sum.
const YEAR_OFFSETS = new Uint32Array(YEAR_COUNT);
const FLAT_LOOKUP = buildFlatLookup();

function buildFlatLookup(): Uint8Array {
  // Pre-calculate total size & year offsets
  let total = 0;
  for (let i = 0; i < YEAR_COUNT; i++) {
    YEAR_OFFSETS[i] = total;
    total += isLeapYear(SOLAR_TERM_START_YEAR + i) ? 366 : 365;
  }

  // 0xFF = no solar term (sentinel)
  const buf = new Uint8Array(total);
  buf.fill(0xFF);

  for (let i = 0; i < YEAR_COUNT; i++) {
    const off = YEAR_OFFSETS[i];
    const dayIndexes = SOLAR_TERM_DAY_INDEXES_BY_YEAR[i];
    for (let j = 0; j < dayIndexes.length; j++) {
      buf[off + dayIndexes[j]] = j;
    }
  }
  return buf;
}

export function lookupSolarTerm(
  year: number,
  dayIndex: number,
  locale: ChineseLocale = 'zh-CN',
): SolarTermInfo | null {
  const yi = year - SOLAR_TERM_START_YEAR;
  if (yi < 0 || yi >= YEAR_COUNT) return null;
  const dayCount = isLeapYear(year) ? 366 : 365;
  if (dayIndex < 0 || dayIndex >= dayCount) return null;
  const idx = FLAT_LOOKUP[YEAR_OFFSETS[yi] + dayIndex];
  return idx === 0xFF ? null : getInfos(locale)[idx];
}
