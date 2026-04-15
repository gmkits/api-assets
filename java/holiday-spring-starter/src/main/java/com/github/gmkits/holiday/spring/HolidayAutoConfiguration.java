package com.github.gmkits.holiday.spring;

import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.core.HolidayServiceBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Strings;

import java.nio.file.Paths;

/**
 * HolidayService 的 Spring Boot 自动配置。
 */
@Configuration
@EnableConfigurationProperties(HolidayProperties.class)
public class HolidayAutoConfiguration {

    /**
     * 当用户未自定义 {@link HolidayService} Bean 时，按配置创建默认实现。
     */
    @Bean
    @ConditionalOnMissingBean
    public HolidayService holidayService(HolidayProperties properties) {
        HolidayServiceBuilder builder = new HolidayServiceBuilder()
                .defaultRegion(properties.getDefaultRegion())
                .enableClasspathFallback(properties.isClasspathFallback());
        if (!Strings.isNullOrEmpty(properties.getDataPath())) {
            builder.dataPath(Paths.get(properties.getDataPath()));
        }
        return builder.build();
    }
}
