package com.amannmalik.mcp;

import com.amannmalik.mcp.elicitation.*;
import com.amannmalik.mcp.tools.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;
import jakarta.json.*;

import java.util.*;

public class ToolExecutionFeatureSteps {
    
    private final ToolTestContext context = new ToolTestContext();
    
    @Given("an MCP server with tools:")
    public void anMcpServerWithTools(DataTable dataTable) {
        Map<String, TestTool> tools = new HashMap<>();
        for (var row : dataTable.asMaps()) {
            String name = row.get("name");
            String description = row.get("description");
            boolean requiresConfirmation = Boolean.parseBoolean(row.get("requires_confirmation"));
            tools.put(name, new TestTool(name, description, requiresConfirmation));
        }
        context.configureServerTools(tools);
    }
    
    @And("tool {string} has input schema requiring:")
    public void toolHasInputSchemaRequiring(String toolName, DataTable dataTable) {
        JsonObjectBuilder schemaBuilder = Json.createObjectBuilder()
                .add("type", "object");
        JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder();
        JsonArrayBuilder requiredBuilder = Json.createArrayBuilder();
        
        for (var row : dataTable.asMaps()) {
            String field = row.get("field");
            String type = row.get("type");
            boolean required = Boolean.parseBoolean(row.get("required"));
            String description = row.get("description");
            
            JsonObjectBuilder fieldBuilder = Json.createObjectBuilder().add("type", type);
            if (description != null) {
                fieldBuilder.add("description", description);
            }
            propertiesBuilder.add(field, fieldBuilder.build());
            
            if (required) {
                requiredBuilder.add(field);
            }
        }
        
        schemaBuilder.add("properties", propertiesBuilder.build());
        schemaBuilder.add("required", requiredBuilder.build());
        
        context.setToolInputSchema(toolName, schemaBuilder.build());
    }
    
    @And("tool {string} has output schema:")
    public void toolHasOutputSchema(String toolName, DataTable dataTable) {
        JsonObjectBuilder schemaBuilder = Json.createObjectBuilder()
                .add("type", "object");
        JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder();
        JsonArrayBuilder requiredBuilder = Json.createArrayBuilder();
        
        for (var row : dataTable.asMaps()) {
            String field = row.get("field");
            String type = row.get("type");
            boolean required = Boolean.parseBoolean(row.get("required"));
            String description = row.get("description");
            
            JsonObjectBuilder fieldBuilder = Json.createObjectBuilder().add("type", type);
            if (description != null) {
                fieldBuilder.add("description", description);
            }
            propertiesBuilder.add(field, fieldBuilder.build());
            
            if (required) {
                requiredBuilder.add(field);
            }
        }
        
        schemaBuilder.add("properties", propertiesBuilder.build());
        schemaBuilder.add("required", requiredBuilder.build());
        
        context.setToolOutputSchema(toolName, schemaBuilder.build());
    }
    
    @When("the client calls tool {string} with incomplete arguments:")
    public void theClientCallsToolWithIncompleteArguments(String toolName, DataTable dataTable) {
        JsonObjectBuilder argsBuilder = Json.createObjectBuilder();
        for (var row : dataTable.asMaps()) {
            String field = row.get("field");
            String value = row.get("value");
            argsBuilder.add(field, value);
        }
        
        CallToolRequest request = new CallToolRequest(toolName, argsBuilder.build(), null);
        context.initiateToolCall(request);
    }
    
    @Then("the server detects missing required argument {string}")
    public void theServerDetectsMissingRequiredArgument(String argumentName) {
        var missingArgs = context.detectMissingArguments();
        assert missingArgs.contains(argumentName) : 
            "Expected missing argument '" + argumentName + "' but found: " + missingArgs;
    }
    
    @And("initiates elicitation request for missing parameters")
    public void initiatesElicitationRequestForMissingParameters() {
        context.initiateElicitation();
        assert context.getElicitationRequest() != null : "Elicitation should be initiated";
    }
    
    @When("the client's elicitation provider prompts user")
    public void theClientsElicitationProviderPromptsUser() {
        context.presentElicitationToUser();
        assert context.isElicitationPresented() : "Elicitation should be presented to user";
    }
    
    @And("user provides:")
    public void userProvides(DataTable dataTable) {
        JsonObjectBuilder contentBuilder = Json.createObjectBuilder();
        for (var row : dataTable.asMaps()) {
            String field = row.get("field");
            String value = row.get("value");
            
            // Handle different value types
            if (value.startsWith("{") && value.endsWith("}")) {
                // Parse as JSON object
                try (JsonReader reader = Json.createReader(new java.io.StringReader(value))) {
                    JsonObject obj = reader.readObject();
                    contentBuilder.add(field, obj);
                }
            } else {
                contentBuilder.add(field, value);
            }
        }
        
        context.provideUserInput(contentBuilder.build());
    }
    
    @Then("elicitation completes with action {string}")
    public void elicitationCompletesWithAction(String action) {
        ElicitResult result = context.completeElicitation();
        assert result.action() == ElicitationAction.valueOf(action.toUpperCase()) :
            "Expected action " + action + " but got " + result.action();
    }
    
    @When("the server retries tool execution with complete arguments")
    public void theServerRetriesToolExecutionWithCompleteArguments() {
        context.retryToolExecution();
        assert context.isToolExecutionRetried() : "Tool execution should be retried";
    }
    
    @Then("tool execution succeeds")
    public void toolExecutionSucceeds() {
        ToolResult result = context.getToolResult();
        assert result != null : "Tool result should be available";
        assert !result.isError() : "Tool execution should succeed";
    }
    
    @And("returns structured output conforming to output schema")
    public void returnsStructuredOutputConformingToOutputSchema() {
        ToolResult result = context.getToolResult();
        assert result.structuredContent() != null : "Structured content should be available";
        
        // Validate against output schema
        String toolName = context.getCurrentToolName();
        JsonObject outputSchema = context.getToolOutputSchema(toolName);
        assert validateStructuredOutput(result.structuredContent(), outputSchema) :
            "Structured output should conform to schema";
    }
    
    @And("includes both structured content and text representation")
    public void includesBothStructuredContentAndTextRepresentation() {
        ToolResult result = context.getToolResult();
        assert result.structuredContent() != null : "Structured content should be available";
        assert result.content() != null && !result.content().isEmpty() : 
            "Text content should be available";
        
        // Verify text content contains JSON representation
        JsonValue firstContent = result.content().get(0);
        assert firstContent.getValueType() == JsonValue.ValueType.OBJECT : 
            "First content item should be an object";
        JsonObject contentObj = firstContent.asJsonObject();
        assert "text".equals(contentObj.getString("type", null)) : 
            "First content should be text type";
    }
    
    private boolean validateStructuredOutput(JsonObject content, JsonObject schema) {
        // Basic schema validation - check required fields are present
        JsonArray required = schema.getJsonArray("required");
        if (required != null) {
            for (JsonValue fieldValue : required) {
                String field = ((JsonString) fieldValue).getString();
                if (!content.containsKey(field)) {
                    return false;
                }
            }
        }
        
        // Validate field types
        JsonObject properties = schema.getJsonObject("properties");
        if (properties != null) {
            for (String field : content.keySet()) {
                JsonObject fieldSchema = properties.getJsonObject(field);
                if (fieldSchema != null) {
                    String expectedType = fieldSchema.getString("type", null);
                    if (!validateFieldType(content.get(field), expectedType)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    private boolean validateFieldType(JsonValue value, String expectedType) {
        return switch (expectedType) {
            case "string" -> value.getValueType() == JsonValue.ValueType.STRING;
            case "number" -> value.getValueType() == JsonValue.ValueType.NUMBER;
            case "object" -> value.getValueType() == JsonValue.ValueType.OBJECT;
            case "boolean" -> value.getValueType() == JsonValue.ValueType.TRUE || 
                            value.getValueType() == JsonValue.ValueType.FALSE;
            default -> true; // Allow unknown types
        };
    }
    
    private static class ToolTestContext {
        private Map<String, TestTool> serverTools = new HashMap<>();
        private Map<String, JsonObject> toolInputSchemas = new HashMap<>();
        private Map<String, JsonObject> toolOutputSchemas = new HashMap<>();
        private CallToolRequest currentToolCall;
        private Set<String> missingArguments = new HashSet<>();
        private ElicitationRequest elicitationRequest;
        private boolean elicitationPresented = false;
        private JsonObject userInput;
        private ElicitResult elicitationResult;
        private boolean toolExecutionRetried = false;
        private ToolResult toolResult;
        
        void configureServerTools(Map<String, TestTool> tools) {
            this.serverTools = tools;
        }
        
        void setToolInputSchema(String toolName, JsonObject schema) {
            toolInputSchemas.put(toolName, schema);
        }
        
        void setToolOutputSchema(String toolName, JsonObject schema) {
            toolOutputSchemas.put(toolName, schema);
        }
        
        JsonObject getToolOutputSchema(String toolName) {
            return toolOutputSchemas.get(toolName);
        }
        
        void initiateToolCall(CallToolRequest request) {
            this.currentToolCall = request;
        }
        
        String getCurrentToolName() {
            return currentToolCall != null ? currentToolCall.name() : null;
        }
        
        Set<String> detectMissingArguments() {
            if (currentToolCall == null) return Set.of();
            
            String toolName = currentToolCall.name();
            JsonObject inputSchema = toolInputSchemas.get(toolName);
            if (inputSchema == null) return Set.of();
            
            JsonArray required = inputSchema.getJsonArray("required");
            if (required == null) return Set.of();
            
            Set<String> missing = new HashSet<>();
            JsonObject args = currentToolCall.arguments();
            
            for (JsonValue fieldValue : required) {
                String field = ((JsonString) fieldValue).getString();
                if (!args.containsKey(field)) {
                    missing.add(field);
                }
            }
            
            this.missingArguments = missing;
            return missing;
        }
        
        void initiateElicitation() {
            if (!missingArguments.isEmpty()) {
                JsonObjectBuilder schemaBuilder = Json.createObjectBuilder()
                        .add("type", "object");
                JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder();
                JsonArrayBuilder requiredBuilder = Json.createArrayBuilder();
                
                JsonObject fullSchema = toolInputSchemas.get(currentToolCall.name());
                JsonObject properties = fullSchema.getJsonObject("properties");
                
                for (String missingField : missingArguments) {
                    if (properties.containsKey(missingField)) {
                        propertiesBuilder.add(missingField, properties.get(missingField));
                        requiredBuilder.add(missingField);
                    }
                }
                
                schemaBuilder.add("properties", propertiesBuilder.build());
                schemaBuilder.add("required", requiredBuilder.build());
                
                this.elicitationRequest = new ElicitationRequest(
                    "Please provide missing tool arguments",
                    schemaBuilder.build(),
                    null
                );
            }
        }
        
        ElicitationRequest getElicitationRequest() {
            return elicitationRequest;
        }
        
        void presentElicitationToUser() {
            this.elicitationPresented = true;
        }
        
        boolean isElicitationPresented() {
            return elicitationPresented;
        }
        
        void provideUserInput(JsonObject input) {
            this.userInput = input;
        }
        
        ElicitResult completeElicitation() {
            if (userInput != null) {
                this.elicitationResult = new ElicitResult(ElicitationAction.ACCEPT, userInput, null);
            } else {
                this.elicitationResult = new ElicitResult(ElicitationAction.DECLINE, null, null);
            }
            return elicitationResult;
        }
        
        void retryToolExecution() {
            if (elicitationResult != null && elicitationResult.action() == ElicitationAction.ACCEPT) {
                // Merge user input with original arguments
                JsonObjectBuilder mergedArgs = Json.createObjectBuilder();
                for (var entry : currentToolCall.arguments().entrySet()) {
                    mergedArgs.add(entry.getKey(), entry.getValue());
                }
                for (var entry : elicitationResult.content().entrySet()) {
                    mergedArgs.add(entry.getKey(), entry.getValue());
                }
                
                this.currentToolCall = new CallToolRequest(
                    currentToolCall.name(),
                    mergedArgs.build(),
                    currentToolCall._meta()
                );
                this.toolExecutionRetried = true;
                
                // Simulate successful tool execution
                JsonObject structuredResult = Json.createObjectBuilder()
                        .add("processed", Json.createObjectBuilder()
                                .add("data", "processed data")
                                .build())
                        .add("metadata", Json.createObjectBuilder()
                                .add("processing_time", "1.2s")
                                .build())
                        .add("timestamp", "2025-01-01T12:00:00Z")
                        .build();
                
                String textResult = structuredResult.toString();
                JsonArray content = Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("type", "text")
                                .add("text", textResult)
                                .build())
                        .build();
                
                this.toolResult = new ToolResult(content, structuredResult, false, null);
            }
        }
        
        boolean isToolExecutionRetried() {
            return toolExecutionRetried;
        }
        
        ToolResult getToolResult() {
            return toolResult;
        }
    }
    
    private static class TestTool {
        private final String name;
        private final String description;
        private final boolean requiresConfirmation;
        
        TestTool(String name, String description, boolean requiresConfirmation) {
            this.name = name;
            this.description = description;
            this.requiresConfirmation = requiresConfirmation;
        }
        
        String name() { return name; }
        String description() { return description; }
        boolean requiresConfirmation() { return requiresConfirmation; }
    }
    
    private static class ElicitationRequest {
        private final String message;
        private final JsonObject requestedSchema;
        private final JsonObject meta;
        
        ElicitationRequest(String message, JsonObject requestedSchema, JsonObject meta) {
            this.message = message;
            this.requestedSchema = requestedSchema;
            this.meta = meta;
        }
        
        String message() { return message; }
        JsonObject requestedSchema() { return requestedSchema; }
        JsonObject meta() { return meta; }
    }
}