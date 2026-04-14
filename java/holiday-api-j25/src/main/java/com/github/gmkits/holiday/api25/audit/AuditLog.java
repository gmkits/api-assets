package com.github.gmkits.holiday.api25.audit;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * 审计日志条目，记录每次 API 调用的详细信息。
 *
 * <p>用于安全审计、流量分析和问题排查。
 * 日志条目通过异步方式写入，不阻塞请求处理。</p>
 */
@Getter
@Builder
@ToString
public class AuditLog {

    /** 请求唯一标识 */
    private final String requestId;

    /** 请求时间 */
    private final Instant timestamp;

    /** HTTP 方法 */
    private final String method;

    /** 请求 URI */
    private final String uri;

    /** 查询参数 */
    private final String queryString;

    /** 客户端 IP（支持代理头） */
    private final String clientIp;

    /** User-Agent */
    private final String userAgent;

    /** HTTP 响应状态码 */
    private final int statusCode;

    /** 请求处理耗时（毫秒） */
    private final long durationMs;

    /** 响应体大小（字节，可能为 -1 表示未知） */
    private final long responseSize;

    /** 异常信息（如果有） */
    private final String errorMessage;
}
