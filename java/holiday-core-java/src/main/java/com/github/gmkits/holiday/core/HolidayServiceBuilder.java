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
     * 创建使用 {@code CN} 默认区域并允许 classpath 回退的构建器。
     */
    public HolidayServiceBuilder() {
    }

    /**
     * 设置默认地区代码。
     *
     * @param region 默认区域代码，例如 {@code CN}
     * @return 当前构建器
     */
    public HolidayServiceBuilder defaultRegion(String region) {
        this.defaultRegion = region;
        return this;
    }

    /**
     * 设置 bundle 数据目录，目录结构应为 {@code {dataPath}/{regionCode}/{year}.hday}。
     *
     * @param path bundle 根目录
     * @return 当前构建器
     */
    public HolidayServiceBuilder dataPath(Path path) {
        this.dataPath = path;
        return this;
    }

    /**
     * 设置统一离线资产根目录。
     *
     * <p>目录结构为：
     * {@code calendar/calendar.cdat} 和
     * {@code holidays/bundles/{region}/{year}.hday}。日历资产是 JVM 级只读表，
     * 应在第一次农历或节气查询前配置。</p>
     *
     * @param path 统一日期资产根目录
     * @return 当前构建器
     */
    public HolidayServiceBuilder assetPath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        System.setProperty(ASSET_ROOT_PROPERTY, normalized.toString());
        this.dataPath = normalized.resolve("holidays").resolve("bundles");
        return this;
    }

    /**
     * 设置找不到文件系统 bundle 时是否回退到 classpath。
     *
     * @param enabled 允许回退时为 {@code true}
     * @return 当前构建器
     */
    public HolidayServiceBuilder enableClasspathFallback(boolean enabled) {
        this.classpathFallback = enabled;
        return this;
    }

    /**
     * 构建服务实例。
     *
     * @return 线程安全的节假日查询服务
     */
    public HolidayService build() {
        return new HolidayServiceImpl(defaultRegion, dataPath, classpathFallback);
    }
}
