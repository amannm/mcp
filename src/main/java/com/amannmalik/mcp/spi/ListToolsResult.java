package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListToolsResult(List<Tool> tools,
                              Cursor nextCursor,
                              JsonObject _meta) implements PaginatedResult<Tool> {
    public ListToolsResult {
        tools = Immutable.list(tools);
        nextCursor = nextCursor == null ? Cursor.End.INSTANCE : nextCursor;
        ValidationUtil.requireMeta(_meta);
    }
    /// Return an immutable view to avoid exposing internal representation.
    @Override
    public List<Tool> tools() {
        return List.copyOf(tools);
    }

    @Override
    public List<Tool> items() {
        return List.copyOf(tools);
    }
}
