package com.amannmalik.mcp.prompts;

import java.util.List;

public record PromptPage(List<Prompt> prompts, String nextCursor) {
    public PromptPage {
        prompts = prompts == null ? List.of() : List.copyOf(prompts);
    }
}
