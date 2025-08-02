package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

import java.util.List;

public record ListPromptsResult(List<Prompt> prompts,
                                String nextCursor,
                                JsonObject _meta) {
    public ListPromptsResult {
        prompts = prompts == null ? List.of() : List.copyOf(prompts);
        MetaValidator.requireValid(_meta);
    }
}
