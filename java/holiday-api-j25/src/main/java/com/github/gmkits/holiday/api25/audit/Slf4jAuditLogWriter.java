package com.github.gmkits.holiday.api25.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 默认审计日志写入实现：通过 SLF4J 以结构化格式输出到名为 {@code AUDIT} 的 logger。
 *
 * <p>日志格式方便 ELK/Loki 等日志平台采集和检索。
 * 可通过 logback 配置将 AUDIT logger 导向独立的审计日志文件。</p>
 */
@Slf4j(topic = "AUDIT")
@Component
public class Slf4jAuditLogWriter implements AuditLogWriter {

    @Override
    public void write(AuditLog auditLog) {
        log.info("requestId={} method={} uri={} query={} clientIp={} userAgent={} status={} durationMs={} responseSize={} error={}",
                auditLog.getRequestId(),
                auditLog.getMethod(),
                auditLog.getUri(),
                auditLog.getQueryString() == null ? "-" : auditLog.getQueryString(),
                auditLog.getClientIp(),
                auditLog.getUserAgent() == null ? "-" : auditLog.getUserAgent(),
                auditLog.getStatusCode(),
                auditLog.getDurationMs(),
                auditLog.getResponseSize(),
                auditLog.getErrorMessage() == null ? "-" : auditLog.getErrorMessage());
    }
}
