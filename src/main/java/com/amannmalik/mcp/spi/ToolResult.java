package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.codec.ContentBlockJsonCodec;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.util.PlatformLog;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.lang.System.Logger;

public record ToolResult(JsonArray content,
                         JsonObject structuredContent,
                         Boolean isError,
                         JsonObject _meta) implements Result {

    private static final JsonCodec<ContentBlock> CONTENT_BLOCK_CODEC = new ContentBlockJsonCodec();
    private static final Logger LOG = PlatformLog.get(ToolResult.class);

    public ToolResult {
        content = sanitize(content == null ? JsonValue.EMPTY_JSON_ARRAY : content);
        if (isError == null) {
            isError = Boolean.FALSE;
        }
        ValidationUtil.requireMeta(_meta);
    }

    private static JsonArray sanitize(JsonArray arr) {
        var b = Json.createArrayBuilder();
        for (var v : arr) {
            var block = decode(v);
            b.add(block == null ? v : CONTENT_BLOCK_CODEC.toJson(block));
        }
        return b.build();
    }

    private static ContentBlock decode(JsonValue v) {
        if (v.getValueType() != JsonValue.ValueType.OBJECT) {
            return null;
        }
        try {
            return CONTENT_BLOCK_CODEC.fromJson(v.asJsonObject());
        } catch (IllegalArgumentException e) {
            LOG.log(Logger.Level.DEBUG, () -> "invalid content block: " + v);
            return null;
        }
    }
}
