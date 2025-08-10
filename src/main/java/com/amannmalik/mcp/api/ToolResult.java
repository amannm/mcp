package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ContentBlockJsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.Set;

public record ToolResult(JsonArray content,
                         JsonObject structuredContent,
                         Boolean isError,
                         JsonObject _meta) {

    private static final JsonCodec<ContentBlock> CONTENT_BLOCK_CODEC = new ContentBlockJsonCodec();

    static final JsonCodec<ToolResult> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(ToolResult r) {
            JsonObjectBuilder b = Json.createObjectBuilder()
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
            JsonArray content = obj.getJsonArray("content");
            if (content == null) throw new IllegalArgumentException("content required");
            JsonObject structured = obj.getJsonObject("structuredContent");
            boolean isError = obj.getBoolean("isError", false);
            JsonObject meta = obj.getJsonObject("_meta");
            return new ToolResult(content, structured, isError, meta);
        }
    };

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
                }
            }
            b.add(v);
        }
        return b.build();
    }

    @Override
    public String toString() {
        return CODEC.toJson(this).toString();
    }

}
