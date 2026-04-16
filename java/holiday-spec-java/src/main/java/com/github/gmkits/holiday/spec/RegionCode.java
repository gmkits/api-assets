package com.github.gmkits.holiday.spec;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

/**
 * 对已校验区域代码字符串的不可变值对象封装。
 *
 * <p>合法区域代码仅允许包含 ASCII 字母、数字和连字符，
 * 例如 {@code "CN"}、{@code "CN-BJ"}。</p>
 */
@Getter
@EqualsAndHashCode
@ToString
public final class RegionCode {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Za-z0-9\\-]+$");

    private final String code;

    /**
     * 在校验格式后创建新的 {@code RegionCode}。
     *
     * @param code 区域代码字符串
     * @throws IllegalArgumentException 当 {@code code} 为 null、空字符串，
     *                                  或包含字母、数字、连字符之外的字符时抛出
     */
    public RegionCode(String code) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(code),
                "区域代码不能为 null 或空字符串");
        Preconditions.checkArgument(VALID_PATTERN.matcher(code).matches(),
                "无效的区域代码: '%s'，仅允许字母、数字和连字符", code);
        this.code = code;
    }
}
