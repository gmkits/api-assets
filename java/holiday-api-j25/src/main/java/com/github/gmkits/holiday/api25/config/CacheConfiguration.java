package com.github.gmkits.holiday.api25.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 分层本地缓存配置。
 *
 * <p>为不同类型的缓存使用独立的 Caffeine 配置，确保每层的
 * maximumSize 和 TTL 可以单独调优：</p>
 * <ul>
 *   <li>{@code dayInfo}: 高频单日查询，大容量短 TTL</li>
 *   <li>{@code yearInfo}: 整年查询，中等容量长 TTL</li>
 *   <li>{@code bundleMetadata}: 元数据，小容量长 TTL</li>
 * </ul>
 */
@Configuration
public class CacheConfiguration {

    @Bean
    @Primary
    public CacheManager cacheManager(HolidayApi25Properties properties) {
        HolidayApi25Properties.Cache cfg = properties.getCache();

        // 单日缓存：大容量
        CaffeineCacheManager dayCacheManager = new CaffeineCacheManager("dayInfo");
        dayCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(cfg.getDayMaximumSize())
                .expireAfterWrite(cfg.getExpireAfterWrite())
                .recordStats());

        // 年缓存：中等容量，年数据变化少，TTL 加倍
        CaffeineCacheManager yearCacheManager = new CaffeineCacheManager("yearInfo");
        yearCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(cfg.getYearMaximumSize())
                .expireAfterWrite(cfg.getExpireAfterWrite().multipliedBy(2))
                .recordStats());

        // 元数据缓存：小容量，几乎不变
        CaffeineCacheManager metadataCacheManager = new CaffeineCacheManager("bundleMetadata");
        metadataCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(cfg.getMetadataMaximumSize())
                .expireAfterWrite(cfg.getExpireAfterWrite().multipliedBy(4))
                .recordStats());

        CompositeCacheManager composite = new CompositeCacheManager(
                dayCacheManager, yearCacheManager, metadataCacheManager);
        composite.setFallbackToNoOpCache(false);
        return composite;
    }
}
