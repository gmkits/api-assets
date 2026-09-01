package com.github.gmkits.apiassets.calendar.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Calendar API 的部署配置。 */
@ConfigurationProperties(prefix = "calendar")
public final class CalendarProperties {

    private String defaultRegion = "CN";
    private String assetPath;
    private String upstreamToken;
    private String releaseVersion = "1.0.0-rc.2";
    private String sourceCommit = "unknown";

    public String getDefaultRegion() { return defaultRegion; }
    public void setDefaultRegion(String value) { this.defaultRegion = value; }
    public String getAssetPath() { return assetPath; }
    public void setAssetPath(String value) { this.assetPath = value; }
    public String getUpstreamToken() { return upstreamToken; }
    public void setUpstreamToken(String value) { this.upstreamToken = value; }
    public String getReleaseVersion() { return releaseVersion; }
    public void setReleaseVersion(String value) { this.releaseVersion = value; }
    public String getSourceCommit() { return sourceCommit; }
    public void setSourceCommit(String value) { this.sourceCommit = value; }

    /** 外部资产目录启用时不允许回退到镜像内置数据。 */
    public boolean usesExternalAssets() {
        return assetPath != null && !assetPath.isBlank();
    }
}
