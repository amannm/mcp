package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.util.Pagination;
import jakarta.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.function.Function;


public final class InMemoryToolProvider implements ToolProvider {
    private final List<Tool> tools;
    private final Map<String, Function<JsonObject, ToolResult>> handlers;

    public InMemoryToolProvider(List<Tool> tools, Map<String, Function<JsonObject, ToolResult>> handlers) {
        this.tools = tools == null ? List.of() : List.copyOf(tools);
        this.handlers = handlers == null ? Map.of() : Map.copyOf(handlers);
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
        return f.apply(arguments == null ? jakarta.json.Json.createObjectBuilder().build() : arguments);
    }
}
