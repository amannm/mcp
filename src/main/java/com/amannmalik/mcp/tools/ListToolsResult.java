package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.*;

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
        MetaValidator.requireValid(_meta);
    }
}
