package com.amannmalik.mcp.prompts;

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
    public List<Prompt> items() {
        return prompts;
    }
}
