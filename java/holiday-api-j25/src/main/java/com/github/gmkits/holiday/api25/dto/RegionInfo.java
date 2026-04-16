package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 地区信息。
 */
@Value
public class RegionInfo {
    String code;
    Map<String, String> name;

    @Builder
    private RegionInfo(String code, Map<String, String> name) {
        this.code = code;
        this.name = name == null || name.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(name));
    }
}
