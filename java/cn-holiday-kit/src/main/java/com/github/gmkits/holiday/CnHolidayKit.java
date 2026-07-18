package com.github.gmkits.holiday;

import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.core.HolidayServiceBuilder;
import com.github.gmkits.holiday.lunar.LunarCalendar;
import com.github.gmkits.holiday.lunar.LunarInfo;

import java.nio.file.Path;
import java.time.LocalDate;

/**
 * cn-holiday-kit 的统一 Java 入口。
 *
 * <p>业务项目只需依赖 {@code com.github.gmkits:cn-holiday-kit}。原有
 * {@code core/spec/lunar} 包名继续保留，避免已有调用方迁移。</p>
 */
public final class CnHolidayKit {

    private CnHolidayKit() {
    }

    /**
     * 使用随库发布的离线日期资产。
     */
    public static HolidayService create() {
        return builder().build();
    }

    /**
     * 从一个统一资产根目录加载农历、节气和节假日数据。
     */
    public static HolidayService fromAssets(Path assetRoot) {
        return builder().assetPath(assetRoot).build();
    }

    public static HolidayServiceBuilder builder() {
        return new HolidayServiceBuilder();
    }

    public static LunarInfo solarToLunar(LocalDate date) {
        return LunarCalendar.solarToLunar(date);
    }

    public static LocalDate lunarToSolar(int year, int month, int day) {
        return LunarCalendar.lunarToSolar(year, month, day);
    }

    public static LocalDate lunarToSolar(int year, int month, int day, boolean leapMonth) {
        return LunarCalendar.lunarToSolar(year, month, day, leapMonth);
    }

    public static LunarCalendar.SolarTermInfo[] getSolarTerms(int year) {
        return LunarCalendar.getSolarTerms(year);
    }

    public static String getSolarTerm(LocalDate date) {
        return LunarCalendar.getSolarTerm(date);
    }

    public static String getAssetSource() {
        return LunarCalendar.getAssetSource();
    }
}
