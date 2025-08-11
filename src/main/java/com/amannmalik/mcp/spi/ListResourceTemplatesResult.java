package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates,
                                          Cursor nextCursor,
                                          JsonObject _meta) implements PaginatedResult<ResourceTemplate> {
    public ListResourceTemplatesResult {
        resourceTemplates = Immutable.list(resourceTemplates);
        nextCursor = nextCursor == null ? Cursor.End.INSTANCE : nextCursor;
        ValidationUtil.requireMeta(_meta);
    }

    @Override
    public List<ResourceTemplate> items() {
        return resourceTemplates;
    }
}
