package com.github.gmkits.apiassets.calendar.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.gmkits.apiassets.calendar.spec.DayInfo;
import com.github.gmkits.apiassets.calendar.spec.LunarDateInfo;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 统一 HTTP JSON 与通用 DayInfo Schema 的字段名。
 *
 * <p>Java 库继续使用惯用的 {@code isHoliday()} 布尔访问器；HTTP 层通过
 * Jackson MixIn 输出 {@code isHoliday}，避免给纯 Java 库引入 Jackson 依赖。</p>
 */
@Configuration
public class DayInfoJsonConfiguration {

    /**
     * 注册不侵入 Java 值对象的 JSON 字段映射。
     *
     * @return Jackson 构建器定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer dayInfoJsonCustomizer() {
        return builder -> {
            builder.mixIn(DayInfo.class, DayInfoMixin.class);
            builder.mixIn(LunarDateInfo.class, LunarDateInfoMixin.class);
        };
    }

    private abstract static class DayInfoMixin {
        @JsonProperty("isHoliday")
        abstract boolean isHoliday();

        @JsonProperty("isOfficialHoliday")
        abstract boolean isOfficialHoliday();

        @JsonProperty("isWorkday")
        abstract boolean isWorkday();

        @JsonProperty("isWeekend")
        abstract boolean isWeekend();

        @JsonProperty("isStatutoryHoliday")
        abstract boolean isStatutoryHoliday();

        @JsonProperty("isAdjustedWorkday")
        abstract boolean isAdjustedWorkday();
    }

    private abstract static class LunarDateInfoMixin {
        @JsonProperty("isLeapMonth")
        abstract boolean isLeapMonth();
    }
}
