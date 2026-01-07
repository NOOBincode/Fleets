package org.example.fleets.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import lombok.extern.slf4j.Slf4j;
import org.example.fleets.common.api.CommonResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 处理 Sa-Token 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public CommonResult<?> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        log.warn("未登录访问 [{}]: {}", request.getRequestURI(), e.getMessage());
        return CommonResult.failed(ErrorCode.UNAUTHORIZED.getCode(), "请先登录");
    }
    
    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public CommonResult<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 [{}] {}: {}", request.getRequestURI(), e.getCode(), e.getMessage());
        return CommonResult.failed(e.getCode(), e.getMessage());
    }
    
    /**
     * 处理参数校验异常（@Valid）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<?> handleValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String errors = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        log.warn("参数校验失败 [{}]: {}", request.getRequestURI(), errors);
        return CommonResult.failed(ErrorCode.VALIDATE_FAILED.getCode(), errors);
    }
    
    /**
     * 处理参数绑定异常
     */
    @ExceptionHandler(BindException.class)
    public CommonResult<?> handleBindException(BindException e, HttpServletRequest request) {
        String errors = e.getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        
        log.warn("参数绑定失败 [{}]: {}", request.getRequestURI(), errors);
        return CommonResult.failed(ErrorCode.VALIDATE_FAILED.getCode(), errors);
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public CommonResult<?> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("非法参数 [{}]: {}", request.getRequestURI(), e.getMessage());
        return CommonResult.failed(ErrorCode.VALIDATE_FAILED.getCode(), e.getMessage());
    }
    
    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public CommonResult<?> handleNullPointerException(NullPointerException e, HttpServletRequest request) {
        log.error("空指针异常 [{}]", request.getRequestURI(), e);
        return CommonResult.failed(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误，请联系管理员");
    }
    
    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public CommonResult<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{}]", request.getRequestURI(), e);
        return CommonResult.failed(ErrorCode.SYSTEM_ERROR.getCode(), "系统繁忙，请稍后重试");
    }
}
