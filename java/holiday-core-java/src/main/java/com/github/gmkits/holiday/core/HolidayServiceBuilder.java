package com.github.gmkits.holiday.core;

import java.nio.file.Path;

/**
 * {@link HolidayService} 构建器。
 */
public final class HolidayServiceBuilder {

    private String defaultRegion = "CN";
    private Path dataPath;
    private boolean classpathFallback = true;

    /**
     * 设置默认地区代码。
     */
    public HolidayServiceBuilder defaultRegion(String region) {
        this.defaultRegion = region;
        return this;
    }

    /**
     * 设置 bundle 数据目录，目录结构应为 {@code {dataPath}/{regionCode}/{year}.hday}。
     */
    public HolidayServiceBuilder dataPath(Path path) {
        this.dataPath = path;
        return this;
    }

    /**
     * 设置找不到文件系统 bundle 时是否回退到 classpath。
     */
    public HolidayServiceBuilder enableClasspathFallback(boolean enabled) {
        this.classpathFallback = enabled;
        return this;
    }

    /**
     * 构建服务实例。
     */
    public HolidayService build() {
        return new HolidayServiceImpl(defaultRegion, dataPath, classpathFallback);
    }
}
