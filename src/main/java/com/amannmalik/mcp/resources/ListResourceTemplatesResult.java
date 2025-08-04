package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.ValidationUtil;
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
                    (page, meta) -> new ListResourceTemplatesResult(page.items(), page.nextCursor(), meta));

    public ListResourceTemplatesResult {
        resourceTemplates = Immutable.list(resourceTemplates);
        ValidationUtil.requireMeta(_meta);
    }
}
