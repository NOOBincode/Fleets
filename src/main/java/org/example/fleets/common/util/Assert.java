package org.example.fleets.common.util;

import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * 断言工具类
 * 用于参数校验
 */
public class Assert {
    
    /**
     * 断言对象不为空
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言对象不为空
     */
    public static void notNull(Object object, ErrorCode errorCode) {
        if (object == null) {
            throw new BusinessException(errorCode);
        }
    }
    
    /**
     * 断言字符串不为空
     */
    public static void hasText(String text, String message) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言字符串不为空
     */
    public static void hasText(String text, ErrorCode errorCode) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException(errorCode);
        }
    }
    
    /**
     * 断言集合不为空
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (CollectionUtils.isEmpty(collection)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言条件为真
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言条件为真
     */
    public static void isTrue(boolean expression, ErrorCode errorCode) {
        if (!expression) {
            throw new BusinessException(errorCode);
        }
    }
    
    /**
     * 断言条件为假
     */
    public static void isFalse(boolean expression, String message) {
        if (expression) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言数字大于0
     */
    public static void positive(Long number, String message) {
        if (number == null || number <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }
    
    /**
     * 断言数字大于等于0
     */
    public static void notNegative(Long number, String message) {
        if (number == null || number < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }
}
