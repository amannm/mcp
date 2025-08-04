package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.*;

public record ListResourcesResult(List<Resource> resources,
                                  String nextCursor,
                                  JsonObject _meta) {
    public static final JsonCodec<ListResourcesResult> CODEC =
            AbstractEntityCodec.paginatedResult(
                    "resources",
                    "resource",
                    r -> new Pagination.Page<>(r.resources(), r.nextCursor()),
                    ListResourcesResult::_meta,
                    Resource.CODEC,
                    (page, meta) -> new ListResourcesResult(page.items(), page.nextCursor(), meta));

    public ListResourcesResult {
        resources = Immutable.list(resources);
        MetaValidator.requireValid(_meta);
    }
}
