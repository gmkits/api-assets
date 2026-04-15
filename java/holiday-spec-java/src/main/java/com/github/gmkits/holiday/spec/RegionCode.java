package com.github.gmkits.holiday.spec;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.regex.Pattern;

/**
 * An immutable value class that wraps a validated region code string.
 *
 * <p>Valid region codes contain only ASCII letters, digits, and hyphens
 * (e.g. {@code "CN"}, {@code "CN-BJ"}).</p>
 */
@Getter
@EqualsAndHashCode
@ToString
public final class RegionCode {

    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Za-z0-9\\-]+$");

    private final String code;

    /**
     * Creates a new {@code RegionCode} after validating its format.
     *
     * @param code the region code string
     * @throws IllegalArgumentException if {@code code} is null, empty, or
     *                                  contains characters other than letters,
     *                                  digits, and hyphens
     */
    public RegionCode(String code) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(code),
                "区域代码不能为 null 或空字符串");
        Preconditions.checkArgument(VALID_PATTERN.matcher(code).matches(),
                "无效的区域代码: '%s'，仅允许字母、数字和连字符", code);
        this.code = code;
    }
}
