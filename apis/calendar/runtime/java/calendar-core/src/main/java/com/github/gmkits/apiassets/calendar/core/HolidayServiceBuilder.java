package com.github.gmkits.apiassets.calendar.core;

import java.nio.file.Path;

/**
 * {@link HolidayService} 构建器。
 */
public final class HolidayServiceBuilder {

    private static final String ASSET_ROOT_PROPERTY = "calendar.assets.path";

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
     * 设置统一离线资产根目录。
     *
     * <p>目录结构为：
     * {@code calendar/calendar.cdat} 和
     * {@code holidays/bundles/{region}/{year}.hday}。日历资产是 JVM 级只读表，
     * 应在第一次农历或节气查询前配置。同一个 JVM 只能配置一个根目录；
     * 后续使用不同目录会立即拒绝，避免多个服务实例静默互相污染。</p>
     *
     * @param path 统一日期资产根目录
     * @return 当前构建器
     */
    public HolidayServiceBuilder assetPath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("asset path must not be null");
        }
        Path normalized = path.toAbsolutePath().normalize();
        synchronized (HolidayServiceBuilder.class) {
            String configured = System.getProperty(ASSET_ROOT_PROPERTY);
            if (configured != null && !configured.isBlank()) {
                Path existing = Path.of(configured).toAbsolutePath().normalize();
                if (!existing.equals(normalized)) {
                    throw new IllegalStateException(
                            "calendar assets are already configured for " + existing
                                    + "; refusing to replace them with " + normalized);
                }
            } else {
                System.setProperty(ASSET_ROOT_PROPERTY, normalized.toString());
            }
        }
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
