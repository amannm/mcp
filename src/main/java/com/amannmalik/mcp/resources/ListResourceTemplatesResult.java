package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.*;

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
                    (items, pr) -> new ListResourceTemplatesResult(items, pr.nextCursor(), pr._meta()));

    public ListResourceTemplatesResult {
        resourceTemplates = Immutable.list(resourceTemplates);
        MetaValidator.requireValid(_meta);
    }
}
