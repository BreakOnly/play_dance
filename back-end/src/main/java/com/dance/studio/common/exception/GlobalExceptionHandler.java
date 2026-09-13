package com.dance.studio.common.exception;

import com.dance.studio.common.Result;
import com.dance.studio.common.ResultCode;

import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(
            org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> unsupported(Exception e) {
        return Result.error(1405, "不支持该操作");
    }

    @ExceptionHandler({
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.bind.MissingRequestHeaderException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    })
    @org.springframework.web.bind.annotation.ResponseStatus(
            org.springframework.http.HttpStatus.BAD_REQUEST)
    public Result<Void> malformed(Exception e) {
        return Result.error(1400, "请求参数不正确");
    }

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValid(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getDefaultMessage() : "参数校验失败";
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.error(ResultCode.SYSTEM_ERROR);
    }
}
