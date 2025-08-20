package org.kolar.kolarcodercopilot.tools;


import org.kolar.kolarcodercopilot.ToolResult;
import org.kolar.kolarcodercopilot.schema.JsonSchema;
import org.kolar.kolarcodercopilot.schema.SchemaValidator;
import org.kolar.kolarcodercopilot.service.ToolExecutionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Base abstract class for tools
 * All tools should inherit from this class
 */
public abstract class BaseTool<P> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String name;
    protected final String displayname;
    protected final String description;
    protected final JsonSchema parameterSchema;
    protected final boolean isOutPutMarkdown;
    protected final boolean canUpdateOutput;

    protected SchemaValidator schemaValidator;



    public BaseTool(String name, String displayname, String description, JsonSchema parameterSchema) {
       this(name, displayname, description, parameterSchema, true, false);
   }

   public BaseTool(String name, String displayname, String description, JsonSchema parameterSchema, boolean canUpdateOutput, boolean isOutPutMarkdown) {
       this.name = name;
       this.displayname = displayname;
       this.description = description;
       this.parameterSchema = parameterSchema;
       this.isOutPutMarkdown = isOutPutMarkdown;
       this.canUpdateOutput = canUpdateOutput;
   }

    /**
     * set validator schema (through DI)
     * @param schemaValidator
     */
    public void setSchemaValidator(SchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator;
    }

    /**
     * validator tool parameters
     * @param params
     * @return
     */
    public String ValidatorToolParams(P params){
        if(parameterSchema == null || schemaValidator == null){
            logger.warn("schema validator or schema validator is null, skipping validator");
            return null;
        }
        try{
            return schemaValidator.validator(parameterSchema, params);
        }catch (Exception e){
            logger.error("Fatal validate err:", e);
            return "Param validation failed: " + e.getMessage();
        }
    }


    /**
     * Execute tool
     * @param params
     * @return execute result
     */
    public abstract CompletableFuture<ToolResult> execute(P params);

    /**
     * Get tools description (for AI understanding)
     * @Param params
     * @return tool description
     */
     public String getDescription(P params) {
         return description;
     }

    public Logger getLogger() {
        return logger;
    }

    public String getName() {
        return name;
    }

    public String getDisplayname() {
        return displayname;
    }

    public String getDescription() {
        return description;
    }

    public JsonSchema getParameterSchema() {
        return parameterSchema;
    }

    public boolean isOutPutMarkdown() {
        return isOutPutMarkdown;
    }

    public boolean isCanUpdateOutput() {
        return canUpdateOutput;
    }

    public SchemaValidator getSchemaValidator() {
        return schemaValidator;
    }





}
