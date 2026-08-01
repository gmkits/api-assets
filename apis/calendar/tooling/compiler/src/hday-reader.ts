import type {
  CalendarSystem,
  CommonMeta,
  MaterializedDay,
  MaterializedYearData,
  MultiLangNames,
} from './spec.js';
import {
  CALENDAR_SYSTEM_CODES,
  parseHdayBundle,
} from './spec.js';
import { indexToDate } from './materializer.js';

const CODE_TO_CALENDAR: Record<number, CalendarSystem> = {};
for (const [key, value] of Object.entries(CALENDAR_SYSTEM_CODES)) {
  CODE_TO_CALENDAR[value] = key as CalendarSystem;
}

/**
 * Decode a v2 `.hday` file back to the canonical materialized view.
 *
 * The strict binary validation lives in the compiler's local specification, so compiler
 * inspection and browser/Node runtime parsing cannot drift apart.
 */
export function readHday(buf: Buffer): MaterializedYearData {
  const data = buf.buffer.slice(
    buf.byteOffset,
    buf.byteOffset + buf.byteLength,
  ) as ArrayBuffer;
  const bundle = parseHdayBundle(data);
  const hasBit = (bits: Uint32Array, index: number): boolean =>
    (bits[index >>> 5] & (1 << (index & 31))) !== 0;

  const days: Record<string, MaterializedDay> = {};
  for (let index = 0; index < bundle.header.dayCount; index++) {
    const nameIndex = bundle.days.nameListIndexes[index];
    const labelIndex = bundle.days.labelListIndexes[index];
    days[indexToDate(bundle.header.year, index)] = {
      isHoliday: hasBit(bundle.days.holidayBits, index),
      isWorkday: hasBit(bundle.days.workdayBits, index),
      isWeekend: hasBit(bundle.days.weekendBits, index),
      isStatutoryHoliday: hasBit(bundle.days.statutoryBits, index),
      isAdjustedWorkday: hasBit(bundle.days.adjustedBits, index),
      holidayNames:
        nameIndex < 0 ? {} : bundle.names[nameIndex] as MultiLangNames,
      labels: labelIndex < 0 ? [] : [...bundle.labels[labelIndex]],
    };
  }

  const year = bundle.header.year;
  const regionCode = bundle.header.regionCode;
  const meta: CommonMeta = {
    specVersion: bundle.metadata.specVersion ?? '2.0.0',
    bundleId: `${regionCode}-${year}`,
    regionCode,
    parentRegionCode: null,
    year,
    validFrom: `${year}-01-01`,
    validTo: `${year}-12-31`,
    calendarSystem:
      CODE_TO_CALENDAR[bundle.header.calendarSystem] ?? 'GREGORIAN',
    timezone: '',
    weekendMask: ['SAT', 'SUN'],
    locales: [],
    sourceVersion: bundle.metadata.sourceVersion ?? '',
    generatedAt: bundle.metadata.generatedAt ?? '',
    generator: { name: '', version: '' },
    extensions: {},
  };
  return { meta, days };
}
