package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.server.roots.validation.InputSanitizer;
import com.amannmalik.mcp.server.roots.validation.MetaValidator;
import jakarta.json.JsonObject;

public record Tool(String name,
                   String title,
                   String description,
                   JsonObject inputSchema,
                   JsonObject outputSchema,
                   ToolAnnotations annotations,
                   JsonObject _meta) implements DisplayNameProvider {
    public Tool {
        name = InputSanitizer.requireClean(name);
        if (inputSchema == null) {
            throw new IllegalArgumentException("inputSchema is required");
        }
        title = InputSanitizer.cleanNullable(title);
        description = InputSanitizer.cleanNullable(description);
        annotations = annotations == null || (
                annotations.title() == null &&
                        annotations.readOnlyHint() == null &&
                        annotations.destructiveHint() == null &&
                        annotations.idempotentHint() == null &&
                        annotations.openWorldHint() == null
        ) ? null : annotations;
        MetaValidator.requireValid(_meta);
    }

    @Override
    public String displayName() {
        if (title != null) return title;
        if (annotations != null && annotations.title() != null) return annotations.title();
        return name;
    }
}
