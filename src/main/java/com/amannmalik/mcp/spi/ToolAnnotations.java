package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.SpiPreconditions;

public record ToolAnnotations(
        String title,
        Boolean readOnlyHint,
        Boolean destructiveHint,
        Boolean idempotentHint,
        Boolean openWorldHint
) {
    public ToolAnnotations {
        title = SpiPreconditions.cleanNullable(title);
    }
}
