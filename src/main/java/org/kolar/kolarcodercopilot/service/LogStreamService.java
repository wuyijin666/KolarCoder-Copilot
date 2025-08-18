package org.kolar.kolarcodercopilot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE日志推送服务
 * 负责将AOP日志实时推送到前端
 */
@Service
public class LogStreamService {

    private static final Logger logger = LoggerFactory.getLogger(LogStreamService.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 活跃的SSE连接 taskId -> SseEmitter
    private final Map<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();

    // JSON 序列化器
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 创建SSE连接
     */
    public SseEmitter createConnection(String taskId) {
        logger.info("🔗 建立SSE连接: taskId={}", taskId);

        SseEmitter emitter = new SseEmitter(0L); //无超时
        // 连接处理
        emitter.onCompletion(() -> {
            logger.info("✅ SSE连接完成: taskId={}", taskId);
            activeConnections.remove(taskId);
        });
        emitter.onTimeout(() -> {
            logger.warn("⏰ SSE连接超时: taskId={}", taskId);
            activeConnections.remove(taskId);
        });
        emitter.onError((e) -> {
            logger.error("❌ SSE连接错误: taskId={}, error={}", taskId, e.getMessage());
            activeConnections.remove(taskId);
        });
        // 保存连接
        activeConnections.put(taskId, emitter);
        // 发送连接成功消息
       // ?  sendLogEvent(taskId, activeConnections.get(taskId));
        sendLogEvent(taskId, LoginEvent.createConnectionEvent(taskId));

        return emitter;
    }

    /**
     * 关闭SSE连接
     * @param taskId
     */
    public void closeConnection(String taskId) {
        SseEmitter emitter = activeConnections.remove(taskId);
       if(emitter != null) {
           try {
               emitter.complete();
               logger.info("🔚 关闭SSE连接: taskId={}",  taskId);
           }catch (Exception e) {
               logger.info("关闭SSE连接失败: taskId={}",  taskId);
           }
       }
    }



    /**
     * 推送AI分析过程
     */
    public void pushAnalysisStep(String taskId, String stepName, String description, String status) {
        AnalysisEvent  analysisEvent = new AnalysisEvent();
        analysisEvent.setType("Analysis Step");
        analysisEvent.setStepName(stepName);
        analysisEvent.setTaskId(taskId);
        analysisEvent.setMessage(description);
        analysisEvent.setStatus(status);
        analysisEvent.setTimestamp(LocalDateTime.now().format(formatter));
        analysisEvent.setIcon(getAnalysisIcon(stepName));

        sendLogEvent(taskId, analysisEvent);
    }


    /**
     * SSE 发送日志事件到前端
     */
    public void sendLogEvent(String taskId, Object analysisEvent) {
        SseEmitter emitter = activeConnections.get(taskId);
        if (emitter != null) {
            try {
                String jsonData = objectMapper.writeValueAsString(analysisEvent);
                logger.info("📤 准备推送日志事件: taskId={}, type={}, jsonData={}", taskId,
                        analysisEvent instanceof LoginEvent ? ((LoginEvent)analysisEvent).getType() : "unknown",
                        jsonData);

                emitter.send(SseEmitter.event()
                        .name("log")
                        .data(jsonData));

                logger.info("✅ 日志事件推送成功: taskId={}", taskId);
            } catch (IOException e) {
                logger.error("推送日志失败:  taskId={}, error={}", taskId, e.getMessage());
                activeConnections.remove(taskId);
            }
        } else {
            logger.warn("⚠️ 未找到SSE连接: taskId={}, 无法推送事件", taskId);
        }
    }

    /**
     * 推送任务分析过程
     * @param taskId
     * @param userMessage
     */
    public void pushTaskAnalysisStart(String taskId, String userMessage) {
        AnalysisEvent event = new AnalysisEvent();
        event.setType("TASK_ANALYSIS_START");
        event.setTaskId(taskId);
        event.setStepName("任务分析");
        event.setDescription("开始分析用户需求: " + (userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage));
        event.setStatus("ANALYZING");
        event.setMessage("AI正在分析您的需求...");
        event.setTimestamp(LocalDateTime.now().format(formatter));
        event.setIcon("🧠");

        sendLogEvent(taskId, event);

    }

    /**
     * 推送任务完成事件
     * @param taskId
     */
    public void pushTaskComplete(String taskId){
        LoginEvent event = new LoginEvent();
        event.setType("TASK_COMPLETE");
        event.setTaskId(taskId);
        event.setMessage("对话任务处理完成");
        event.setTimestamp(LocalDateTime.now().format(formatter));

        sendLogEvent(taskId, event);

        // 延迟关闭连接
        new Thread(() ->{
            try {
                Thread.sleep(2000);  // 等待2秒让前端处理完事件
                closeConnection(taskId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 获取分析步骤图标
     */
    private String getAnalysisIcon(String stepName) {
        switch (stepName) {
            case "任务分析": return "🧠";
            case "需求理解": return "💡";
            case "执行计划": return "📋";
            case "技术选型": return "🔧";
            case "架构设计": return "🏗️";
            case "文件规划": return "📁";
            case "代码生成": return "💻";
            case "测试验证": return "✅";
            default: return "🔍";
        }
    }


    /**
     * 推送执行计划生成事件
     */
    public void pushExecutionPlanGenerated(String taskId, String planSummary) {
        AnalysisEvent event = new AnalysisEvent();
        event.setType("EXECUTION_PLAN");
        event.setTaskId(taskId);
        event.setStepName("执行计划");
        event.setDescription(planSummary);
        event.setStatus("COMPLETED");
        event.setMessage("执行计划已生成");
        event.setTimestamp(LocalDateTime.now().format(formatter));
        event.setIcon("📋");

        sendLogEvent(taskId, event);
    }
}
