package com.amannmalik.mcp.prompts;

import java.util.List;

/** Result for a {@code prompts/list} request. */
public record ListPromptsResult(List<Prompt> prompts, String nextCursor) {
    public ListPromptsResult {
        prompts = prompts == null ? List.of() : List.copyOf(prompts);
    }

    @Override
    public List<Prompt> prompts() {
        return List.copyOf(prompts);
    }
}
