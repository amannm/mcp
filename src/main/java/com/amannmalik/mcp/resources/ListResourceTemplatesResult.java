package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.Pagination;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates,
                                          String nextCursor,
                                          JsonObject _meta) {
    public static final JsonCodec<ListResourceTemplatesResult> CODEC =
            AbstractEntityCodec.paginatedResult(
                    "resourceTemplates",
                    "resourceTemplate",
                    r -> new Pagination.Page<>(r.resourceTemplates(), r.nextCursor()),
                    ListResourceTemplatesResult::_meta,
                    ResourceTemplate.CODEC,
                    (page, meta) -> new ListResourceTemplatesResult(page.items(), page.nextCursor(), meta));

    public ListResourceTemplatesResult {
        resourceTemplates = Immutable.list(resourceTemplates);
        ValidationUtil.requireMeta(_meta);
    }
}
