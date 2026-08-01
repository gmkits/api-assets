package com.github.gmkits.holiday.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HTTP 服务配置，绑定到 {@code holiday.*}。
 */
@ConfigurationProperties(prefix = "holiday")
public final class HolidayProperties {

    private String defaultRegion = "CN";
    private String assetPath;
    private boolean classpathFallback = true;

    /**
     * 返回默认地区。
     *
     * @return 地区代码
     */
    public String getDefaultRegion() {
        return defaultRegion;
    }

    /**
     * 设置默认地区。
     *
     * @param defaultRegion 地区代码
     */
    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    /**
     * 返回统一离线资产目录。
     *
     * @return 资产目录；未配置时为 {@code null}
     */
    public String getAssetPath() {
        return assetPath;
    }

    /**
     * 设置统一离线资产目录。
     *
     * @param assetPath 包含 {@code calendar} 与 {@code holidays} 的目录
     */
    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }

    /**
     * 返回是否允许使用 JAR 内置资产。
     *
     * @return 允许回退时为 {@code true}
     */
    public boolean isClasspathFallback() {
        return classpathFallback;
    }

    /**
     * 设置是否允许使用 JAR 内置资产。
     *
     * @param classpathFallback 是否允许回退
     */
    public void setClasspathFallback(boolean classpathFallback) {
        this.classpathFallback = classpathFallback;
    }
}
