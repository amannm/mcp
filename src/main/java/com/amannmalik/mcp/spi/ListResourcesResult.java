package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListResourcesResult(List<Resource> resources,
                                  Cursor nextCursor,
                                  JsonObject _meta) implements PaginatedResult<Resource> {
    public ListResourcesResult {
        resources = Immutable.list(resources);
        nextCursor = nextCursor == null ? Cursor.End.INSTANCE : nextCursor;
        ValidationUtil.requireMeta(_meta);
    }
    /// Return an immutable view to avoid exposing internal representation.
    @Override
    public List<Resource> resources() {
        return List.copyOf(resources);
    }

    @Override
    public List<Resource> items() {
        return List.copyOf(resources);
    }
}
