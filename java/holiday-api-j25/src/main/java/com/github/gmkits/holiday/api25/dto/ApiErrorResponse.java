package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 统一错误响应。
 *
 * <p>所有异常统一转换为此结构，保证客户端可用统一的错误处理逻辑。</p>
 */
@Getter
@Builder
public class ApiErrorResponse {
    /** 固定为 false */
    private final boolean success;

    /** 服务器时间戳 */
    private final Instant timestamp;

    /** 请求唯一标识 */
    private final String requestId;

    /** 请求路径 */
    private final String path;

    /** 错误详情 */
    private final ApiError error;
}
