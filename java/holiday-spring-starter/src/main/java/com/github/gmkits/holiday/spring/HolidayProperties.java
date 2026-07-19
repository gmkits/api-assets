package com.github.gmkits.holiday.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

/**
 * Holiday Starter 配置属性，绑定到 {@code holiday.*} 命名空间。
 */
@ConfigurationProperties(prefix = "holiday")
public class HolidayProperties {

    /** 默认地区代码。 */
    private String defaultRegion = "CN";

    /** bundle 数据目录，格式为 {@code {dataPath}/{regionCode}/{year}.hday}。 */
    private String dataPath;

    /** 统一离线资产根目录；配置后优先于 dataPath。 */
    private String assetPath;

    /** 找不到文件系统 bundle 时是否回退到 classpath。 */
    private boolean classpathFallback = true;

    /**
     * 返回默认地区代码。
     *
     * @return 默认地区代码
     */
    public String getDefaultRegion() {
        return defaultRegion;
    }

    /**
     * 设置默认地区代码。
     *
     * @param defaultRegion 默认地区代码，例如 {@code CN}
     */
    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    /**
     * 返回旧版 bundle 数据目录。
     *
     * @return bundle 数据目录；未配置时为 {@code null}
     */
    public String getDataPath() {
        return dataPath;
    }

    /**
     * 设置旧版 bundle 数据目录。
     *
     * @param dataPath bundle 数据目录
     */
    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    /**
     * 返回统一离线资产根目录。
     *
     * @return 统一离线资产根目录；未配置时为 {@code null}
     */
    public String getAssetPath() {
        return assetPath;
    }

    /**
     * 设置统一离线资产根目录。
     *
     * @param assetPath 统一离线资产根目录
     */
    public void setAssetPath(String assetPath) {
        this.assetPath = assetPath;
    }

    /**
     * 判断是否允许回退到 classpath 内置资产。
     *
     * @return 允许回退时返回 {@code true}
     */
    public boolean isClasspathFallback() {
        return classpathFallback;
    }

    /**
     * 设置是否允许回退到 classpath 内置资产。
     *
     * @param classpathFallback 允许回退时为 {@code true}
     */
    public void setClasspathFallback(boolean classpathFallback) {
        this.classpathFallback = classpathFallback;
    }

    /**
     * 按全部配置字段比较两个属性对象。
     *
     * @param other 待比较对象
     * @return 配置字段相同时返回 {@code true}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HolidayProperties)) return false;
        HolidayProperties that = (HolidayProperties) other;
        return that.canEqual(this)
                && classpathFallback == that.classpathFallback
                && Objects.equals(defaultRegion, that.defaultRegion)
                && Objects.equals(dataPath, that.dataPath)
                && Objects.equals(assetPath, that.assetPath);
    }

    /**
     * 允许子类参与对称的相等性判断。
     *
     * @param other 待比较对象
     * @return 对象属于 {@code HolidayProperties} 体系时返回 {@code true}
     */
    protected boolean canEqual(Object other) {
        return other instanceof HolidayProperties;
    }

    /**
     * 返回全部配置字段的组合哈希值。
     *
     * @return 组合哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(defaultRegion, dataPath, assetPath, classpathFallback);
    }

    /**
     * 返回便于诊断的配置摘要。
     *
     * @return 配置摘要字符串
     */
    @Override
    public String toString() {
        return "HolidayProperties{defaultRegion='" + defaultRegion + "', dataPath='"
                + dataPath + "', assetPath='" + assetPath + "', classpathFallback="
                + classpathFallback + "}";
    }
}
