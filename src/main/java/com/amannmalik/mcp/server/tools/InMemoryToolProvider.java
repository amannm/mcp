package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.util.ListChangeSupport;
import com.amannmalik.mcp.util.Pagination;
import com.amannmalik.mcp.validation.SchemaValidator;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class InMemoryToolProvider implements ToolProvider {
    private final List<Tool> tools;
    private final Map<String, Function<JsonObject, ToolResult>> handlers;
    private final ListChangeSupport<ToolListListener> listChangeSupport = new ListChangeSupport<>();

    public InMemoryToolProvider(List<Tool> tools, Map<String, Function<JsonObject, ToolResult>> handlers) {
        this.tools = tools == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(tools);
        this.handlers = handlers == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(handlers);
    }

    @Override
    public Pagination.Page<Tool> list(String cursor) {
        return Pagination.page(tools, cursor, 100);
    }

    @Override
    public ToolResult call(String name, JsonObject arguments) {
        var f = handlers.get(name);
        if (f == null) throw new IllegalArgumentException("Unknown tool");
        Tool tool = tools.stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown tool"));
        JsonObject args = arguments == null ? JsonValue.EMPTY_JSON_OBJECT : arguments;
        SchemaValidator.validate(tool.inputSchema(), args);
        ToolResult result = f.apply(args);
        if (tool.outputSchema() != null) {
            if (result.structuredContent() == null) {
                throw new IllegalStateException("structured result required");
            }
            SchemaValidator.validate(tool.outputSchema(), result.structuredContent());
        }
        if (result.structuredContent() != null) {
            boolean hasText = false;
            for (var v : result.content()) {
                if (v.getValueType() == JsonValue.ValueType.OBJECT) {
                    JsonObject o = v.asJsonObject();
                    if ("text".equals(o.getString("type", null)) &&
                            result.structuredContent().toString().equals(o.getString("text", null))) {
                        hasText = true;
                        break;
                    }
                }
            }
            if (!hasText) {
                var b = Json.createArrayBuilder(result.content());
                b.add(Json.createObjectBuilder()
                        .add("type", "text")
                        .add("text", result.structuredContent().toString())
                        .build());
                result = new ToolResult(b.build(), result.structuredContent(), result.isError(), result._meta());
            }
        }
        return result;
    }

    @Override
    public ToolListSubscription subscribeList(ToolListListener listener) {
        var sub = listChangeSupport.subscribe(listener);
        return sub::close;
    }

    @Override
    public boolean supportsListChanged() {
        return true;
    }

    public void addTool(Tool tool, Function<JsonObject, ToolResult> handler) {
        if (tool == null) throw new IllegalArgumentException("tool required");
        if (tools.stream().anyMatch(t -> t.name().equals(tool.name()))) {
            throw new IllegalArgumentException("Duplicate tool name: " + tool.name());
        }
        tools.add(tool);
        if (handler != null) handlers.put(tool.name(), handler);
        notifyListeners();
    }

    public void removeTool(String name) {
        tools.removeIf(t -> t.name().equals(name));
        handlers.remove(name);
        notifyListeners();
    }

    private void notifyListeners() {
        listChangeSupport.notifyListeners();
    }
}
