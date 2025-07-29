package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.util.PaginatedResult;
import com.amannmalik.mcp.util.PaginationCodec;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

public final class ToolCodec {
    private ToolCodec() {
    }

    public static JsonObject toJsonObject(Tool tool) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("name", tool.name());
        if (tool.title() != null) builder.add("title", tool.title());
        if (tool.description() != null) builder.add("description", tool.description());
        builder.add("inputSchema", tool.inputSchema());
        if (tool.outputSchema() != null) builder.add("outputSchema", tool.outputSchema());
        if (tool.annotations() != null) builder.add("annotations", toJsonObject(tool.annotations()));
        if (tool._meta() != null) builder.add("_meta", tool._meta());
        return builder.build();
    }

    public static JsonObject toJsonObject(ToolPage page) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        page.tools().forEach(t -> arr.add(toJsonObject(t)));
        JsonObjectBuilder builder = Json.createObjectBuilder().add("tools", arr);
        PaginationCodec.toJsonObject(new PaginatedResult(page.nextCursor())).forEach(builder::add);
        return builder.build();
    }

    public static JsonObject toJsonObject(ToolResult result) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("content", result.content());
        if (result.isError()) builder.add("isError", true);
        if (result.structuredContent() != null) {
            builder.add("structuredContent", result.structuredContent());
        }
        return builder.build();
    }

    public static JsonObject toJsonObject(ToolListChangedNotification n) {
        if (n == null) throw new IllegalArgumentException("notification required");
        return Json.createObjectBuilder().build();
    }

    public static ToolListChangedNotification toToolListChangedNotification(JsonObject obj) {
        if (obj != null && !obj.isEmpty()) {
            throw new IllegalArgumentException("unexpected fields");
        }
        return new ToolListChangedNotification();
    }

    private static JsonObject toJsonObject(ToolAnnotations ann) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (ann.title() != null) b.add("title", ann.title());
        if (ann.readOnlyHint() != null) b.add("readOnlyHint", ann.readOnlyHint());
        if (ann.destructiveHint() != null) b.add("destructiveHint", ann.destructiveHint());
        if (ann.idempotentHint() != null) b.add("idempotentHint", ann.idempotentHint());
        if (ann.openWorldHint() != null) b.add("openWorldHint", ann.openWorldHint());
        return b.build();
    }
}
