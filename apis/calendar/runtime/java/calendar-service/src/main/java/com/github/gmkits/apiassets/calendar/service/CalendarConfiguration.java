package com.github.gmkits.apiassets.calendar.service;

import com.github.gmkits.apiassets.calendar.core.HolidayService;
import com.github.gmkits.apiassets.calendar.core.HolidayServiceBuilder;
import com.github.gmkits.apiassets.calendar.lunar.LunarCalendar;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.nio.file.Path;

/** 组装唯一的日历运行时，并在接受流量前验证全部离线资产。 */
@Configuration
@EnableConfigurationProperties(CalendarProperties.class)
public class CalendarConfiguration {

    @Bean
    public HolidayService holidayService(CalendarProperties properties,
                                         ValidatedAssetStore assets) {
        HolidayServiceBuilder builder = new HolidayServiceBuilder()
                .defaultRegion(properties.getDefaultRegion())
                .enableClasspathFallback(!properties.usesExternalAssets());
        if (properties.usesExternalAssets()) {
            builder.assetPath(Path.of(properties.getAssetPath()));
        }
        HolidayService service = builder.build();

        // 逐年加载会同时验证 manifest SHA-256、hday CRC32 和业务索引。
        for (int year = assets.holidayStartYear(); year <= assets.holidayEndYear(); year++) {
            service.getYear(assets.region(), year);
        }
        LunarCalendar.solarToLunar(java.time.LocalDate.of(1900, 1, 31));
        LunarCalendar.getSolarTerms(1901);
        return service;
    }

    /** 为确定性 JSON 响应补充条件请求能力，已有二进制 ETag 不会被覆盖。 */
    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> jsonEtagFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/v1/calendar/*");
        registration.setOrder(100);
        return registration;
    }
}
