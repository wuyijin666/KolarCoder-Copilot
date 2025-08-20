package org.kolar.kolarcodercopilot.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import org.kolar.kolarcodercopilot.ToolResult;
import org.kolar.kolarcodercopilot.config.AppProperties;
import org.kolar.kolarcodercopilot.model.ProjectContext;
import org.kolar.kolarcodercopilot.schema.JsonSchema;
import org.kolar.kolarcodercopilot.service.ProjectContextAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;

import java.nio.file.Path;
import java.nio.file.Paths;
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


    @Override
    public CompletableFuture<ToolResult> execute(SmartEditParams params) {

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