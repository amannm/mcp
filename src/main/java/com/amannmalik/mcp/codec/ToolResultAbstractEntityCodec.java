package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.ToolResult;
import jakarta.json.*;

import java.util.Set;

public final class ToolResultAbstractEntityCodec extends AbstractEntityCodec<ToolResult> {
    @Override
    public JsonObject toJson(ToolResult r) {
        var b = Json.createObjectBuilder()
                .add("content", r.content());
        if (r.isError()) b.add("isError", true);
        if (r.structuredContent() != null) b.add("structuredContent", r.structuredContent());
        if (r._meta() != null) b.add("_meta", r._meta());
        return b.build();
    }

    @Override
    public ToolResult fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("content", "structuredContent", "isError", "_meta"));
        var content = obj.getJsonArray("content");
        if (content == null) throw new IllegalArgumentException("content required");
        var structured = obj.getJsonObject("structuredContent");
        var isError = obj.getBoolean("isError", false);
        var meta = obj.getJsonObject("_meta");
        return new ToolResult(content, structured, isError, meta);
    }
}
