package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record Tool(String name,
                   String title,
                   String description,
                   JsonObject inputSchema,
                   JsonObject outputSchema,
                   ToolAnnotations annotations,
                   JsonObject _meta) implements DisplayNameProvider {

    public Tool {
        name = ValidationUtil.requireClean(name);
        if (inputSchema == null) {
            throw new IllegalArgumentException("inputSchema is required");
        }
        title = ValidationUtil.cleanNullable(title);
        description = ValidationUtil.cleanNullable(description);
        annotations = annotations == null || (
                annotations.title() == null &&
                        annotations.readOnlyHint() == null &&
                        annotations.destructiveHint() == null &&
                        annotations.idempotentHint() == null &&
                        annotations.openWorldHint() == null
        ) ? null : annotations;
        ValidationUtil.requireMeta(_meta);
    }

    @Override
    public String displayName() {
        if (title != null) return title;
        if (annotations != null && annotations.title() != null) return annotations.title();
        return name;
    }

}
