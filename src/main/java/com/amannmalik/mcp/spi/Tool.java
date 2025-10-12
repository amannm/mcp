package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.ToolContract;
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
        inputSchema = ToolContract.requireInputSchema(inputSchema);
        title = ValidationUtil.cleanNullable(title);
        description = ValidationUtil.cleanNullable(description);
        annotations = ToolContract.normalizeAnnotations(annotations);
        ValidationUtil.requireMeta(_meta);
    }
}
