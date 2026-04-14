package com.github.gmkits.holiday.spring;

import com.github.gmkits.holiday.core.HolidayService;
import com.github.gmkits.holiday.core.HolidayServiceBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

/**
 * Spring Boot auto-configuration for {@link HolidayService}.
 *
 * <p>Registers a default {@link HolidayService} bean if none is already
 * present in the application context. Configuration is driven by
 * {@link HolidayProperties}.</p>
 */
@Configuration
@EnableConfigurationProperties(HolidayProperties.class)
public class HolidayAutoConfiguration {

    /**
     * Creates a {@link HolidayService} bean from the bound properties
     * unless one is already defined.
     *
     * @param properties the holiday configuration properties
     * @return a new {@link HolidayService} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public HolidayService holidayService(HolidayProperties properties) {
        HolidayServiceBuilder builder = new HolidayServiceBuilder()
                .defaultRegion(properties.getDefaultRegion())
                .enableClasspathFallback(properties.isClasspathFallback());
        if (properties.getDataPath() != null && !properties.getDataPath().isEmpty()) {
            builder.dataPath(Paths.get(properties.getDataPath()));
        }
        return builder.build();
    }
}
