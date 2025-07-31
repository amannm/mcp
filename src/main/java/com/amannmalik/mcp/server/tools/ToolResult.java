package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.content.ContentCodec;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public record ToolResult(JsonArray content,
                         JsonObject structuredContent,
                         Boolean isError,
                         JsonObject _meta) {
    public ToolResult {
        content = sanitize(content == null ? JsonValue.EMPTY_JSON_ARRAY : content);
        if (isError == null) isError = Boolean.FALSE;
        MetaValidator.requireValid(_meta);
    }

    private static JsonArray sanitize(JsonArray arr) {
        JsonArrayBuilder b = Json.createArrayBuilder();
        for (JsonValue v : arr) {
            if (v.getValueType() == JsonValue.ValueType.OBJECT) {
                try {
                    ContentBlock c = ContentCodec.toContentBlock(v.asJsonObject());
                    b.add(ContentCodec.toJsonObject(c));
                    continue;
                } catch (IllegalArgumentException ignore) {
                }
            }
            b.add(v);
        }
        return b.build();
    }

}
