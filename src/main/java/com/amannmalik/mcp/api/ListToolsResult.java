package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListToolsResult(List<Tool> tools,
                              String nextCursor,
                              JsonObject _meta) {
    static final JsonCodec<ListToolsResult> LIST_TOOLS_RESULT_JSON_CODEC =
            AbstractEntityCodec.paginatedResult(
                    "tools",
                    "tool",
                    r -> new Pagination.Page<>(r.tools(), r.nextCursor()),
                    ListToolsResult::_meta,
                    new ToolAbstractEntityCodec(),
                    (page, meta) -> new ListToolsResult(page.items(), page.nextCursor(), meta));

    public ListToolsResult {
        tools = Immutable.list(tools);
        ValidationUtil.requireMeta(_meta);
    }

    @Override
    public String toString() {
        return LIST_TOOLS_RESULT_JSON_CODEC.toJson(this).toString();
    }
}
