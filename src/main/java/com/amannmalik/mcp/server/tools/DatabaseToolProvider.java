package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.validation.SchemaValidator;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.List;
import java.util.Map;

/** Minimal ToolProvider that returns canned results for SQL queries. */
public final class DatabaseToolProvider implements ToolProvider {
    private final Tool tool;
    private final Map<String, JsonArray> queries;

    public DatabaseToolProvider(Map<String, JsonArray> queries) {
        JsonObject schema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder().add("sql", Json.createObjectBuilder().add("type", "string")))
                .add("required", Json.createArrayBuilder().add("sql"))
                .build();
        this.tool = new Tool("query", "Database Query", "Execute SQL", schema, null, null);
        this.queries = Map.copyOf(queries);
    }

    @Override
    public ToolPage list(String cursor) {
        return new ToolPage(List.of(tool), null);
    }

    @Override
    public ToolResult call(String name, JsonObject arguments) {
        if (!tool.name().equals(name)) throw new IllegalArgumentException("Unknown tool");
        SchemaValidator.validate(tool.inputSchema(), arguments);
        String sql = arguments.getString("sql");
        JsonArray rows = queries.get(sql);
        if (rows == null) {
            return new ToolResult(JsonValue.EMPTY_JSON_ARRAY, null, true);
        }
        return new ToolResult(rows, null, false);
    }
}
