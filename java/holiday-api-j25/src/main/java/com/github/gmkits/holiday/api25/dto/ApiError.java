package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 统一错误体。
 */
@Getter
@Builder
public class ApiError {
    private final String code;
    private final String message;
    private final Map<String, Object> details;
}
