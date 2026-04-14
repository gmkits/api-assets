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

// --- Date Helpers ---

/**
 * Check if a year is a leap year.
 */
export function isLeapYear(year: number): boolean {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}

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

  const daysInMonth = [31, isLeapYear(year) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  let index = 0;
  for (let m = 0; m < month - 1; m++) {
    index += daysInMonth[m];
  }
  return index + day - 1;
}

/**
 * Convert a 0-based day-of-year index back to a YYYY-MM-DD string.
 */
export function indexToDate(year: number, index: number): string {
  const daysInMonth = [31, isLeapYear(year) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  let remaining = index;
  let month = 0;
  while (month < 12 && remaining >= daysInMonth[month]) {
    remaining -= daysInMonth[month];
    month++;
  }
  const mm = String(month + 1).padStart(2, '0');
  const dd = String(remaining + 1).padStart(2, '0');
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
 * Merge multi-language names, appending new names to existing ones.
 */
function mergeNames(target: MultiLangNames, source: MultiLangNames): MultiLangNames {
  const result = { ...target };
  for (const [locale, names] of Object.entries(source)) {
    const existing = result[locale] ?? [];
    const merged = [...existing];
    for (const name of names) {
      if (!merged.includes(name)) {
        merged.push(name);
      }
    }
    result[locale] = merged;
  }
  return result;
}

/**
 * Merge label arrays, deduplicating.
 */
function mergeLabels(existing: string[], incoming: string[]): string[] {
  const result = [...existing];
  for (const label of incoming) {
    if (!result.includes(label)) {
      result.push(label);
    }
  }
  return result;
}

/**
 * Collect all dates a rule applies to.
 */
function expandRuleDates(rule: HolidayRule): string[] {
  if (rule.type === 'FIXED_DATE' && rule.date) {
    return [rule.date];
  }
  if (rule.type === 'DATE_RANGE' && rule.from && rule.to) {
    const dates: string[] = [];
    const start = new Date(rule.from + 'T00:00:00Z');
    const end = new Date(rule.to + 'T00:00:00Z');
    for (let d = new Date(start); d <= end; d.setUTCDate(d.getUTCDate() + 1)) {
      dates.push(d.toISOString().slice(0, 10));
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
      isHoliday: false,
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
