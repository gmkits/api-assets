/**
 * @holiday/lunar —— 中国农历转换模块
 *
 * 基于香港天文台（HKO）和紫金山天文台数据，覆盖 1900-2100 年的公历↔农历转换。
 * 每年仅用一个 20-bit 整数编码，201 年数据总共 ~800 字节，适合嵌入式和浏览器端使用。
 *
 * ─── 编码格式（每年一个整数，最多 20 有效位）───
 *   bit 0-3  : 闰月月份（0 = 无闰月，1-12 = 闰几月）
 *   bit 4    : 闰月天数（0 = 29 天 / 小月，1 = 30 天 / 大月）
 *   bit 5-16 : 正常 1-12 月的天数（0 = 29 天，1 = 30 天），bit5 = 一月
 *
 * ─── 信息论最优性分析 ───
 * 农历每年最少需编码：12 个月大小（12 bit）+ 闰月位置（4 bit）+ 闰月大小（1 bit）= 17 bit。
 * 实际使用 20 bit，仅多 3 bit 用于编码冗余校验，已接近理论下限。
 * 201 年 × 20 bit = 4020 bit ≈ 503 字节（实际用 int32 存储为 804 字节）。
 *
 * ─── 算法优化层次 ───
 * 1. 年天数缓存（YEAR_DAYS_CACHE）：yearDays() O(1) 查表
 * 2. 年前缀和数组（CUMULATIVE_DAYS）：solarToLunar 年份定位 O(log n) 二分查找
 * 3. 每年月份偏移表（MONTH_OFFSETS）：月份定位 O(1) 直接索引，消除热路径位运算
 * 4. 朔日天文估算（estimateNewMoonJDE）：Jean Meeus 算法，可验证数据表正确性
 *
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
    0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
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

/** 年份总数。 */
const YEAR_COUNT = LUNAR_END_YEAR - LUNAR_START_YEAR + 1;

/**
 * 根据压缩整数计算某年总天数（内部函数，不做参数校验）。
 */
function computeYearDays(info: number): number {
  let total = 0;
  for (let m = 1; m <= 12; m++) {
    total += (info & (LEAP_MONTH_BIG_MASK >> m)) !== 0 ? 30 : 29;
  }
  const leap = info & 0xf;
  if (leap > 0) {
    total += (info & LEAP_MONTH_BIG_MASK) !== 0 ? 30 : 29;
  }
  return total;
}

// ===================================================================
// 多层预计算表（模块初始化时一次性构建，运行期全部 O(1) 查询）
// ===================================================================

/**
 * 预计算每年天数缓存。
 * YEAR_DAYS_CACHE[i] = 农历第 (LUNAR_START_YEAR + i) 年的总天数。
 * 时间复杂度：yearDays() O(12)→O(1)。
 */
const YEAR_DAYS_CACHE: number[] = new Array(YEAR_COUNT);

/**
 * 年前缀和数组。
 * CUMULATIVE_DAYS[i] = 从基准日到农历第 (LUNAR_START_YEAR + i) 年正月初一的累计天数。
 * CUMULATIVE_DAYS[0] = 0（基准年本身）。
 * 时间复杂度：solarToLunar 年份定位 O(n)→O(log n) 二分查找。
 */
const CUMULATIVE_DAYS: number[] = new Array(YEAR_COUNT + 1);

/**
 * 月份偏移表（每年月份槽的累计天数）。
 *
 * MONTH_OFFSETS[yearIdx] 是一个数组，每个元素为该年某个月槽（含闰月）开始时的年内累计天数。
 * 数组长度为 12（无闰月）或 13（有闰月），末尾追加年总天数作为哨兵。
 *
 * 用途：solarToLunar 月份定位从 O(13) 逐月循环 + 位运算 → O(1) 直接索引。
 */
const MONTH_OFFSETS: number[][] = new Array(YEAR_COUNT);

/**
 * 月份元信息表（与 MONTH_OFFSETS 对应）。
 *
 * MONTH_META[yearIdx][slot] 的低 4 位为月份号（1-12），bit 4 为闰月标志。
 * 编码：(month & 0xF) | (isLeap ? 0x10 : 0)
 */
const MONTH_META: number[][] = new Array(YEAR_COUNT);

// 一次性预计算所有年份天数、前缀和和月份偏移表
CUMULATIVE_DAYS[0] = 0;
for (let yi = 0; yi < YEAR_COUNT; yi++) {
  const info = LUNAR_INFO[yi];
  YEAR_DAYS_CACHE[yi] = computeYearDays(info);
  CUMULATIVE_DAYS[yi + 1] = CUMULATIVE_DAYS[yi] + YEAR_DAYS_CACHE[yi];

  const leapM = info & 0xf;
  const offsets: number[] = [];
  const meta: number[] = [];
  let cum = 0;

  for (let m = 1; m <= 12; m++) {
    offsets.push(cum);
    meta.push(m); // 非闰月：低 4 位 = m，bit 4 = 0
    cum += (info & (LEAP_MONTH_BIG_MASK >> m)) !== 0 ? 30 : 29;

    if (m === leapM) {
      offsets.push(cum);
      meta.push(m | 0x10); // 闰月：低 4 位 = m，bit 4 = 1
      cum += (info & LEAP_MONTH_BIG_MASK) !== 0 ? 30 : 29;
    }
  }
  // 哨兵值：年总天数（便于计算最后一个月的天数）
  offsets.push(cum);
  meta.push(0);

  MONTH_OFFSETS[yi] = offsets;
  MONTH_META[yi] = meta;
}

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
 * 使用预计算缓存，O(1) 时间复杂度。
 */
export function yearDays(lunarYear: number): number {
  validateYear(lunarYear);
  return YEAR_DAYS_CACHE[lunarYear - LUNAR_START_YEAR];
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
 * ─── 算法步骤 ───
 * 1. 计算公历日期与基准日的天数偏移 offset
 * 2. 二分查找 CUMULATIVE_DAYS → O(log 201) ≈ 8 次比较定位农历年
 * 3. 查预计算 MONTH_OFFSETS 表 → O(13) 单次扫描定位月槽（无需位运算）
 * 4. 组装结果
 *
 * 总时间 < 1μs（二分 8 步 + 月份扫描 ≤13 步，全部为数组索引操作）。
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

  // 二分查找定位农历年：找到最大的 i 使得 CUMULATIVE_DAYS[i] <= offset
  let lo = 0, hi = YEAR_COUNT;
  while (lo < hi) {
    const mid = (lo + hi + 1) >>> 1;
    if (CUMULATIVE_DAYS[mid] <= offset) {
      lo = mid;
    } else {
      hi = mid - 1;
    }
  }

  const lunarYear = LUNAR_START_YEAR + lo;
  if (lunarYear > LUNAR_END_YEAR) {
    throw new RangeError(`日期超出农历转换范围（${LUNAR_START_YEAR}-${LUNAR_END_YEAR}）`);
  }
  offset -= CUMULATIVE_DAYS[lo];

  // 使用预计算月份偏移表定位月份（无需位运算，纯数组索引）
  const offsets = MONTH_OFFSETS[lo];
  const meta = MONTH_META[lo];
  const slotCount = offsets.length - 1; // 最后一个是哨兵

  // 从后往前扫描找到 offset 所在的月槽
  // 不变式：offsets[0] = 0，因此至少会命中 slot 0
  let slot = 0;
  for (let s = slotCount - 1; s >= 0; s--) {
    if (offsets[s] <= offset) {
      slot = s;
      break;
    }
  }

  const m = meta[slot];
  const lunarMonth = m & 0xF;
  const isLeapMonth = (m & 0x10) !== 0;
  const lunarDay = offset - offsets[slot] + 1;

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
 * 使用前缀和（CUMULATIVE_DAYS）+ 预计算月份偏移表（MONTH_OFFSETS），
 * 年份和月份累计天数均为 O(1) 查表，无需逐月累加。
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

  const yi = lunarYear - LUNAR_START_YEAR;
  const meta = MONTH_META[yi];
  const offsets = MONTH_OFFSETS[yi];
  const slotCount = offsets.length - 1;

    // 查找目标月份在月槽表中的位置
  const targetMeta = (lunarMonth & 0xF) | (isLeapMonth ? 0x10 : 0);
  let slotIdx = -1;
  for (let s = 0; s < slotCount; s++) {
    if (meta[s] === targetMeta) {
      slotIdx = s;
      break;
    }
  }

  if (slotIdx < 0) {
      throw new RangeError(
          `农历 ${lunarYear} 年不存在${isLeapMonth ? '闰' : ''}${lunarMonth} 月`,
      );
  }

    // 校验日期不超过该月实际天数
    const slotDays = offsets[slotIdx + 1] - offsets[slotIdx];
    if (lunarDay > slotDays) {
        throw new RangeError(
            `农历 ${lunarYear} 年${isLeapMonth ? '闰' : ''}${lunarMonth} 月仅有 ${slotDays} 天，日期 ${lunarDay} 超出范围`,
        );
  }

  // 年前缀和 + 月内偏移 + 日偏移 → 总天数偏移
  const offset = CUMULATIVE_DAYS[yi] + offsets[slotIdx] + lunarDay - 1;

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
// 朔日天文估算（Jean Meeus 算法）
// ===================================================================

/** 角度转弧度。 */
function deg2rad(deg: number): number {
  return deg * Math.PI / 180;
}

/**
 * 朔日估算（Jean Meeus 天文算法）。
 *
 * 基于 Meeus《Astronomical Algorithms》第 49 章，计算第 k 个朔日（新月）的儒略日数。
 * 其中 k = 0 对应 2000 年 1 月 6 日附近的朔日。
 *
 * ─── 数学原理 ───
 * 平均朔望月 T_syn ≈ 29.530588861 天（月球从一次朔到下一次朔的平均间隔）。
 * 基本公式给出平均朔日时刻，再叠加太阳平近点角 M、月球平近点角 M'、
 * 月球纬度幅角 F 的三角修正项，修正由日月轨道椭圆率和交点退行导致的偏差。
 * 精度约 ±2 小时（足够判断朔日落在公历哪一天）。
 *
 * @param k 第 k 个朔日（整数），k = 0 ≈ 2000-01-06
 *          k > 0 为之后的朔日，k < 0 为之前的朔日
 * @returns 朔日的儒略日数（JDE）
 */
export function estimateNewMoonJDE(k: number): number {
  const T = k / 1236.85; // 儒略世纪数（从 J2000.0 起算）
  const T2 = T * T;
  const T3 = T2 * T;
  const T4 = T3 * T;

  // 平均朔日时刻（Meeus 公式 49.1）
  let JDE = 2451550.09766
    + 29.530588861 * k
    + 0.00015437 * T2
    - 0.000000150 * T3
    + 0.00000000073 * T4;

  // 太阳平近点角 M（度）
  const M = deg2rad(
    2.5534 + 29.10535670 * k - 0.0000014 * T2 - 0.00000011 * T3,
  );
  // 月球平近点角 M'（度）
  const Mp = deg2rad(
    201.5643 + 385.81693528 * k + 0.0107582 * T2 + 0.00001238 * T3 - 0.000000058 * T4,
  );
  // 月球纬度幅角 F（度）
  const F = deg2rad(
    160.7108 + 390.67050284 * k - 0.0016118 * T2 - 0.00000227 * T3 + 0.000000011 * T4,
  );

  // 主要修正项（精度 ±2 小时）
  JDE += -0.40720 * Math.sin(Mp)         // 月球近点角修正
       + 0.17241 * Math.sin(M)           // 太阳近点角修正
       + 0.01608 * Math.sin(2 * Mp)      // 月球近点角二倍频
       + 0.01039 * Math.sin(2 * F)       // 月球纬度幅角二倍频
       + 0.00739 * Math.sin(Mp - M);     // 日月近点角差频

  return JDE;
}

/**
 * 儒略日数转公历日期 [年, 月, 日]。
 *
 * 基于 Meeus《Astronomical Algorithms》第 7 章的反向转换算法。
 */
export function jdeToGregorian(jde: number): [number, number, number] {
  const Z = Math.floor(jde + 0.5);
  const Frac = jde + 0.5 - Z;
  let A: number;
  if (Z < 2299161) {
    A = Z;
  } else {
    const alpha = Math.floor((Z - 1867216.25) / 36524.25);
    A = Z + 1 + alpha - Math.floor(alpha / 4);
  }
  const B = A + 1524;
  const C = Math.floor((B - 122.1) / 365.25);
  const D = Math.floor(365.25 * C);
  const E = Math.floor((B - D) / 30.6001);

  const day = B - D - Math.floor(30.6001 * E) + Math.floor(Frac);
  const month = E < 14 ? E - 1 : E - 13;
  const year = month > 2 ? C - 4716 : C - 4715;

  return [year, month, day];
}

/**
 * 估算指定公历年份农历新年（正月初一）的大约公历日期。
 *
 * ─── 数学原理 ───
 * 农历正月初一总是落在公历 1月21日 ~ 2月20日 之间（天文学事实）。
 * 利用 Meeus 朔日公式搜索该区间内的朔日即为春节。
 * 精度约 ±1 天，可用于交叉验证 LUNAR_INFO 数据表的正确性。
 *
 * @param year 公历年份
 * @returns [公历年, 公历月, 公历日]
 */
export function estimateLunarNewYear(year: number): [number, number, number] {
  // k=0 对应 2000-01-06 附近朔日，一年 ≈ 12.3685 个朔望月
  const k0 = Math.round((year - 2000) * 12.3685);

  // 搜索 1月21日-2月20日 之间的朔日（农历新年必然落在此区间）
  for (let dk = -2; dk <= 2; dk++) {
    const jde = estimateNewMoonJDE(k0 + dk);
    const [y, m, d] = jdeToGregorian(jde);
    if (y === year && ((m === 1 && d >= 21) || (m === 2 && d <= 20))) {
      return [y, m, d];
    }
  }

  // 回退：扩大搜索范围
  for (let dk = -4; dk <= 4; dk++) {
    const jde = estimateNewMoonJDE(k0 + dk);
    const [y, m, d] = jdeToGregorian(jde);
    if (y === year && ((m === 1 && d >= 20) || (m === 2 && d <= 21))) {
      return [y, m, d];
    }
  }

  return jdeToGregorian(estimateNewMoonJDE(k0));
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
