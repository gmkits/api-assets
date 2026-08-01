/**
 * @holiday/core —— 查询引擎
 *
 * 负责把 `.hday` 解析后的低层结构转换成对外可用的 `DayInfo`，
 * 并为单日、区间、整年查询提供可复用的高性能查询视图。
 */

import type {
  CalendarSystem,
  ChineseLocale,
  DayInfo,
  GanZhiInfo,
  LunarDateInfo,
  MultiLangNames,
} from '@holiday/spec';
import {
  LUNAR_END_YEAR,
  LUNAR_START_YEAR,
  lunarToSolar,
  monthDays,
  solarToLunar,
  solarYearToLunar,
  getDiZhi,
  getGanZhi,
  getShengXiao,
  getTianGan,
} from '@holiday/lunar';
import type { LunarInfo as RawLunarInfo } from '@holiday/lunar';
import {
  CALENDAR_SYSTEM_CODES,
  DAY_FLAGS,
  LEAP_MONTH_OFFSETS,
  NO_INDEX,
  MONTH_OFFSETS,
  isLeapYear,
} from '@holiday/spec';

import type {
  DayEntry,
  HdayBundle,
  NameListEntry,
} from './hday-parser.js';
import { lookupSolarTerm } from './solar-terms.js';
import { resolveFestivals } from './festivals.js';

/** 平年 dayIndex -> [month, day] 预计算表。 */
const NON_LEAP_MONTH_DAY_TABLE = buildMonthDayTable(false);
/** 闰年 dayIndex -> [month, day] 预计算表。 */
const LEAP_MONTH_DAY_TABLE = buildMonthDayTable(true);
/** `YYYY-MM-DD` 日期格式校验。 */
const DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;
/** 农历支持的最早公历日期。 */
const LUNAR_MIN_SOLAR_MONTH = 1;
/** 农历支持的最早公历日期。 */
const LUNAR_MIN_SOLAR_DAY = 31;
/** 农历支持的最晚公历日期，安装资产后按需计算一次。 */
let lunarMaxSolarDate: [number, number, number] | undefined;

function getLunarMaxSolarDate(): [number, number, number] {
  if (!lunarMaxSolarDate) {
    lunarMaxSolarDate = lunarToSolar(
      LUNAR_END_YEAR,
      12,
      monthDays(LUNAR_END_YEAR, 12),
    );
  }
  return lunarMaxSolarDate;
}

interface BundleQueryView {
  dayInfos: DayInfo[];
  workdayPrefix: Uint16Array;
  nextStatutoryIndex: Int16Array;
}

const bundleViewCache = new WeakMap<HdayBundle, Map<string, BundleQueryView>>();
const resolvedNameCache = new WeakMap<NameListEntry, MultiLangNames>();
const resolvedLabelCache = new WeakMap<NameListEntry, string[]>();

export {isLeapYear};

function buildMonthDayTable(leap: boolean): Array<[number, number]> {
  const monthLengths = leap
    ? [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
    : [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  const table = new Array<[number, number]>(leap ? 366 : 365);
  let index = 0;

  for (let month = 1; month <= 12; month++) {
    for (let day = 1; day <= monthLengths[month - 1]; day++) {
      table[index++] = [month, day];
    }
  }

  return table;
}

function getMonthOffsets(year: number): readonly number[] {
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
  const match = DATE_PATTERN.exec(dateStr);
  if (!match) {
    throw new Error(`日期格式错误: "${dateStr}"，应为 YYYY-MM-DD`);
  }
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const monthLengths = isLeapYear(year)
    ? [31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
    : [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  if (month < 1 || month > 12 || day < 1 || day > monthLengths[month - 1]) {
    throw new RangeError(`日期不存在: "${dateStr}"`);
  }
  return [year, month, day];
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
      throw new RangeError(`dayIndex 超出范围: ${dayIndex}`);
  }
  return result;
}

function resolveCalendarSystem(code: number): CalendarSystem {
  if (code === CALENDAR_SYSTEM_CODES.CHINESE_LUNAR) {
    return 'CHINESE_LUNAR';
  }
  return 'GREGORIAN';
}

function isSolarDateWithinLunarRange(
  year: number,
  month: number,
  day: number,
): boolean {
  const [maxYear, maxMonth, maxDay] = getLunarMaxSolarDate();
  if (year < LUNAR_START_YEAR || year > maxYear) {
    return false;
  }
  if (
    year === LUNAR_START_YEAR &&
    (month < LUNAR_MIN_SOLAR_MONTH ||
      (month === LUNAR_MIN_SOLAR_MONTH && day < LUNAR_MIN_SOLAR_DAY))
  ) {
    return false;
  }
  if (
    year === maxYear &&
    (month > maxMonth || (month === maxMonth && day > maxDay))
  ) {
    return false;
  }
  return true;
}

function buildLunarInfo(
  year: number,
  month: number,
  day: number,
  locale: ChineseLocale,
): LunarDateInfo | null {
  if (!isSolarDateWithinLunarRange(year, month, day)) {
    return null;
  }
  const lunar = solarToLunar(year, month, day, locale);
  return {
    year: lunar.year,
    month: lunar.month,
    day: lunar.day,
    isLeapMonth: lunar.isLeapMonth,
    monthName: lunar.monthName,
    dayName: lunar.dayName,
  };
}

function buildGanZhiInfo(
  lunar: LunarDateInfo | null,
  locale: ChineseLocale,
): GanZhiInfo | null {
  if (!lunar) return null;
  return {
    yearName: getGanZhi(lunar.year),
    heavenlyStem: getTianGan(lunar.year),
    earthlyBranch: getDiZhi(lunar.year),
    zodiac: getShengXiao(lunar.year, locale),
  };
}

interface ChineseCalendarFields {
  lunar: LunarDateInfo | null;
  solarTerm: DayInfo['solarTerm'];
  ganZhi: GanZhiInfo | null;
}

function buildChineseCalendarFields(
  year: number,
  month: number,
  day: number,
  dayIndex: number,
  locale: ChineseLocale = 'zh-CN',
  rawLunar?: RawLunarInfo | null,
): ChineseCalendarFields {
  const lunar = rawLunar === undefined
    ? buildLunarInfo(year, month, day, locale)
    : rawLunar === null
      ? null
      : {
          year: rawLunar.year,
          month: rawLunar.month,
          day: rawLunar.day,
          isLeapMonth: rawLunar.isLeapMonth,
          monthName: rawLunar.monthName,
          dayName: rawLunar.dayName,
        };
  const solarTerm = lookupSolarTerm(year, dayIndex, locale);
  return {
    lunar,
    solarTerm: solarTerm ?? null,
    ganZhi: buildGanZhiInfo(lunar, locale),
  };
}

/**
 * 把名称列表解析为 `locale -> 名称数组`。
 */
export function resolveNames(
  entry: NameListEntry | undefined,
  strings: ReadonlyArray<string>,
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
  strings: ReadonlyArray<string>,
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
  strings: ReadonlyArray<string>,
  nameLists: ReadonlyArray<NameListEntry>,
  calendarFields: ChineseCalendarFields = {
    lunar: null,
    solarTerm: null,
    ganZhi: null,
  },
  sourceVersion = '',
): DayInfo {
  const nameList =
    entry.nameListIndex !== NO_INDEX
      ? nameLists[entry.nameListIndex]
      : undefined;

  const labelList =
    entry.labelListIndex !== NO_INDEX
      ? nameLists[entry.labelListIndex]
      : undefined;
  const holidayNames = resolveNames(nameList, strings);

  return {
    date: dateStr,
    regionCode,
    calendarSystem: resolveCalendarSystem(calSystem),
    isHoliday: (entry.flags & DAY_FLAGS.IS_HOLIDAY) !== 0,
    isOfficialHoliday:
      (entry.flags & DAY_FLAGS.IS_HOLIDAY) !== 0
      && Object.keys(holidayNames).length > 0,
    isWorkday: (entry.flags & DAY_FLAGS.IS_WORKDAY) !== 0,
    isWeekend: (entry.flags & DAY_FLAGS.IS_WEEKEND) !== 0,
    isStatutoryHoliday: (entry.flags & DAY_FLAGS.IS_STATUTORY_HOLIDAY) !== 0,
    isAdjustedWorkday: (entry.flags & DAY_FLAGS.IS_ADJUSTED_WORKDAY) !== 0,
    holidayNames,
    labels: resolveLabels(labelList, strings),
    lunar: calendarFields.lunar,
    solarTerm: calendarFields.solarTerm,
    ganZhi: calendarFields.ganZhi,
    festivals: resolveFestivals(
      dateStr,
      calendarFields.lunar,
      calendarFields.solarTerm,
    ),
    sourceVersion,
  };
}

function buildBundleQueryView(bundle: HdayBundle, locale: ChineseLocale): BundleQueryView {
  const { year, regionCode, calendarSystem } = bundle.header;
  const monthDayTable = getMonthDayTable(year);
  const dayInfos = new Array<DayInfo>(bundle.days.length);
  const workdayPrefix = new Uint16Array(bundle.days.length + 1);
  const nextStatutoryIndex = new Int16Array(bundle.days.length);
  let lunarYear: RawLunarInfo[] | null = null;
  try {
    lunarYear = solarYearToLunar(year, locale);
  } catch {
    // 完整年度超出精确农历范围时保持原有 null 字段语义。
  }

  for (let index = 0; index < bundle.days.length; index++) {
    const [month, day] = monthDayTable[index];
    const calendarFields = buildChineseCalendarFields(
      year,
      month,
      day,
      index,
      locale,
      lunarYear === null ? undefined : lunarYear[index],
    );
    const nameIndex = bundle.days.nameListIndexes[index];
    const labelIndex = bundle.days.labelListIndexes[index];
    const holidayNames = nameIndex < 0
      ? {}
      : bundle.names[nameIndex] as MultiLangNames;
    const labels = labelIndex < 0
      ? []
      : bundle.labels[labelIndex] as string[];
    const isHoliday = hasBit(bundle.days.holidayBits, index);
    const date = formatDate(year, month, day);
    dayInfos[index] = {
      date,
      regionCode,
      calendarSystem: resolveCalendarSystem(calendarSystem),
      isHoliday,
      isOfficialHoliday:
        isHoliday && Object.keys(holidayNames).length > 0,
      isWorkday: hasBit(bundle.days.workdayBits, index),
      isWeekend: hasBit(bundle.days.weekendBits, index),
      isStatutoryHoliday: hasBit(bundle.days.statutoryBits, index),
      isAdjustedWorkday: hasBit(bundle.days.adjustedBits, index),
      holidayNames,
      labels,
      lunar: calendarFields.lunar,
      solarTerm: calendarFields.solarTerm,
      ganZhi: calendarFields.ganZhi,
      festivals: resolveFestivals(
        date,
        calendarFields.lunar,
        calendarFields.solarTerm,
      ),
      sourceVersion: bundle.metadata?.sourceVersion ?? '',
    };
    workdayPrefix[index + 1] =
      workdayPrefix[index] + (dayInfos[index].isWorkday ? 1 : 0);
  }

  let next = -1;
  for (let index = dayInfos.length - 1; index >= 0; index--) {
    if (dayInfos[index].isStatutoryHoliday) next = index;
    nextStatutoryIndex[index] = next;
  }

  return { dayInfos, workdayPrefix, nextStatutoryIndex };
}

function hasBit(words: Uint32Array, index: number): boolean {
  return (words[index >>> 5] & (1 << (index & 31))) !== 0;
}

function getBundleQueryView(bundle: HdayBundle, locale: ChineseLocale = 'zh-CN'): BundleQueryView {
  let localeMap = bundleViewCache.get(bundle);
  if (!localeMap) {
    localeMap = new Map();
    bundleViewCache.set(bundle, localeMap);
  }
  let view = localeMap.get(locale);
  if (!view) {
    view = buildBundleQueryView(bundle, locale);
    localeMap.set(locale, view);
  }
  return view;
}

/**
 * 查询单日。
 */
export function queryDay(bundle: HdayBundle, dateStr: string, locale?: ChineseLocale): DayInfo | null {
  const [year, month, day] = parseDate(dateStr);
  if (year !== bundle.header.year) return null;
  const index = dayOfYear(year, month, day);
  if (index < 0 || index >= bundle.days.length) return null;
  return getBundleQueryView(bundle, locale).dayInfos[index];
}

/**
 * 查询整年。
 */
export function queryYear(bundle: HdayBundle, locale?: ChineseLocale): DayInfo[] {
  return getBundleQueryView(bundle, locale).dayInfos.slice();
}

/**
 * 查询同一年内的连续区间。
 */
export function queryRange(
  bundle: HdayBundle,
  startDayIndex = 0,
  endDayIndex = bundle.days.length - 1,
  locale?: ChineseLocale,
): DayInfo[] {
  const start = Math.max(0, startDayIndex);
  const end = Math.min(bundle.days.length - 1, endDayIndex);
  if (start > end) return [];
  return getBundleQueryView(bundle, locale).dayInfos.slice(start, end + 1);
}

/**
 * 统计同一年闭区间内的工作日数量，使用前缀和避免逐日扫描。
 */
export function countBundleWorkdays(
  bundle: HdayBundle,
  startDayIndex = 0,
  endDayIndex = bundle.days.length - 1,
  locale?: ChineseLocale,
): number {
  const start = Math.max(0, startDayIndex);
  const end = Math.min(bundle.days.length - 1, endDayIndex);
  if (start > end) return 0;
  const prefix = getBundleQueryView(bundle, locale).workdayPrefix;
  return prefix[end + 1] - prefix[start];
}

/**
 * 从年内索引开始 O(1) 查询下一个法定节假日。
 */
export function findBundleStatutoryHoliday(
  bundle: HdayBundle,
  startDayIndex = 0,
  locale?: ChineseLocale,
): DayInfo | null {
  const start = Math.max(0, startDayIndex);
  if (start >= bundle.days.length) return null;
  const view = getBundleQueryView(bundle, locale);
  const index = view.nextStatutoryIndex[start];
  return index < 0 ? null : view.dayInfos[index];
}
