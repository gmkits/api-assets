package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 地区信息。
 */
@Getter
@Builder
public class RegionInfo {
    private final String code;
    private final Map<String, String> name;
}
