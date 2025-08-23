package org.kolar.kolarcodercopilot.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.kolar.kolarcodercopilot.service.LogStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.SyncFailedException;

@Aspect
@Component
public class MyToolLoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(MyToolLoggingAspect.class);

    // 主要的拦截逻辑
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping))")
    public Object interceptMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Long startTime = System.currentTimeMillis();

        try{
            // 执行前逻辑
            logBefore(joinPoint);
            // 执行中
            Object result = joinPoint.proceed();

            // 执行后逻辑
            logAfter(joinPoint, result, System.currentTimeMillis() - startTime);
            return result;

        }catch (Exception e){
            // 异常逻辑
            logError(joinPoint,  e,   System.currentTimeMillis() - startTime);
            throw e;
        }
    }

    private void logBefore(ProceedingJoinPoint joinPoint) throws Exception{
        String methodName = joinPoint.getSignature().getName();
        logger.info("开始执行: {}", methodName);
    }

    private void logAfter(ProceedingJoinPoint joinPoint, Object result, long time) throws Exception{
        String methodName = joinPoint.getSignature().getName();
        logger.info("执行完成: {} 执行耗时: {}ms", methodName,time);
    }

    private void logError(ProceedingJoinPoint joinPoint, Exception e,  long time){
        String methodName = joinPoint.getSignature().getName();
        logger.info("执行失败: {}, 耗时 :{}ms, 失败错误为:{}", methodName, time,e.getMessage());
    }

}
