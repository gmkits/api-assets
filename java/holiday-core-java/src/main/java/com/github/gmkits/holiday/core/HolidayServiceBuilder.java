package com.github.gmkits.holiday.core;

import java.nio.file.Path;

/**
 * Builder for constructing {@link HolidayService} instances.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * HolidayService svc = new HolidayServiceBuilder()
 *         .defaultRegion("CN")
 *         .dataPath(Paths.get("/data/bundles"))
 *         .enableClasspathFallback(true)
 *         .build();
 * }</pre>
 */
public final class HolidayServiceBuilder {

    private String defaultRegion = "CN";
    private Path dataPath;
    private boolean classpathFallback = true;

    /**
     * Sets the default region code used when no region is specified.
     *
     * @param region the region code (e.g. "CN")
     * @return this builder
     */
    public HolidayServiceBuilder defaultRegion(String region) {
        this.defaultRegion = region;
        return this;
    }

    /**
     * Sets the filesystem directory containing {@code .hday} bundle files.
     *
     * <p>Expected layout: {@code {dataPath}/{regionCode}/{year}.hday}</p>
     *
     * @param path the data directory path
     * @return this builder
     */
    public HolidayServiceBuilder dataPath(Path path) {
        this.dataPath = path;
        return this;
    }

    /**
     * Enables or disables classpath fallback for loading bundles.
     *
     * <p>When enabled the service will attempt to load bundles from the
     * classpath at {@code bundles/{regionCode}/{year}.hday} if they are
     * not found on the filesystem.</p>
     *
     * @param enabled {@code true} to enable classpath fallback
     * @return this builder
     */
    public HolidayServiceBuilder enableClasspathFallback(boolean enabled) {
        this.classpathFallback = enabled;
        return this;
    }

    /**
     * Builds and returns a new {@link HolidayService}.
     *
     * @return the constructed service instance
     */
    public HolidayService build() {
        return new HolidayServiceImpl(defaultRegion, dataPath, classpathFallback);
    }
}
