package com.github.gmkits.holiday.spec;

import java.util.Objects;

/**
 * 中国农历年的干支与生肖属性。
 *
 * <p>本对象采用农历年边界：春节当天进入新的干支年。由于干支月、干支日存在
 * 流派和换日边界差异，本库只返回无歧义且与离线农历数据一致的年柱属性。</p>
 */
public final class GanZhiInfo {

    private final String yearName;
    private final String heavenlyStem;
    private final String earthlyBranch;
    private final String zodiac;

    /**
     * 创建干支纪年信息。
     *
     * @param yearName 干支纪年，例如 {@code 乙巳}
     * @param heavenlyStem 天干，例如 {@code 乙}
     * @param earthlyBranch 地支，例如 {@code 巳}
     * @param zodiac 生肖，例如 {@code 蛇}
     */
    public GanZhiInfo(String yearName, String heavenlyStem,
                      String earthlyBranch, String zodiac) {
        this.yearName = Objects.requireNonNull(yearName, "yearName");
        this.heavenlyStem = Objects.requireNonNull(heavenlyStem, "heavenlyStem");
        this.earthlyBranch = Objects.requireNonNull(earthlyBranch, "earthlyBranch");
        this.zodiac = Objects.requireNonNull(zodiac, "zodiac");
    }

    /**
     * 返回不带“年”后缀的干支纪年。
     *
     * @return 干支纪年
     */
    public String getYearName() { return yearName; }

    /**
     * 返回年干。
     *
     * @return 天干
     */
    public String getHeavenlyStem() { return heavenlyStem; }

    /**
     * 返回年支。
     *
     * @return 地支
     */
    public String getEarthlyBranch() { return earthlyBranch; }

    /**
     * 返回生肖。
     *
     * @return 生肖
     */
    public String getZodiac() { return zodiac; }

    /**
     * 返回干支信息的调试字符串。
     *
     * @return 包含干支纪年的简要字符串
     */
    @Override
    public String toString() {
        return "GanZhiInfo{yearName='" + yearName + "', zodiac='" + zodiac + "'}";
    }
}
