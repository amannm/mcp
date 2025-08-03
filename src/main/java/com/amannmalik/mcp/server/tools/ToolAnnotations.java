package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.server.roots.validation.InputSanitizer;

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
