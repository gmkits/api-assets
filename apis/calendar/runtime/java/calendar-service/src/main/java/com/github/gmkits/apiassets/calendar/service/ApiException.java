package com.github.gmkits.apiassets.calendar.service;

import org.springframework.http.HttpStatus;

/** 携带稳定 API 错误码的调用方错误。 */
public final class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    private ApiException(HttpStatus status, String code, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
    }

    public static ApiException badRequest(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", detail);
    }

    public static ApiException notFound(String code, String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, code, detail);
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
}
