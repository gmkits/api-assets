package com.github.gmkits.holiday.api25.validation;

import jakarta.validation.constraints.Pattern;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 地区代码校验。
 *
 * <p>对外查询参数 {@code regionCode} 的统一约束（2-32 位大写字母 / 数字 / 下划线 / 连字符）。
 * 抽取为元注解避免在每个控制器方法上重复声明 {@link Pattern}。</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE_USE})
@Pattern(regexp = "^[A-Z0-9_-]{2,32}$",
        message = "regionCode 必须是 2-32 位大写字母、数字、下划线或连字符")
public @interface RegionCode {
}
