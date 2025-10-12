package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.CursorCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates,
                                          Cursor nextCursor,
                                          JsonObject _meta) implements PaginatedResult<ResourceTemplate> {
    public ListResourceTemplatesResult {
        resourceTemplates = ValidationUtil.immutableList(resourceTemplates);
        nextCursor = CursorCodec.requireCursor(nextCursor);
        ValidationUtil.requireMeta(_meta);
    }

    @Override
    public List<ResourceTemplate> resourceTemplates() {
        return ValidationUtil.copyList(resourceTemplates);
    }

    @Override
    public List<ResourceTemplate> items() {
        return ValidationUtil.copyList(resourceTemplates);
    }
}
