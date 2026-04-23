package com.github.gmkits.holiday.sdk.j25;

/**
 * SDK 调用失败时抛出。
 *
 * <p>携带 HTTP 状态码（远程模式）或 {@code -1}（本地/网络层错误）。
 * 远程错误的响应体片段会保留在 {@code body} 字段，便于排查。</p>
 */
public class HolidayClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String body;

    public HolidayClientException(String message) {
        this(message, -1, null, null);
    }

    public HolidayClientException(String message, Throwable cause) {
        this(message, -1, null, cause);
    }

    public HolidayClientException(String message, int statusCode, String body) {
        this(message, statusCode, body, null);
    }

    public HolidayClientException(String message, int statusCode, String body, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.body = body;
    }

    /** HTTP 状态码；网络/解析错误为 {@code -1}。 */
    public int statusCode() {
        return statusCode;
    }

    /** 远程错误响应体片段；其它情况为 {@code null}。 */
    public String body() {
        return body;
    }
}
