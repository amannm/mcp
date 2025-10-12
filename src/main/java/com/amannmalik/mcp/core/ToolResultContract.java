package com.amannmalik.mcp.core;

import com.amannmalik.mcp.codec.ContentBlockJsonCodec;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.spi.ContentBlock;
import com.amannmalik.mcp.spi.ToolResult;
import com.amannmalik.mcp.util.PlatformLog;
import jakarta.json.*;

import java.lang.System.Logger;

public final class ToolResultContract {
    private static final JsonCodec<ContentBlock> CONTENT_BLOCK_CODEC = new ContentBlockJsonCodec();
    private static final Logger LOG = PlatformLog.get(ToolResult.class);

    private ToolResultContract() {
    }

    public static JsonArray sanitizeContent(JsonArray content) {
        var source = content == null ? JsonValue.EMPTY_JSON_ARRAY : content;
        var builder = Json.createArrayBuilder();
        for (var value : source) {
            var block = decode(value);
            builder.add(block == null ? value : CONTENT_BLOCK_CODEC.toJson(block));
        }
        return builder.build();
    }

    public static Boolean normalizeErrorFlag(Boolean isError) {
        return isError == null ? Boolean.FALSE : isError;
    }

    public static void requireMeta(JsonObject meta) {
        SpiPreconditions.requireMeta(meta);
    }

    private static ContentBlock decode(JsonValue value) {
        if (!(value instanceof JsonObject obj)) {
            return null;
        }
        try {
            return CONTENT_BLOCK_CODEC.fromJson(obj);
        } catch (IllegalArgumentException e) {
            LOG.log(Logger.Level.DEBUG, () -> "invalid content block: " + obj);
            return null;
        }
    }
}
