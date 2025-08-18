package org.kolar.kolarcodercopilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class TaskSummaryService {
    private static final Logger logger = LoggerFactory.getLogger(TaskSummaryService.class);

    private static final Pattern[] ACTION_PATTERNS = {
            Pattern.compile("(?i)creating?\\s+(?:a\\s+)?(?:new\\s+)?(.{1,50}?)(?:\\s+file|\\s+directory|\\s+project)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)writing?\\s+(?:to\\s+)?(.{1,50}?)(?:\\s+file)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)reading?\\s+(?:from\\s+)?(.{1,50}?)(?:\\s+file)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)editing?\\s+(.{1,50}?)(?:\\s+file)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)listing?\\s+(?:the\\s+)?(.{1,50}?)(?:\\s+directory)?", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)analyzing?\\s+(.{1,50}?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)generating?\\s+(.{1,50}?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?i)building?\\s+(.{1,50}?)", Pattern.CASE_INSENSITIVE)
    };



    /**
     * 估算任务复杂度和预期轮数
     */
    public int estimateTaskComplexity(String initialMessage) {
        if (initialMessage == null) return 1;

        String lowerMessage = initialMessage.toLowerCase();
        int complexity = 1;

        // 基于关键词估算复杂度
        if (lowerMessage.contains("project") || lowerMessage.contains("项目")) complexity += 3;
        if (lowerMessage.contains("complete") || lowerMessage.contains("完整")) complexity += 2;
        if (lowerMessage.contains("multiple") || lowerMessage.contains("多个")) complexity += 2;
        if (lowerMessage.contains("full-stack") || lowerMessage.contains("全栈")) complexity += 4;
        if (lowerMessage.contains("website") || lowerMessage.contains("网站")) complexity += 2;
        if (lowerMessage.contains("api") || lowerMessage.contains("接口")) complexity += 2;

        // 基于文件操作数量估算
        long fileOperations = lowerMessage.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .filter(s -> s.matches(".*(?:create|write|edit|file|directory).*"))
                .count();

        complexity += (int) Math.min(fileOperations / 2, 5);

        return Math.min(complexity, 15); // 最大15轮
    }
}
