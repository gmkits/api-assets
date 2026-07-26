package com.github.gmkits.holiday.spec;

/**
 * 节气信息。
 *
 * <p>作为 {@link DayInfo#getSolarTerm()} 的稳定值对象，
 * 提供命中日期对应的稳定索引和中文名。</p>
 */
public final class SolarTermInfo {

    private final int index;
    private final String name;

    /**
     * 创建节气信息。
     *
     * @param index 节气索引，范围 0–23
     * @param name 节气中文名
     */
    public SolarTermInfo(int index, String name) {
        this.index = index;
        this.name = name;
    }

    /**
     * 返回节气索引。
     *
     * @return 节气索引，范围 0–23
     */
    public int getIndex() { return index; }

    /**
     * 返回节气中文名。
     *
     * @return 节气中文名
     */
    public String getName() { return name; }

    /**
     * 返回节气信息的调试字符串。
     *
     * @return 节气信息的调试字符串
     */
    @Override
    public String toString() {
        return "SolarTermInfo{index=" + index + ", name='" + name + "'}";
    }
}
