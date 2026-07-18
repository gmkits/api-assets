package com.github.gmkits.holiday.core;

import java.nio.file.Path;

/**
 * {@link HolidayService} 构建器。
 */
public final class HolidayServiceBuilder {

    private static final String ASSET_ROOT_PROPERTY = "cn.holiday.assets.path";

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
     * 设置统一离线资产根目录。
     *
     * <p>目录结构为：
     * {@code calendar/lunar-years.hex}、{@code calendar/solar-terms.csv} 和
     * {@code holidays/bundles/{region}/{year}.hday}。日历资产是 JVM 级只读表，
     * 应在第一次农历或节气查询前配置。</p>
     */
    public HolidayServiceBuilder assetPath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        System.setProperty(ASSET_ROOT_PROPERTY, normalized.toString());
        this.dataPath = normalized.resolve("holidays").resolve("bundles");
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
