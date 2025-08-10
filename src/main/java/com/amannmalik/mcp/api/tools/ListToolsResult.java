package com.amannmalik.mcp.api.tools;

import com.amannmalik.mcp.api.JsonCodec;
import com.amannmalik.mcp.api.Pagination;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListToolsResult(List<Tool> tools,
                              String nextCursor,
                              JsonObject _meta) {
     public static final JsonCodec<ListToolsResult> CODEC =
            AbstractEntityCodec.paginatedResult(
                    "tools",
                    "tool",
                    r -> new Pagination.Page<>(r.tools(), r.nextCursor()),
                    ListToolsResult::_meta,
                    Tool.CODEC,
                    (page, meta) -> new ListToolsResult(page.items(), page.nextCursor(), meta));

    public ListToolsResult {
        tools = Immutable.list(tools);
        ValidationUtil.requireMeta(_meta);
    }
}
