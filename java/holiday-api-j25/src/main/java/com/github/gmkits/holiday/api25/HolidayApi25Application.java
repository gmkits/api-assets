package com.github.gmkits.holiday.api25;

import com.github.gmkits.holiday.api25.config.HolidayApi25Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Java 25 / Spring Boot 4 内网 API 服务入口。
 */
@SpringBootApplication
@EnableCaching
@ConfigurationPropertiesScan(basePackageClasses = HolidayApi25Properties.class)
public class HolidayApi25Application {

    public static void main(String[] args) {
        SpringApplication.run(HolidayApi25Application.class, args);
    }
}
