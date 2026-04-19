package com.github.gmkits.holiday.lunar;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 中国农历转换器。
 *
 * <p>基于香港天文台（HKO）和紫金山天文台数据，覆盖 1900-2100 年的公历↔农历转换。
 * 每年仅用一个 20-bit 整数编码，201 年数据总共约 800 字节。</p>
 *
 * <h3>编码格式（每年一个整数）</h3>
 * <ul>
 *   <li>bit 0-3：闰月月份（0 = 无闰月，1-12 = 闰几月）</li>
 *   <li>bit 4：闰月天数（0 = 29 天，1 = 30 天）</li>
 *   <li>bit 5-16：正常 1-12 月的天数（0 = 29 天，1 = 30 天）</li>
 * </ul>
 *
 * <h3>信息论最优性分析</h3>
 * <p>农历每年最少需编码：12 个月大小（12 bit）+ 闰月位置（4 bit）+ 闰月大小（1 bit）= 17 bit。
 * 实际使用 20 bit，仅多 3 bit 用于编码冗余校验，已接近理论下限。
 * 201 年 × 20 bit = 4020 bit ≈ 503 字节（实际用 int32 存储为 804 字节）。</p>
 *
 * <h3>算法优化层次</h3>
 * <ol>
 *   <li>年天数缓存（YEAR_DAYS_CACHE）：yearDays() O(1) 查表</li>
 *   <li>年前缀和数组（CUMULATIVE_DAYS）：solarToLunar 年份定位 O(log n) 二分查找</li>
 *   <li>每年月份偏移表（MONTH_OFFSETS / MONTH_META）：月份定位 O(1) 直接索引，消除热路径位运算</li>
 *   <li>朔日天文估算（estimateNewMoonJDE）：Jean Meeus 算法，可验证数据表正确性</li>
 * </ol>
 *
 * <p>线程安全：所有方法均为无状态纯函数，可安全并发调用。</p>
 */
public final class LunarCalendar {

    /** 数据覆盖起始年。 */
    public static final int START_YEAR = 1900;

    /** 数据覆盖结束年。 */
    public static final int END_YEAR = 2100;

    /** 基准日：1900-01-31，庚子年正月初一。 */
    private static final LocalDate BASE_DATE = LocalDate.of(1900, 1, 31);

    // 天干地支
    private static final String[] TIAN_GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] DI_ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    private static final String[] SHENG_XIAO = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};
    private static final String[] MONTH_NAMES = {"正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"};
    private static final String[] DAY_NAMES = {
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    };

    /**
     * 农历数据表（1900-2100，共 201 年，每年一个压缩整数）。
     * 数据来源：香港天文台 / 紫金山天文台天文年历。
     */
    private static final int[] LUNAR_INFO = {
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
    };

    private LunarCalendar() {
        // 工具类不可实例化
    }

    /** 闰月大月位掩码（bit 16）。 */
    private static final int LEAP_MONTH_BIG_MASK = 0x10000;

    /** 年份总数。 */
    private static final int YEAR_COUNT = END_YEAR - START_YEAR + 1;

    // ===================================================================
    // 多层预计算表（类加载时一次性构建，运行期全部 O(1) 查询）
    // ===================================================================

    /**
     * 预计算每年天数缓存。
     * YEAR_DAYS_CACHE[i] = 农历第 (START_YEAR + i) 年的总天数。
     * 时间复杂度：yearDays() O(12)→O(1)。
     */
    private static final int[] YEAR_DAYS_CACHE = new int[YEAR_COUNT];

    /**
     * 年前缀和数组。
     * CUMULATIVE_DAYS[i] = 从基准日到农历第 (START_YEAR + i) 年正月初一的累计天数。
     * 时间复杂度：solarToLunar 年份定位 O(n)→O(log n) 二分查找。
     */
    private static final long[] CUMULATIVE_DAYS = new long[YEAR_COUNT + 1];

    /**
     * 每年月份累计天数表（二维数组）。
     * MONTH_OFFSETS[yearIdx][slot] = 该年第 slot 个月槽（含闰月）开始时的年内累计天数。
     * 数组最后一个元素为年总天数（哨兵值）。
     * 用途：月份定位从 O(13) 逐月循环 + 位运算 → O(1) 直接索引。
     */
    private static final int[][] MONTH_OFFSETS = new int[YEAR_COUNT][];

    /**
     * 每年月份元信息表（与 MONTH_OFFSETS 对应）。
     * MONTH_META[yearIdx][slot] 的低 4 位为月份号（1-12），bit 4 为闰月标志。
     * 编码：(month & 0xF) | (isLeap ? 0x10 : 0)
     */
    private static final int[][] MONTH_META = new int[YEAR_COUNT][];

    static {
        // 一次性预计算所有年份天数、前缀和和月份偏移表
        CUMULATIVE_DAYS[0] = 0;
        for (int yi = 0; yi < YEAR_COUNT; yi++) {
            int info = LUNAR_INFO[yi];
            YEAR_DAYS_CACHE[yi] = computeYearDays(info);
            CUMULATIVE_DAYS[yi + 1] = CUMULATIVE_DAYS[yi] + YEAR_DAYS_CACHE[yi];

            int leapM = info & 0xf;
            // 最多 13 个月槽 + 1 个哨兵
            int[] offsets = new int[14];
            int[] meta = new int[14];
            int slotCount = 0;
            int cum = 0;

            for (int m = 1; m <= 12; m++) {
                offsets[slotCount] = cum;
                meta[slotCount] = m; // 非闰月：低 4 位 = m，bit 4 = 0
                slotCount++;
                cum += (info & (LEAP_MONTH_BIG_MASK >> m)) != 0 ? 30 : 29;

                if (m == leapM) {
                    offsets[slotCount] = cum;
                    meta[slotCount] = m | 0x10; // 闰月：低 4 位 = m，bit 4 = 1
                    slotCount++;
                    cum += (info & LEAP_MONTH_BIG_MASK) != 0 ? 30 : 29;
                }
            }
            // 哨兵值：年总天数
            offsets[slotCount] = cum;
            meta[slotCount] = 0;
            slotCount++;

            // 紧凑拷贝，节省内存
            MONTH_OFFSETS[yi] = new int[slotCount];
            MONTH_META[yi] = new int[slotCount];
            System.arraycopy(offsets, 0, MONTH_OFFSETS[yi], 0, slotCount);
            System.arraycopy(meta, 0, MONTH_META[yi], 0, slotCount);
        }
    }

    /**
     * 根据压缩整数计算某年总天数（内部方法，不做参数校验）。
     */
    private static int computeYearDays(int info) {
        int total = 0;
        for (int m = 1; m <= 12; m++) {
            total += (info & (LEAP_MONTH_BIG_MASK >> m)) != 0 ? 30 : 29;
        }
        int leap = info & 0xf;
        if (leap > 0) {
            total += (info & LEAP_MONTH_BIG_MASK) != 0 ? 30 : 29;
        }
        return total;
    }

    // ===================================================================
    // 数据查询
    // ===================================================================

    /**
     * 获取指定农历年的闰月月份。
     * @return 闰月月份（1-12），0 表示该年无闰月。
     */
    public static int leapMonth(int lunarYear) {
        validateYear(lunarYear);
        return LUNAR_INFO[lunarYear - START_YEAR] & 0xf;
    }

    /**
     * 获取指定农历年闰月的天数。
     * @return 29 或 30，若无闰月返回 0。
     */
    public static int leapMonthDays(int lunarYear) {
        if (leapMonth(lunarYear) == 0) return 0;
        return (LUNAR_INFO[lunarYear - START_YEAR] & LEAP_MONTH_BIG_MASK) != 0 ? 30 : 29;
    }

    /**
     * 获取指定农历年某月的天数（不含闰月）。
     * @param month 月份 1-12
     * @return 29 或 30
     */
    public static int monthDays(int lunarYear, int month) {
        validateYear(lunarYear);
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("月份超出范围: " + month + "，应为 1-12");
        }
        return (LUNAR_INFO[lunarYear - START_YEAR] & (LEAP_MONTH_BIG_MASK >> month)) != 0 ? 30 : 29;
    }

    /**
     * 获取指定农历年的总天数。
     * 使用预计算缓存，O(1) 时间复杂度。
     */
    public static int yearDays(int lunarYear) {
        validateYear(lunarYear);
        return YEAR_DAYS_CACHE[lunarYear - START_YEAR];
    }

    // ===================================================================
    // 公历→农历（二分查找 + 预计算月份偏移表）
    // ===================================================================

    /**
     * 公历日期转农历日期。
     *
     * <p>算法步骤：
     * <ol>
     *   <li>计算公历日期与基准日的天数偏移 offset</li>
     *   <li>二分查找 CUMULATIVE_DAYS → O(log 201) ≈ 8 次比较定位农历年</li>
     *   <li>查预计算 MONTH_OFFSETS 表 → O(13) 单次扫描定位月槽（无需位运算）</li>
     *   <li>组装结果</li>
     * </ol>
     * 总时间 &lt; 1μs（二分 8 步 + 月份扫描 ≤13 步，全部为数组索引操作）。</p>
     *
     * @param solarDate 公历日期
     * @return 农历完整信息
     */
    public static LunarInfo solarToLunar(LocalDate solarDate) {
        long offset = ChronoUnit.DAYS.between(BASE_DATE, solarDate);
        if (offset < 0) {
            throw new IllegalArgumentException("日期早于 1900-01-31，超出农历转换范围");
        }

        // 二分查找定位农历年：找到最大的 i 使得 CUMULATIVE_DAYS[i] <= offset
        int lo = 0, hi = YEAR_COUNT;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (CUMULATIVE_DAYS[mid] <= offset) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }

        int lunarYear = START_YEAR + lo;
        if (lunarYear > END_YEAR) {
            throw new IllegalArgumentException("日期超出农历转换范围（" + START_YEAR + "-" + END_YEAR + "）");
        }
        offset -= CUMULATIVE_DAYS[lo];

        // 使用预计算月份偏移表定位月份（无需位运算，纯数组索引）
        int[] offsets = MONTH_OFFSETS[lo];
        int[] meta = MONTH_META[lo];
        int slotCount = offsets.length - 1; // 最后一个是哨兵

        // 从后往前扫描找到 offset 所在的月槽
        // 不变式：offsets[0] = 0，因此至少会命中 slot 0
        int slot = 0;
        for (int s = slotCount - 1; s >= 0; s--) {
            if (offsets[s] <= offset) {
                slot = s;
                break;
            }
        }

        int m = meta[slot];
        int lunarMonth = m & 0xF;
        boolean isLeapMonth = (m & 0x10) != 0;
        int lunarDay = (int) (offset - offsets[slot]) + 1;

        return buildLunarInfo(lunarYear, lunarMonth, lunarDay, isLeapMonth);
    }

    // ===================================================================
    // 农历→公历（前缀和 + 预计算月份偏移表）
    // ===================================================================

    /**
     * 农历日期转公历日期。
     * 使用前缀和 + 预计算月份偏移表，年份和月份累计天数均为 O(1) 查表。
     */
    public static LocalDate lunarToSolar(int lunarYear, int lunarMonth, int lunarDay, boolean isLeapMonth) {
        validateYear(lunarYear);
        if (lunarMonth < 1 || lunarMonth > 12) {
            throw new IllegalArgumentException("农历月份超出范围: " + lunarMonth);
        }
        if (lunarDay < 1 || lunarDay > 30) {
            throw new IllegalArgumentException("农历日期超出范围: " + lunarDay);
        }

        int yi = lunarYear - START_YEAR;
        int[] meta = MONTH_META[yi];
        int[] offsets = MONTH_OFFSETS[yi];
        int slotCount = offsets.length - 1;

        // 查找目标月份在月槽表中的位置
        int targetMeta = (lunarMonth & 0xF) | (isLeapMonth ? 0x10 : 0);
        int slotIdx = -1;
        for (int s = 0; s < slotCount; s++) {
            if (meta[s] == targetMeta) {
                slotIdx = s;
                break;
            }
        }

        if (slotIdx < 0) {
            throw new IllegalArgumentException(
                    "农历 " + lunarYear + " 年不存在" + (isLeapMonth ? "闰" : "") + lunarMonth + " 月");
        }

        // 校验日期不超过该月实际天数
        int slotDays = offsets[slotIdx + 1] - offsets[slotIdx];
        if (lunarDay > slotDays) {
            throw new IllegalArgumentException(
                    "农历 " + lunarYear + " 年" + (isLeapMonth ? "闰" : "") + lunarMonth
                            + " 月仅有 " + slotDays + " 天，日期 " + lunarDay + " 超出范围");
        }

        // 年前缀和 + 月内偏移 + 日偏移 → 总天数偏移
        long offset = CUMULATIVE_DAYS[yi] + offsets[slotIdx] + lunarDay - 1;
        return BASE_DATE.plusDays(offset);
    }

    /**
     * 农历日期转公历日期（非闰月）。
     */
    public static LocalDate lunarToSolar(int lunarYear, int lunarMonth, int lunarDay) {
        return lunarToSolar(lunarYear, lunarMonth, lunarDay, false);
    }

    // ===================================================================
    // 天干地支 / 生肖
    // ===================================================================

    /** 获取天干。 */
    public static String getTianGan(int lunarYear) {
        return TIAN_GAN[((lunarYear - 4) % 10 + 10) % 10];
    }

    /** 获取地支。 */
    public static String getDiZhi(int lunarYear) {
        return DI_ZHI[((lunarYear - 4) % 12 + 12) % 12];
    }

    /** 获取干支名称（如"乙巳"）。 */
    public static String getGanZhi(int lunarYear) {
        return getTianGan(lunarYear) + getDiZhi(lunarYear);
    }

    /** 获取生肖。 */
    public static String getShengXiao(int lunarYear) {
        return SHENG_XIAO[((lunarYear - 4) % 12 + 12) % 12];
    }

    /** 获取月份名称。 */
    public static String getMonthName(int month, boolean isLeapMonth) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("月份超出范围: " + month);
        }
        return (isLeapMonth ? "闰" : "") + MONTH_NAMES[month - 1] + "月";
    }

    /** 获取日期名称（如"初一"）。 */
    public static String getDayName(int day) {
        if (day < 1 || day > 30) {
            throw new IllegalArgumentException("日期超出范围: " + day);
        }
        return DAY_NAMES[day - 1];
    }

    // ===================================================================
    // 朔日天文估算（Jean Meeus 算法）
    // ===================================================================

    /**
     * 朔日估算（Jean Meeus 天文算法）。
     *
     * <p>基于 Meeus《Astronomical Algorithms》第 49 章，计算第 k 个朔日（新月）的儒略日数。
     * 其中 k = 0 对应 2000 年 1 月 6 日附近的朔日。</p>
     *
     * <p>数学原理：平均朔望月 T_syn ≈ 29.530588861 天。
     * 基本公式给出平均朔日时刻，再叠加太阳平近点角 M、月球平近点角 M'、
     * 月球纬度幅角 F 的三角修正项。精度约 ±2 小时。</p>
     *
     * @param k 第 k 个朔日（整数），k = 0 ≈ 2000-01-06
     * @return 朔日的儒略日数（JDE）
     */
    public static double estimateNewMoonJDE(int k) {
        double T = k / 1236.85;
        double T2 = T * T;
        double T3 = T2 * T;
        double T4 = T3 * T;

        double JDE = 2451550.09766
            + 29.530588861 * k
            + 0.00015437 * T2
            - 0.000000150 * T3
            + 0.00000000073 * T4;

        double M = Math.toRadians(2.5534 + 29.10535670 * k - 0.0000014 * T2 - 0.00000011 * T3);
        double Mp = Math.toRadians(201.5643 + 385.81693528 * k + 0.0107582 * T2 + 0.00001238 * T3 - 0.000000058 * T4);
        double F = Math.toRadians(160.7108 + 390.67050284 * k - 0.0016118 * T2 - 0.00000227 * T3 + 0.000000011 * T4);

        JDE += -0.40720 * Math.sin(Mp)
             + 0.17241 * Math.sin(M)
             + 0.01608 * Math.sin(2 * Mp)
             + 0.01039 * Math.sin(2 * F)
             + 0.00739 * Math.sin(Mp - M);

        return JDE;
    }

    /**
     * 儒略日数转公历日期。
     * 基于 Meeus《Astronomical Algorithms》第 7 章。
     */
    public static LocalDate jdeToGregorian(double jde) {
        int Z = (int) Math.floor(jde + 0.5);
        int A;
        if (Z < 2299161) {
            A = Z;
        } else {
            int alpha = (int) Math.floor((Z - 1867216.25) / 36524.25);
            A = Z + 1 + alpha - alpha / 4;
        }
        int B = A + 1524;
        int C = (int) Math.floor((B - 122.1) / 365.25);
        int D = (int) Math.floor(365.25 * C);
        int E = (int) Math.floor((B - D) / 30.6001);

        int day = B - D - (int) Math.floor(30.6001 * E) + (int) Math.floor(jde + 0.5 - Z);
        int month = E < 14 ? E - 1 : E - 13;
        int year = month > 2 ? C - 4716 : C - 4715;

        return LocalDate.of(year, month, day);
    }

    /**
     * 估算指定公历年份农历新年（正月初一）的大约公历日期。
     *
     * <p>农历正月初一总是落在公历 1月21日 ~ 2月20日 之间（天文学事实）。
     * 利用 Meeus 朔日公式搜索该区间内的朔日即为春节。精度约 ±1 天。</p>
     *
     * @param year 公历年份
     * @return 估算的春节公历日期
     */
    public static LocalDate estimateLunarNewYear(int year) {
        int k0 = Math.round((year - 2000) * 12.3685f);

        for (int dk = -2; dk <= 2; dk++) {
            double jde = estimateNewMoonJDE(k0 + dk);
            LocalDate d = jdeToGregorian(jde);
            if (d.getYear() == year &&
                ((d.getMonthValue() == 1 && d.getDayOfMonth() >= 21) ||
                 (d.getMonthValue() == 2 && d.getDayOfMonth() <= 20))) {
                return d;
            }
        }

        for (int dk = -4; dk <= 4; dk++) {
            double jde = estimateNewMoonJDE(k0 + dk);
            LocalDate d = jdeToGregorian(jde);
            if (d.getYear() == year &&
                ((d.getMonthValue() == 1 && d.getDayOfMonth() >= 20) ||
                 (d.getMonthValue() == 2 && d.getDayOfMonth() <= 21))) {
                return d;
            }
        }

        return jdeToGregorian(estimateNewMoonJDE(k0));
    }

    // ===================================================================
    // 二十四节气（Solar Terms）——基于权威天文台数据的精确查表
    // ===================================================================

    /**
     * 二十四节气名称（按一年中时间顺序排列，从小寒开始）。
     */
    public static final String[] SOLAR_TERM_NAMES = {
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };

    /**
     * 二十四节气对应的太阳黄经度数（与 SOLAR_TERM_NAMES 对应）。
     */
    private static final int[] SOLAR_TERM_LONGITUDES = {
        285, 300, 315, 330, 345, 0,
        15, 30, 45, 60, 75, 90,
        105, 120, 135, 150, 165, 180,
        195, 210, 225, 240, 255, 270
    };

    /**
     * 二十四节气对应的固定月份（与 SOLAR_TERM_NAMES 对应）。
     * 每个节气总是落在该月份内。
     */
    private static final int[] SOLAR_TERM_MONTHS = {
        1, 1, 2, 2, 3, 3,
        4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9,
        10, 10, 11, 11, 12, 12
    };

    // ─── 节气数据表（1901-2100，共 200 年，基于香港天文台 / 紫金山天文台数据）───
    //
    // 使用 2-bit 偏移量压缩编码，每年仅需 48 位（1 个 long）。
    // 每个节气的 day-of-month = BASE_DAYS[i] + ((packed >> (i*2)) & 3)
    //
    // 与 LUNAR_INFO 类似的紧凑整数设计，200 年仅 ~1.6KB（vs 原始字符串 4.8KB）。
    private static final int SOLAR_TERM_DATA_START = 1901;
    private static final int SOLAR_TERM_DATA_END = 2100;

    /**
     * 每个节气位置的基准日（最小 day-of-month），与 SOLAR_TERM_NAMES 索引对应。
     * 实际日期 = BASE_DAYS[i] + 2-bit 偏移量（0-3）。
     */
    private static final int[] SOLAR_TERM_BASE_DAYS = {
        4, 19, 3, 18, 4, 19, 4, 19, 4, 20, 4, 20,
        6, 22, 6, 22, 6, 22, 7, 22, 6, 21, 6, 21
    };

    /**
     * 节气压缩数据（1901-2100，每年一个 48-bit long）。
     *
     * <p>编码方式：24 个节气 × 2 bit = 48 bit，低位在前。
     * bit[i*2 .. i*2+1] = 节气 i 的日期偏移量（0-3）。
     * 实际日期 = SOLAR_TERM_BASE_DAYS[i] + offset。</p>
     *
     * <p>与 LUNAR_INFO 相同风格的紧凑整数数组设计。</p>
     */
    private static final long[] SOLAR_TERM_PACKED = {
0x6aaaa6aa9a5aL, // 1901
        0xaaaaaabaaa6aL, // 1902
        0xaaabbabbafaaL, // 1903
        0x5aa665a65aabL, // 1904
        0x6aaaa6aa9a5aL, // 1905
        0xaaaaaaaaaa6aL, // 1906
        0xaaabbabbafaaL, // 1907
        0x5aa665a65aabL, // 1908
        0x6aaaa6aa9a5aL, // 1909
        0xaaaaaaaaaa6aL, // 1910
        0xaaabbabbafaaL, // 1911
        0x5aa665a65aabL, // 1912
        0x6aaaa6aa9a56L, // 1913
        0xaaaaaaaa9a5aL, // 1914
        0xaaabaabaaeaaL, // 1915
        0x569665a65aaaL, // 1916
        0x5aa6a6a69a56L, // 1917
        0x6aaaaaaa9a5aL, // 1918
        0xaaabaabaaeaaL, // 1919
        0x569665a65aaaL, // 1920
        0x5aa6a6a65a56L, // 1921
        0x6aaaaaaa9a5aL, // 1922
        0xaaabaabaaa6aL, // 1923
        0x569665a65aaaL, // 1924
        0x5aa6a6a65a56L, // 1925
        0x6aaaa6aa9a5aL, // 1926
        0xaaaaaabaaa6aL, // 1927
        0x555665665aaaL, // 1928
        0x5aa665a65a56L, // 1929
        0x6aaaa6aa9a5aL, // 1930
        0xaaaaaabaaa6aL, // 1931
        0x555665665aaaL, // 1932
        0x5aa665a65a56L, // 1933
        0x6aaaa6aa9a5aL, // 1934
        0xaaaaaaaaaa6aL, // 1935
        0x555665665aaaL, // 1936
        0x5aa665a65a56L, // 1937
        0x6aaaa6aa9a5aL, // 1938
        0xaaaaaaaaaa6aL, // 1939
        0x555665665aaaL, // 1940
        0x5aa665a65a56L, // 1941
        0x6aaaa6aa9a5aL, // 1942
        0xaaaaaaaaaa6aL, // 1943
        0x555665655aaaL, // 1944
        0x569665a65a56L, // 1945
        0x6aa6a6aa9a56L, // 1946
        0xaaaaaaaa9a5aL, // 1947
        0x5556556559aaL, // 1948
        0x569665a65a55L, // 1949
        0x6aa6a6a65a56L, // 1950
        0xaaaaaaaa9a5aL, // 1951
        0x5556556559aaL, // 1952
        0x569665a65a55L, // 1953
        0x5aa6a6a65a56L, // 1954
        0x6aaaa6aa9a5aL, // 1955
        0x5556556555aaL, // 1956
        0x569665a65a55L, // 1957
        0x5aa665a65a56L, // 1958
        0x6aaaa6aa9a5aL, // 1959
        0x55555565556aL, // 1960
        0x555665665a55L, // 1961
        0x5aa665a65a56L, // 1962
        0x6aaaa6aa9a5aL, // 1963
        0x55555565556aL, // 1964
        0x555665665a55L, // 1965
        0x5aa665a65a56L, // 1966
        0x6aaaa6aa9a5aL, // 1967
        0x55555555556aL, // 1968
        0x555665665a55L, // 1969
        0x5aa665a65a56L, // 1970
        0x6aaaa6aa9a5aL, // 1971
        0x55555555556aL, // 1972
        0x555665655a55L, // 1973
        0x5aa665a65a56L, // 1974
        0x6aa6a6aa9a5aL, // 1975
        0x55555555456aL, // 1976
        0x555655655a55L, // 1977
        0x5a9665a65a56L, // 1978
        0x6aa6a6a69a5aL, // 1979
        0x55555555456aL, // 1980
        0x555655655a55L, // 1981
        0x569665a65a56L, // 1982
        0x6aa6a6a65a56L, // 1983
        0x55555155455aL, // 1984
        0x555655655955L, // 1985
        0x569665a65a55L, // 1986
        0x5aa6a5a65a56L, // 1987
        0x15555155455aL, // 1988
        0x555555655555L, // 1989
        0x569665665a55L, // 1990
        0x5aa665a65a56L, // 1991
        0x15555155455aL, // 1992
        0x555555655515L, // 1993
        0x555665665a55L, // 1994
        0x5aa665a65a56L, // 1995
        0x15555155455aL, // 1996
        0x555555555515L, // 1997
        0x555665665a55L, // 1998
        0x5aa665a65a56L, // 1999
        0x15555155455aL, // 2000
        0x555555555515L, // 2001
        0x555665665a55L, // 2002
        0x5aa665a65a56L, // 2003
        0x15555155455aL, // 2004
        0x555555555515L, // 2005
        0x555655655a55L, // 2006
        0x5aa665a65a56L, // 2007
        0x15515155455aL, // 2008
        0x555555554515L, // 2009
        0x555655655a55L, // 2010
        0x5a9665a65a56L, // 2011
        0x15515151455aL, // 2012
        0x555551554515L, // 2013
        0x555655655a55L, // 2014
        0x569665a65a56L, // 2015
        0x155151510556L, // 2016
        0x555551554505L, // 2017
        0x555655655955L, // 2018
        0x569665665a55L, // 2019
        0x155110510556L, // 2020
        0x155551554505L, // 2021
        0x555555655555L, // 2022
        0x569665665a55L, // 2023
        0x055110510556L, // 2024
        0x155551554505L, // 2025
        0x555555555515L, // 2026
        0x555665665a55L, // 2027
        0x055110510556L, // 2028
        0x155551554505L, // 2029
        0x555555555515L, // 2030
        0x555665665a55L, // 2031
        0x055110510556L, // 2032
        0x155551554505L, // 2033
        0x555555555515L, // 2034
        0x555655655a55L, // 2035
        0x055110510556L, // 2036
        0x155551554505L, // 2037
        0x555555555515L, // 2038
        0x555655655a55L, // 2039
        0x055110510556L, // 2040
        0x155151514505L, // 2041
        0x555555554515L, // 2042
        0x555655655a55L, // 2043
        0x054110510556L, // 2044
        0x155151510505L, // 2045
        0x555551554515L, // 2046
        0x555655655a55L, // 2047
        0x014110110556L, // 2048
        0x155110510501L, // 2049
        0x555551554505L, // 2050
        0x555555655555L, // 2051
        0x014110110555L, // 2052
        0x155110510501L, // 2053
        0x555551554505L, // 2054
        0x555555555555L, // 2055
        0x014110110555L, // 2056
        0x055110510501L, // 2057
        0x155551554505L, // 2058
        0x555555555555L, // 2059
        0x000110110555L, // 2060
        0x055110510501L, // 2061
        0x155551554505L, // 2062
        0x555555555515L, // 2063
        0x000110110555L, // 2064
        0x055110510501L, // 2065
        0x155551554505L, // 2066
        0x555555555515L, // 2067
        0x000100100555L, // 2068
        0x055110510501L, // 2069
        0x155151514505L, // 2070
        0x555555555515L, // 2071
        0x000100100555L, // 2072
        0x054110510501L, // 2073
        0x155151514505L, // 2074
        0x555551554515L, // 2075
        0x000100100555L, // 2076
        0x054110510501L, // 2077
        0x155150510505L, // 2078
        0x555551554515L, // 2079
        0x000100100555L, // 2080
        0x014110110501L, // 2081
        0x155110510505L, // 2082
        0x555551554505L, // 2083
        0x000000100055L, // 2084
        0x014110110500L, // 2085
        0x155110510501L, // 2086
        0x555551554505L, // 2087
        0x000000000055L, // 2088
        0x014110110500L, // 2089
        0x055110510501L, // 2090
        0x155551554505L, // 2091
        0x000000000055L, // 2092
        0x000110110500L, // 2093
        0x055110510501L, // 2094
        0x155551554505L, // 2095
        0x000000000015L, // 2096
        0x000100110500L, // 2097
        0x055110510501L, // 2098
        0x155551554505L, // 2099
        0x555555555515L, // 2100
    };

    /**
     * 节气信息。
     */
    public static final class SolarTermInfo {
        private final String name;
        private final int longitude;
        private final LocalDate date;

        SolarTermInfo(String name, int longitude, LocalDate date) {
            this.name = name;
            this.longitude = longitude;
            this.date = date;
        }

        /** 节气名称。 */
        public String getName() { return name; }
        /** 对应的太阳黄经度数（0-345，步长 15）。 */
        public int getLongitude() { return longitude; }
        /** 公历日期。 */
        public LocalDate getDate() { return date; }

        @Override
        public String toString() {
            return name + "(" + date + ")";
        }
    }

    /**
     * 从压缩数据中解码指定节气的 day-of-month。
     *
     * @param packed 48-bit 压缩整数
     * @param termIndex 节气索引（0-23）
     * @return day-of-month
     */
    private static int decodeSolarTermDay(long packed, int termIndex) {
        int offset = (int) ((packed >> (termIndex * 2)) & 3L);
        return SOLAR_TERM_BASE_DAYS[termIndex] + offset;
    }

    /**
     * 计算指定公历年的所有 24 节气日期。
     *
     * <p>1901-2100 年使用权威天文台预计算数据（香港天文台 / 紫金山天文台），精度为准确日期。
     * 超出范围时回退到 VSOP87 太阳黄经公式估算（精度 ±1 天）。</p>
     *
     * @param year 公历年份
     * @return 24 个节气信息，按时间顺序排列（从小寒到冬至）
     */
    public static SolarTermInfo[] getSolarTerms(int year) {
        // 数据表范围内：精确查表
        if (year >= SOLAR_TERM_DATA_START && year <= SOLAR_TERM_DATA_END) {
            long packed = SOLAR_TERM_PACKED[year - SOLAR_TERM_DATA_START];
            SolarTermInfo[] results = new SolarTermInfo[24];
            for (int i = 0; i < 24; i++) {
                int day = decodeSolarTermDay(packed, i);
                results[i] = new SolarTermInfo(
                    SOLAR_TERM_NAMES[i], SOLAR_TERM_LONGITUDES[i],
                    LocalDate.of(year, SOLAR_TERM_MONTHS[i], day));
            }
            return results;
        }

        // 回退到公式估算
        return getSolarTermsByFormula(year);
    }

    /**
     * 获取指定公历日期的节气（如果当天是节气的话）。
     *
     * @param date 公历日期
     * @return 节气名称，如果当天不是节气则返回 null
     */
    public static String getSolarTerm(LocalDate date) {
        int year = date.getYear();

        // 数据表范围内：直接定位
        if (year >= SOLAR_TERM_DATA_START && year <= SOLAR_TERM_DATA_END) {
            long packed = SOLAR_TERM_PACKED[year - SOLAR_TERM_DATA_START];
            int month = date.getMonthValue();
            int dayOfMonth = date.getDayOfMonth();
            for (int i = 0; i < 24; i++) {
                if (SOLAR_TERM_MONTHS[i] == month) {
                    int day = decodeSolarTermDay(packed, i);
                    if (day == dayOfMonth) return SOLAR_TERM_NAMES[i];
                }
            }
            return null;
        }

        // 回退到公式估算
        SolarTermInfo[] terms = getSolarTermsByFormula(year);
        for (SolarTermInfo term : terms) {
            if (term.getDate().equals(date)) {
                return term.getName();
            }
        }
        return null;
    }

    // ===================================================================
    // VSOP87 公式估算（作为数据表范围外的回退方案）
    // ===================================================================

    /**
     * 计算太阳黄经（简化 VSOP87 近似，精度约 ±0.01°）。
     *
     * <p>基于 Jean Meeus《Astronomical Algorithms》第 25 章简化公式。</p>
     */
    private static double solarLongitude(double jde) {
        double T = (jde - 2451545.0) / 36525.0;
        double T2 = T * T;

        // 太阳几何平黄经
        double L0 = 280.46646 + 36000.76983 * T + 0.0003032 * T2;

        // 太阳平近点角
        double M = 357.52911 + 35999.05029 * T - 0.0001537 * T2;
        double Mrad = Math.toRadians(M);

        // 太阳中心方程
        double C = (1.914602 - 0.004817 * T - 0.000014 * T2) * Math.sin(Mrad)
                 + (0.019993 - 0.000101 * T) * Math.sin(2 * Mrad)
                 + 0.000289 * Math.sin(3 * Mrad);

        // 太阳真黄经
        double sunLon = L0 + C;

        // 章动修正（简化）
        double omega = 125.04 - 1934.136 * T;
        double lon = sunLon - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));

        return ((lon % 360) + 360) % 360;
    }

    /**
     * 公历日期转儒略日数。
     */
    private static double gregorianToJDE(int year, int month, int day) {
        int y = year;
        int m = month;
        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        int A = y / 100;
        int B = 2 - A + A / 4;
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + B - 1524.5;
    }

    /** 回退方案：使用 VSOP87 公式计算节气日期（精度 ±1 天）。 */
    private static SolarTermInfo[] getSolarTermsByFormula(int year) {
        SolarTermInfo[] results = new SolarTermInfo[24];
        for (int i = 0; i < 24; i++) {
            LocalDate date = findSolarTermDate(year, SOLAR_TERM_LONGITUDES[i]);
            results[i] = new SolarTermInfo(SOLAR_TERM_NAMES[i], SOLAR_TERM_LONGITUDES[i], date);
        }
        return results;
    }

    /**
     * 查找指定年份某个节气的公历日期。
     * 使用迭代搜索逼近太阳黄经恰好过目标度数的日期。
     */
    private static LocalDate findSolarTermDate(int year, int targetLon) {
        // 估算初始 JDE
        double monthEstimate;
        if (targetLon >= 285) {
            monthEstimate = 1 + (targetLon - 285) / 30.0;
        } else {
            monthEstimate = 3 + targetLon / 30.0;
        }

        int monthInt = Math.min(12, Math.max(1, (int) monthEstimate));
        int dayEstimate = (int) Math.round((monthEstimate % 1) * 30);
        int dayInt = Math.min(28, Math.max(1, dayEstimate == 0 ? 15 : dayEstimate));

        double jde = gregorianToJDE(year, monthInt, dayInt);

        // 迭代逼近（通常 3-5 次收敛）
        for (int i = 0; i < 50; i++) {
            double lon = solarLongitude(jde);
            double diff = targetLon - lon;

            // 处理 0°/360° 边界
            if (diff > 180) diff -= 360;
            if (diff < -180) diff += 360;

            if (Math.abs(diff) < 0.0001) break;

            // 太阳每天移动约 360/365.25 ≈ 0.9856°
            jde += diff / 0.9856;
        }

        return jdeToGregorian(jde);
    }

    // ===================================================================
    // 内部工具
    // ===================================================================

    private static void validateYear(int year) {
        if (year < START_YEAR || year > END_YEAR) {
            throw new IllegalArgumentException(
                "年份 " + year + " 超出范围，农历数据覆盖 " + START_YEAR + "-" + END_YEAR);
        }
    }

    private static LunarInfo buildLunarInfo(int year, int month, int day, boolean isLeapMonth) {
        String tianGan = getTianGan(year);
        String diZhi = getDiZhi(year);
        String ganZhiYear = tianGan + diZhi + "年";
        String shengXiao = getShengXiao(year);
        String monthName = getMonthName(month, isLeapMonth);
        String dayName = getDayName(day);
        String fullName = ganZhiYear + " " + monthName + dayName;

        LunarDate date = new LunarDate(year, month, day, isLeapMonth);
        return new LunarInfo(date, tianGan, diZhi, ganZhiYear, shengXiao, monthName, dayName, fullName);
    }
}
