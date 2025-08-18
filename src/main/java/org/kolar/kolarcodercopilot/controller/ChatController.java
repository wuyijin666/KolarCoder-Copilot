package org.kolar.kolarcodercopilot.controller;

import org.kolar.kolarcodercopilot.dto.ChatRequestDto;
import org.kolar.kolarcodercopilot.service.ContinuousConversationService;
import org.kolar.kolarcodercopilot.service.ToolExecutionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 聊天控制器
 * 处理与 AI 地对话与工具调用
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;
    private final ContinuousConversationService continuousConversationService;
    private final ToolExecutionLogger toolExecutionLogger;

    // 简单地会话存储 （生产环境建议db / redis）
    private final List<Message> chatHistory = new ArrayList<>();

    public ChatController(ChatClient chatClient, ContinuousConversationService continuousConversationService, ToolExecutionLogger toolExecutionLogger) {
        this.chatClient = chatClient;
        this.continuousConversationService = continuousConversationService;
        this.toolExecutionLogger = toolExecutionLogger;
    }

    /**
     * 处理复杂请求对话 - 支持连续工具调用
     * 异步处理响应
     */
    @PostMapping("/message")
    public Mono<ChatResponseDto> sendMessage(@RequestBody ChatRequestDto request){

            return Mono.fromCallable(() ->{
                try {
                    logger.info("💬 ========== 新的聊天请求 ==========");
                    logger.info("📝 用户消息: {}", request.getMessage());
                    logger.info("🕐 请求时间: {}", java.time.LocalDateTime.now());

                    // 同一使用异步模式 让大模型自己决定是否需要工具调用
                    String taskId = continuousConversationService.startTask(request.getMessage());
                    logger.info("🆔 任务ID: {}", taskId);

                    // 记录任务开始
                    toolExecutionLogger.logToolStatistics(); // 显示当前工具使用统计
                    // 异步执行连续对话
                    CompletableFuture.runAsync(() ->{
                        try {
                            logger.info("🚀 开始异步执行连续对话任务: {}", taskId);
                            continuousConversationService.executeContinuousConversation(taskId, request.getMessage(), chatHistory);
                            logger.info("✅ 连续对话任务完成: {}", taskId);
                        } catch (Exception e) {
                            logger.error("❌ 异步对话执行错误: {}", e.getMessage(), e);
                        }
                    });

                    // 返回异步响应结果
                    ChatResponseDto chatResponseDto = new ChatResponseDto();
                    chatResponseDto.setTaskId(taskId);
                    chatResponseDto.setMessage("任务已启动, 正在进行中...");
                    chatResponseDto.setSuccess(true);
                    chatResponseDto.setAsyncTask(true);

                    logger.info("📤 返回响应: taskId={}, 异步任务已启动", taskId);

                    return chatResponseDto;
                } catch (Exception e) {
                    logger.error("Error process chat message", e);
                    ChatResponseDto errorResponse = new ChatResponseDto();
                    errorResponse.setMessage("Error:" + e.getMessage());
                    errorResponse.setSuccess(false);
                    return errorResponse;
                }
            });
    }

    /**
     * 处理简单请求对话
     * 流式响应 SSE
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(@RequestBody ChatRequestDto request){
        logger.info("🌊 开始流式对话: {}", request.getMessage());

        return Flux.create(sink -> {
            UserMessage userMessage = new UserMessage(request.getMessage());
            chatHistory.add(userMessage);

            try {
                // 使用Spring AI 的流式api
                Flux<String> contentStream = chatClient.prompt()
                        .messages(chatHistory)
                        .stream()
                        .content();
                // 订阅流式内容并转发给前端
                contentStream
                        .doOnNext(content ->{
                            logger.debug("📨 流式内容片段: {}" , content);
                            // 发送SSE格式数据
                            sink.next("data: " + content + "\n\n");
                        })
                        .doOnComplete(() ->{
                            logger.info("✅ 流式对话完成");
                            sink.next("data: [complete]\n\n");
                            sink.complete();
                        })
                        .doOnError(error ->{
                            logger.error("❌ 流式对话错误: {}", error.getMessage());
                            sink.error(error);
                        })
                        .subscribe();
            } catch (Exception e) {
               logger.error("❌ 流式对话启动失败: {}", e.getMessage());
               sink.error(e);
            }
        });
    }


    public static class ChatResponseDto {
        private String taskId;
        private String message;
        private boolean success;
        private boolean asyncTask;
        private boolean streamResponse;
        private int totalTurns;
        private boolean reachedMaxTurns;
        private String stopReason;
        private long totalDurationMs;

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public boolean isAsyncTask() {
            return asyncTask;
        }

        public void setAsyncTask(boolean asyncTask) {
            this.asyncTask = asyncTask;
        }

        public boolean isStreamResponse() {
            return streamResponse;
        }

        public void setStreamResponse(boolean streamResponse) {
            this.streamResponse = streamResponse;
        }

        public int getTotalTurns() {
            return totalTurns;
        }

        public void setTotalTurns(int totalTurns) {
            this.totalTurns = totalTurns;
        }

        public boolean isReachedMaxTurns() {
            return reachedMaxTurns;
        }

        public void setReachedMaxTurns(boolean reachedMaxTurns) {
            this.reachedMaxTurns = reachedMaxTurns;
        }

        public String getStopReason() {
            return stopReason;
        }

        public void setStopReason(String stopReason) {
            this.stopReason = stopReason;
        }

        public long getTotalDurationMs() {
            return totalDurationMs;
        }

        public void setTotalDurationMs(long totalDurationMs) {
            this.totalDurationMs = totalDurationMs;
        }



    }




}
