package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

import java.util.List;

public record ListPromptsResult(List<Prompt> prompts,
                                String nextCursor,
                                JsonObject _meta) {
    public ListPromptsResult {
        prompts = Immutable.list(prompts);
        MetaValidator.requireValid(_meta);
    }
}
