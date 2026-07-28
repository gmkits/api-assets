// ============================================================
// 节假日平台 —— 共享类型定义
// ============================================================

// --- 枚举 ---

/** 单日状态分类。 */
export type DayKind =
  | 'STATUTORY_HOLIDAY'
  | 'OFFICIAL_HOLIDAY'
  | 'ADJUSTED_WORKDAY'
  | 'NORMAL_WORKDAY'
  | 'NORMAL_WEEKEND';

/** Canonical 规则类型。 */
export type RuleType =
  | 'FIXED_DATE'
  | 'DATE_RANGE'
  | 'WEEKDAY_OVERRIDE'
  | 'LUNAR_DATE'
  | 'RECURRENCE'
  | 'PATCH';

/** 数据来源类型。 */
export type SourceType =
  | 'GOV_NOTICE'
  | 'ICS_FEED'
  | 'THIRD_PARTY_JSON'
  | 'CSV_IMPORT'
  | 'MANUAL_ENTRY'
  | 'ENTERPRISE_PATCH';

/** 历法体系。 */
export type CalendarSystem = 'GREGORIAN' | 'CHINESE_LUNAR';

/** 标准节假日标签。 */
export type HolidayLabel =
  | 'NEW_YEAR'
  | 'SPRING_FESTIVAL'
  | 'TOMB_SWEEPING'
  | 'LABOUR_DAY'
  | 'DRAGON_BOAT'
  | 'MID_AUTUMN'
  | 'NATIONAL_DAY'
  | 'STATUTORY'
  | 'ADJUSTED_WORKDAY'
  | 'BRIDGE_DAY';

/** 星期枚举。 */
export type WeekDay = 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT' | 'SUN';

/** 中文 locale：简体（默认）或繁体。 */
export type ChineseLocale = 'zh-CN' | 'zh-TW';

// --- 常量 ---

/** `.hday` 二进制格式中的日期位标记。 */
export const DAY_FLAGS = {
  IS_HOLIDAY: 1 << 0,
  IS_WORKDAY: 1 << 1,
  IS_WEEKEND: 1 << 2,
  IS_STATUTORY_HOLIDAY: 1 << 3,
  IS_ADJUSTED_WORKDAY: 1 << 4,
  HAS_NAME: 1 << 5,
  HAS_LABEL: 1 << 6,
} as const;

/** `.hday` 二进制格式中的历法数值编码。 */
export const CALENDAR_SYSTEM_CODES: Record<CalendarSystem, number> = {
  GREGORIAN: 0x00,
  CHINESE_LUNAR: 0x01,
};

export * from './hday-format.js';

// --- DTO ---

/** 多语言字符串映射：`locale -> 字符串数组`。 */
export interface MultiLangNames {
  [locale: string]: string[];
}

/**
 * `CommonMeta` 中的生成器信息。
 */
export interface GeneratorInfo {
  /** 生成工具名称。 */
  name: string;
  /** 生成工具版本。 */
  version: string;
}

/**
 * `CommonMeta` 是多种数据层共用的元数据头。
 *
 * <p>可出现在 canonical、materialized 与 manifest 等结构中。</p>
 */
export interface CommonMeta {
  /** 规范版本号。 */
  specVersion: string;
  /** 唯一 bundle 标识，例如 `CN-2026`。 */
  bundleId: string;
  /** 地区代码，例如 `CN`、`CN-SH`。 */
  regionCode: string;
  /** 继承用父地区代码；顶级地区为 `null`。 */
  parentRegionCode: string | null;
  /** 公历年份。 */
  year: number;
  /** 生效起始日期。 */
  validFrom: string;
  /** 生效结束日期。 */
  validTo: string;
  /** 主历法体系。 */
  calendarSystem: CalendarSystem;
  /** IANA 时区标识。 */
  timezone: string;
  /** 默认周末掩码。 */
  weekendMask: WeekDay[];
  /** 支持的语言列表。 */
  locales: string[];
  /** 数据源版本（CalVer）。 */
  sourceVersion: string;
  /** 生成时间。 */
  generatedAt: string;
  /** 生成工具信息。 */
  generator: GeneratorInfo;
  /** 扩展数据。 */
  extensions: Record<string, unknown>;
}

/**
 * TypeScript 侧统一日信息对象。
 *
 * <p>它与 Java/HTTP 侧保持语义对齐，但字段命名采用
 * `isHoliday/isWorkday/...` 这一 TypeScript 风格。</p>
 */
export interface DayInfo {
  /** 日期，格式为 `YYYY-MM-DD`。 */
  date: string;
  /** 地区代码。 */
  regionCode: string;
  /** 历法体系。 */
  calendarSystem: CalendarSystem;
  /** 是否为休息日；包含自然周末和官方放假安排。 */
  isHoliday: boolean;
  /** 是否属于官方公布的放假安排。 */
  isOfficialHoliday: boolean;
  /** 是否为工作日。 */
  isWorkday: boolean;
  /** 是否为默认周末。 */
  isWeekend: boolean;
  /** 是否为法定节假日。 */
  isStatutoryHoliday: boolean;
  /** 是否为调休补班。 */
  isAdjustedWorkday: boolean;
  /** 多语言节假日名称。 */
  holidayNames: MultiLangNames;
  /** 标签列表。 */
  labels: string[];
  /** 对应农历日期；超出离线农历资产范围时为 null。 */
  lunar: LunarDateInfo | null;
  /** 当天命中的二十四节气；非节气日为 null。 */
  solarTerm: SolarTermInfo | null;
  /** 农历年的天干、地支、干支纪年与生肖。 */
  ganZhi: GanZhiInfo | null;
  /** 当天命中的传统节日、公共节日和纪念日；不代表一定放假。 */
  festivals: FestivalInfo[];
  /** 数据版本。 */
  sourceVersion: string;
}

/** 与工作日状态相互独立的节日或纪念日。 */
export interface FestivalInfo {
  /** 稳定的大写英文代码。 */
  code: string;
  /** 多语言显示名称。 */
  names: Record<string, string>;
}

// --- Canonical Types ---

/** Source provenance record */
export interface SourceRecord {
  /** Unique source identifier */
  id: string;
  /** Source type */
  type: SourceType;
  /** Human-readable title */
  title: string;
  /** Source URL (optional) */
  url?: string;
  /** Publication date (YYYY-MM-DD) */
  publishedAt: string;
}

/** A holiday rule in canonical format */
export interface HolidayRule {
  /** Unique rule identifier */
  id: string;
  /** Rule type */
  type: RuleType;
  /** Day classification */
  dayKind: DayKind;
  /** Multi-language display names */
  displayNames: MultiLangNames;
  /** Label tags */
  labels: string[];
  /** References to source records by ID */
  sourceRefs: string[];
  /** Specific date for FIXED_DATE rules (YYYY-MM-DD) */
  date?: string;
  /** Range start for DATE_RANGE rules (YYYY-MM-DD) */
  from?: string;
  /** Range end for DATE_RANGE rules (YYYY-MM-DD) */
  to?: string;
  /** Calendar system for LUNAR_DATE rules */
  calendarSystem?: CalendarSystem;
  /** Lunar month for LUNAR_DATE rules */
  month?: number;
  /** Lunar day for LUNAR_DATE rules */
  day?: number;
}

/** Complete canonical document */
export interface CanonicalDocument {
  /** Metadata */
  meta: CommonMeta;
  /** Data source provenance records */
  sources: SourceRecord[];
  /** Holiday rules */
  rules: HolidayRule[];
  /** Exception overrides */
  overrides: HolidayRule[];
  /** Extension data */
  extensions: Record<string, unknown>;
}

// --- Materialized Types ---

/** A single day's materialized data (expanded from rules) */
export interface MaterializedDay {
  /** Whether this day is a holiday (day off) */
  isHoliday: boolean;
  /** Whether this day is a working day */
  isWorkday: boolean;
  /** Whether this day falls on a default weekend */
  isWeekend: boolean;
  /** Whether this day is a statutory holiday */
  isStatutoryHoliday: boolean;
  /** Whether this day is an adjusted workday */
  isAdjustedWorkday: boolean;
  /** Multi-language holiday names */
  holidayNames: MultiLangNames;
  /** Label tags */
  labels: string[];
}

/** Complete materialized year data */
export interface MaterializedYearData {
  /** Metadata */
  meta: CommonMeta;
  /** Map of date string (YYYY-MM-DD) to day data */
  days: Record<string, MaterializedDay>;
}

// --- Manifest Types ---

/** A single bundle entry in the manifest */
export interface BundleEntry {
  /** Path to .hday file relative to manifest */
  file: string;
  /** SHA-256 hash of the .hday file */
  sha256: string;
  /** CRC32 checksum of the .hday file */
  crc32: string;
  /** Data source version */
  sourceVersion: string;
  /** File size in bytes */
  size: number;
}

/** Complete manifest document */
export interface Manifest {
  /** Specification version */
  specVersion: string;
  /** Bundle format version */
  bundleFormatVersion: string;
  /** Default region code */
  defaultRegion: string;
  /** ISO 8601 publish timestamp */
  publishedAt: string;
  /** Nested map: regionCode → year → BundleEntry */
  bundles: Record<string, Record<string, BundleEntry>>;
}

// --- 中国历法类型 ---

/**
 * 农历日期信息。
 *
 * <p>由 DayInfo.lunar 直接提供，仅描述农历日期；干支和生肖
 * 由 DayInfo.ganZhi 独立提供。</p>
 */
export interface LunarDateInfo {
  /** 农历年。 */
  year: number;
  /** 农历月（1-12）。 */
  month: number;
  /** 农历日（1-30）。 */
  day: number;
  /** 是否闰月。 */
  isLeapMonth: boolean;
  /** 月份中文名（如"正月"）。 */
  monthName: string;
  /** 日期中文名（如"初一"）。 */
  dayName: string;
}

/**
 * 节气信息。
 *
 * <p>由 DayInfo.solarTerm 直接提供，非节气日为 null。</p>
 */
export interface SolarTermInfo {
  /** 稳定节气索引（0-23）。 */
  index: number;
  /** 节气中文名。 */
  name: string;
}

/**
 * 中国农历年的干支与生肖属性。
 *
 * <p>采用农历年边界；春节当天进入新的干支年。本库不返回存在
 * 流派与换日边界差异的干支月、干支日。</p>
 */
export interface GanZhiInfo {
  /** 不带“年”后缀的干支纪年，如“乙巳”。 */
  yearName: string;
  /** 年干，如“乙”。 */
  heavenlyStem: string;
  /** 年支，如“巳”。 */
  earthlyBranch: string;
  /** 生肖，如“蛇”。 */
  zodiac: string;
}

// --- 公历日期工具 ---

/** 平年每月开始前累计天数。 */
export const MONTH_OFFSETS = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334] as const;

/** 闰年每月开始前累计天数。 */
export const LEAP_MONTH_OFFSETS = [0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335] as const;

/** 判断公历年份是否为闰年。 */
export function isLeapYear(year: number): boolean {
    return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}
