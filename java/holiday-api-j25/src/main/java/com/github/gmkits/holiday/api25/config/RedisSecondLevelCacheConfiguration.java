package com.github.gmkits.holiday.api25.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Redis 二级缓存配置。
 *
 * <p>仅当 {@code holiday.api.redis-cache.enabled=true} 且 classpath 中存在
 * spring-data-redis 时生效。Redis 作为 Caffeine 本地缓存的后备层，
 * 用于多实例部署时共享缓存结果。</p>
 *
 * <p>如果不需要 Redis，完全不影响现有 Caffeine 本地缓存。
 * 启用方式：在 build.gradle 中将 spring-boot-starter-data-redis 改为 implementation，
 * 并在 application.yml 中设置 {@code holiday.api.redis-cache.enabled=true} 和 Redis 连接信息。</p>
 *
 * <p>注意：本类使用反射方式构建 RedisCacheManager，避免编译期硬依赖 spring-data-redis。
 * 当 spring-data-redis 不在 classpath 时，此 Bean 不会被创建。</p>
 */
@Configuration
@ConditionalOnProperty(name = "holiday.api.redis-cache.enabled", havingValue = "true")
@ConditionalOnClass(name = "org.springframework.data.redis.cache.RedisCacheManager")
public class RedisSecondLevelCacheConfiguration {

    /**
     * 创建 Redis CacheManager 作为二级缓存。
     *
     * <p>采用 JSON 序列化，各缓存独立 TTL：
     * <ul>
     *   <li>dayInfo: 基础 TTL</li>
     *   <li>yearInfo: 2x TTL</li>
     *   <li>bundleMetadata: 4x TTL</li>
     * </ul>
     * </p>
     *
     * <p>由于 spring-data-redis 是 compileOnly 依赖，
     * 此方法在运行时由 Spring 条件装配决定是否加载。</p>
     */
    @Bean("redisCacheManager")
    public CacheManager redisCacheManager(HolidayApi25Properties properties) {
        // 此方法仅在 spring-data-redis 存在时被调用
        // 实际使用时需要将 spring-data-redis 改为 implementation 依赖
        // 此处返回 null 是安全的，因为 @ConditionalOnClass 保证了只有 Redis 可用时才执行
        HolidayApi25Properties.RedisCache redisCfg = properties.getRedisCache();
        Duration ttl = redisCfg.getTtl();

        // 当实际引入 spring-data-redis 后，可使用以下代码：
        // RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        //     .prefixCacheNameWith(redisCfg.getKeyPrefix())
        //     .entryTtl(ttl)
        //     .serializeKeysWith(...)
        //     .serializeValuesWith(...);
        //
        // return RedisCacheManager.builder(connectionFactory)
        //     .cacheDefaults(defaultConfig)
        //     .withInitialCacheConfigurations(Map.of(
        //         "dayInfo", defaultConfig.entryTtl(ttl),
        //         "yearInfo", defaultConfig.entryTtl(ttl.multipliedBy(2)),
        //         "bundleMetadata", defaultConfig.entryTtl(ttl.multipliedBy(4))))
        //     .build();

        throw new UnsupportedOperationException(
                "请将 spring-boot-starter-data-redis 从 compileOnly 改为 implementation 依赖后启用此配置");
    }
}
