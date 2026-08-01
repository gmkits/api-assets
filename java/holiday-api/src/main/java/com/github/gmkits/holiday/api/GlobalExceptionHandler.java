package com.github.gmkits.holiday.api;

import com.github.gmkits.holiday.api.dto.ErrorResponse;
import com.github.gmkits.holiday.core.HolidayDataUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 将控制器异常归一化为稳定的 JSON 错误响应。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理离线数据未覆盖的地区或年份。
     *
     * @param ex 数据覆盖异常
     * @return HTTP 404 错误响应
     */
    @ExceptionHandler(HolidayDataUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleDataUnavailable(
            HolidayDataUnavailableException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * 处理缺少必填查询参数。
     *
     * @param ex Spring 参数绑定异常
     * @return HTTP 400 错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "缺少必填参数: " + ex.getParameterName());
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * 处理查询参数类型或日期格式错误。
     *
     * @param ex Spring 参数类型异常
     * @return HTTP 400 错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "参数 '" + ex.getName() + "' 的值无效: " + ex.getValue());
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * 处理业务参数校验错误。
     *
     * @param ex 参数异常
     * @return HTTP 400 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * 隐藏未预期异常的内部细节。
     *
     * @param ex 未预期异常
     * @return HTTP 500 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "服务内部错误");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
