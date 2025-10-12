package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.CursorCodec;
import com.amannmalik.mcp.core.SpiPreconditions;
import jakarta.json.JsonObject;

import java.util.List;

public record ListResourcesResult(List<Resource> resources,
                                  Cursor nextCursor,
                                  JsonObject _meta) implements PaginatedResult<Resource> {
    public ListResourcesResult {
        resources = SpiPreconditions.immutableList(resources);
        nextCursor = CursorCodec.requireCursor(nextCursor);
        SpiPreconditions.requireMeta(_meta);
    }

    @Override
    public List<Resource> resources() {
        return SpiPreconditions.copyList(resources);
    }

    @Override
    public List<Resource> items() {
        return SpiPreconditions.copyList(resources);
    }
}
