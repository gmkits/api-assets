package com.github.gmkits.holiday.spec;

/**
 * 节假日日历中使用的日期分类枚举。
 */
public enum DayKind {

    /** 法律规定的节假日（如国庆节）。 */
    STATUTORY_HOLIDAY,

    /** 放假安排中的节假日，不一定属于法定节假日。 */
    OFFICIAL_HOLIDAY,

    /** 因调休而安排的补班工作日。 */
    ADJUSTED_WORKDAY,

    /** 普通工作日（周一至周五，且不是节假日）。 */
    NORMAL_WORKDAY,

    /** 普通周末（周六或周日）。 */
    NORMAL_WEEKEND
}
