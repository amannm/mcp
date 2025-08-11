package com.amannmalik.mcp.api;

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
