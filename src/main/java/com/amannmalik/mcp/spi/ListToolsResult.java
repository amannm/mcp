package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.CursorCodec;
import com.amannmalik.mcp.core.SpiPreconditions;
import jakarta.json.JsonObject;

import java.util.List;

public record ListToolsResult(List<Tool> tools,
                              Cursor nextCursor,
                              JsonObject _meta) implements PaginatedResult<Tool> {
    public ListToolsResult {
        tools = SpiPreconditions.immutableList(tools);
        nextCursor = CursorCodec.requireCursor(nextCursor);
        SpiPreconditions.requireMeta(_meta);
    }

    @Override
    public List<Tool> tools() {
        return SpiPreconditions.copyList(tools);
    }

    @Override
    public List<Tool> items() {
        return SpiPreconditions.copyList(tools);
    }
}
