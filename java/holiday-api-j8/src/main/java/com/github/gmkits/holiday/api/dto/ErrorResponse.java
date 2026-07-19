package com.github.gmkits.holiday.api.dto;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Java 8 API 的不可变错误响应。
 */
public final class ErrorResponse {

    private final int status;
    private final String message;
    private final LocalDateTime timestamp;

    /**
     * 创建带当前服务器时间的错误响应。
     *
     * @param status HTTP 状态码
     * @param message 面向调用方的错误说明
     */
    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 返回 HTTP 状态码。
     *
     * @return HTTP 状态码
     */
    public int getStatus() {
        return status;
    }

    /**
     * 返回错误说明。
     *
     * @return 错误说明
     */
    public String getMessage() {
        return message;
    }

    /**
     * 返回响应创建时间。
     *
     * @return 响应创建时的本地服务器时间
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * 按状态码、消息和时间戳比较错误响应。
     *
     * @param other 待比较对象
     * @return 三个字段相同时返回 {@code true}
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ErrorResponse)) return false;
        ErrorResponse that = (ErrorResponse) other;
        return status == that.status
                && Objects.equals(message, that.message)
                && Objects.equals(timestamp, that.timestamp);
    }

    /**
     * 返回三个响应字段的组合哈希值。
     *
     * @return 组合哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(status, message, timestamp);
    }

    /**
     * 返回便于日志查看的错误响应。
     *
     * @return 错误响应字符串
     */
    @Override
    public String toString() {
        return "ErrorResponse{status=" + status + ", message='" + message
                + "', timestamp=" + timestamp + "}";
    }
}
