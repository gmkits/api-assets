package com.github.gmkits.holiday.api25.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地缓存配置。
 */
@Configuration
public class CacheConfiguration {

    @Bean
    public CacheManager cacheManager(HolidayApi25Properties properties) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(Math.max(
                        properties.getCache().getDayMaximumSize(),
                        Math.max(properties.getCache().getYearMaximumSize(), properties.getCache().getMetadataMaximumSize())
                ))
                .expireAfterWrite(properties.getCache().getExpireAfterWrite());
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("dayInfo", "yearInfo", "bundleMetadata");
        cacheManager.setCaffeine(builder);
        return cacheManager;
    }
}
