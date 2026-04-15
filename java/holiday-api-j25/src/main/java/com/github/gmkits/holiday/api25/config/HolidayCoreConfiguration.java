package com.github.gmkits.holiday.api25.config;

import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.core.HolidayServiceBuilder;
import com.google.common.base.Strings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

/**
 * 组装底层 holiday core 查询服务。
 */
@Configuration
public class HolidayCoreConfiguration {

    @Bean
    public HolidayService holidayService(HolidayApi25Properties properties) {
        HolidayServiceBuilder builder = new HolidayServiceBuilder()
                .defaultRegion(properties.getDefaultRegion())
                .enableClasspathFallback(properties.isClasspathFallback());
        if (!Strings.isNullOrEmpty(properties.getDataPath())) {
            builder.dataPath(Paths.get(properties.getDataPath()));
        }
        return builder.build();
    }
}
