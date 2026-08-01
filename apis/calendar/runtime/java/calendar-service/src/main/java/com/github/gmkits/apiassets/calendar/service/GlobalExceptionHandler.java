package com.github.gmkits.apiassets.calendar.service;

import com.github.gmkits.apiassets.calendar.core.HolidayDataUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** 把所有 HTTP 失败转换成稳定的 {@code application/problem+json}。 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiProblem> handleApi(ApiException exception, HttpServletRequest request) {
        return response(exception.status(), exception.code(), exception.getMessage(), request);
    }

    @ExceptionHandler(HolidayDataUnavailableException.class)
    public ResponseEntity<ApiProblem> handleUnavailable(
            HolidayDataUnavailableException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "CALENDAR_DATA_NOT_AVAILABLE",
                exception.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiProblem> handleMissingRoute(
            NoResourceFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "ASSET_NOT_FOUND",
                "请求的资源不存在", request);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ApiProblem> handleInvalid(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT",
                invalidDetail(exception), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblem> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOG.error("未处理的 Calendar API 异常", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "服务内部错误", request);
    }

    private static ResponseEntity<ApiProblem> response(
            HttpStatus status, String code, String detail, HttpServletRequest request) {
        String requestId = String.valueOf(request.getAttribute(RequestIdFilter.ATTRIBUTE));
        ApiProblem problem = new ApiProblem(
                "urn:api-assets:calendar:error:" + code.toLowerCase().replace('_', '-'),
                status.getReasonPhrase(), status.value(), detail,
                request.getRequestURI(), code, requestId);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private static String invalidDetail(Exception exception) {
        if (exception instanceof MissingServletRequestParameterException missing) {
            return "缺少必填参数: " + missing.getParameterName();
        }
        if (exception instanceof MethodArgumentTypeMismatchException mismatch) {
            return "参数 '" + mismatch.getName() + "' 的值无效: " + mismatch.getValue();
        }
        return exception.getMessage() == null ? "请求参数无效" : exception.getMessage();
    }
}
