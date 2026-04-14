package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 统一错误响应。
 */
@Getter
@Builder
public class ApiErrorResponse {
    private final boolean success;
    private final Instant timestamp;
    private final String requestId;
    private final ApiError error;
}
