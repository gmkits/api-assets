package com.github.gmkits.holiday.api25.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * holiday-api-j25 配置。
 */
@Data
@ConfigurationProperties(prefix = "holiday.api")
public class HolidayApi25Properties {

    private String apiVersion = "2.0.0";
    private String defaultRegion = "CN";
    private String dataPath;
    private boolean classpathFallback = true;
    private String manifestLocation = "classpath:manifest.json";
    private boolean preloadCurrentAndNextYear = true;
    private List<String> preloadRegions = new ArrayList<String>();
    private List<Integer> preloadYears = new ArrayList<Integer>();
    private Cache cache = new Cache();

    @Data
    public static class Cache {
        private long dayMaximumSize = 4096;
        private long yearMaximumSize = 128;
        private long metadataMaximumSize = 256;
        private Duration expireAfterWrite = Duration.ofMinutes(30);
    }
}
