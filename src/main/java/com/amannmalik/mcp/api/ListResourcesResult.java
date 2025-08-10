package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListResourcesResult(List<Resource> resources,
                                  String nextCursor,
                                  JsonObject _meta) {
     static final JsonCodec<ListResourcesResult> CODEC =
            AbstractEntityCodec.paginatedResult(
                    "resources",
                    "resource",
                    r -> new Pagination.Page<>(r.resources(), r.nextCursor()),
                    ListResourcesResult::_meta,
                    Resource.CODEC,
                    (page, meta) -> new ListResourcesResult(page.items(), page.nextCursor(), meta));

    public ListResourcesResult {
        resources = Immutable.list(resources);
        ValidationUtil.requireMeta(_meta);
    }
}
