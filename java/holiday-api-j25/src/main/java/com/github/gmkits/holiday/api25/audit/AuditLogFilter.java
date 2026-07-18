package com.github.gmkits.holiday.api25.audit;

import com.github.gmkits.holiday.api25.config.RequestIdFilter;
import com.github.gmkits.holiday.api25.config.HolidayApi25Properties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

/**
 * 审计日志过滤器，拦截所有 /api/** 请求并记录调用详情。
 *
 * <p>在 {@link RequestIdFilter} 之后执行，保证 requestId 已生成。
 * 日志写入通过虚拟线程异步执行，不阻塞请求响应。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuditLogFilter extends OncePerRequestFilter {

    private final AuditLogWriter auditLogWriter;
    private final HolidayApi25Properties properties;
    private final AsyncTaskExecutor applicationTaskExecutor;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 只审计 API 请求，忽略 actuator、swagger 等
        return !path.startsWith("/api/") || !properties.getAudit().isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.nanoTime();
        String errorMessage = null;

        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            errorMessage = ex.getMessage();
            throw ex;
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            AuditLog auditLog = AuditLog.builder()
                    .requestId(RequestIdFilter.resolveRequestId(request))
                    .timestamp(Instant.now())
                    .method(request.getMethod())
                    .uri(request.getRequestURI())
                    .queryString(request.getQueryString())
                    .clientIp(resolveClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .statusCode(response.getStatus())
                    .durationMs(durationMs)
                    .responseSize(resolveResponseSize(response))
                    .errorMessage(errorMessage)
                    .build();

            try {
                applicationTaskExecutor.execute(() -> {
                    try {
                        auditLogWriter.write(auditLog);
                    } catch (Exception ex) {
                        log.warn("审计日志写入失败: {}", ex.getMessage());
                    }
                });
            } catch (RuntimeException ex) {
                log.warn("审计日志任务提交失败: {}", ex.getMessage());
            }
        }
    }

    private static int resolveResponseSize(HttpServletResponse response) {
        String contentLength = response.getHeader("Content-Length");
        if (contentLength == null) {
            return -1;
        }
        try {
            return Integer.parseInt(contentLength);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * 解析客户端真实 IP，支持常见反向代理头。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多个 IP，取第一个
                int comma = ip.indexOf(',');
                return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
