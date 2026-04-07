package org.example.fleets.common.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * 用于记录方法执行的操作日志
 * 
 * 使用示例：
 * @OperationLog(module = "好友模块", operation = "添加好友")
 * public boolean addFriend(Long userId, FriendAddDTO addDTO) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    
    /**
     * 模块名称
     */
    String module();
    
    /**
     * 操作名称
     */
    String operation();
    
    /**
     * 是否记录方法参数
     */
    boolean logArgs() default true;
    
    /**
     * 是否记录返回值
     */
    boolean logResult() default false;
    
    /**
     * 是否记录执行耗时
     */
    boolean logCost() default true;
}
