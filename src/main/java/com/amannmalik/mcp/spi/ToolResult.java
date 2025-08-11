package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.codec.ContentBlockJsonCodec;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

public record ToolResult(JsonArray content,
                         JsonObject structuredContent,
                         Boolean isError,
                         JsonObject _meta) {

    private static final JsonCodec<ContentBlock> CONTENT_BLOCK_CODEC = new ContentBlockJsonCodec();

    public ToolResult {
        content = sanitize(content == null ? JsonValue.EMPTY_JSON_ARRAY : content);
        if (isError == null) isError = Boolean.FALSE;
        ValidationUtil.requireMeta(_meta);
    }

    private static JsonArray sanitize(JsonArray arr) {
        JsonArrayBuilder b = Json.createArrayBuilder();
        for (JsonValue v : arr) {
            if (v.getValueType() == JsonValue.ValueType.OBJECT) {
                try {
                    ContentBlock c = CONTENT_BLOCK_CODEC.fromJson(v.asJsonObject());
                    b.add(CONTENT_BLOCK_CODEC.toJson(c));
                    continue;
                } catch (IllegalArgumentException ignore) {
                    // TODO: log
                }
            }
            b.add(v);
        }
        return b.build();
    }
}
