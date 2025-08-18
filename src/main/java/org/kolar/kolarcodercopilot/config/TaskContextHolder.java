package org.kolar.kolarcodercopilot.config;

/**
 *  任务上下文管理 供AOP切面使用
 */
public class TaskContextHolder {
    private static final ThreadLocal<String> taskIdHolder = new ThreadLocal<>();
    /**
     * 设置当前任务Id
     */
    public static void setCurrentTaskId(String taskId) { taskIdHolder.set(taskId); }
    /**
     * 获取当前任务Id
     */
    public static String getCurrentTaskId() { return taskIdHolder.get(); }
    /**
     * 清除当前任务
     */
    public static void clearCurrentTaskId() { taskIdHolder.remove(); }
    /**
     * 检查当前任务Id是否存在
     */
    public static boolean hasCurrentTaskId() { return taskIdHolder.get() != null; }

}
