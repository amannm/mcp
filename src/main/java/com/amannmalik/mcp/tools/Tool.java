package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

public record Tool(String name,
                   String title,
                   String description,
                   JsonObject inputSchema,
                   JsonObject outputSchema,
                   ToolAnnotations annotations,
                   JsonObject _meta) implements DisplayNameProvider {
    public static final JsonCodec<Tool> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(Tool tool) {
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("name", tool.name())
                    .add("inputSchema", tool.inputSchema());
            if (tool.title() != null) b.add("title", tool.title());
            if (tool.description() != null) b.add("description", tool.description());
            if (tool.outputSchema() != null) b.add("outputSchema", tool.outputSchema());
            if (tool.annotations() != null) b.add("annotations", ToolAnnotations.CODEC.toJson(tool.annotations()));
            if (tool._meta() != null) b.add("_meta", tool._meta());
            return b.build();
        }

        @Override
        public Tool fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            String name = obj.getString("name", null);
            if (name == null) throw new IllegalArgumentException("name required");
            String title = obj.getString("title", null);
            String description = obj.getString("description", null);
            JsonObject inputSchema = obj.getJsonObject("inputSchema");
            if (inputSchema == null) throw new IllegalArgumentException("inputSchema required");
            JsonObject outputSchema = obj.getJsonObject("outputSchema");
            ToolAnnotations ann = obj.containsKey("annotations") ?
                    ToolAnnotations.CODEC.fromJson(obj.getJsonObject("annotations")) : null;
            JsonObject meta = obj.getJsonObject("_meta");
            return new Tool(name, title, description, inputSchema, outputSchema, ann, meta);
        }
    };

    public Tool {
        name = InputSanitizer.requireClean(name);
        if (inputSchema == null) {
            throw new IllegalArgumentException("inputSchema is required");
        }
        title = InputSanitizer.cleanNullable(title);
        description = InputSanitizer.cleanNullable(description);
        annotations = annotations == null || (
                annotations.title() == null &&
                        annotations.readOnlyHint() == null &&
                        annotations.destructiveHint() == null &&
                        annotations.idempotentHint() == null &&
                        annotations.openWorldHint() == null
        ) ? null : annotations;
        MetaValidator.requireValid(_meta);
    }

    @Override
    public String displayName() {
        if (title != null) return title;
        if (annotations != null && annotations.title() != null) return annotations.title();
        return name;
    }
}
