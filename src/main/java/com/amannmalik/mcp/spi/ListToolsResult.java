package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.CursorCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListToolsResult(List<Tool> tools,
                              Cursor nextCursor,
                              JsonObject _meta) implements PaginatedResult<Tool> {
    public ListToolsResult {
        tools = ValidationUtil.immutableList(tools);
        nextCursor = CursorCodec.requireCursor(nextCursor);
        ValidationUtil.requireMeta(_meta);
    }

    @Override
    public List<Tool> tools() {
        return ValidationUtil.copyList(tools);
    }

    @Override
    public List<Tool> items() {
        return ValidationUtil.copyList(tools);
    }
}
