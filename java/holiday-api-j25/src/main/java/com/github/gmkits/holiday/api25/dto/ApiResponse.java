package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 统一成功响应。
 */
@Getter
@Builder
public class ApiResponse<T> {
    private final boolean success;
    private final Instant timestamp;
    private final String requestId;
    private final T data;
}
