package org.kolar.kolarcodercopilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;

/**
 * 应用配置类
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private WorkSpace workspace = new WorkSpace();
    private Security security = new Security();
    private Tools tools = new Tools();
    private Browser browser = new Browser();

    public WorkSpace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkSpace workspace) {
        this.workspace = workspace;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Tools getTools() {
        return tools;
    }

    public void setTools(Tools tools) {
        this.tools = tools;
    }

    public Browser getBrowser() {
        return browser;
    }

    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    /**
     * 工作空间配置 TODO (未看)
     */
    public static class WorkSpace {
        // 使用 Paths.get() 和 File.separator 实现跨平台兼容
        private String rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace").toString();
        private long maxFileSize = 10485760L; // 10MB
        private List<String> allowedExtensions = List.of(
                ".txt", ".md", ".java", ".js", ".ts", ".json", ".xml",
                ".yml", ".yaml", ".properties", ".html", ".css", ".sql"
        );

        // Getters and Setters
        public String getRootDirectory() { return rootDirectory; }
        public void setRootDirectory(String rootDirectory) {
            // 确保设置的路径也是跨平台兼容的
            this.rootDirectory = Paths.get(rootDirectory).toString();
        }

        public long getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }

        public List<String> getAllowedExtensions() { return allowedExtensions; }
        public void setAllowedExtensions(List<String> allowedExtensions) { this.allowedExtensions = allowedExtensions; }

    }

    /**
     * 安全配置
     */
    public static class Security {

    }

    /**
     * 工具配置
     */
    public static class Tools {

    }
    /**
     * 浏览器配置
     */
    public static class Browser {

    }

    /**
     * 审批模式
     */
    public enum ApprovalMode{
        DEFAULT, // 默认 遇危险操作需要用户确认
        AUTO_EDIT, // 自动编辑模式 ，文件编辑不需确认
        YOLO   // 完全自动模式，所有操作均不需要确认
    }


}
