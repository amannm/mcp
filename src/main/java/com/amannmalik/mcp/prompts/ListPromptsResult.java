package com.amannmalik.mcp.prompts;

import java.util.List;

public record ListPromptsResult(List<Prompt> prompts, String nextCursor) {
    public ListPromptsResult {
        prompts = prompts == null ? List.of() : List.copyOf(prompts);
    }
}
