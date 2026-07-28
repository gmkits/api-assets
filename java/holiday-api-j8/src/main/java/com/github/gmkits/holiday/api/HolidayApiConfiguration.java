package com.github.gmkits.holiday.api;

import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.core.HolidayServiceBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

/**
 * HTTP 服务内部配置。
 */
@Configuration
@EnableConfigurationProperties(HolidayProperties.class)
public class HolidayApiConfiguration {

    /**
     * 创建唯一的线程安全日期查询服务。
     *
     * @param properties HTTP 服务配置
     * @return 日期查询服务
     */
    @Bean
    public HolidayService holidayService(HolidayProperties properties) {
        HolidayServiceBuilder builder = new HolidayServiceBuilder()
                .defaultRegion(properties.getDefaultRegion())
                .enableClasspathFallback(properties.isClasspathFallback());
        if (hasText(properties.getAssetPath())) {
            builder.assetPath(Paths.get(properties.getAssetPath()));
        }
        return builder.build();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
