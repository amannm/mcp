package com.amannmalik.mcp.core;

import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListPromptsResult(List<Prompt> prompts,
                                Cursor nextCursor,
                                JsonObject _meta) implements PaginatedResult<Prompt> {
    public ListPromptsResult {
        prompts = Immutable.list(prompts);
        nextCursor = nextCursor == null ? Cursor.End.INSTANCE : nextCursor;
        ValidationUtil.requireMeta(_meta);
    }

    @Override
    public List<Prompt> prompts() {
        return List.copyOf(prompts);
    }

    @Override
    public List<Prompt> items() {
        return List.copyOf(prompts);
    }
}
