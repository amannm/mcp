package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.Tool;
import com.amannmalik.mcp.api.ToolAnnotations;
import jakarta.json.*;

import java.util.Set;

public final class ToolAbstractEntityCodec extends AbstractEntityCodec<Tool> {

    static final JsonCodec<ToolAnnotations> TOOL_ANNOTATIONS_JSON_CODEC = new ToolAnnotationsAbstractEntityCodec();

    @Override
    public JsonObject toJson(Tool tool) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("name", tool.name())
                .add("inputSchema", tool.inputSchema());
        if (tool.title() != null) b.add("title", tool.title());
        if (tool.description() != null) b.add("description", tool.description());
        if (tool.outputSchema() != null) b.add("outputSchema", tool.outputSchema());
        if (tool.annotations() != null) b.add("annotations", TOOL_ANNOTATIONS_JSON_CODEC.toJson(tool.annotations()));
        if (tool._meta() != null) b.add("_meta", tool._meta());
        return b.build();
    }

    @Override
    public Tool fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("name", "title", "description", "inputSchema", "outputSchema", "annotations", "_meta"));
        String name = requireString(obj, "name");
        JsonObject inputSchema = getObject(obj, "inputSchema");
        if (inputSchema == null) throw new IllegalArgumentException("inputSchema required");
        String title = obj.getString("title", null);
        String description = obj.getString("description", null);
        JsonObject outputSchema = obj.getJsonObject("outputSchema");
        ToolAnnotations ann = obj.containsKey("annotations") ?
                TOOL_ANNOTATIONS_JSON_CODEC.fromJson(getObject(obj, "annotations")) : null;
        JsonObject meta = obj.getJsonObject("_meta");
        return new Tool(name, title, description, inputSchema, outputSchema, ann, meta);
    }
}
