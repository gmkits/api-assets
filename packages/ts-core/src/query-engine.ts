/**
 * @holiday/core — Query Engine
 *
 * Converts the low-level binary structures produced by the `.hday` parser
 * into high-level {@link DayInfo} DTOs suitable for API consumers.
 *
 * Key responsibilities:
 * - Interpret {@link DayEntry} flag bits using `DAY_FLAGS` from `@holiday/spec`
 * - Resolve holiday names from the name-list table, grouped by locale
 * - Resolve label strings from the name-list table
 * - Compute calendar metadata (date string, day-of-year index, leap year)
 *
 * @module
 */

import type {
  CalendarSystem,
  DayInfo,
  MultiLangNames,
} from '@holiday/spec';
import {
  CALENDAR_SYSTEM_CODES,
  DAY_FLAGS,
  NO_INDEX,
} from '@holiday/spec';

import type {
  DayEntry,
  HdayBundle,
  NameListEntry,
} from './hday-parser.js';

// ---------------------------------------------------------------------------
// Calendar utilities
// ---------------------------------------------------------------------------

/**
 * Determine whether a Gregorian year is a leap year.
 *
 * @param year - The calendar year.
 * @returns `true` if `year` is a leap year.
 */
export function isLeapYear(year: number): boolean {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}

/** Cumulative day counts up to (but not including) each month — non-leap. */
const MONTH_OFFSETS = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334];

/**
 * Compute the 0-based day-of-year index for a Gregorian date.
 *
 * January 1 = 0, February 1 = 31, etc.
 *
 * @param year  - Calendar year (used only for leap-year adjustment).
 * @param month - Month number (1–12).
 * @param day   - Day of month (1–31).
 * @returns 0-based day-of-year index.
 */
export function dayOfYear(year: number, month: number, day: number): number {
  let doy = MONTH_OFFSETS[month - 1] + (day - 1);
  // Add one day for months after February in leap years
  if (month > 2 && isLeapYear(year)) {
    doy += 1;
  }
  return doy;
}

/**
 * Parse an ISO 8601 date string (`YYYY-MM-DD`) into its numeric components.
 *
 * @param dateStr - Date string in `YYYY-MM-DD` format.
 * @returns Tuple of `[year, month, day]`.
 * @throws {Error} If the string does not match the expected format.
 */
export function parseDate(dateStr: string): [number, number, number] {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dateStr);
  if (!match) {
    throw new Error(
      `Invalid date format: "${dateStr}" — expected YYYY-MM-DD`,
    );
  }
  return [Number(match[1]), Number(match[2]), Number(match[3])];
}

/**
 * Format year / month / day into an ISO `YYYY-MM-DD` string.
 *
 * @param year  - Calendar year.
 * @param month - Month (1–12).
 * @param day   - Day of month (1–31).
 * @returns Formatted date string.
 */
export function formatDate(year: number, month: number, day: number): string {
  const y = String(year).padStart(4, '0');
  const m = String(month).padStart(2, '0');
  const d = String(day).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * Compute (month, day) from a 0-based day-of-year index.
 *
 * @param year     - Calendar year.
 * @param dayIndex - 0-based day-of-year index (0 = Jan 1).
 * @returns Tuple of `[month, day]` (month is 1-based).
 */
export function monthDayFromIndex(
  year: number,
  dayIndex: number,
): [number, number] {
  const leap = isLeapYear(year);
  const offsets = [...MONTH_OFFSETS];
  // Adjust for leap year: months after Feb shift by one day
  if (leap) {
    for (let i = 2; i < 12; i++) {
      offsets[i] += 1;
    }
  }

  // Find the last month whose cumulative offset is ≤ dayIndex
  let month = 1;
  for (let i = 11; i >= 0; i--) {
    if (dayIndex >= offsets[i]) {
      month = i + 1;
      break;
    }
  }

  const day = dayIndex - offsets[month - 1] + 1;
  return [month, day];
}

// ---------------------------------------------------------------------------
// Name / label resolution
// ---------------------------------------------------------------------------

/**
 * Resolve the calendar-system string from a numeric code.
 *
 * @param code - Numeric calendar system code from the header.
 * @returns The matching {@link CalendarSystem}, or `"GREGORIAN"` as default.
 */
function resolveCalendarSystem(code: number): CalendarSystem {
  if (code === CALENDAR_SYSTEM_CODES.CHINESE_LUNAR) {
    return 'CHINESE_LUNAR';
  }
  return 'GREGORIAN';
}

/**
 * Resolve holiday names from a name-list entry.
 *
 * Each pair in the name list maps a locale key → localised name. Pairs whose
 * `keyIndex` is `NO_INDEX` (0xFFFF) are labels, not names, and are skipped.
 *
 * @param entry   - The name-list entry (or `undefined` if no names).
 * @param strings - The string pool.
 * @returns A {@link MultiLangNames} map (locale → names array).
 */
export function resolveNames(
  entry: NameListEntry | undefined,
  strings: string[],
): MultiLangNames {
  const result: MultiLangNames = {};
  if (!entry) return result;

  for (const { keyIndex, valueIndex } of entry.pairs) {
    // Skip label entries (key = 0xFFFF)
    if (keyIndex === NO_INDEX) continue;

    const locale = strings[keyIndex];
    const name = strings[valueIndex];
    if (locale === undefined || name === undefined) continue;

    if (!result[locale]) {
      result[locale] = [];
    }
    result[locale].push(name);
  }

  return result;
}

/**
 * Resolve label strings from a name-list entry.
 *
 * Label pairs have `keyIndex === 0xFFFF`; the value index points to the
 * label string (e.g. `"NEW_YEAR"`, `"STATUTORY"`).
 *
 * @param entry   - The name-list entry (or `undefined` if no labels).
 * @param strings - The string pool.
 * @returns An array of label strings.
 */
export function resolveLabels(
  entry: NameListEntry | undefined,
  strings: string[],
): string[] {
  if (!entry) return [];

  const labels: string[] = [];
  for (const { keyIndex, valueIndex } of entry.pairs) {
    if (keyIndex !== NO_INDEX) continue; // only labels
    const label = strings[valueIndex];
    if (label !== undefined) {
      labels.push(label);
    }
  }
  return labels;
}

// ---------------------------------------------------------------------------
// DayInfo construction
// ---------------------------------------------------------------------------

/**
 * Convert a single {@link DayEntry} (plus its associated string / name-list
 * tables) into a {@link DayInfo} DTO.
 *
 * @param entry       - The raw day entry from the DAY_TABLE.
 * @param dateStr     - The date string in `YYYY-MM-DD` format.
 * @param regionCode  - The region code (e.g. `"CN"`).
 * @param calSystem   - Numeric calendar system code from the header.
 * @param strings     - The string pool.
 * @param nameLists   - The name-list table.
 * @returns A fully-populated {@link DayInfo} object.
 */
export function dayEntryToDayInfo(
  entry: DayEntry,
  dateStr: string,
  regionCode: string,
  calSystem: number,
  strings: string[],
  nameLists: NameListEntry[],
): DayInfo {
  const nameList =
    entry.nameListIndex !== NO_INDEX
      ? nameLists[entry.nameListIndex]
      : undefined;

  const labelList =
    entry.labelListIndex !== NO_INDEX
      ? nameLists[entry.labelListIndex]
      : undefined;

  return {
    date: dateStr,
    regionCode,
    calendarSystem: resolveCalendarSystem(calSystem),
    isHoliday: (entry.flags & DAY_FLAGS.IS_HOLIDAY) !== 0,
    isWorkday: (entry.flags & DAY_FLAGS.IS_WORKDAY) !== 0,
    isWeekend: (entry.flags & DAY_FLAGS.IS_WEEKEND) !== 0,
    isStatutoryHoliday: (entry.flags & DAY_FLAGS.IS_STATUTORY_HOLIDAY) !== 0,
    isAdjustedWorkday: (entry.flags & DAY_FLAGS.IS_ADJUSTED_WORKDAY) !== 0,
    holidayNames: resolveNames(nameList, strings),
    labels: resolveLabels(labelList, strings),
    sourceVersion: '',
    extensions: {},
  };
}

/**
 * Query a single date from a parsed bundle.
 *
 * @param bundle  - Parsed `.hday` bundle.
 * @param dateStr - Date in `YYYY-MM-DD` format.
 * @returns The {@link DayInfo} for the date, or `null` if the date falls
 *   outside the bundle's year.
 */
export function queryDay(bundle: HdayBundle, dateStr: string): DayInfo | null {
  const [year, month, day] = parseDate(dateStr);

  if (year !== bundle.header.year) {
    return null;
  }

  const index = dayOfYear(year, month, day);
  if (index < 0 || index >= bundle.days.length) {
    return null;
  }

  return dayEntryToDayInfo(
    bundle.days[index],
    dateStr,
    bundle.header.regionCode,
    bundle.header.calendarSystem,
    bundle.strings,
    bundle.nameLists,
  );
}

/**
 * Query all days in a bundle and return them as a {@link DayInfo} array.
 *
 * @param bundle - Parsed `.hday` bundle.
 * @returns Array of {@link DayInfo} for every day in the year (365 or 366).
 */
export function queryYear(bundle: HdayBundle): DayInfo[] {
  const { year } = bundle.header;
  const results: DayInfo[] = [];

  for (let i = 0; i < bundle.days.length; i++) {
    const [month, day] = monthDayFromIndex(year, i);
    const dateStr = formatDate(year, month, day);
    results.push(
      dayEntryToDayInfo(
        bundle.days[i],
        dateStr,
        bundle.header.regionCode,
        bundle.header.calendarSystem,
        bundle.strings,
        bundle.nameLists,
      ),
    );
  }

  return results;
}
