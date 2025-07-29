package com.amannmalik.mcp.server.tools;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import com.amannmalik.mcp.validation.InputSanitizer;


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
                if ("text".equals(o.getString("type", null)) && o.containsKey("text")) {
                    b.add(Json.createObjectBuilder(o)
                            .add("text", InputSanitizer.requireClean(o.getString("text")))
                            .build());
                    continue;
                }
            }
            b.add(v);
        }
        return b.build();
    }
}
