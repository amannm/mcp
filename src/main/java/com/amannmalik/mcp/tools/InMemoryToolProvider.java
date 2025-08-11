package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.api.ToolProvider;
import com.amannmalik.mcp.api.model.Tool;
import com.amannmalik.mcp.api.model.ToolResult;
import com.amannmalik.mcp.core.InMemoryProvider;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class InMemoryToolProvider extends InMemoryProvider<Tool> implements ToolProvider {
    private final Map<String, Function<JsonObject, ToolResult>> handlers;

    public InMemoryToolProvider(List<Tool> tools, Map<String, Function<JsonObject, ToolResult>> handlers) {
        super(tools);
        this.handlers = handlers == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(handlers);
    }

    private static ToolResult withStructuredText(ToolResult result) {
        if (hasStructuredText(result)) return result;
        var b = Json.createArrayBuilder(result.content());
        b.add(Json.createObjectBuilder()
                .add("type", "text")
                .add("text", result.structuredContent().toString())
                .build());
        return new ToolResult(b.build(), result.structuredContent(), result.isError(), result._meta());
    }

    private static boolean hasStructuredText(ToolResult result) {
        String text = result.structuredContent().toString();
        for (JsonValue v : result.content()) {
            if (v.getValueType() != JsonValue.ValueType.OBJECT) continue;
            JsonObject o = v.asJsonObject();
            if ("text".equals(o.getString("type", null)) && text.equals(o.getString("text", null))) return true;
        }
        return false;
    }

    @Override
    public Optional<Tool> find(String name) {
        if (name == null) throw new IllegalArgumentException("name required");
        for (Tool t : items) {
            if (t.name().equals(name)) return Optional.of(t);
        }
        return Optional.empty();
    }

    @Override
    public ToolResult call(String name, JsonObject arguments) {
        var f = handlers.get(name);
        if (f == null) throw new IllegalArgumentException("Unknown tool");
        Tool tool = items.stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown tool"));
        JsonObject args = arguments == null ? JsonValue.EMPTY_JSON_OBJECT : arguments;
        ValidationUtil.validateSchema(tool.inputSchema(), args);
        ToolResult result = f.apply(args);
        if (tool.outputSchema() != null) {
            if (result.structuredContent() == null) {
                throw new IllegalStateException("structured result required");
            }
            ValidationUtil.validateSchema(tool.outputSchema(), result.structuredContent());
        }
        if (result.structuredContent() != null) result = withStructuredText(result);
        return result;
    }

    public void addTool(Tool tool, Function<JsonObject, ToolResult> handler) {
        if (tool == null) throw new IllegalArgumentException("tool required");
        if (items.stream().anyMatch(t -> t.name().equals(tool.name()))) {
            throw new IllegalArgumentException("Duplicate tool name: " + tool.name());
        }
        items.add(tool);
        if (handler != null) handlers.put(tool.name(), handler);
        notifyListeners();
    }

    public void removeTool(String name) {
        items.removeIf(t -> t.name().equals(name));
        handlers.remove(name);
        notifyListeners();
    }
}
