package org.kolar.kolarcodercopilot.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JSON schema validator
 * 用于根据定义的schema验证工具参数
 */
@Component
public class SchemaValidator {

    private static final Logger logger = LoggerFactory.getLogger(SchemaValidator.class);

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory schemaFactory;

    public SchemaValidator() {
        this.objectMapper = new ObjectMapper();
        this.schemaFactory = com.networknt.schema.JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    /**
     *  validate JSON against schema
     */
    public String validate(JsonSchema schema, Object data) {
        try{
            //  将自定义JsonSchema对象序列化为标准JSONSchema字符串
            String schemaJson = objectMapper.writeValueAsString(schema);
            logger.debug("Schema JSON: {}", schemaJson);

            // 创建Json Schema验证器
            com.networknt.schema.JsonSchema jsonSchema = schemaFactory.getSchema(schemaJson);

            // 数据转换
            String dataJson = objectMapper.writeValueAsString(data);
            JsonNode dataNode = objectMapper.readTree(dataJson);
            logger.debug("Data JSON: {}", dataJson);

            // 数据验证 调用第三方库进行验证
            Set<ValidationMessage> errors = jsonSchema.validate(dataNode);
            if(errors.isEmpty()) {
                logger.debug("Schema validated passed");
                return null;
            }else{
                // 错误聚合: 使用stream API对错误结果进行聚合
                 String errorStream = errors.stream()
                        .map(ValidationMessage::getMessage).
                        collect(Collectors.joining(";"));
                logger.warn("Schema validation failed: {}", errorStream);
                 return errorStream;
            }
        }catch (Exception e){
            String errorStream = "schema validation err: " + e.getMessage();
            logger.error(errorStream, e);
            return errorStream;
        }
    }

    /**
     * simple schema validator
     * used when schema validator failed
     * @param schema
     * @param data
     * @return
     */
    public String validateSimple(JsonSchema schema, Object data) {
       if(schema == null || data == null){
           logger.debug("Schema or Data object is null");
           return null;
       }

       // 基础类型检查
        String expectedType = schema.getType();
       if(expectedType != null){
           String actualType = getDataType(data);
           if(!isCompatible(expectedType, actualType)){
              return String.format("Expected type %s but got %s", expectedType, actualType);
           }
       }

       // 必填字段检查 (only for Object type)  只有对象类型才需要检查必填字段
        if("object".equals(expectedType) && schema.getRequiredFields() != null){
            if(!(data instanceof Map<?,?>)){
                return "Expected object type for required field validation";
            }

            Map<String, Object> dataMap = (Map<String, Object>)data;

            for(String requiredFiled : schema.getRequiredFields()){
                if(!dataMap.containsKey(requiredFiled) || dataMap.get(requiredFiled) == null){
                    return "Missing Required field " + requiredFiled;
                }
            }
        }
        return null; // validation passed
    }

    public boolean isCompatible(String expectedType, String actualType) {
        if(actualType.equals(expectedType)){
            return true;
        }

        // Number type compatibility
        if("number".equals(expectedType) && "integer".equals(actualType)){
            return true;
        }
        return false;
    }

    public String getDataType(Object data) {
        if(data == null){return "null";}
        if(data instanceof String){return "string";}
        if(data instanceof Integer || data instanceof Long){return "integer";}
        if(data instanceof Boolean){return "boolean";}
        if(data instanceof Number){return "number";}
        if(data instanceof Map<?,?>){return "object";}
        if(data instanceof List<?>){return "array";}
        return "unknown";
    }
}
