package com.github.gmkits.holiday.spec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 公历日对应的节日或纪念日。
 *
 * <p>节日与“当天是否放假”相互独立。例如元宵节会出现在本列表中，
 * 但通常不是休息日；具体工作状态仍以 {@link DayInfo#isWorkday()} 为准。</p>
 */
public final class FestivalInfo {

    private final String code;
    private final Map<String, String> names;

    /**
     * 创建节日信息。
     *
     * @param code 稳定的大写英文代码
     * @param names 语言区域到显示名称的映射
     */
    public FestivalInfo(String code, Map<String, String> names) {
        this.code = Objects.requireNonNull(code, "code");
        this.names = Collections.unmodifiableMap(new LinkedHashMap<>(
                Objects.requireNonNull(names, "names")));
    }

    /**
     * 返回稳定节日代码。
     *
     * @return 节日代码
     */
    public String getCode() {
        return code;
    }

    /**
     * 返回不可变多语言名称映射。
     *
     * @return 语言区域到名称的映射
     */
    public Map<String, String> getNames() {
        return names;
    }

    /**
     * 返回节日调试字符串。
     *
     * @return 包含代码的简要字符串
     */
    @Override
    public String toString() {
        return "FestivalInfo{code='" + code + "'}";
    }
}
