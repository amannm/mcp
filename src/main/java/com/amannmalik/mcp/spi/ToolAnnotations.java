package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

public record ToolAnnotations(
        String title,
        Boolean readOnlyHint,
        Boolean destructiveHint,
        Boolean idempotentHint,
        Boolean openWorldHint
) {
    public ToolAnnotations {
        title = ValidationUtil.cleanNullable(title);
    }
}
