package org.kolar.kolarcodercopilot.service;

import org.kolar.kolarcodercopilot.config.TaskContextHolder;
import org.kolar.kolarcodercopilot.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连续对话服务
 */
@Service
public class ContinuousConversationService {
    private final static Logger logger = LoggerFactory.getLogger(ContinuousConversationService.class);

    private final ChatClient chatClient;


    @Autowired
    private LogStreamService logStreamService;


    private final static int MAX_TURNS = 20; // 最大轮数限制
    private final static long TURN_TIMEOUT_MS = 60_000; // 单轮对话超时时间（毫秒） 60秒
    private final static long TOTAL_TIMEOUT_MS = 10 * TURN_TIMEOUT_MS; // 总对话超时时间（毫秒） 10分钟

    private final TaskSummaryService taskSummaryService;

    //状态容器
    private final Map<String, TaskStatus> taskStatusMap = new ConcurrentHashMap<>();
    private final Map<String, ConversationResult>  conversationResultMap = new ConcurrentHashMap<>();

    public ContinuousConversationService(ChatClient chatClient, TaskSummaryService taskSummaryService) {
        this.chatClient = chatClient;
        this.taskSummaryService = taskSummaryService;
    }



    public String startTask(String initialMessage){
       String taskId = UUID.randomUUID().toString();
       TaskStatus status = new TaskStatus(taskId);

       // 估算任务复杂度
        int turns = taskSummaryService.estimateTaskComplexity(initialMessage);
        status.setTotalEstimatedTurns(turns);
        status.setCurrentAction("任务开始分析");

        return taskId;
    }

    public ConversationResult executeContinuousConversation(String taskId, String initialMessage, List<Message> chatHistory) throws IllegalAccessException {
        TaskStatus taskStatus = taskStatusMap.get(taskId);
        if(taskStatus == null){
              throw new IllegalAccessException("task is not found" + taskId);
        }
        // 设置任务上下文 供AOP切面使用
        TaskContextHolder.setCurrentTaskId(taskId);

        long currentStartTime = System.currentTimeMillis();
        logger.info("start continuous conversstion with message: {}", initialMessage);

        // 推送任务分析开始事件
        logStreamService.pushTaskAnalysisStart(taskId, initialMessage);

        // 更新任务状态
        taskStatus.setCurrentAction("AI 正在分析需求...");
        taskStatus.setCurrentTurn(0);

        // 推送需求理解步骤
        logStreamService.pushAnalysisStep(taskId, "需求理解",
                "正在理解和分析用户需求的具体内容", "ANALYZING");

        // 推送需求分析完成
        logStreamService.pushAnalysisStep(taskId,"需求分析",
                "已完成需求分析，开始制定执行计划", "COMPLETED");
        // 推送执行计划生成
        logStreamService.pushAnalysisStep(taskId, "执行计划",
                "正在生成详细的执行计划和步骤", "ANALYZING");
        // 创建工作副本
        List<Message> workingHistory = new ArrayList<>();
        StringBuilder fullResponse = new StringBuilder();
        List<String> turnResponses = new ArrayList<>();

        // 保存用户初始信息
        UserMessage userMessage = new UserMessage(initialMessage);
        workingHistory.add(userMessage);

        int turnCount = 0;
        boolean shouldContinue = true;
        String stopReason = null;

        try{
            // 模拟执行计划生成时间
            Thread.sleep(500);
            // 推送执行计划完成
            String planSummary = "已生成执行计划: 分析项目结构 -> 创建必要文件 -> 实现核心功能 -> 测试验证";
            logStreamService.pushExecutionPlanGenerated(taskId, planSummary);

            while(shouldContinue && turnCount < MAX_TURNS) {
                turnCount++;

                taskStatus.setCurrentTurn(turnCount);
                taskStatus.setCurrentAction(String.format("正在进行第 %d 轮对话...", turnCount));

                // 检查超时
                long elapedTime = System.currentTimeMillis() - currentStartTime;
                if(elapedTime > TOTAL_TIMEOUT_MS) {
                    logger.warn("conversation is expired after {} ms", elapedTime);
                    stopReason = "Total conversation timeout expired";
                    break;
                }

                try{
                    // 开始执行单轮对话
                    TurnResult turnResult = executeSingleTurn(workingHistory, turnCount);

                    if( !turnResult.isSuccess){
                        logger.error("Turn: {} err: {}", turnCount, turnResult.getErrorMessage());
                        stopReason = "Turn execution failed: " + turnResult.getErrorMessage();
                        break;
                    }
                    // 添加响应到历史
                    String responseText = turnResult.getResponse();
                    if(responseText != null && !responseText.trim().isEmpty()){
                        AssistantMessage assistantMessage = new AssistantMessage(responseText);  // AI助手响应消息的封装
                        workingHistory.add(assistantMessage);

                        // 累积响应
                        if(responseText != null){
                            fullResponse.append("\n\n");
                        }
                        fullResponse.append(responseText);
                        turnResponses.add(responseText);

                       // 更新任务状态 - 显示当前任务的简短摘要
                        String responseSummary = responseText.length() > 100 ?
                        responseText.substring(0,100) + "..." : responseText;
                        taskStatus.setCurrentAction(String.format("第 %d 轮对话， 完成: %s", turnCount, responseSummary));
                    }
                    // 判断是否继续
                    taskStatus.setCurrentAction(String.format("判断第 %d 轮对话是否继续...", turnCount));
                    shouldContinue = ShouldContinueConversation(workingHistory, turnCount, responseText);


                    if(shouldContinue && turnCount < MAX_TURNS) {
                        // 添加继续提示
                        String continuePrompt = getContinuePrompt(turnCount);
                        UserMessage continueMessage = new UserMessage(continuePrompt);
                        workingHistory.add(continueMessage);

                        logger.debug("added continue prompt for turn: {} {}", turnCount, continuePrompt);
                        taskStatus.setCurrentAction(String.format("继续进行第 %d 轮对话", turnCount+1));
                    }else{
                        taskStatus.setCurrentAction("对话即将结束...");
                    }

                }catch(Exception e){
                    logger.error("Error conversation in turn{} :{}", turnCount, e.getMessage());
                    stopReason = "Exception conversation in turn" + turnCount + ": " + e.getMessage();

                    // 添加错误信息到响应中
                    String errorMessage = "❌ Exception conversation in turn" + turnCount + ": " + e.getMessage();
                    if(fullResponse.length() > 0){
                        fullResponse.append("\n\n");
                    }
                    fullResponse.append(errorMessage);
                    turnResponses.add(errorMessage);

                    // 更新任务状态出错
                    taskStatus.setStatus("FAILED");
                    taskStatus.setCurrentAction(String.format("执行出错: {}" , e.getMessage()));
                    taskStatus.setErrorMessage(e.getMessage());
                    break;
                }
            }

            long totalDuration = System.currentTimeMillis() - currentStartTime;
            logger.info("Continuous conversation completed after {} turns in {}ms. Stop reason: {}",
                    turnCount, totalDuration, stopReason);

            // 创建结果对象
            ConversationResult result = new ConversationResult(
                    fullResponse.toString(),
                    turnResponses,
                    workingHistory,
                    turnCount,
                    turnCount >= MAX_TURNS,
                    stopReason,
                    totalDuration
            );
            // 更新任务状态完成
            taskStatus.setStatus("COMPLETED");
            taskStatus.setCurrentAction("对话完成");
            String summry = String.format("对话已完成，共 %d 轮, 用时 %.1f 秒",
                    turnCount, totalDuration / 1000.0);
            if(stopReason != null) {
                summry += ", 停止原因: " + stopReason;
            }
            taskStatus.setSummary(summry);

            // 存储结果
            conversationResultMap.put(taskId, result);

            // 推送对话完成结果
            logStreamService.pushTaskComplete(taskId);
            return result;
        }catch (Exception e){
           // 处理整个对话中的异常
            logger.info("Error continuous conversation error:{}", e.getMessage());
            taskStatus.setStatus("FAILED");
            taskStatus.setErrorMessage(e.getMessage());
            taskStatus.setCurrentAction("执行失败");
        }finally {
            // 清理对话上下文
            TaskContextHolder.clearCurrentTaskId();
        }
        return null;
    }

    /**
     * 执行单轮对话
     * @param chatHistory
     * @param turnCount
     * @return
     * @throws IllegalAccessException
     */
    public TurnResult executeSingleTurn(List<Message> chatHistory, int turnCount) throws IllegalAccessException {}

    /**
     * 连续对话结果
     */
    public static class ConversationResult {
        private final String fullResponse;
        private final List<String> turnResponses;
        private final List<Message> finalHistory;
        private final int totalTurns;
        private final boolean reachedMaxTurns;
        private final String stopReason;
        private final long totalDurationMs;

        public ConversationResult(String fullResponse, List<String> turnResponses,
                                  List<Message> finalHistory, int totalTurns, boolean reachedMaxTurns,
                                  String stopReason, long totalDurationMs) {
            this.fullResponse = fullResponse;
            this.turnResponses = turnResponses;
            this.finalHistory = finalHistory;
            this.totalTurns = totalTurns;
            this.reachedMaxTurns = reachedMaxTurns;
            this.stopReason = stopReason;
            this.totalDurationMs = totalDurationMs;
        }

        public String getFullResponse() { return fullResponse; }
        public List<String> getTurnResponses() { return turnResponses; }
        public List<Message> getFinalHistory() { return finalHistory; }
        public int getTotalTurns() { return totalTurns; }
        public boolean isReachedMaxTurns() { return reachedMaxTurns; }
        public String getStopReason() { return stopReason; }
        public long getTotalDurationMs() { return totalDurationMs; }
    }

    /**
     * 执行单轮对话结果
     */
    public final static class TurnResult {
        private boolean isSuccess;
        private String response;
        private String errorMessage;

        public boolean isSuccess() {
            return isSuccess;
        }

        public void setSuccess(boolean success) {
            isSuccess = success;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public TurnResult() {

        }
        public TurnResult(boolean isSuccess, String response, String errorResponse) {
            this.isSuccess = isSuccess;
            this.response = response;
            this.errorMessage = errorResponse;
        }

    }
}
