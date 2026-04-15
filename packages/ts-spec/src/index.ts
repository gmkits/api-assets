// ============================================================
// Holiday Data Platform — Shared Type Definitions
// ============================================================

// --- Enums ---

/** Classification of a day's status */
export type DayKind =
  | 'STATUTORY_HOLIDAY'
  | 'OFFICIAL_HOLIDAY'
  | 'ADJUSTED_WORKDAY'
  | 'NORMAL_WORKDAY'
  | 'NORMAL_WEEKEND';

/** Type of rule in canonical spec */
export type RuleType =
  | 'FIXED_DATE'
  | 'DATE_RANGE'
  | 'WEEKDAY_OVERRIDE'
  | 'LUNAR_DATE'
  | 'RECURRENCE'
  | 'PATCH';

/** Type of data source */
export type SourceType =
  | 'GOV_NOTICE'
  | 'ICS_FEED'
  | 'THIRD_PARTY_JSON'
  | 'CSV_IMPORT'
  | 'MANUAL_ENTRY'
  | 'ENTERPRISE_PATCH';

/** Calendar system */
export type CalendarSystem = 'GREGORIAN' | 'CHINESE_LUNAR';

/** Standard holiday labels */
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

/** Day of week values */
export type WeekDay = 'MON' | 'TUE' | 'WED' | 'THU' | 'FRI' | 'SAT' | 'SUN';

// --- Constants ---

/** Day flag bit positions for .hday binary format */
export const DAY_FLAGS = {
  IS_HOLIDAY: 1 << 0,
  IS_WORKDAY: 1 << 1,
  IS_WEEKEND: 1 << 2,
  IS_STATUTORY_HOLIDAY: 1 << 3,
  IS_ADJUSTED_WORKDAY: 1 << 4,
  HAS_NAME: 1 << 5,
  HAS_LABEL: 1 << 6,
} as const;

/** Section type codes for .hday binary format */
export const SECTION_TYPES = {
  DAY_TABLE: 0x0001,
  STRING_TABLE: 0x0002,
  NAME_LIST_TABLE: 0x0003,
  EXT_JSON: 0x0004,
} as const;

/** Calendar system numeric codes for .hday binary format */
export const CALENDAR_SYSTEM_CODES: Record<CalendarSystem, number> = {
  GREGORIAN: 0x00,
  CHINESE_LUNAR: 0x01,
};

/** .hday file magic bytes */
export const HDAY_MAGIC = 'HDAY';

/** .hday header size in bytes */
export const HDAY_HEADER_SIZE = 32;

/** .hday section table entry size in bytes */
export const HDAY_SECTION_ENTRY_SIZE = 8;

/** .hday day table entry size in bytes */
export const HDAY_DAY_ENTRY_SIZE = 8;

/** No-index sentinel for .hday name/label/ext indices */
export const NO_INDEX = 0xFFFF;

// --- DTOs ---

/** Multi-language string map: locale → string array */
export interface MultiLangNames {
  [locale: string]: string[];
}

/**
 * Generator information embedded in CommonMeta.
 */
export interface GeneratorInfo {
  /** Generator tool name */
  name: string;
  /** Generator tool version */
  version: string;
}

/**
 * CommonMeta — shared metadata structure used across all data layers.
 * Present in canonical, materialized, and manifest files.
 */
export interface CommonMeta {
  /** Specification version (semver) */
  specVersion: string;
  /** Unique bundle identifier, e.g., "CN-2026" */
  bundleId: string;
  /** Region code (e.g., "CN", "CN-SH") */
  regionCode: string;
  /** Parent region code for inheritance, null if top-level */
  parentRegionCode: string | null;
  /** Calendar year */
  year: number;
  /** Start of validity period (YYYY-MM-DD) */
  validFrom: string;
  /** End of validity period (YYYY-MM-DD) */
  validTo: string;
  /** Primary calendar system */
  calendarSystem: CalendarSystem;
  /** IANA timezone identifier */
  timezone: string;
  /** Default weekend days */
  weekendMask: WeekDay[];
  /** Supported locales */
  locales: string[];
  /** Data source version (CalVer: YYYY.MM.DD) */
  sourceVersion: string;
  /** ISO 8601 timestamp when data was generated */
  generatedAt: string;
  /** Generator tool information */
  generator: GeneratorInfo;
  /** Extension data */
  extensions: Record<string, unknown>;
}

/**
 * DayInfoDTO — the unified day information transfer object.
 * Identical structure across Java, TypeScript, and HTTP API responses.
 */
export interface DayInfo {
  /** Date in YYYY-MM-DD format */
  date: string;
  /** Region code */
  regionCode: string;
  /** Calendar system */
  calendarSystem: CalendarSystem;
  /** Whether this day is a holiday (day off) */
  isHoliday: boolean;
  /** Whether this day is a working day */
  isWorkday: boolean;
  /** Whether this day falls on a default weekend (Sat/Sun) */
  isWeekend: boolean;
  /** Whether this day is a statutory holiday proper (法定节假日) */
  isStatutoryHoliday: boolean;
  /** Whether this day is an adjusted workday (调休补班) */
  isAdjustedWorkday: boolean;
  /** Multi-language holiday names */
  holidayNames: MultiLangNames;
  /** Label tags */
  labels: string[];
  /** Data source version */
  sourceVersion: string;
  /** Extension data */
  extensions: Record<string, unknown>;
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

// --- 农历扩展类型 ---

/**
 * 农历日期信息。
 *
 * <p>用于 DayInfo.extensions 中的 "lunar" 字段，
 * 提供公历日期对应的农历信息。</p>
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
  /** 干支年名（如"乙巳年"）。 */
  ganZhiYear: string;
  /** 生肖。 */
  shengXiao: string;
  /** 月份中文名（如"正月"）。 */
  monthName: string;
  /** 日期中文名（如"初一"）。 */
  dayName: string;
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
