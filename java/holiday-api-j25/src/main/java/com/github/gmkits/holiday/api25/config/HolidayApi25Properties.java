package com.github.gmkits.holiday.api25.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * holiday-api-j25 全量配置。
 *
 * <p>包含：缓存、限流、审计日志、预热等。</p>
 */
@Data
@ConfigurationProperties(prefix = "holiday.api")
public class HolidayApi25Properties {

    /** API 版本号 */
    private String apiVersion = "2.0.0";

    /** 默认地区 */
    private String defaultRegion = "CN";

    /** 数据文件路径（为空时使用 classpath） */
    private String dataPath;

    /** 是否回退到 classpath 加载数据 */
    private boolean classpathFallback = true;

    /** manifest 文件位置 */
    private String manifestLocation = "classpath:manifest.json";

    /** 启动时是否预热当前年和下一年 */
    private boolean preloadCurrentAndNextYear = true;

    /** 预热的地区列表 */
    private List<String> preloadRegions = new ArrayList<>();

    /** 预热的年份列表 */
    private List<Integer> preloadYears = new ArrayList<>();

    /** 本地缓存配置 */
    private Cache cache = new Cache();

    /** 限流配置 */
    private RateLimit rateLimit = new RateLimit();

    /** 审计日志配置 */
    private Audit audit = new Audit();

    @Data
    public static class Cache {
        /** 单日缓存最大条目数 */
        private long dayMaximumSize = 4096;

        /** 年查询缓存最大条目数 */
        private long yearMaximumSize = 128;

        /** 元数据缓存最大条目数 */
        private long metadataMaximumSize = 256;

        /** 本地缓存写入后过期时间 */
        private Duration expireAfterWrite = Duration.ofMinutes(30);
    }

    @Data
    public static class RateLimit {
        /** 是否启用限流 */
        private boolean enabled = true;

        /** 全局 QPS 上限（0 = 不限） */
        private int globalQps = 500;

        /** 单 IP QPS 上限（0 = 不限） */
        private int perIpQps = 50;
    }

    @Data
    public static class Audit {
        /** 是否启用审计日志 */
        private boolean enabled = true;
    }
}
