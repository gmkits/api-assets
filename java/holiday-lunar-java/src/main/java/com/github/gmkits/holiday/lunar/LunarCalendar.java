package com.github.gmkits.holiday.lunar;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 中国农历转换器。
 *
 * <p>基于香港天文台（HKO）和紫金山天文台数据，覆盖 1900-2100 年的公历↔农历转换。
 * 每年仅用一个 20-bit 整数编码，201 年数据总共约 800 字节。</p>
 *
 * <p>编码格式（每年一个整数）：
 * <ul>
 *   <li>bit 0-3：闰月月份（0 = 无闰月，1-12 = 闰几月）</li>
 *   <li>bit 4：闰月天数（0 = 29 天，1 = 30 天）</li>
 *   <li>bit 5-16：正常 1-12 月的天数（0 = 29 天，1 = 30 天）</li>
 * </ul></p>
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
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a4d0, 0x0d150, 0x0f252,
        0x0d520,
    };

    private LunarCalendar() {
        // 工具类不可实例化
    }

    /** 闰月大月位掩码（bit 16）。 */
    private static final int LEAP_MONTH_BIG_MASK = 0x10000;

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
     */
    public static int yearDays(int lunarYear) {
        validateYear(lunarYear);
        int total = 0;
        int info = LUNAR_INFO[lunarYear - START_YEAR];
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
    // 公历→农历
    // ===================================================================

    /**
     * 公历日期转农历日期。
     *
     * @param solarDate 公历日期
     * @return 农历完整信息
     */
    public static LunarInfo solarToLunar(LocalDate solarDate) {
        long offset = ChronoUnit.DAYS.between(BASE_DATE, solarDate);
        if (offset < 0) {
            throw new IllegalArgumentException("日期早于 1900-01-31，超出农历转换范围");
        }

        // 定位农历年
        int lunarYear = START_YEAR;
        int daysInYear;
        while (lunarYear <= END_YEAR) {
            daysInYear = yearDays(lunarYear);
            if (offset < daysInYear) break;
            offset -= daysInYear;
            lunarYear++;
        }
        if (lunarYear > END_YEAR) {
            throw new IllegalArgumentException("日期超出农历转换范围（" + START_YEAR + "-" + END_YEAR + "）");
        }

        // 定位农历月和日
        int leap = leapMonth(lunarYear);
        int lunarMonth = 1;
        boolean isLeapMonth = false;
        int daysInMonth;
        boolean found = false;

        for (int m = 1; m <= 12; m++) {
            daysInMonth = monthDays(lunarYear, m);
            if (offset < daysInMonth) {
                lunarMonth = m;
                found = true;
                break;
            }
            offset -= daysInMonth;

            if (m == leap) {
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

        int lunarDay = (int) offset + 1;
        return buildLunarInfo(lunarYear, lunarMonth, lunarDay, isLeapMonth);
    }

    // ===================================================================
    // 农历→公历
    // ===================================================================

    /**
     * 农历日期转公历日期。
     */
    public static LocalDate lunarToSolar(int lunarYear, int lunarMonth, int lunarDay, boolean isLeapMonth) {
        validateYear(lunarYear);
        if (lunarMonth < 1 || lunarMonth > 12) {
            throw new IllegalArgumentException("农历月份超出范围: " + lunarMonth);
        }
        if (lunarDay < 1 || lunarDay > 30) {
            throw new IllegalArgumentException("农历日期超出范围: " + lunarDay);
        }

        long offset = 0;
        for (int y = START_YEAR; y < lunarYear; y++) {
            offset += yearDays(y);
        }

        int leap = leapMonth(lunarYear);
        for (int m = 1; m < lunarMonth; m++) {
            offset += monthDays(lunarYear, m);
            if (m == leap) {
                offset += leapMonthDays(lunarYear);
            }
        }

        if (isLeapMonth && lunarMonth == leap) {
            offset += monthDays(lunarYear, lunarMonth);
        }

        offset += lunarDay - 1;
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
