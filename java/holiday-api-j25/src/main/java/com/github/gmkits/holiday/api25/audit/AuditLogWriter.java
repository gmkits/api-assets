package com.github.gmkits.holiday.api25.audit;

/**
 * 审计日志写入接口。
 *
 * <p>默认实现使用 SLF4J 输出到日志文件，
 * 可替换为数据库或消息队列等持久化方案。</p>
 */
public interface AuditLogWriter {

    /**
     * 写入一条审计日志。
     */
    void write(AuditLog auditLog);
}
