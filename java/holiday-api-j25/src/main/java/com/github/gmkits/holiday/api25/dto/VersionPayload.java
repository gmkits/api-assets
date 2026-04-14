package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 版本信息。
 */
@Getter
@Builder
public class VersionPayload {
    private final String apiVersion;
    private final String specVersion;
    private final String bundleFormatVersion;
    private final String publishedAt;
    private final List<String> regions;
}
