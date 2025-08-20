package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.core.InMemoryProvider;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.util.JsonSchemaValidator;
import jakarta.json.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class InMemoryToolProvider extends InMemoryProvider<Tool> implements ToolProvider {
    private final Map<String, Function<JsonObject, ToolResult>> handlers;
    private final Map<String, Tool> byName;

    public InMemoryToolProvider(List<Tool> tools, Map<String, Function<JsonObject, ToolResult>> handlers) {
        super(tools);
        this.handlers = handlers == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(handlers);
        this.byName = new ConcurrentHashMap<>();
        if (tools != null) {
            tools.forEach(t -> byName.put(t.name(), t));
        }
    }

    private static ToolResult withStructuredText(ToolResult result) {
        if (hasStructuredText(result)) {
            return result;
        }
        var b = Json.createArrayBuilder(result.content());
        b.add(Json.createObjectBuilder()
                .add("type", "text")
                .add("text", result.structuredContent().toString())
                .build());
        return new ToolResult(b.build(), result.structuredContent(), result.isError(), result._meta());
    }

    private static boolean hasStructuredText(ToolResult result) {
        var text = result.structuredContent().toString();
        for (var v : result.content()) {
            if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                continue;
            }
            var o = v.asJsonObject();
            if ("text".equals(o.getString("type", null)) && text.equals(o.getString("text", null))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<Tool> find(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name required");
        }
        return Optional.ofNullable(byName.get(name));
    }

    @Override
    public ToolResult call(String name, JsonObject arguments) {
        var tool = byName.get(name);
        var handler = handlers.get(name);
        if (tool == null || handler == null) {
            throw new IllegalArgumentException("Unknown tool");
        }
        var args = arguments == null ? JsonValue.EMPTY_JSON_OBJECT : arguments;
        JsonSchemaValidator.validate(tool.inputSchema(), args);
        var result = handler.apply(args);
        if (tool.outputSchema() != null) {
            if (result.structuredContent() == null) {
                throw new IllegalStateException("structured result required");
            }
            JsonSchemaValidator.validate(tool.outputSchema(), result.structuredContent());
        }
        if (result.structuredContent() != null) {
            result = withStructuredText(result);
        }
        return result;
    }

    public void addTool(Tool tool, Function<JsonObject, ToolResult> handler) {
        if (tool == null) {
            throw new IllegalArgumentException("tool required");
        }
        if (byName.containsKey(tool.name())) {
            throw new IllegalArgumentException("Duplicate tool name: " + tool.name());
        }
        items.add(tool);
        byName.put(tool.name(), tool);
        if (handler != null) {
            handlers.put(tool.name(), handler);
        }
        notifyListChanged();
    }

    public void removeTool(String name) {
        items.removeIf(t -> t.name().equals(name));
        byName.remove(name);
        handlers.remove(name);
        notifyListChanged();
    }
}
