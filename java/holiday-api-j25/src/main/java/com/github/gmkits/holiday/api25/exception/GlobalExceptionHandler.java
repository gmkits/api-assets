package com.github.gmkits.holiday.api25.exception;

import com.github.gmkits.holiday.api25.dto.ApiErrorResponse;
import com.github.gmkits.holiday.api25.dto.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一异常处理。
 *
 * <p>所有 API 异常统一转换为 {@link ApiErrorResponse} 格式，
 * 包含 requestId、时间戳、错误码和详情。
 * 5xx 错误自动记录堆栈日志，4xx 不记录堆栈（减少日志噪音）。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("业务异常 [{}]: {}", ex.getCode(), ex.getMessage(), ex);
        }
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponses.error(ex.getCode(), ex.getMessage(), ex.getDetails(), request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                               HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiResponses.error("INVALID_PARAMETER",
                        "缺少必填参数: " + ex.getParameterName(),
                        Collections.singletonMap("parameter", ex.getParameterName()),
                        request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                               HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiResponses.error("INVALID_PARAMETER",
                        "参数格式错误: " + ex.getName(),
                        Collections.<String, Object>singletonMap("value", String.valueOf(ex.getValue())),
                        request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                         HttpServletRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest()
                .body(ApiResponses.error("VALIDATION_FAILED", "请求参数校验失败", details, request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                      HttpServletRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(v ->
                details.put(v.getPropertyPath().toString(), v.getMessage()));
        return ResponseEntity.badRequest()
                .body(ApiResponses.error("VALIDATION_FAILED", "请求参数校验失败", details, request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
                                                              HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiResponses.error("INVALID_REQUEST_BODY", "请求体无法解析", null, request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException ex,
                                                           HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponses.error("NOT_FOUND", "请求的资源不存在: " + request.getRequestURI(), null, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("未捕获异常: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponses.error("INTERNAL_ERROR", "服务内部错误", null, request));
    }
}
