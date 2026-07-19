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
 *
 * <p>发布物是面向 Java 8 编译的单一 JAR。JDK 9 及以上运行时可通过稳定自动模块名
 * {@code com.github.gmkits.holiday} 放入 module path；JDK 8 会忽略模块清单项。</p>
 */
public final class CnHolidayKit {

    private CnHolidayKit() {
    }

    /**
     * 使用随库发布的离线日期资产。
     *
     * @return 使用默认区域 {@code CN} 的线程安全查询服务
     */
    public static HolidayService create() {
        return builder().build();
    }

    /**
     * 从一个统一资产根目录加载农历、节气和节假日数据。
     *
     * <p>日历资产在 JVM 第一次使用时初始化；应在任何农历或节气查询前调用。
     * 节假日 bundle 可在替换文件后通过 {@link HolidayService#clearCache()} 热更新。</p>
     *
     * @param assetRoot 包含 {@code calendar} 和 {@code holidays} 的资产根目录
     * @return 使用指定外部资产的线程安全查询服务
     */
    public static HolidayService fromAssets(Path assetRoot) {
        return builder().assetPath(assetRoot).build();
    }

    /**
     * 创建高级配置构建器。
     *
     * @return 新的服务构建器
     */
    public static HolidayServiceBuilder builder() {
        return new HolidayServiceBuilder();
    }

    /**
     * 将公历日期转换为农历完整信息。
     *
     * @param date 公历日期
     * @return 对应的农历完整信息
     * @throws IllegalArgumentException 日期超出 1900–2100 数据范围时抛出
     */
    public static LunarInfo solarToLunar(LocalDate date) {
        return LunarCalendar.solarToLunar(date);
    }

    /**
     * 将非闰月农历日期转换为公历日期。
     *
     * @param year 农历年
     * @param month 农历月，范围 1–12
     * @param day 农历日，范围 1–30
     * @return 对应公历日期
     * @throws IllegalArgumentException 日期无效或超出数据范围时抛出
     */
    public static LocalDate lunarToSolar(int year, int month, int day) {
        return LunarCalendar.lunarToSolar(year, month, day);
    }

    /**
     * 将包含闰月标记的农历日期转换为公历日期。
     *
     * @param year 农历年
     * @param month 农历月，范围 1–12
     * @param day 农历日，范围 1–30
     * @param leapMonth 是否为闰月
     * @return 对应公历日期
     * @throws IllegalArgumentException 日期无效或超出数据范围时抛出
     */
    public static LocalDate lunarToSolar(int year, int month, int day, boolean leapMonth) {
        return LunarCalendar.lunarToSolar(year, month, day, leapMonth);
    }

    /**
     * 查询指定公历年的全部二十四节气。
     *
     * @param year 公历年份
     * @return 从小寒到冬至排列的 24 个节气；调用方可安全修改返回数组
     */
    public static LunarCalendar.SolarTermInfo[] getSolarTerms(int year) {
        return LunarCalendar.getSolarTerms(year);
    }

    /**
     * 查询指定日期命中的节气。
     *
     * @param date 公历日期
     * @return 节气中文名；当天不是节气时返回 {@code null}
     */
    public static String getSolarTerm(LocalDate date) {
        return LunarCalendar.getSolarTerm(date);
    }

    /**
     * 返回当前日历资产来源，便于诊断部署配置。
     *
     * @return classpath 资源位置或外部资产绝对路径
     */
    public static String getAssetSource() {
        return LunarCalendar.getAssetSource();
    }
}
