package org.example.fleets.common.util;

import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;

/**
 * 断言工具类
 * 用于简化异常抛出
 */
public class Assert {
    
    /**
     * 断言对象不为空
     */
    public static void notNull(Object object, ErrorCode errorCode) {
        if (object == null) {
            throw new BusinessException(errorCode);
        }
    }
    
    /**
     * 断言对象不为空
     */
    public static void notNull(Object object, ErrorCode errorCode, String message) {
        if (object == null) {
            throw new BusinessException(errorCode.getCode(), message);
        }
    }
    
    /**
     * 断言对象不为空
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 断言条件为真
     */
    public static void isTrue(boolean condition, ErrorCode errorCode) {
        if (!condition) {
            throw new BusinessException(errorCode);
        }
    }
    
    /**
     * 断言条件为真
     */
    public static void isTrue(boolean condition, ErrorCode errorCode, String message) {
        if (!condition) {
            throw new BusinessException(errorCode.getCode(), message);
        }
    }
    
    /**
     * 断言条件为真
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 断言条件为假
     */
    public static void isFalse(boolean condition, ErrorCode errorCode) {
        if (condition) {
            throw new BusinessException(errorCode);
        }
    }
    
    /**
     * 断言条件为假
     */
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new BusinessException(message);
        }
    }
    
    /**
     * 断言字符串不为空
     */
    public static void hasText(String text, ErrorCode errorCode) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(errorCode);
        }
    }
    
    /**
     * 断言字符串不为空
     */
    public static void hasText(String text, String message) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(message);
        }
    }
}
