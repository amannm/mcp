package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.ValidationUtil;
import jakarta.json.*;

import java.util.*;

public record ListPromptsResult(List<Prompt> prompts,
                                String nextCursor,
                                JsonObject _meta) {
    public static final JsonCodec<ListPromptsResult> CODEC =
            AbstractEntityCodec.paginatedResult(
                    "prompts",
                    "prompt",
                    r -> new Pagination.Page<>(r.prompts(), r.nextCursor()),
                    ListPromptsResult::_meta,
                    Prompt.CODEC,
                    (page, meta) -> new ListPromptsResult(page.items(), page.nextCursor(), meta));

    public ListPromptsResult {
        prompts = Immutable.list(prompts);
        ValidationUtil.requireMeta(_meta);
    }
}
