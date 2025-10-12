package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.CursorCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListResourcesResult(List<Resource> resources,
                                  Cursor nextCursor,
                                  JsonObject _meta) implements PaginatedResult<Resource> {
    public ListResourcesResult {
        resources = ValidationUtil.immutableList(resources);
        nextCursor = CursorCodec.requireCursor(nextCursor);
        ValidationUtil.requireMeta(_meta);
    }

    @Override
    public List<Resource> resources() {
        return ValidationUtil.copyList(resources);
    }

    @Override
    public List<Resource> items() {
        return ValidationUtil.copyList(resources);
    }
}
