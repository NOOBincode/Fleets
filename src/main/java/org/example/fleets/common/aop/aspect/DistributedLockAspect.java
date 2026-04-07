package org.example.fleets.common.aop.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.fleets.common.aop.annotation.DistributedLock;
import org.example.fleets.common.cache.GenericCacheService;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.redisson.api.RLock;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面
 * 通过 AOP 实现非侵入式的分布式锁控制
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final GenericCacheService genericCacheService;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(distributedLock)")
    public Object executeWithLock(ProceedingJoinPoint pjp, DistributedLock distributedLock) throws Throwable {
        String lockKey = parseLockKey(pjp, distributedLock.key());
        
        RLock lock = genericCacheService.getLock(lockKey);
        
        try {
            boolean acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );
            
            if (!acquired) {
                log.warn("获取分布式锁失败，key: {}", lockKey);
                throw new BusinessException(ErrorCode.FAILED, distributedLock.message());
            }
            
            log.debug("获取分布式锁成功，key: {}", lockKey);
            return pjp.proceed();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("获取分布式锁被中断，key: {}", lockKey, e);
            throw new BusinessException(ErrorCode.FAILED, "操作被中断，请稍后再试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("释放分布式锁，key: {}", lockKey);
            }
        }
    }

    /**
     * 解析 SpEL 表达式生成锁的 key
     */
    private String parseLockKey(ProceedingJoinPoint pjp, String keyExpression) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Object[] args = pjp.getArgs();
        
        String[] parameterNames = nameDiscoverer.getParameterNames(method);
        if (parameterNames == null || parameterNames.length == 0) {
            return keyExpression;
        }
        
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }
        
        Expression expression = parser.parseExpression(keyExpression);
        return expression.getValue(context, String.class);
    }
}
