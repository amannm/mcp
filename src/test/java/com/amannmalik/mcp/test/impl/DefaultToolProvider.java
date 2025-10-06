package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.*;
import jakarta.json.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class DefaultToolProvider implements ToolProvider {
    private final List<Tool> tools = new CopyOnWriteArrayList<>(DefaultServerFixtures.TOOLS);
    private final Map<String, Tool> byName = new HashMap<>();
    private final Map<String, Function<JsonObject, ToolResult>> handlers = new HashMap<>(DefaultServerFixtures.TOOL_HANDLERS);
    private final List<Runnable> listChangedListeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean changeSimulationStarted = new AtomicBoolean();

    public DefaultToolProvider() {
        tools.forEach(tool -> byName.put(tool.name(), tool));
    }

    @Override
    public Pagination.Page<Tool> list(Cursor cursor) {
        return Pagination.page(tools, cursor == null ? Cursor.Start.INSTANCE : cursor, Pagination.DEFAULT_PAGE_SIZE);
    }

    @Override
    public AutoCloseable onListChanged(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        listChangedListeners.add(listener);
        startSimulation();
        return () -> listChangedListeners.remove(listener);
    }

    @Override
    public boolean supportsListChanged() {
        return true;
    }

    @Override
    public void close() {
        listChangedListeners.clear();
        handlers.clear();
        byName.clear();
        tools.clear();
        changeSimulationStarted.set(false);
    }

    @Override
    public Optional<Tool> find(String name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(byName.get(name));
    }

    @Override
    public ToolResult call(String name, JsonObject arguments) {
        Objects.requireNonNull(name, "name");
        var tool = byName.get(name);
        var handler = handlers.get(name);
        if (tool == null || handler == null) {
            throw new IllegalArgumentException("Unknown tool");
        }
        var args = arguments == null ? JsonValue.EMPTY_JSON_OBJECT : arguments;
        validateArguments(tool, args);
        var result = handler.apply(args);
        if (tool.outputSchema() != null && result.structuredContent() == null) {
            throw new IllegalStateException("structured result required");
        }
        return result;
    }

    private void validateArguments(Tool tool, JsonObject args) {
        var schema = tool.inputSchema();
        if (schema == null || !"object".equals(schema.getString("type", null))) {
            return;
        }
        var required = schema.getJsonArray("required");
        if (required != null) {
            for (var r : required.getValuesAs(JsonString.class)) {
                if (!args.containsKey(r.getString())) {
                    throw new IllegalArgumentException("Missing required field: " + r.getString());
                }
            }
        }
        var properties = schema.getJsonObject("properties");
        if (properties == null) {
            return;
        }
        for (var e : args.entrySet()) {
            var name = e.getKey();
            var value = e.getValue();
            if (!properties.containsKey(name)) {
                throw new IllegalArgumentException("Unexpected field: " + name);
            }
            var propSchema = properties.getJsonObject(name);
            validateType(name, propSchema, value);
        }
    }

    private void validateType(String name, JsonObject schema, JsonValue value) {
        var type = schema.getString("type", null);
        if (type == null) {
            return;
        }
        switch (type) {
            case "string" -> {
                if (!(value instanceof JsonString)) {
                    throw new IllegalArgumentException("Expected string for " + name);
                }
            }
            case "object" -> {
                if (!(value instanceof JsonObject obj)) {
                    throw new IllegalArgumentException("Expected object for " + name);
                }
                validateNestedObject(obj, schema.getJsonObject("properties"));
            }
            default -> {
                // For this test implementation, only string validation is required.
            }
        }
    }

    private void validateNestedObject(JsonObject value, JsonObject schema) {
        if (schema == null) {
            return;
        }
        for (var entry : schema.entrySet()) {
            if (!value.containsKey(entry.getKey())) {
                continue;
            }
            validateType(entry.getKey(), entry.getValue().asJsonObject(), value.get(entry.getKey()));
        }
    }

    private void startSimulation() {
        if (changeSimulationStarted.compareAndSet(false, true)) {
            Thread.ofVirtual().start(() -> {
                for (int attempt = 0; attempt < 10; attempt++) {
                    try {
                        Thread.sleep(1_000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    listChangedListeners.forEach(Runnable::run);
                }
            });
        }
    }
}
