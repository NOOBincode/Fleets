package org.example.fleets.common.aop.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 性能监控切面
 * 自动监控 Controller 层方法的执行性能
 */
@Slf4j
@Aspect
@Component
public class PerformanceAspect {

    private static final long SLOW_THRESHOLD_MS = 1000;

    @Pointcut("execution(* org.example.fleets..controller..*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object monitorPerformance(ProceedingJoinPoint pjp) throws Throwable {
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = ((MethodSignature) pjp.getSignature()).getMethod().getName();
        String fullName = className + "." + methodName;
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = pjp.proceed();
            
            long cost = System.currentTimeMillis() - startTime;
            
            if (cost > SLOW_THRESHOLD_MS) {
                log.warn("[性能告警] {} 执行耗时: {}ms (超过阈值 {}ms)", 
                        fullName, cost, SLOW_THRESHOLD_MS);
            } else {
                log.debug("[性能监控] {} 执行耗时: {}ms", fullName, cost);
            }
            
            return result;
            
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("[性能监控] {} 执行异常 | 耗时: {}ms", fullName, cost, e);
            throw e;
        }
    }
}
