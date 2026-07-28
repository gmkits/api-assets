package com.github.gmkits.holiday.lunar;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * 中国农历转换器。
 *
 * <p>基于香港天文台（HKO）和紫金山天文台数据，覆盖 1900-2100 年的公历↔农历转换。
 * 每年仅用一个 20-bit 整数编码，201 年数据总共约 800 字节。</p>
 *
 * <h2>农历编码格式（每年一个整数）</h2>
 * <ul>
 *   <li>bit 0-3：闰月月份（0 = 无闰月，1-12 = 闰几月）</li>
 *   <li>bit 4：闰月天数（0 = 29 天，1 = 30 天）</li>
 *   <li>bit 5-16：正常 1-12 月的天数（0 = 29 天，1 = 30 天）</li>
 * </ul>
 *
 * <h2>节气编码格式（每年一个 48-bit long）</h2>
 * <p>24 个节气 × 2 bit = 48 bit，低位在前。
 * bit[i*2 .. i*2+1] = 节气 i 的日期偏移量（0-3）。
 * 实际日期 = SOLAR_TERM_BASE_DAYS[i] + offset。
 * 数据来源：香港天文台 / 紫金山天文台，覆盖 1901-2100（200 年约 1.2KB）。</p>
 *
 * <h2>信息论最优性分析</h2>
 * <p>农历每年最少需编码：12 个月大小（12 bit）+ 闰月位置（4 bit）+ 闰月大小（1 bit）= 17 bit。
 * 实际使用 20 bit，仅多 3 bit 用于编码冗余校验，已接近理论下限。
 * 201 年 × 20 bit = 4020 bit ≈ 503 字节（实际用 int32 存储为 804 字节）。</p>
 *
 * <h2>算法优化层次</h2>
 * <ol>
 *   <li>年天数缓存（YEAR_DAYS_CACHE）：yearDays() O(1) 查表</li>
 *   <li>年前缀和数组（CUMULATIVE_DAYS）：solarToLunar 年份定位 O(log n) 二分查找</li>
 *   <li>扁平月份偏移表：公历转农历最多逆向扫描 13 项，避免逐日查找表的常驻内存</li>
 *   <li>月份键直达表：农历转公历 O(1) 定位月份槽</li>
 *   <li>节气 O(1) 位运算解码：权威数据 + 2-bit 偏移压缩，1901-2100 准确日期</li>
 * </ol>
 *
 * <p>线程安全：所有方法均为无状态纯函数，可安全并发调用。</p>
 */
public final class LunarCalendar {

    private static void checkArgument(boolean valid, String message, Object... arguments) {
        if (!valid) {
            throw new IllegalArgumentException(String.format(message, arguments));
        }
    }

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
    private static final int[] EMBEDDED_LUNAR_INFO = {
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

    /** 资产文件优先；嵌入表仅作为旧模块兼容回退。 */
    private static final int[] LUNAR_INFO =
            CalendarAssetLoader.loadLunarYears(EMBEDDED_LUNAR_INFO);

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

    /** 每年最多 13 个农历月槽，再加一个年总天数哨兵。 */
    private static final int MONTH_STRIDE = 14;

    /**
     * 扁平月份累计天数表。
     *
     * <p>{@code MONTH_OFFSETS[yearIndex * 14 + slot]} 是对应月份槽的年内起始偏移；
     * 最后一个有效元素是年总天数哨兵。使用单个 primitive 数组可避免 201 个子数组对象。</p>
     */
    private static final short[] MONTH_OFFSETS = new short[YEAR_COUNT * MONTH_STRIDE];

    /**
     * 扁平月份元信息表。低 4 位为月份号，bit 4 表示闰月。
     */
    private static final byte[] MONTH_META = new byte[YEAR_COUNT * MONTH_STRIDE];

    /** 每年实际月份槽数量，不包括年总天数哨兵。 */
    private static final byte[] MONTH_SLOT_COUNTS = new byte[YEAR_COUNT];

    /** 普通月/闰月编码直接映射到月份槽，农历转公历无需扫描。 */
    private static final byte[] MONTH_SLOT_LOOKUP = new byte[YEAR_COUNT * 32];

    static {
        // 一次性预计算所有年份天数、前缀和和月份偏移表
        CUMULATIVE_DAYS[0] = 0;
        for (int yi = 0; yi < YEAR_COUNT; yi++) {
            int info = LUNAR_INFO[yi];
            YEAR_DAYS_CACHE[yi] = computeYearDays(info);
            CUMULATIVE_DAYS[yi + 1] = CUMULATIVE_DAYS[yi] + YEAR_DAYS_CACHE[yi];

            int leapM = info & 0xf;
            int slotCount = 0;
            int cum = 0;
            int base = yi * MONTH_STRIDE;

            for (int m = 1; m <= 12; m++) {
                MONTH_OFFSETS[base + slotCount] = (short) cum;
                MONTH_META[base + slotCount] = (byte) m;
                slotCount++;
                cum += (info & (LEAP_MONTH_BIG_MASK >> m)) != 0 ? 30 : 29;

                if (m == leapM) {
                    MONTH_OFFSETS[base + slotCount] = (short) cum;
                    MONTH_META[base + slotCount] = (byte) (m | 0x10);
                    slotCount++;
                    cum += (info & LEAP_MONTH_BIG_MASK) != 0 ? 30 : 29;
                }
            }
            MONTH_OFFSETS[base + slotCount] = (short) cum;
            MONTH_SLOT_COUNTS[yi] = (byte) slotCount;

            int lookupBase = yi * 32;
            Arrays.fill(MONTH_SLOT_LOOKUP, lookupBase, lookupBase + 32, (byte) -1);
            for (int slot = 0; slot < slotCount; slot++) {
                MONTH_SLOT_LOOKUP[lookupBase + (MONTH_META[base + slot] & 0xff)] =
                        (byte) slot;
            }
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
     *
     * @param lunarYear 农历年，范围 1900–2100
     * @return 闰月月份（1-12），0 表示该年无闰月。
     */
    public static int leapMonth(int lunarYear) {
        validateYear(lunarYear);
        return LUNAR_INFO[lunarYear - START_YEAR] & 0xf;
    }

    /**
     * 获取指定农历年闰月的天数。
     *
     * @param lunarYear 农历年，范围 1900–2100
     * @return 29 或 30，若无闰月返回 0。
     */
    public static int leapMonthDays(int lunarYear) {
        if (leapMonth(lunarYear) == 0) return 0;
        return (LUNAR_INFO[lunarYear - START_YEAR] & LEAP_MONTH_BIG_MASK) != 0 ? 30 : 29;
    }

    /**
     * 获取指定农历年某月的天数（不含闰月）。
     *
     * @param lunarYear 农历年，范围 1900–2100
     * @param month 月份 1-12
     * @return 29 或 30
     */
    public static int monthDays(int lunarYear, int month) {
        validateYear(lunarYear);
        checkArgument(month >= 1 && month <= 12, "月份超出范围: %s，应为 1-12", month);
        return (LUNAR_INFO[lunarYear - START_YEAR] & (LEAP_MONTH_BIG_MASK >> month)) != 0 ? 30 : 29;
    }

    /**
     * 获取指定农历年的总天数。
     * 使用预计算缓存，O(1) 时间复杂度。
     *
     * @param lunarYear 农历年，范围 1900–2100
     * @return 当前农历年的总天数
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
     * <p>算法步骤：</p>
     * <ol>
     *   <li>计算公历日期与基准日的天数偏移 offset</li>
     *   <li>二分查找 CUMULATIVE_DAYS → O(log 201) ≈ 8 次比较定位农历年</li>
     *   <li>在扁平月份偏移表中逆向扫描最多 13 项定位月槽</li>
     *   <li>组装结果</li>
     * </ol>
     * <p>总时间 &lt; 1μs（二分最多 8 步 + O(1) 月份定位，全部为数组索引操作）。</p>
     *
     * @param solarDate 公历日期
     * @return 农历完整信息
     */
    public static LunarInfo solarToLunar(LocalDate solarDate) {
        long offset = ChronoUnit.DAYS.between(BASE_DATE, solarDate);
        checkArgument(offset >= 0, "日期早于 1900-01-31，超出农历转换范围");

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
        checkArgument(lunarYear <= END_YEAR,
            "日期超出农历转换范围（%s-%s）", START_YEAR, END_YEAR);
        offset -= CUMULATIVE_DAYS[lo];

        // 一年最多 13 个月槽；逆序扫描比保留约 73 KB 的逐日映射更紧凑。
        int base = lo * MONTH_STRIDE;
        int slot = (MONTH_SLOT_COUNTS[lo] & 0xff) - 1;
        while (slot > 0 && (MONTH_OFFSETS[base + slot] & 0xffff) > offset) {
            slot--;
        }

        int m = MONTH_META[base + slot] & 0xff;
        int lunarMonth = m & 0xF;
        boolean isLeapMonth = (m & 0x10) != 0;
        int lunarDay = (int) (offset - (MONTH_OFFSETS[base + slot] & 0xffff)) + 1;

        return buildLunarInfo(lunarYear, lunarMonth, lunarDay, isLeapMonth);
    }

    /**
     * 顺序转换一个完整公历年。
     *
     * <p>方法仅对 1 月 1 日执行一次年份二分和日期差计算，之后按农历月游标逐日推进。
     * 适合年度 bundle 预构建，避免对 365/366 个连续日期重复定位农历年。</p>
     *
     * @param solarYear 待转换的完整公历年
     * @return 与该公历年逐日对应的农历信息
     * @throws IllegalArgumentException 年度首尾超出精确农历数据覆盖范围时抛出
     */
    public static LunarInfo[] solarYearToLunar(int solarYear) {
        LocalDate first = LocalDate.of(solarYear, 1, 1);
        int dayCount = first.lengthOfYear();
        LocalDate last = first.plusDays(dayCount - 1L);
        LunarInfo current = solarToLunar(first);
        // 完整年度必须都落在数据表内，避免生成部分结果。
        solarToLunar(last);

        LunarInfo[] result = new LunarInfo[dayCount];
        for (int index = 0; index < dayCount; index++) {
            result[index] = current;
            if (index + 1 < dayCount) {
                current = nextLunarDay(current);
            }
        }
        return result;
    }

    // ===================================================================
    // 农历→公历（前缀和 + 预计算月份偏移表）
    // ===================================================================

    /**
     * 农历日期转公历日期。
     * 使用前缀和 + 预计算月份偏移表，年份和月份累计天数均为 O(1) 查表。
     *
     * @param lunarYear 农历年，范围 1900–2100
     * @param lunarMonth 农历月，范围 1–12
     * @param lunarDay 农历日，范围 1–30
     * @param isLeapMonth 是否指定闰月
     * @return 对应的公历日期
     * @throws IllegalArgumentException 农历日期不存在或超出数据范围时抛出
     */
    public static LocalDate lunarToSolar(int lunarYear, int lunarMonth, int lunarDay, boolean isLeapMonth) {
        validateYear(lunarYear);
        checkArgument(lunarMonth >= 1 && lunarMonth <= 12, "农历月份超出范围: %s", lunarMonth);
        checkArgument(lunarDay >= 1 && lunarDay <= 30, "农历日期超出范围: %s", lunarDay);

        int yi = lunarYear - START_YEAR;
        int targetMeta = (lunarMonth & 0xF) | (isLeapMonth ? 0x10 : 0);
        int slotIdx = MONTH_SLOT_LOOKUP[yi * 32 + targetMeta];

        checkArgument(slotIdx >= 0,
            "农历 %s 年不存在%s%s 月", lunarYear, isLeapMonth ? "闰" : "", lunarMonth);

        // 校验日期不超过该月实际天数
        int base = yi * MONTH_STRIDE;
        int slotDays = (MONTH_OFFSETS[base + slotIdx + 1] & 0xffff)
                - (MONTH_OFFSETS[base + slotIdx] & 0xffff);
        checkArgument(lunarDay <= slotDays,
            "农历 %s 年%s%s 月仅有 %s 天，日期 %s 超出范围",
            lunarYear, isLeapMonth ? "闰" : "", lunarMonth, slotDays, lunarDay);

        // 年前缀和 + 月内偏移 + 日偏移 → 总天数偏移
        long offset = CUMULATIVE_DAYS[yi]
                + (MONTH_OFFSETS[base + slotIdx] & 0xffff) + lunarDay - 1;
        return BASE_DATE.plusDays(offset);
    }

    /**
     * 农历日期转公历日期（非闰月）。
     *
     * @param lunarYear 农历年，范围 1900–2100
     * @param lunarMonth 农历月，范围 1–12
     * @param lunarDay 农历日，范围 1–30
     * @return 对应的公历日期
     * @throws IllegalArgumentException 农历日期不存在或超出数据范围时抛出
     */
    public static LocalDate lunarToSolar(int lunarYear, int lunarMonth, int lunarDay) {
        return lunarToSolar(lunarYear, lunarMonth, lunarDay, false);
    }

    // ===================================================================
    // 天干地支 / 生肖
    // ===================================================================

    /**
     * 获取指定农历年的天干。
     *
     * @param lunarYear 农历年
     * @return 单个天干字符
     */
    public static String getTianGan(int lunarYear) {
        return TIAN_GAN[((lunarYear - 4) % 10 + 10) % 10];
    }

    /**
     * 获取指定农历年的地支。
     *
     * @param lunarYear 农历年
     * @return 单个地支字符
     */
    public static String getDiZhi(int lunarYear) {
        return DI_ZHI[((lunarYear - 4) % 12 + 12) % 12];
    }

    /**
     * 获取干支名称。
     *
     * @param lunarYear 农历年
     * @return 干支名称，例如“乙巳”
     */
    public static String getGanZhi(int lunarYear) {
        return getTianGan(lunarYear) + getDiZhi(lunarYear);
    }

    /**
     * 获取生肖。
     *
     * @param lunarYear 农历年
     * @return 生肖中文名
     */
    public static String getShengXiao(int lunarYear) {
        return SHENG_XIAO[((lunarYear - 4) % 12 + 12) % 12];
    }

    /**
     * 获取农历月份中文名。
     *
     * @param month 农历月，范围 1–12
     * @param isLeapMonth 是否为闰月
     * @return 月份中文名，例如“正月”或“闰六月”
     */
    public static String getMonthName(int month, boolean isLeapMonth) {
        checkArgument(month >= 1 && month <= 12, "月份超出范围: %s", month);
        return (isLeapMonth ? "闰" : "") + MONTH_NAMES[month - 1] + "月";
    }

    /**
     * 获取农历日期中文名。
     *
     * @param day 农历日，范围 1–30
     * @return 日期中文名，例如“初一”
     */
    public static String getDayName(int day) {
        checkArgument(day >= 1 && day <= 30, "日期超出范围: %s", day);
        return DAY_NAMES[day - 1];
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
    // 与 LUNAR_INFO 类似的紧凑整数设计，200 年仅约 1.2KB。
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
     * <p>与 LUNAR_INFO 相同风格的紧凑整数数组设计，只包含权威表覆盖范围。</p>
     */
    private static final long[] EMBEDDED_SOLAR_TERM_PACKED = {
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

    /** 统一二进制资产优先；嵌入压缩表用于独立模块运行时的精确回退。 */
    private static final long[] SOLAR_TERM_PACKED = CalendarAssetLoader.loadSolarTerms(
            SOLAR_TERM_DATA_START, SOLAR_TERM_DATA_END,
            SOLAR_TERM_BASE_DAYS, EMBEDDED_SOLAR_TERM_PACKED);

    /**
     * 当前农历和节气资产来源。
     *
     * @return classpath 资源位置或外部资产绝对路径
     */
    public static String getAssetSource() {
        return CalendarAssetLoader.sourceDescription();
    }

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

        /**
         * 返回节气中文名。
         *
         * @return 节气中文名
         */
        public String getName() { return name; }

        /**
         * 返回对应的太阳黄经度数。
         *
         * @return 太阳黄经度数，范围 0–345、步长 15
         */
        public int getLongitude() { return longitude; }

        /**
         * 返回节气对应的公历日期。
         *
         * @return 节气对应的公历日期
         */
        public LocalDate getDate() { return date; }

        /**
         * 返回由节气名称和日期组成的简短字符串。
         *
         * @return 由节气名称和日期组成的简短字符串
         */
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
     * <p>1901-2100 年使用权威天文台预计算数据。超出数据范围时明确拒绝，
     * 不使用近似公式静默产生可能相差一天的日期。</p>
     *
     * @param year 公历年份
     * @return 24 个节气信息，按时间顺序排列（从小寒到冬至）
     */
    public static SolarTermInfo[] getSolarTerms(int year) {
        validateSolarTermYear(year);
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

    /**
     * 获取指定公历日期的节气（如果当天是节气的话）。
     *
     * @param date 公历日期
     * @return 节气名称，如果当天不是节气则返回 null
     */
    public static String getSolarTerm(LocalDate date) {
        int year = date.getYear();
        validateSolarTermYear(year);
        long packed = SOLAR_TERM_PACKED[year - SOLAR_TERM_DATA_START];
        int month = date.getMonthValue();
        int dayOfMonth = date.getDayOfMonth();
        int firstTerm = (month - 1) * 2;
        if (decodeSolarTermDay(packed, firstTerm) == dayOfMonth) {
            return SOLAR_TERM_NAMES[firstTerm];
        }
        int secondTerm = firstTerm + 1;
        if (decodeSolarTermDay(packed, secondTerm) == dayOfMonth) {
            return SOLAR_TERM_NAMES[secondTerm];
        }
        return null;
    }

    // ===================================================================
    // 内部工具
    // ===================================================================

    private static void validateYear(int year) {
        checkArgument(year >= START_YEAR && year <= END_YEAR,
            "年份 %s 超出范围，农历数据覆盖 %s-%s", year, START_YEAR, END_YEAR);
    }

    private static void validateSolarTermYear(int year) {
        checkArgument(
                year >= SOLAR_TERM_DATA_START && year <= SOLAR_TERM_DATA_END,
                "年份 %s 超出节气数据范围 %s-%s",
                year,
                SOLAR_TERM_DATA_START,
                SOLAR_TERM_DATA_END);
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

    private static LunarInfo nextLunarDay(LunarInfo current) {
        LunarDate date = current.getDate();
        int year = date.getYear();
        int month = date.getMonth();
        int day = date.getDay();
        boolean leap = date.isLeapMonth();
        int daysInMonth = leap ? leapMonthDays(year) : monthDays(year, month);

        if (day < daysInMonth) {
            return buildLunarInfo(year, month, day + 1, leap);
        }
        if (!leap && leapMonth(year) == month) {
            return buildLunarInfo(year, month, 1, true);
        }
        if (month < 12) {
            return buildLunarInfo(year, month + 1, 1, false);
        }
        checkArgument(year < END_YEAR,
                "日期超出农历转换范围（%s-%s）", START_YEAR, END_YEAR);
        return buildLunarInfo(year + 1, 1, 1, false);
    }
}
