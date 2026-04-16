package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 版本信息。
 */
@Value
public class VersionPayload {
    String apiVersion;
    String specVersion;
    String bundleFormatVersion;
    String publishedAt;
    List<String> regions;

    @Builder
    private VersionPayload(String apiVersion,
                           String specVersion,
                           String bundleFormatVersion,
                           String publishedAt,
                           List<String> regions) {
        this.apiVersion = apiVersion;
        this.specVersion = specVersion;
        this.bundleFormatVersion = bundleFormatVersion;
        this.publishedAt = publishedAt;
        this.regions = regions == null ? List.of() : List.copyOf(regions);
    }
}
