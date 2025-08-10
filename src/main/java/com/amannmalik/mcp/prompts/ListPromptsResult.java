package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.api.Pagination;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

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
