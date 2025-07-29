package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.server.resources.Resource;
import com.amannmalik.mcp.server.resources.ResourceBlock;
import com.amannmalik.mcp.server.resources.ResourcesCodec;
import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;


public record ToolResult(JsonArray content,
                         JsonObject structuredContent,
                         boolean isError) {
    public ToolResult {
        content = sanitize(content == null ? JsonValue.EMPTY_JSON_ARRAY : content);
    }

    private static JsonArray sanitize(JsonArray arr) {
        JsonArrayBuilder b = Json.createArrayBuilder();
        for (JsonValue v : arr) {
            if (v.getValueType() == JsonValue.ValueType.OBJECT) {
                JsonObject o = v.asJsonObject();
                String type = o.getString("type", null);
                if ("text".equals(type) && o.containsKey("text")) {
                    b.add(Json.createObjectBuilder(o)
                            .add("text", InputSanitizer.requireClean(o.getString("text")))
                            .build());
                    continue;
                }
                if ("resource_link".equals(type)) {
                    b.add(toResourceLink(o));
                    continue;
                }
                if ("resource".equals(type) && o.containsKey("resource")) {
                    b.add(toEmbeddedResource(o));
                    continue;
                }
            }
            b.add(v);
        }
        return b.build();
    }

    private static JsonObject toResourceLink(JsonObject obj) {
        Resource r = ResourcesCodec.toResource(obj);
        JsonObject base = ResourcesCodec.toJsonObject(r);
        JsonObjectBuilder result = Json.createObjectBuilder(base)
                .add("type", "resource_link");
        if (obj.containsKey("_meta")) result.add("_meta", obj.getJsonObject("_meta"));
        return result.build();
    }

    private static JsonObject toEmbeddedResource(JsonObject obj) {
        JsonObject resourceObj = obj.getJsonObject("resource");
        if (resourceObj == null) throw new IllegalArgumentException("resource required");
        ResourceBlock block = ResourcesCodec.toResourceBlock(resourceObj);
        JsonObjectBuilder result = Json.createObjectBuilder()
                .add("type", "resource")
                .add("resource", ResourcesCodec.toJsonObject(block));
        if (obj.containsKey("annotations")) result.add("annotations", obj.getJsonObject("annotations"));
        if (obj.containsKey("_meta")) result.add("_meta", obj.getJsonObject("_meta"));
        return result.build();
    }
}
