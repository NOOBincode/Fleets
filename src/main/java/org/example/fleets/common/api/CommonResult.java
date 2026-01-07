package org.example.fleets.common.api;

import lombok.Data;
import org.example.fleets.common.enums.ResultCode;

/**
 * 通用响应结果
 */
@Data
public class CommonResult<T> {

    private long code;
    private String message;
    private T data;

    protected CommonResult() {
    }

    protected CommonResult(long code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功返回结果
     */
    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 成功返回结果
     */
    public static <T> CommonResult<T> success(T data, String message) {
        return new CommonResult<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    /**
     * 失败返回结果
     */
    public static <T> CommonResult<T> failed(T data, String message) {
        return new CommonResult<>(ResultCode.FAILED.getCode(), message, data);
    }
    
    /**
     * 失败返回结果（带错误码）
     */
    public static <T> CommonResult<T> failed(long code, String message) {
        return new CommonResult<>(code, message, null);
    }

    /**
     * 参数验证失败返回结果
     */
    public static <T> CommonResult<T> validateFailed(String message) {
        return new CommonResult<>(ResultCode.VALIDATE_FAILED.getCode(), message, null);
    }

    /**
     * 未登录返回结果
     */
    public static <T> CommonResult<T> unauthorized(String message) {
        return new CommonResult<>(ResultCode.UNAUTHORIZED.getCode(), message, null);
    }

    /**
     * 未授权返回结果
     */
    public static <T> CommonResult<T> forbidden(String message) {
        return new CommonResult<>(ResultCode.FORBIDDEN.getCode(), message, null);
    }
}
