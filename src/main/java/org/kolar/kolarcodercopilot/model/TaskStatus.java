package org.kolar.kolarcodercopilot.model;

import lombok.Data;

@Data
public class TaskStatus {
    private String taskId;
    private String status;  // RUNNING COMPLETED FALSE
    private String currentAction;
    private String summary;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    private int currentTurn;
    private String errorMessage;

    public int getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(int currentTurn) {
        this.currentTurn = currentTurn;
    }

    private int totalEstimatedTurns;

    public TaskStatus(String taskId) {}

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentAction() {
        return currentAction;
    }

    public void setCurrentAction(String currentAction) {
        this.currentAction = currentAction;
    }

    public int getTotalEstimatedTurns() {
        return totalEstimatedTurns;
    }

    public void setTotalEstimatedTurns(int totalEstimatedTurns) {
        this.totalEstimatedTurns = totalEstimatedTurns;
    }

}
