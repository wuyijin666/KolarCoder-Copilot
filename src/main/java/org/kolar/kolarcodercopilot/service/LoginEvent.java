package org.kolar.kolarcodercopilot.service;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志事件基类
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginEvent {
    private String type;
    private String taskId;
    private String message;
    private String timestamp;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Constructors
    public LoginEvent() {}
    public LoginEvent(String type, String taskId, String message) {}

    public LoginEvent(String type, String taskId, String message, String timestamp) {
    }

    // static factory function
    public static Object createConnectionEvent(String taskId) {
        LoginEvent event = new LoginEvent();
        event.setType("CONNECTION_ESTABLISHED");
        event.setTaskId(taskId);
        event.setMessage("SSE连接已建立");
        event.setTimestamp(LocalDateTime.now().format(formatter));
        return event;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }


    @Override
    public String toString() {
        return "LogEvent{" +
                "type='" + type + '\'' +
                ", taskId='" + taskId + '\'' +
                ", message='" + message + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}



