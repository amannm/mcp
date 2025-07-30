package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.util.PaginatedRequest;
import com.amannmalik.mcp.util.PaginatedResult;
import com.amannmalik.mcp.util.PaginationCodec;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import java.util.ArrayList;
import java.util.List;

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

    public static JsonObject toJsonObject(ListToolsRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        return PaginationCodec.toJsonObject(new PaginatedRequest(req.cursor()));
    }

    public static JsonObject toJsonObject(ListToolsResult page) {
        return toJsonObject(new ToolPage(page.tools(), page.nextCursor()));
    }

    public static JsonObject toJsonObject(ToolResult result) {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("content", result.content());
        if (result.isError()) builder.add("isError", true);
        if (result.structuredContent() != null) {
            builder.add("structuredContent", result.structuredContent());
        }
        if (result._meta() != null) builder.add("_meta", result._meta());
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

    public static Tool toTool(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String name = obj.getString("name", null);
        if (name == null) throw new IllegalArgumentException("name required");
        String title = obj.getString("title", null);
        String description = obj.getString("description", null);
        JsonObject inputSchema = obj.getJsonObject("inputSchema");
        if (inputSchema == null) throw new IllegalArgumentException("inputSchema required");
        JsonObject outputSchema = obj.getJsonObject("outputSchema");
        ToolAnnotations ann = obj.containsKey("annotations") ? toToolAnnotations(obj.getJsonObject("annotations")) : null;
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        return new Tool(name, title, description, inputSchema, outputSchema, ann, meta);
    }

    public static ToolPage toToolPage(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        JsonArray arr = obj.getJsonArray("tools");
        if (arr == null) throw new IllegalArgumentException("tools required");
        List<Tool> tools = new ArrayList<>();
        for (JsonValue v : arr) {
            if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException("tool must be object");
            }
            tools.add(toTool(v.asJsonObject()));
        }
        String cursor = PaginationCodec.toPaginatedResult(obj).nextCursor();
        return new ToolPage(tools, cursor);
    }

    public static ListToolsResult toListToolsResult(JsonObject obj) {
        ToolPage page = toToolPage(obj);
        return new ListToolsResult(page.tools(), page.nextCursor());
    }

    public static ListToolsRequest toListToolsRequest(JsonObject obj) {
        String cursor = PaginationCodec.toPaginatedRequest(obj).cursor();
        return new ListToolsRequest(cursor);
    }

    public static ToolResult toToolResult(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        JsonArray content = obj.getJsonArray("content");
        if (content == null) throw new IllegalArgumentException("content required");
        JsonObject structured = obj.getJsonObject("structuredContent");
        boolean isError = obj.getBoolean("isError", false);
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        return new ToolResult(content, structured, isError, meta);
    }

    public static CallToolRequest toCallToolRequest(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String name = obj.getString("name", null);
        if (name == null) throw new IllegalArgumentException("name required");
        JsonObject arguments = obj.getJsonObject("arguments");
        return new CallToolRequest(name, arguments);
    }

    private static ToolAnnotations toToolAnnotations(JsonObject obj) {
        String title = obj.getString("title", null);
        Boolean readOnly = obj.containsKey("readOnlyHint") ? obj.getBoolean("readOnlyHint") : null;
        Boolean destructive = obj.containsKey("destructiveHint") ? obj.getBoolean("destructiveHint") : null;
        Boolean idempotent = obj.containsKey("idempotentHint") ? obj.getBoolean("idempotentHint") : null;
        Boolean openWorld = obj.containsKey("openWorldHint") ? obj.getBoolean("openWorldHint") : null;
        return new ToolAnnotations(title, readOnly, destructive, idempotent, openWorld);
    }
}
