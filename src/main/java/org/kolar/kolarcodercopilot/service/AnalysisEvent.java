package org.kolar.kolarcodercopilot.service;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * AI分析过程事件类
 * 增添分析过程相关字段
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisEvent extends LoginEvent{
    private String stepName;   // 步骤名称
    private String status; // 状态 ANALYSIS COMPLETED ERROR
    private Long executeTime; // 执行时间
    private String description;  // 描述
    private String icon; // 图标
    private String details; // 详细信息

    public AnalysisEvent() {
        super();
    }
    public AnalysisEvent(String type, String taskId, String message, String timestamp,String stepName, String status, Long executeTime, String description, String icon, String details) {
        super(type, taskId, message, timestamp);
        this.stepName = stepName;
        this.status = status;
        this.executeTime = executeTime;
        this.description = description;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Long executeTime) {
        this.executeTime = executeTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
    @Override
    public String toString() {
        return "AnalysisEvent{" +
                "stepName='" + stepName + '\'' +
                ", description='" + description + '\'' +
                ", icon='" + icon + '\'' +
                ", status='" + status + '\'' +
                ", executeTime=" + executeTime +
                ", details='" + details + '\'' +
                "} " + super.toString();
    }
}
