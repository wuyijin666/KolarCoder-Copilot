package org.kolar.kolarcodercopilot.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.kolar.kolarcodercopilot.config.AppProperties;
import org.kolar.kolarcodercopilot.model.ProjectContext;
import org.kolar.kolarcodercopilot.schema.JsonSchema;
import org.kolar.kolarcodercopilot.service.ProjectContextAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * smart editing tool
 * provides intelligent multi-file editing capabilities based on project context understanding
 */
@Component
public class SmartEditTool extends BaseTool<SmartEditTool.SmartEditParams>{
    private static final Logger logger = LoggerFactory.getLogger(SmartEditTool.class);

    @Autowired
    public ProjectContextAnalyzer projectContextAnalyzer;
    @Autowired
    public ChatClient chatClient;

    private final String rootDirectory;
    private final AppProperties appProperties;
    @Autowired
    private ChatModel chatModel;

    public SmartEditTool(AppProperties appProperties){
        super(
                "smart_edit",
                "SmartEdit",
                "",
                createSchema()
        );
        this.appProperties = appProperties;
        this.rootDirectory = appProperties.getWorkspace().getRootDirectory();
    }

    private static JsonSchema createSchema() {
        return org.kolar.kolarcodercopilot.schema.JsonSchema.object()
                .addProperty("project_path", org.kolar.kolarcodercopilot.schema.JsonSchema.String("Absolute path to the project root directory to analyze and edit"))
                .addProperty("edit_description", org.kolar.kolarcodercopilot.schema.JsonSchema.String("Natural language description of the desired changes. " +
                        "Examples: 'Add a new REST endpoint for user management', "+
                        "'Refactor the authentication logic' , 'Update dependencies to lastest versions'"
                ))
                .addProperty("target_files", org.kolar.kolarcodercopilot.schema.JsonSchema.array(org.kolar.kolarcodercopilot.schema.JsonSchema.String("Optional: Specific files to focus on. If not provided, the tool will determine which files to edit based on description")))
                .addProperty("scope", org.kolar.kolarcodercopilot.schema.JsonSchema.String("Edit scope: 'single_file', 'related_files', or 'project_wide'. Default: 'related_files'"))
                .addProperty("dry_run", org.kolar.kolarcodercopilot.schema.JsonSchema.bool("If true, only analyze and show what would be changed without making actual changes. Default: false"))
                .required("project_path", "edit_description");
    }

    public enum EditScope {
        SINGLE_FILE("single_file","Edit only one file"),
        RELATED_FILES("related_files", "Edit related files that are affected by the change"),
        PROJECT_WIDE("project_wide", "Make project-wide changes including configuration files");

        private final String value;
        private final String description;

        EditScope(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public static EditScope forString(String value) {
            for(EditScope scope : EditScope.values()){
                if(scope.value.equals(value)){
                    return scope;
                }
            }
            return RELATED_FILES; // Default
        }
        public String getValue() {
            return value;
        }
        public String getDescription() {
            return description;
        }
    }

    /**
     * MCP约束
     * Smart edit tool method for Spring AI integration(集成)
     */
    @Tool(name="smart_edit", description = "Intelligently edit projects based on natural language descriptions")
    public String smartEdit(String editDescription, String projectPath, String scope, Boolean dryRun){
        try{
            // 构建SmartEditParams对象
            SmartEditParams params = new SmartEditParams();
            params.setEditDescription(editDescription);
            params.setProjectPath(projectPath);
            params.setScope(scope);
            params.setDryRun(dryRun);

            // 进行参数验证
            String validation = validateToolParams(params);
            if(validation != null){
                logger.info("Validation failed for {}",validation);
                return validation;
            }

            // execute the tool
            ToolResult result = execute(params).join(); // 异步执行
            if(result.isSuccess()){
                return result.getLlmContent();
            }else {
                return "Error: " + result.getErrorMessage();
            }

        }catch (Exception e){
            logger.error("Error while executing smart edit tool",e);
            return "Error: " + e.getMessage();
        }

    }

    @Override
    public String validateToolParams(SmartEditParams params){
        String baseValidation = super.validateToolParams(params);
        if (baseValidation != null) {
            return baseValidation;
        }

        if (params.projectPath == null || params.projectPath.trim().isEmpty()) {
            return "Project path cannot be empty";
        }

        if (params.editDescription == null || params.editDescription.trim().isEmpty()) {
            return "Edit description cannot be empty";
        }

        Path projectPath = Paths.get(params.projectPath);
        if (!projectPath.isAbsolute()) {
            return "Project path must be absolute: " + params.projectPath;
        }

        if (!Files.exists(projectPath)) {
            return "Project path does not exist: " + params.projectPath;
        }

        if (!Files.isDirectory(projectPath)) {
            return "Project path must be a directory: " + params.projectPath;
        }

        if (!isWithinWorkspace(projectPath)) {
            return "Project path must be within the workspace directory: " + params.projectPath;
        }
        return null;
    }

    @Override
    public CompletableFuture<ToolResult> execute(SmartEditParams params) {
        // CompleteFuture异步编程 处理AI延迟调用
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting smart edit for project:{}", params.getProjectPath());
                logger.info("Edit description:{}", params.getEditDescription());

                // 1. 分析项目上下文
                Path projectPath = Paths.get(rootDirectory, params.getProjectPath());
                ProjectContext context = projectContextAnalyzer.analyzeProject(projectPath);

                // 2. 构建执行计划
                EditPlan plan = generateEditPlan(params, context);
                if(params.dryRun || params.getDryRun() != null){
                    return ToolResult.success("Dry run completed. Edit plan successfully.",
                            plan.toString()
                    );
                }
                // 3. 执行执行计划
               EditResult result = excuteEditPlan(plan);
                logger.info("Smart edit result for project:{}", params.getProjectPath());
                return ToolResult.success(
                        result.getSummary(),
                        result.getDetails()
                );
            }catch (Exception e){
                logger.error("Error during smart edit execution", e);
                return ToolResult.error(e.getMessage());
            }
        });
    }

    /**
     * 创建执行计划
     * @param params
     * @param context
     * @return
     */
    private EditPlan generateEditPlan(SmartEditParams params, ProjectContext context) {
        logger.info("Generating Edit Plan for :{}", params.editDescription);

        EditPlan editPlan = new EditPlan();
        editPlan.setDescription(params.editDescription);
        editPlan.setScope(EditScope.forString(params.getScope()));
        editPlan.setProjectContext(context);

        // use AI to analyze edit intent（意图） and generate specific steps
        String editContext = buildEditContext(context, params.editDescription);

        List<EditStep> editSteps = generateEditSteps(editContext, params);

        editPlan.setSteps(editSteps);
        return editPlan;
    }

    /**
     * 构建 上下文感知系统
     * ！！！ 逻辑设计让AI可以更智能地做决策
     * 重点： projectContext的分析与建模
     * build edit context from project context and description
     * @param context
     * @param editDescription
     * @return
     */
    private String buildEditContext(ProjectContext context, String editDescription) {
        StringBuilder contextBuilder = new StringBuilder();

        contextBuilder.append("PROJECT CONTEXT:\n");
        contextBuilder.append("Type: ").append(context.getProjectType().getDisplayName()).append("\n");
        contextBuilder.append("Language: ").append(context.getProjectType().getPrimaryLanguage()).append("\n");

        if (context.getProjectStructure() != null) {
            contextBuilder.append("Structure: ").append(context.getProjectStructure().getProjectSummary()).append("\n");
        }
        if(context.getDependencies() != null) {
            contextBuilder.append("Dependencies: ").append(context.getDependencySummary()).append("\n");
        }
        contextBuilder.append("\nEDIT REQUEST:").append(editDescription);
        return contextBuilder.toString();
    }

    /**
     * use AI to generate edit steps
     */
    private List<EditStep> generateEditSteps(String editContext, SmartEditParams params) {
        List<EditStep> steps = new ArrayList<>();

        try{
            // 构建结构化的AI提示词
            String prompt = generateEditPlanPrompt(editContext, params);
            // 将提示词包装为用户信息
            List<Message> message = List.of(new UserMessage(prompt));
            // 与AI大模型进行交互
            ChatResponse response = ChatClient.create(chatModel)
                    .prompt()
                    .messages(message)
                    .call()
                    .chatResponse();
            String aiResponse = response.getResult().getOutput().getText();
            steps = parseEditStepFromAI(aiResponse, params);

        }catch (Exception e){
            logger.error("Failed to generate AI-Based edit steps, using fallback", e);
            // 降级机制 AI服务不可用时，优雅降级， 实现用户无感知切换
            steps = generateFallBackEditSteps(params); //AI失败时的备选方案
        }
        return steps;
    }

    private String generateEditPlanPrompt(String editContext, SmartEditParams params) {
        return String.format("""
            You are an expert software developer. Based on the project context below, 
            create a detailed plan to implement the requested changes.
            
            %s
            
            TASK: %s
            
            Please provide a step-by-step plan in the following format:
            STEP 1: [Action] - [File] - [Description]
            STEP 2: [Action] - [File] - [Description]
            ...
            
            Actions can be: CREATE, EDIT, DELETE, RENAME
            Be specific about which files need to be modified and what changes are needed.
            Consider dependencies between files and the overall project structure.
            """ , editContext, params.getEditDescription());
    }
    /**
     * parse edit step from AI response
     */
    private List<EditStep> parseEditStepFromAI(String aiResponse, SmartEditParams params) {
        List<EditStep> steps = new ArrayList<>();
        if(aiResponse == null || aiResponse.trim().isEmpty()){
            logger.warn("AI response is null or empty");
            return steps;
        }
        // 按行分割响应文本
       String[] lines = aiResponse.split("\n");

            for(String line : lines){
                line = line.trim();
                try{
                    if(line.startsWith("STEP") && line.contains(":")){
                        EditStep step = parseEditStepLine(line, params);
                        if(step != null){
                            steps.add(step);
                        }
                    }
                }catch (Exception e){
                    logger.warn("Failed to parse AI-based edit step :{}", line);
                }
            }
        return steps;
    }
    /**
     * parse single edit step line
     */
    private EditStep parseEditStepLine(String line, SmartEditParams params) {
        EditStep step = new EditStep();
        String[] parts = line.split("-");
        if(parts.length >= 3){
            String  actionPart = parts[0].substring(parts[0].lastIndexOf(":") + 1).trim();
            String  filePart = parts[1].trim();
            String  descriptionPart = parts[2].trim();

            step.setAction(actionPart);
            step.setTargetFile(filePart);
            step.setDescription(descriptionPart);
            return step;
        }
        return null;
    }

    /**
     * generate fallback edit step
     */
    private List<EditStep> generateFallBackEditSteps(SmartEditParams params) {
        List<EditStep> steps = new ArrayList<>();

        // simple fallback edit step logic
        if(params.getTargetFiles() != null && !params.getTargetFiles().isEmpty()){
            for(String file : params.getTargetFiles()){
                EditStep step = new EditStep();
                step.setAction("EDIT");
                step.setTargetFile(file);
                step.setDescription("edit file:" + file + "according to" +  params.getEditDescription());
                steps.add(step);
            }
        }else{
            EditStep step = new EditStep();
            step.setAction("ANALYZE");
            step.setTargetFile("*");
            step.setDescription("Analyze project and apply changes: " + params.getEditDescription());
            steps.add(step);
        }
        return null;
    }



    // Inner class definition
    public static class SmartEditParams{

        @JsonProperty("project_path")
        private String projectPath;

        @JsonProperty("edit_description")
        private String editDescription;

        @JsonProperty("target_files")
        private List<String> targetFiles;

        @JsonProperty("scope")
        private String scope = "related_files";

        @JsonProperty("dry_run")
        private Boolean dryRun = false;

        public String getProjectPath() {
            return projectPath;
        }
        public void setProjectPath(String projectPath) {
            this.projectPath = projectPath;
        }

        public String getEditDescription() {
            return editDescription;
        }
        public void setEditDescription(String editDescription) {
            this.editDescription = editDescription;
        }

        public List<String> getTargetFiles() {
            return targetFiles;
        }
        public void setTargetFiles(List<String> targetFiles) {
            this.targetFiles = targetFiles;
        }

        public String getScope() {
            return scope;
        }
        public void setScope(String scope) {
            this.scope = scope;
        }

        public Boolean getDryRun() {
            return dryRun;
        }
        public void setDryRun(Boolean dryRun) {
            this.dryRun = dryRun;
        }
    }

    private static class EditResult{
        private List<String> executeSteps;
        private List<String> errors;
        private String summary;
        private String details;

        public List<String> getExecuteSteps() {
            return executeSteps;
        }
        public void setExecuteSteps(List<String> executeSteps) {
            this.executeSteps = executeSteps;
        }

        public List<String> getErrors() {
            return errors;
        }
        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        public String getSummary() {
            return summary;
        }
        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getDetails() {
            return details;
        }
        public void setDetails(String details) {
            this.details = details;
        }
    }

    public static class EditPlan{
        private String description;
        private EditScope  scope;
        private ProjectContext projectContext;
        private List<EditStep> steps;

        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }

        public EditScope getScope() {
            return scope;
        }
        public void setScope(EditScope scope) {
            this.scope = scope;
        }

        public ProjectContext getProjectContext() {
            return projectContext;
        }
        public void setProjectContext(ProjectContext projectContext) {
            this.projectContext = projectContext;
        }

        public List<EditStep> getSteps() {
            return steps;
        }
        public void setSteps(List<EditStep> steps) {
            this.steps = steps;
        }
    }

    private static class EditStep{
        private String action;
        private String targetFile;
        private String description;

        public String getAction() {
            return action;
        }
        public void setAction(String action) {
            this.action = action;
        }

        public String getTargetFile() {
            return targetFile;
        }
        public void setTargetFile(String targetFile) {
            this.targetFile = targetFile;
        }

        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
    }

}