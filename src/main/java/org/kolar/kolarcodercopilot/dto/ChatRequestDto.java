package org.kolar.kolarcodercopilot.dto;

import lombok.Data;

/**
 * 聊天请求数据传输对象
 */
@Data
public class ChatRequestDto {
    private String message;
    private String sessionId; // 可选 用于会话管理

    public ChatRequestDto() {
    }
    public ChatRequestDto(String message) {}
    public ChatRequestDto(String message, String sessionId) {
        this.message = message;
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    @Override
    public String toString() {
        return "ChatRequestDto{" +
                "message='" + message + '\'' +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}
