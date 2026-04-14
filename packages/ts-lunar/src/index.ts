/**
 * @holiday/lunar —— 中国农历转换模块
 *
 * 基于香港天文台（HKO）和紫金山天文台数据，覆盖 1900-2100 年的公历↔农历转换。
 * 每年仅用一个 20-bit 整数编码，201 年数据总共 ~800 字节，适合嵌入式和浏览器端使用。
 *
 * 编码格式（每年一个整数，最多 20 有效位）：
 *   bit 0-3  : 闰月月份（0 = 无闰月，1-12 = 闰几月）
 *   bit 4    : 闰月天数（0 = 29 天 / 小月，1 = 30 天 / 大月）
 *   bit 5-16 : 正常 1-12 月的天数（0 = 29 天，1 = 30 天），bit5 = 一月
 *
 * 农历新年（正月初一）的公历日期通过累加天数推算，
 * 基准日为 1900-01-31（庚子年正月初一）。
 *
 * @packageDocumentation
 */

// ===================================================================
// 农历数据表（1900-2100，共 201 年，每年一个压缩整数）
// 数据来源：香港天文台 / 紫金山天文台天文年历
// ===================================================================

const LUNAR_INFO: number[] = [
  0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
  0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
  0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
  0x06566, 0x0d4a0, 0x0ea50, 0x16a95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
  0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
  0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
  0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
  0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
  0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
  0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
  0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
  0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
  0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
  0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
  0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
  0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06aa0, 0x1a6c4, 0x0aae0,
  0x092e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
  0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
  0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
  0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a4d0, 0x0d150, 0x0f252,
  0x0d520,
];

/** 数据覆盖范围起始年。 */
export const LUNAR_START_YEAR = 1900;

/** 数据覆盖范围结束年。 */
export const LUNAR_END_YEAR = 2100;

/** 基准日：1900-01-31，庚子年正月初一对应的 Date 毫秒时间戳。 */
const BASE_DATE_MS = Date.UTC(1900, 0, 31);

/** 每天的毫秒数。 */
const MS_PER_DAY = 86400000;

// ===================================================================
// 天干地支
// ===================================================================

/** 十天干。 */
const TIAN_GAN = ['甲', '乙', '丙', '丁', '戊', '己', '庚', '辛', '壬', '癸'] as const;

/** 十二地支。 */
const DI_ZHI = ['子', '丑', '寅', '卯', '辰', '巳', '午', '未', '申', '酉', '戌', '亥'] as const;

/** 十二生肖。 */
const SHENG_XIAO = ['鼠', '牛', '虎', '兔', '龙', '蛇', '马', '羊', '猴', '鸡', '狗', '猪'] as const;

/** 农历月份名。 */
const MONTH_NAMES = ['正', '二', '三', '四', '五', '六', '七', '八', '九', '十', '冬', '腊'] as const;

/** 农历日期名。 */
const DAY_NAMES = [
  '初一', '初二', '初三', '初四', '初五', '初六', '初七', '初八', '初九', '初十',
  '十一', '十二', '十三', '十四', '十五', '十六', '十七', '十八', '十九', '二十',
  '廿一', '廿二', '廿三', '廿四', '廿五', '廿六', '廿七', '廿八', '廿九', '三十',
] as const;

/** 闰月大月位掩码（bit 16）。 */
const LEAP_MONTH_BIG_MASK = 0x10000;

// ===================================================================
// 农历信息查询（位运算解码）
// ===================================================================

/**
 * 获取指定农历年的闰月月份。
 * @returns 闰月月份（1-12），0 表示该年无闰月。
 */
export function leapMonth(lunarYear: number): number {
  validateYear(lunarYear);
  return LUNAR_INFO[lunarYear - LUNAR_START_YEAR] & 0xf;
}

/**
 * 获取指定农历年闰月的天数。
 * @returns 29 或 30，若无闰月返回 0。
 */
export function leapMonthDays(lunarYear: number): number {
  if (leapMonth(lunarYear) === 0) return 0;
  return (LUNAR_INFO[lunarYear - LUNAR_START_YEAR] & LEAP_MONTH_BIG_MASK) !== 0 ? 30 : 29;
}

/**
 * 获取指定农历年某月的天数（不含闰月）。
 * @param month 月份 1-12
 * @returns 29 或 30
 */
export function monthDays(lunarYear: number, month: number): number {
  validateYear(lunarYear);
  if (month < 1 || month > 12) {
    throw new RangeError(`月份超出范围: ${month}，应为 1-12`);
  }
  return (LUNAR_INFO[lunarYear - LUNAR_START_YEAR] & (LEAP_MONTH_BIG_MASK >> month)) !== 0 ? 30 : 29;
}

/**
 * 获取指定农历年的总天数。
 */
export function yearDays(lunarYear: number): number {
  validateYear(lunarYear);
  let total = 0;
  const info = LUNAR_INFO[lunarYear - LUNAR_START_YEAR];

  // 12 个正常月
  for (let m = 1; m <= 12; m++) {
    total += (info & (LEAP_MONTH_BIG_MASK >> m)) !== 0 ? 30 : 29;
  }

  // 闰月
  const leap = info & 0xf;
  if (leap > 0) {
    total += (info & LEAP_MONTH_BIG_MASK) !== 0 ? 30 : 29;
  }

  return total;
}

// ===================================================================
// 农历日期表示
// ===================================================================

/** 农历日期。 */
export interface LunarDate {
  /** 农历年（如 2025）。 */
  year: number;
  /** 农历月（1-12）。 */
  month: number;
  /** 农历日（1-30）。 */
  day: number;
  /** 是否为闰月。 */
  isLeapMonth: boolean;
}

/** 农历完整信息（包含天干地支、生肖、中文表示）。 */
export interface LunarInfo extends LunarDate {
  /** 天干。 */
  tianGan: string;
  /** 地支。 */
  diZhi: string;
  /** 干支年名（如"乙巳年"）。 */
  ganZhiYear: string;
  /** 生肖。 */
  shengXiao: string;
  /** 月份中文名（如"正月"、"闰四月"）。 */
  monthName: string;
  /** 日期中文名（如"初一"、"十五"）。 */
  dayName: string;
  /** 完整中文表示（如"乙巳年 正月初一"）。 */
  fullName: string;
}

// ===================================================================
// 公历 → 农历 转换
// ===================================================================

/**
 * 公历日期转农历日期。
 *
 * 算法：从 1900-01-31（基准日）开始，累加每年天数定位到目标农历年，
 * 再逐月累加定位到月和日，时间复杂度 O(年数 × 13)，实际 < 1μs。
 *
 * @param solarYear 公历年
 * @param solarMonth 公历月（1-12）
 * @param solarDay 公历日
 * @returns 农历日期信息
 */
export function solarToLunar(solarYear: number, solarMonth: number, solarDay: number): LunarInfo {
  const targetMs = Date.UTC(solarYear, solarMonth - 1, solarDay);
  let offset = Math.floor((targetMs - BASE_DATE_MS) / MS_PER_DAY);

  if (offset < 0) {
    throw new RangeError(`日期早于 ${LUNAR_START_YEAR}-01-31，超出农历转换范围`);
  }

  // 定位农历年
  let lunarYear = LUNAR_START_YEAR;
  let daysInYear: number;
  while (lunarYear <= LUNAR_END_YEAR) {
    daysInYear = yearDays(lunarYear);
    if (offset < daysInYear) break;
    offset -= daysInYear;
    lunarYear++;
  }

  if (lunarYear > LUNAR_END_YEAR) {
    throw new RangeError(`日期超出农历转换范围（${LUNAR_START_YEAR}-${LUNAR_END_YEAR}）`);
  }

  // 定位农历月和日
  const leap = leapMonth(lunarYear);
  let lunarMonth = 1;
  let isLeapMonth = false;
  let daysInMonth: number;
  let found = false;

  for (let m = 1; m <= 12; m++) {
    // 正常月
    daysInMonth = monthDays(lunarYear, m);
    if (offset < daysInMonth) {
      lunarMonth = m;
      found = true;
      break;
    }
    offset -= daysInMonth;

    // 如果该月后面有闰月
    if (m === leap) {
      daysInMonth = leapMonthDays(lunarYear);
      if (offset < daysInMonth) {
        lunarMonth = m;
        isLeapMonth = true;
        found = true;
        break;
      }
      offset -= daysInMonth;
    }
  }

  // 若循环结束仍未定位，说明 offset 落在最后一月
  if (!found) {
    lunarMonth = 12;
  }

  const lunarDay = offset + 1;

  return buildLunarInfo(lunarYear, lunarMonth, lunarDay, isLeapMonth);
}

/**
 * 便捷方法：接受 YYYY-MM-DD 格式字符串。
 */
export function solarToLunarFromStr(dateStr: string): LunarInfo {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dateStr);
  if (!match) {
    throw new Error(`日期格式错误: "${dateStr}"，应为 YYYY-MM-DD`);
  }
  return solarToLunar(Number(match[1]), Number(match[2]), Number(match[3]));
}

// ===================================================================
// 农历 → 公历 转换
// ===================================================================

/**
 * 农历日期转公历日期。
 *
 * @returns [公历年, 公历月, 公历日]
 */
export function lunarToSolar(
  lunarYear: number,
  lunarMonth: number,
  lunarDay: number,
  isLeapMonth = false,
): [number, number, number] {
  validateYear(lunarYear);

  if (lunarMonth < 1 || lunarMonth > 12) {
    throw new RangeError(`农历月份超出范围: ${lunarMonth}`);
  }
  if (lunarDay < 1 || lunarDay > 30) {
    throw new RangeError(`农历日期超出范围: ${lunarDay}`);
  }

  // 累加从基准日到目标农历日期的天数
  let offset = 0;

  // 累加从 1900 到目标年之前的所有天数
  for (let y = LUNAR_START_YEAR; y < lunarYear; y++) {
    offset += yearDays(y);
  }

  // 累加到目标月之前的所有月份天数
  const leap = leapMonth(lunarYear);

  for (let m = 1; m < lunarMonth; m++) {
    offset += monthDays(lunarYear, m);
    if (m === leap) {
      offset += leapMonthDays(lunarYear);
    }
  }

  // 如果目标是闰月，还要加上正常月天数
  if (isLeapMonth && lunarMonth === leap) {
    offset += monthDays(lunarYear, lunarMonth);
  }

  // 加上日
  offset += lunarDay - 1;

  // 从基准日加偏移天数
  const resultMs = BASE_DATE_MS + offset * MS_PER_DAY;
  const d = new Date(resultMs);

  return [d.getUTCFullYear(), d.getUTCMonth() + 1, d.getUTCDate()];
}

/**
 * 农历日期转公历 YYYY-MM-DD 字符串。
 */
export function lunarToSolarStr(
  lunarYear: number,
  lunarMonth: number,
  lunarDay: number,
  isLeapMonth = false,
): string {
  const [y, m, d] = lunarToSolar(lunarYear, lunarMonth, lunarDay, isLeapMonth);
  return `${String(y).padStart(4, '0')}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
}

// ===================================================================
// 天干地支 / 生肖
// ===================================================================

/**
 * 获取农历年的天干。
 */
export function getTianGan(lunarYear: number): string {
  return TIAN_GAN[(lunarYear - 4) % 10];
}

/**
 * 获取农历年的地支。
 */
export function getDiZhi(lunarYear: number): string {
  return DI_ZHI[(lunarYear - 4) % 12];
}

/**
 * 获取农历年的干支名称（如"乙巳"）。
 */
export function getGanZhi(lunarYear: number): string {
  return getTianGan(lunarYear) + getDiZhi(lunarYear);
}

/**
 * 获取农历年的生肖。
 */
export function getShengXiao(lunarYear: number): string {
  return SHENG_XIAO[(lunarYear - 4) % 12];
}

/**
 * 获取农历月份名称。
 */
export function getMonthName(month: number, isLeapMonth: boolean): string {
  if (month < 1 || month > 12) {
    throw new RangeError(`月份超出范围: ${month}`);
  }
  return (isLeapMonth ? '闰' : '') + MONTH_NAMES[month - 1] + '月';
}

/**
 * 获取农历日期名称（如"初一"、"十五"）。
 */
export function getDayName(day: number): string {
  if (day < 1 || day > 30) {
    throw new RangeError(`日期超出范围: ${day}`);
  }
  return DAY_NAMES[day - 1];
}

// ===================================================================
// 内部工具
// ===================================================================

function validateYear(year: number): void {
  if (year < LUNAR_START_YEAR || year > LUNAR_END_YEAR) {
    throw new RangeError(
      `年份 ${year} 超出范围，农历数据覆盖 ${LUNAR_START_YEAR}-${LUNAR_END_YEAR}`,
    );
  }
}

function buildLunarInfo(
  year: number,
  month: number,
  day: number,
  isLeapMonth: boolean,
): LunarInfo {
  const tianGan = getTianGan(year);
  const diZhi = getDiZhi(year);
  const ganZhiYear = tianGan + diZhi + '年';
  const shengXiao = getShengXiao(year);
  const monthName = getMonthName(month, isLeapMonth);
  const dayName = getDayName(day);
  const fullName = `${ganZhiYear} ${monthName}${dayName}`;

  return {
    year,
    month,
    day,
    isLeapMonth,
    tianGan,
    diZhi,
    ganZhiYear,
    shengXiao,
    monthName,
    dayName,
    fullName,
  };
}
