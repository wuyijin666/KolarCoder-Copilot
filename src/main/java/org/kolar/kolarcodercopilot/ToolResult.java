package org.kolar.kolarcodercopilot;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 工具执行结果
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResult {
    private final boolean success;
    private final String llmContent;
    private final Object returnDisplay;
    private final String errorMessage;

    public boolean isSuccess() {
        return success;
    }

    public String getLlmContent() {
        return llmContent;
    }

    public Object getReturnDisplay() {
        return returnDisplay;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    ToolResult(boolean success, String llmContent, Object returnDisplay, String errorMessage) {
        this.success = success;
        this.llmContent = llmContent;
        this.returnDisplay = returnDisplay;
        this.errorMessage = errorMessage;
    }

    //  静态工厂
    public static ToolResult success(String llmContent){return new ToolResult(true, llmContent, llmContent, null);}

    public static ToolResult success(String llmContent, Object returnDisplay){
        return new ToolResult(true, llmContent, returnDisplay, null);
    }
    public static ToolResult error(String errorMessage){return new ToolResult(false, null, null, errorMessage);}


    @Override
    public String toString() {
       if(success){
           return "ToolResult{result = true, content = '" + llmContent + "'}";
       }else {
           return "ToolResult{result = false, error = '" + errorMessage + "'}";
       }
    }

}
/**
 * 文件差异对比
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class FileDiff {
}

/**
 * 工具确认详情
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class ToolConfirmationDetails {
}
