package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * bundle 元信息。
 */
@Getter
@Builder
public class BundleMetadataPayload {
    private final String regionCode;
    private final int year;
    private final String file;
    private final String sha256;
    private final String crc32;
    private final String sourceVersion;
    private final long size;
}
