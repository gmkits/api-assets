package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一错误体。
 */
@Value
public class ApiError {
    String code;
    String message;
    Map<String, Object> details;

    @Builder
    private ApiError(String code, String message, Map<String, Object> details) {
        this.code = code;
        this.message = message;
        this.details = details == null || details.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }
}
