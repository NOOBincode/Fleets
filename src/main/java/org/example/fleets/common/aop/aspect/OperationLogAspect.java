package org.example.fleets.common.aop.aspect;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.fleets.common.aop.annotation.OperationLog;
import org.example.fleets.common.exception.BusinessException;
import org.example.fleets.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 操作日志切面
 * 自动记录方法执行的操作日志
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Around("@annotation(operationLog)")
    public Object logOperation(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        String module = operationLog.module();
        String operation = operationLog.operation();
        Long userId = getCurrentUserId();
        String methodName = ((MethodSignature) pjp.getSignature()).getMethod().getName();
        
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("[").append(module).append("] ");
        logBuilder.append(operation).append(" - ");
        
        if (operationLog.logArgs()) {
            logBuilder.append("参数: ").append(Arrays.toString(pjp.getArgs())).append(" | ");
        }
        
        if (userId != null) {
            logBuilder.append("userId: ").append(userId).append(" | ");
        }
        
        log.info("{}开始执行", logBuilder);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = pjp.proceed();
            
            long cost = System.currentTimeMillis() - startTime;
            StringBuilder successLog = new StringBuilder(logBuilder);
            successLog.append("状态: 成功");
            
            if (operationLog.logCost()) {
                successLog.append(" | 耗时: ").append(cost).append("ms");
            }
            
            if (operationLog.logResult() && result != null) {
                successLog.append(" | 结果: ").append(result.toString());
            }
            
            log.info("{}", successLog);
            return result;
            
        } catch (BusinessException e) {
            long cost = System.currentTimeMillis() - startTime;
            log.warn("{}状态: 业务异常 | 错误: {} | 耗时: {}ms", 
                    logBuilder, e.getMessage(), cost);
            throw e;
            
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - startTime;
            log.error("{}状态: 系统异常 | 耗时: {}ms", 
                    logBuilder, cost, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, e);
        }
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
