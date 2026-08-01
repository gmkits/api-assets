package com.github.gmkits.holiday.core;

/**
 * 请求的地区或年份没有可用的离线节假日数据。
 *
 * <p>该异常用于区分“数据未覆盖”和二进制损坏。前者可由 API 映射为
 * {@code 404}，后者仍作为服务端数据故障暴露，不能静默当成普通工作日。</p>
 */
public final class HolidayDataUnavailableException extends RuntimeException {

    /** 缺少数据的地区代码。 */
    private final String regionCode;
    /** 缺少数据的公历年份。 */
    private final int year;

    /**
     * 创建数据缺失异常。
     *
     * @param regionCode 地区代码
     * @param year 公历年份
     */
    public HolidayDataUnavailableException(String regionCode, int year) {
        super("Holiday data is unavailable: " + regionCode + "/" + year);
        this.regionCode = regionCode;
        this.year = year;
    }

    /**
     * 返回缺少数据的地区代码。
     *
     * @return 缺少数据的地区代码
     */
    public String getRegionCode() {
        return regionCode;
    }

    /**
     * 返回缺少数据的公历年份。
     *
     * @return 缺少数据的公历年份
     */
    public int getYear() {
        return year;
    }
}
