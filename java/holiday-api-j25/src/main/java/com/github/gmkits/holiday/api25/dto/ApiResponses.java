package com.github.gmkits.holiday.api25.dto;

import com.github.gmkits.holiday.api25.config.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * 响应构造助手。
 */
public final class ApiResponses {

    private ApiResponses() {
    }

    public static <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return ApiResponse.<T>builder()
                .success(true)
                .timestamp(Instant.now())
                .requestId(RequestIdFilter.resolveRequestId(request))
                .data(data)
                .build();
    }

    public static ApiErrorResponse error(String code, String message, Map<String, Object> details,
                                         HttpServletRequest request) {
        return ApiErrorResponse.builder()
                .success(false)
                .timestamp(Instant.now())
                .requestId(RequestIdFilter.resolveRequestId(request))
                .error(ApiError.builder()
                        .code(code)
                        .message(message)
                        .details(details == null ? Collections.<String, Object>emptyMap() : details)
                        .build())
                .build();
    }
}
