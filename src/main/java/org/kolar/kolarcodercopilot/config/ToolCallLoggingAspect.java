package org.kolar.kolarcodercopilot.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.kolar.kolarcodercopilot.service.LogStreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AOP 专注日志 性能 监控统计等
 */
@Aspect
@Component
public class ToolCallLoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(ToolCallLoggingAspect.class);

    @Autowired
    private LogStreamService  logStreamService;  // 实时日志流推送


    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object interceptToolAnnotation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringTypeName();

        // 详细的参数解析
        String parametersInfo = formatMethodParameters(args);
        String fileInfo = extractFileInfoFromMethodArgs(methodName, args);

        logger.debug("🚀 [Spring AI @Tool] 执行工具: {}.{} | 参数: {} | 文件/目录: {}",
                className, methodName, parametersInfo, fileInfo);

        String taskId = getCurrentTaskId();

        // 推动工具执行概要事件
        if (taskId == null) {
            String summary = generateExecutionSummary(methodName, fileInfo, args);
            String reason = generateExecutionReason(methodName, fileInfo, args);
            logStreamService.pushToolExecuteSummary(taskId, methodName, fileInfo, summary, reason);

        }

        // 推送工具开始执行事件
        if(taskId != null){
            String startMessage = generateStartMessage(methodName, fileInfo);
            logStreamService.pushToolStart(taskId, methodName, fileInfo, startMessage);
        }

        long startTime = System.currentTimeMillis();
        try{
            // 放行！ 让原方法执行
            // 这里是放行信号 ，走SmartEditTool的真正逻辑
            Object proceedResult = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            logger.debug("✅ [Spring AI @Tool] 工具执行成功: {}.{} | 耗时: {}ms | 文件/目录: {} | 参数: {}",
                    className, methodName, executionTime, fileInfo, parametersInfo);

            // 推送工具执行成功事件
            if(taskId != null){
                String successMessage = generateSuccessMessage(methodName, fileInfo, proceedResult, executionTime);
                logStreamService.pushToolExecuteSuccess(taskId, methodName, fileInfo, proceedResult, successMessage, executionTime);
            }
            return proceedResult;

        }catch (Exception e){
            long executionTime = System.currentTimeMillis() - startTime;

            logger.error("❌ [Spring AI @Tool] 工具执行失败: {}.{} | 耗时: {}ms | 文件/目录: {} | 参数: {} | 错误: {}",
                    className, methodName, executionTime, fileInfo, parametersInfo, e.getMessage());

            // 推送工具执行失败事件
            if (taskId != null) {
                String errorMessage = generateErrorMessage(methodName, fileInfo, e.getMessage());
                logStreamService.pushToolError(taskId, methodName, fileInfo, errorMessage, executionTime);
            }
            throw e;
        }
    }



    private String generateErrorMessage(String methodName, String fileInfo, String message) {
            return null;
    }
    private String generateSuccessMessage(String methodName, String fileInfo, Object proceedResult, long executionTime) {
        return null;
    }
    private String generateStartMessage(String methodName, String fileInfo) {
            return null;
    }

    /**
     * 生成工具执行原因
     * 人性化消息生成
     * @param methodName
     * @param fileInfo
     * @param args
     * @return
     */
    private String generateExecutionReason(String methodName, String fileInfo, Object[] args) {
            return null;
    }

    private String generateExecutionSummary(String methodName, String fileInfo, Object[] args) {
        return null;
    }

    /**
     *  从方法参数中直接提取文件信息
     * ！！！ 智能参数解析引擎 ！！！
     */
    private String extractFileInfoFromMethodArgs(String methodName, Object[] args) {
        return null;
    }

    private String formatMethodParameters(Object[] args) {
        return null;
    }

    /**
     * 从线程本地变量或请求上下文中获取
     * @return
     */
    private String getCurrentTaskId() {
        String taskId = TaskContextHolder.getCurrentTaskId();
        return taskId;
    }
}
