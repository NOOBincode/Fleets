package org.example.fleets.common.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 * 用于方法级别的分布式锁控制，防止并发风暴
 * 
 * 使用示例：
 * @DistributedLock(key = "'user:register:' + #username")
 * public void register(String username) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    
    /**
     * 锁的 key，支持 SpEL 表达式
     * 例如：'user:register:' + #username
     */
    String key();
    
    /**
     * 等待获取锁的时间（秒）
     */
    long waitTime() default 10;
    
    /**
     * 锁的持有时间（秒）
     */
    long leaseTime() default 30;
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 获取锁失败的提示信息
     */
    String message() default "操作过于频繁，请稍后再试";
}
