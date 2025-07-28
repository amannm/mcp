package com.amannmalik.mcp.prompts;

import java.util.List;

/** Result of a prompt listing request. */
public record PromptPage(List<Prompt> prompts, String nextCursor) {
    public PromptPage {
        prompts = prompts == null ? List.of() : List.copyOf(prompts);
    }

    @Override
    public List<Prompt> prompts() {
        return List.copyOf(prompts);
    }
}
