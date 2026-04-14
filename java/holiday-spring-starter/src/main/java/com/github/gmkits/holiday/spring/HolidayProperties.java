package com.github.gmkits.holiday.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Holiday starter, bound to the
 * {@code holiday.*} namespace.
 */
@ConfigurationProperties(prefix = "holiday")
public class HolidayProperties {

    /**
     * Default region code used when none is specified.
     */
    private String defaultRegion = "CN";

    /**
     * Filesystem path to the directory containing {@code .hday} bundles.
     * Layout: {@code {dataPath}/{regionCode}/{year}.hday}.
     */
    private String dataPath;

    /**
     * Whether to fall back to classpath-based bundle loading when no
     * filesystem bundle is found.
     */
    private boolean classpathFallback = true;

    /** Returns the default region code. */
    public String getDefaultRegion() { return defaultRegion; }

    /** Sets the default region code. */
    public void setDefaultRegion(String defaultRegion) { this.defaultRegion = defaultRegion; }

    /** Returns the data path. */
    public String getDataPath() { return dataPath; }

    /** Sets the data path. */
    public void setDataPath(String dataPath) { this.dataPath = dataPath; }

    /** Returns whether classpath fallback is enabled. */
    public boolean isClasspathFallback() { return classpathFallback; }

    /** Sets whether classpath fallback is enabled. */
    public void setClasspathFallback(boolean classpathFallback) { this.classpathFallback = classpathFallback; }
}
