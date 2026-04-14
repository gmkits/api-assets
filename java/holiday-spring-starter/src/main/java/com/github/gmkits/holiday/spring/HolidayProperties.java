package com.github.gmkits.holiday.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Holiday Starter 配置属性，绑定到 {@code holiday.*} 命名空间。
 */
@ConfigurationProperties(prefix = "holiday")
public class HolidayProperties {

    /** 默认地区代码。 */
    private String defaultRegion = "CN";

    /** bundle 数据目录，格式为 {@code {dataPath}/{regionCode}/{year}.hday}。 */
    private String dataPath;

    /** 找不到文件系统 bundle 时是否回退到 classpath。 */
    private boolean classpathFallback = true;

    public String getDefaultRegion() { return defaultRegion; }

    public void setDefaultRegion(String defaultRegion) { this.defaultRegion = defaultRegion; }

    public String getDataPath() { return dataPath; }

    public void setDataPath(String dataPath) { this.dataPath = dataPath; }

    public boolean isClasspathFallback() { return classpathFallback; }

    public void setClasspathFallback(boolean classpathFallback) { this.classpathFallback = classpathFallback; }
}
