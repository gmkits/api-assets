package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Value;

/**
 * bundle 元信息。
 */
@Value
@Builder
public class BundleMetadataPayload {
    String regionCode;
    int year;
    String file;
    String sha256;
    String crc32;
    String sourceVersion;
    long size;
}
