package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.CursorCodec;
import com.amannmalik.mcp.core.SpiPreconditions;
import jakarta.json.JsonObject;

import java.util.List;

public record ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates,
                                          Cursor nextCursor,
                                          JsonObject _meta) implements PaginatedResult<ResourceTemplate> {
    public ListResourceTemplatesResult {
        resourceTemplates = SpiPreconditions.immutableList(resourceTemplates);
        nextCursor = CursorCodec.requireCursor(nextCursor);
        SpiPreconditions.requireMeta(_meta);
    }

    @Override
    public List<ResourceTemplate> resourceTemplates() {
        return SpiPreconditions.copyList(resourceTemplates);
    }

    @Override
    public List<ResourceTemplate> items() {
        return SpiPreconditions.copyList(resourceTemplates);
    }
}
