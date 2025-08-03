package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.validation.InputSanitizer;

public record ToolAnnotations(
        String title,
        Boolean readOnlyHint,
        Boolean destructiveHint,
        Boolean idempotentHint,
        Boolean openWorldHint
) {
    public ToolAnnotations {
        title = InputSanitizer.cleanNullable(title);
    }
}
