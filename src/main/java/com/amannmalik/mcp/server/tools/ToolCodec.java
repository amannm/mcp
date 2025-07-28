package com.amannmalik.mcp.server.tools;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/** JSON utilities for tool messages. */
public final class ToolCodec {
    private ToolCodec() {}

    public static JsonObject toJsonObject(Tool tool) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("name", tool.name());
        if (tool.title() != null) builder.add("title", tool.title());
        if (tool.description() != null) builder.add("description", tool.description());
        builder.add("inputSchema", tool.inputSchema());
        if (tool.outputSchema() != null) builder.add("outputSchema", tool.outputSchema());
        if (tool.annotations() != null) builder.add("annotations", toJsonObject(tool.annotations()));
        return builder.build();
    }

    public static JsonObject toJsonObject(ToolPage page) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        page.tools().forEach(t -> arr.add(toJsonObject(t)));
        JsonObjectBuilder builder = Json.createObjectBuilder().add("tools", arr);
        if (page.nextCursor() != null) builder.add("nextCursor", page.nextCursor());
        return builder.build();
    }

    public static JsonObject toJsonObject(ToolResult result) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("content", result.content())
                .add("isError", result.isError());
        if (result.structuredContent() != null) builder.add("structuredContent", result.structuredContent());
        return builder.build();
    }

    private static JsonObject toJsonObject(ToolAnnotations ann) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (!ann.audience().isEmpty()) {
            var arr = Json.createArrayBuilder();
            ann.audience().forEach(a -> arr.add(a.name().toLowerCase()));
            b.add("audience", arr);
        }
        if (ann.priority() != null) b.add("priority", ann.priority());
        if (ann.lastModified() != null) b.add("lastModified", ann.lastModified().toString());
        return b.build();
    }
}
