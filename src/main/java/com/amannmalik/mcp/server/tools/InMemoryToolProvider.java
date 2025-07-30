package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.util.Pagination;
import com.amannmalik.mcp.validation.SchemaValidator;
import jakarta.json.Json;
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
    private final List<ToolListListener> listeners = new CopyOnWriteArrayList<>();

    public InMemoryToolProvider(List<Tool> tools, Map<String, Function<JsonObject, ToolResult>> handlers) {
        this.tools = tools == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(tools);
        this.handlers = handlers == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(handlers);
    }

    @Override
    public ToolPage list(String cursor) {
        Pagination.Page<Tool> page = Pagination.page(tools, cursor, 100);
        return new ToolPage(page.items(), page.nextCursor());
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
        return result;
    }

    @Override
    public ToolListSubscription subscribeList(ToolListListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
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
        listeners.forEach(ToolListListener::listChanged);
    }
}
