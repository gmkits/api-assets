/**
 * @holiday/core —— 查询引擎
 *
 * 负责把 `.hday` 解析后的低层结构转换成对外可用的 `DayInfo`，
 * 并为单日、区间、整年查询提供可复用的高性能查询视图。
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

/** 平年每月开始前累计天数。 */
const MONTH_OFFSETS = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334];
/** 闰年每月开始前累计天数。 */
const LEAP_MONTH_OFFSETS = [0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335];
/** 平年 dayIndex -> [month, day] 预计算表。 */
const NON_LEAP_MONTH_DAY_TABLE = buildMonthDayTable(false);
/** 闰年 dayIndex -> [month, day] 预计算表。 */
const LEAP_MONTH_DAY_TABLE = buildMonthDayTable(true);
/** 空扩展对象可安全复用。 */
const EMPTY_EXTENSIONS: Record<string, never> = {};

interface BundleQueryView {
  dayInfos: DayInfo[];
}

const bundleViewCache = new WeakMap<HdayBundle, BundleQueryView>();
const resolvedNameCache = new WeakMap<NameListEntry, MultiLangNames>();
const resolvedLabelCache = new WeakMap<NameListEntry, string[]>();

/**
 * 判断公历年份是否为闰年。
 */
export function isLeapYear(year: number): boolean {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}

function buildMonthDayTable(leap: boolean): Array<[number, number]> {
  const monthLengths = leap
    ? [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
    : [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  const table: Array<[number, number]> = [];

  for (let month = 1; month <= 12; month++) {
    for (let day = 1; day <= monthLengths[month - 1]; day++) {
      table.push([month, day]);
    }
  }

  return table;
}

function getMonthOffsets(year: number): number[] {
  return isLeapYear(year) ? LEAP_MONTH_OFFSETS : MONTH_OFFSETS;
}

function getMonthDayTable(year: number): Array<[number, number]> {
  return isLeapYear(year) ? LEAP_MONTH_DAY_TABLE : NON_LEAP_MONTH_DAY_TABLE;
}

/**
 * 计算日期在当年的 0 基序号。
 */
export function dayOfYear(year: number, month: number, day: number): number {
  return getMonthOffsets(year)[month - 1] + (day - 1);
}

/**
 * 解析 `YYYY-MM-DD` 格式日期。
 */
export function parseDate(dateStr: string): [number, number, number] {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dateStr);
  if (!match) {
    throw new Error(`Invalid date format: "${dateStr}" — expected YYYY-MM-DD`);
  }
  return [Number(match[1]), Number(match[2]), Number(match[3])];
}

/**
 * 格式化为 `YYYY-MM-DD`。
 */
export function formatDate(year: number, month: number, day: number): string {
  const y = String(year).padStart(4, '0');
  const m = String(month).padStart(2, '0');
  const d = String(day).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * 根据当年 0 基序号还原月份和日期。
 */
export function monthDayFromIndex(
  year: number,
  dayIndex: number,
): [number, number] {
  const result = getMonthDayTable(year)[dayIndex];
  if (!result) {
    throw new RangeError(`Invalid dayIndex: ${dayIndex}`);
  }
  return result;
}

function resolveCalendarSystem(code: number): CalendarSystem {
  if (code === CALENDAR_SYSTEM_CODES.CHINESE_LUNAR) {
    return 'CHINESE_LUNAR';
  }
  return 'GREGORIAN';
}

/**
 * 把名称列表解析为 `locale -> 名称数组`。
 */
export function resolveNames(
  entry: NameListEntry | undefined,
  strings: string[],
): MultiLangNames {
  if (!entry) {
    return {};
  }

  const cached = resolvedNameCache.get(entry);
  if (cached) {
    return cached;
  }

  const result: MultiLangNames = {};
  for (const { keyIndex, valueIndex } of entry.pairs) {
    if (keyIndex === NO_INDEX) continue;

    const locale = strings[keyIndex];
    const name = strings[valueIndex];
    if (locale === undefined || name === undefined) continue;

    if (!result[locale]) {
      result[locale] = [];
    }
    result[locale].push(name);
  }

  resolvedNameCache.set(entry, result);
  return result;
}

/**
 * 把标签列表解析为字符串数组。
 */
export function resolveLabels(
  entry: NameListEntry | undefined,
  strings: string[],
): string[] {
  if (!entry) {
    return [];
  }

  const cached = resolvedLabelCache.get(entry);
  if (cached) {
    return cached;
  }

  const labels: string[] = [];
  for (const { keyIndex, valueIndex } of entry.pairs) {
    if (keyIndex !== NO_INDEX) continue;

    const label = strings[valueIndex];
    if (label !== undefined) {
      labels.push(label);
    }
  }

  resolvedLabelCache.set(entry, labels);
  return labels;
}

/**
 * 将单条 day entry 转换为 `DayInfo`。
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
    extensions: EMPTY_EXTENSIONS,
  };
}

function buildBundleQueryView(bundle: HdayBundle): BundleQueryView {
  const { year, regionCode, calendarSystem } = bundle.header;
  const monthDayTable = getMonthDayTable(year);
  const dayInfos = new Array<DayInfo>(bundle.days.length);

  for (let index = 0; index < bundle.days.length; index++) {
    const [month, day] = monthDayTable[index];
    dayInfos[index] = dayEntryToDayInfo(
      bundle.days[index],
      formatDate(year, month, day),
      regionCode,
      calendarSystem,
      bundle.strings,
      bundle.nameLists,
    );
  }

  return { dayInfos };
}

function getBundleQueryView(bundle: HdayBundle): BundleQueryView {
  const cached = bundleViewCache.get(bundle);
  if (cached) {
    return cached;
  }

  const view = buildBundleQueryView(bundle);
  bundleViewCache.set(bundle, view);
  return view;
}

/**
 * 查询单日。
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

  return getBundleQueryView(bundle).dayInfos[index];
}

/**
 * 查询整年。
 */
export function queryYear(bundle: HdayBundle): DayInfo[] {
  return getBundleQueryView(bundle).dayInfos.slice();
}

/**
 * 查询同一年内的连续区间。
 */
export function queryRange(
  bundle: HdayBundle,
  startDayIndex = 0,
  endDayIndex = bundle.days.length - 1,
): DayInfo[] {
  if (startDayIndex > endDayIndex) {
    return [];
  }

  const start = Math.max(0, startDayIndex);
  const end = Math.min(bundle.days.length - 1, endDayIndex);
  if (start > end) {
    return [];
  }

  return getBundleQueryView(bundle).dayInfos.slice(start, end + 1);
}
