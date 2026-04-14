package com.github.gmkits.holiday.api25.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 统一成功响应。
 *
 * <p>所有查询接口共用此结构，保证客户端可用统一的反序列化逻辑处理。</p>
 */
@Getter
@Builder
public class ApiResponse<T> {
    /** 是否成功 */
    private final boolean success;

    /** 服务器时间戳 */
    private final Instant timestamp;

    /** 请求唯一标识，方便日志追踪 */
    private final String requestId;

    /** 请求路径 */
    private final String path;

    /** 响应数据 */
    private final T data;
}
