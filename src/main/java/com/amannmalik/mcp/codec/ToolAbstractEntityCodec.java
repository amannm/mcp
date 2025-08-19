package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.Tool;
import com.amannmalik.mcp.spi.ToolAnnotations;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Set;

public final class ToolAbstractEntityCodec extends AbstractEntityCodec<Tool> {

    private static final JsonCodec<ToolAnnotations> TOOL_ANNOTATIONS_JSON_CODEC = new ToolAnnotationsAbstractEntityCodec();

    @Override
    public JsonObject toJson(Tool tool) {
        var b = Json.createObjectBuilder()
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
        var name = requireString(obj, "name");
        var inputSchema = getObject(obj, "inputSchema");
        if (inputSchema == null) throw new IllegalArgumentException("inputSchema required");
        var title = obj.getString("title", null);
        var description = obj.getString("description", null);
        var outputSchema = obj.getJsonObject("outputSchema");
        var ann = obj.containsKey("annotations") ?
                TOOL_ANNOTATIONS_JSON_CODEC.fromJson(getObject(obj, "annotations")) : null;
        var meta = obj.getJsonObject("_meta");
        return new Tool(name, title, description, inputSchema, outputSchema, ann, meta);
    }
}
