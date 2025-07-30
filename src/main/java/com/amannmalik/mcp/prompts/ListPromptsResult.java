package com.amannmalik.mcp.prompts;

import java.util.List;
import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record ListPromptsResult(List<Prompt> prompts,
                                String nextCursor,
                                JsonObject _meta) {
    public ListPromptsResult {
        prompts = prompts == null ? List.of() : List.copyOf(prompts);
        MetaValidator.requireValid(_meta);
    }
}
