package org.kolar.kolarcodercopilot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 工具执行 日志记录服务
 */
@Service
public class ToolExecutionLogger {
    private static final Logger logger = LoggerFactory.getLogger(ToolExecutionLogger.class);

    // 工具执行统计
    private final Map<String, ToolStatus> toolStatus = new ConcurrentHashMap<>();

    /**
     * 获取工具执行统计
     */
    public void logToolStatistics(){
        logger.info("📈 ========== 工具执行统计 ==========");
        toolStatus.forEach((toolName, toolStatus) ->{
            logger.info("🔧 工具: {} | 调用: {} | 成功: {} | 失败: {} | 平均耗时: {}ms",
                    toolName,
                    toolStatus.getTotalCalls(),
                    toolStatus.getSuccessCount(),
                    toolStatus.getFailedCount(),
                    toolStatus.getAvgExecutionTime());
        });
        logger.info("📈 ================================");
    }

    /**
     * 工具统计信息内部类
     */
    private static class ToolStatus {
        private long totalCalls = 0;
        private long successCount = 0;
        private long failedCount = 0;
        private long totalExecutionTime = 0;

        private long incrementCalls(){
            return ++totalCalls;
        }
        private long incrementSuccessCount(){
            return ++successCount;
        }
        private long incrementFailedCount(){
            return ++failedCount;
        }
        private long incrementTotalExecutionTime(long time){
            return totalExecutionTime += time;
        }
        private long getTotalCalls() {
            return totalCalls;
        }
        private long getSuccessCount() {
            return successCount;
        }
        private long getFailedCount() {
            return failedCount;
        }
        private long getAvgExecutionTime() {
            if (totalCalls == 0) return 0;
            return totalExecutionTime / totalCalls;
        }


    }

}
