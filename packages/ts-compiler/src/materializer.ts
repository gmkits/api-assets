// ============================================================
// Holiday Data Platform — Materializer
// ============================================================

import type {
  CanonicalDocument,
  MaterializedDay,
  MaterializedYearData,
  HolidayRule,
  MultiLangNames,
  WeekDay,
} from '@holiday/spec';
import {isLeapYear, MONTH_OFFSETS, LEAP_MONTH_OFFSETS} from '@holiday/spec';

export {isLeapYear};

// --- Date Helpers ---

/**
 * Get the number of days in a year (365 or 366).
 */
export function getDaysInYear(year: number): number {
  return isLeapYear(year) ? 366 : 365;
}

/**
 * Convert a YYYY-MM-DD date string to a 0-based day-of-year index.
 */
export function dateToIndex(dateStr: string): number {
  const year = parseInt(dateStr.slice(0, 4), 10);
  const month = parseInt(dateStr.slice(5, 7), 10);
  const day = parseInt(dateStr.slice(8, 10), 10);
    const offsets = isLeapYear(year) ? LEAP_MONTH_OFFSETS : MONTH_OFFSETS;
    return offsets[month - 1] + day - 1;
}

/**
 * Convert a 0-based day-of-year index back to a YYYY-MM-DD string.
 */
export function indexToDate(year: number, index: number): string {
    const offsets = isLeapYear(year) ? LEAP_MONTH_OFFSETS : MONTH_OFFSETS;
    let month = 11;
    while (month > 0 && offsets[month] > index) {
        month--;
  }
    const day = index - offsets[month] + 1;
  const mm = String(month + 1).padStart(2, '0');
    const dd = String(day).padStart(2, '0');
  return `${year}-${mm}-${dd}`;
}

/**
 * Get the WeekDay for a YYYY-MM-DD date string.
 */
export function getWeekday(dateStr: string): WeekDay {
  const d = new Date(dateStr + 'T00:00:00Z');
  const jsDay = d.getUTCDay(); // 0=Sun, 1=Mon, ..., 6=Sat
  const map: WeekDay[] = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
  return map[jsDay];
}

/**
 * Merge multi-language names, deduplicating via Set.
 */
function mergeNames(target: MultiLangNames, source: MultiLangNames): MultiLangNames {
  const result = { ...target };
  for (const [locale, rawNames] of Object.entries(source)) {
    const names: string[] = Array.isArray(rawNames) ? rawNames : [rawNames as unknown as string];
    const existing = result[locale] ?? [];
    const seen = new Set(existing);
    const merged = [...existing];
    for (const name of names) {
      if (!seen.has(name)) { seen.add(name); merged.push(name); }
    }
    result[locale] = merged;
  }
  return result;
}

/**
 * Merge label arrays, deduplicating via Set.
 */
function mergeLabels(existing: string[], incoming: string[]): string[] {
  const seen = new Set(existing);
  const result = [...existing];
  for (const label of incoming) {
    if (!seen.has(label)) { seen.add(label); result.push(label); }
  }
  return result;
}

/**
 * Collect all dates a rule applies to (index arithmetic, no Date allocation for ranges).
 */
function expandRuleDates(rule: HolidayRule): string[] {
  if (rule.type === 'FIXED_DATE' && rule.date) {
    return [rule.date];
  }
  if (rule.type === 'DATE_RANGE' && rule.from && rule.to) {
    const fromYear = parseInt(rule.from.slice(0, 4), 10);
    const startIdx = dateToIndex(rule.from);
    const endIdx = dateToIndex(rule.to);
    const dates: string[] = [];
    for (let i = startIdx; i <= endIdx; i++) {
      dates.push(indexToDate(fromYear, i));
    }
    return dates;
  }
  return [];
}

/**
 * Apply a single rule to a day, mutating the day entry.
 */
function applyRule(day: MaterializedDay, rule: HolidayRule): void {
  switch (rule.dayKind) {
    case 'OFFICIAL_HOLIDAY':
      day.isHoliday = true;
      day.isWorkday = false;
      break;
    case 'STATUTORY_HOLIDAY':
      day.isHoliday = true;
      day.isWorkday = false;
      day.isStatutoryHoliday = true;
      break;
    case 'ADJUSTED_WORKDAY':
      day.isHoliday = false;
      day.isWorkday = true;
      day.isAdjustedWorkday = true;
      break;
    case 'NORMAL_WORKDAY':
      day.isHoliday = false;
      day.isWorkday = true;
      break;
    case 'NORMAL_WEEKEND':
      day.isHoliday = false;
      day.isWorkday = false;
      break;
  }

  day.holidayNames = mergeNames(day.holidayNames, rule.displayNames);
  day.labels = mergeLabels(day.labels, rule.labels);
}

/**
 * Materialize a CanonicalDocument into a full year of day-by-day data.
 *
 * The materializer expands all rules across the year's date range,
 * applying weekend defaults first, then rules in order, then overrides.
 *
 * @param doc - The canonical document to materialize
 * @returns Fully expanded MaterializedYearData with one entry per day
 *
 * @example
 * ```ts
 * const yearData = materialize(canonicalDoc);
 * console.log(yearData.days['2025-01-01'].isHoliday); // true
 * ```
 */
export function materialize(doc: CanonicalDocument): MaterializedYearData {
  const { meta, rules, overrides } = doc;
  const { year, weekendMask } = meta;
  const totalDays = getDaysInYear(year);
  const weekendSet = new Set(weekendMask);

  const days: Record<string, MaterializedDay> = {};

  // Initialize all days with defaults based on weekendMask
  for (let i = 0; i < totalDays; i++) {
    const dateStr = indexToDate(year, i);
    const weekday = getWeekday(dateStr);
    const isWeekend = weekendSet.has(weekday);

    days[dateStr] = {
      // “holiday”统一表示无需工作的休息日，包含自然周末和放假安排。
      // 是否属于官方放假安排可由 holidayNames/labels 判断。
      isHoliday: isWeekend,
      isWorkday: !isWeekend,
      isWeekend,
      isStatutoryHoliday: false,
      isAdjustedWorkday: false,
      holidayNames: {},
      labels: [],
    };
  }

  // Apply rules in order
  for (const rule of rules) {
    const dates = expandRuleDates(rule);
    for (const dateStr of dates) {
      if (days[dateStr]) {
        applyRule(days[dateStr], rule);
      }
    }
  }

  // Apply overrides last (they take precedence)
  for (const rule of overrides) {
    const dates = expandRuleDates(rule);
    for (const dateStr of dates) {
      if (days[dateStr]) {
        applyRule(days[dateStr], rule);
      }
    }
  }

  return {
    meta,
    days,
  };
}
