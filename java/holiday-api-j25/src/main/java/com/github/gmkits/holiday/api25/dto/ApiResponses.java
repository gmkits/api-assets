package com.github.gmkits.holiday.api25.dto;

import com.github.gmkits.holiday.api25.config.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.Map;

/**
 * 统一响应构造助手。
 *
 * <p>统一封装 success / error 响应的创建逻辑，
 * 保证 requestId、timestamp、path 等元信息自动填充。</p>
 */
public final class ApiResponses {

    private ApiResponses() {
    }

    /**
     * 构造成功响应。
     */
    public static <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return ApiResponse.<T>builder()
                .success(true)
                .timestamp(Instant.now())
                .requestId(RequestIdFilter.resolveRequestId(request))
                .path(request.getRequestURI())
                .data(data)
                .build();
    }

    /**
     * 构造错误响应。
     */
    public static ApiErrorResponse error(String code, String message, Map<String, Object> details,
                                         HttpServletRequest request) {
        return ApiErrorResponse.builder()
                .success(false)
                .timestamp(Instant.now())
                .requestId(RequestIdFilter.resolveRequestId(request))
                .path(request.getRequestURI())
                .error(ApiError.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .build())
                .build();
    }
}
