package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.SpiPreconditions;
import com.amannmalik.mcp.core.ToolContract;
import jakarta.json.JsonObject;

public record Tool(String name,
                   String title,
                   String description,
                   JsonObject inputSchema,
                   JsonObject outputSchema,
                   ToolAnnotations annotations,
                   JsonObject _meta) implements DisplayNameProvider {
    public Tool {
        name = SpiPreconditions.requireClean(name);
        inputSchema = ToolContract.requireInputSchema(inputSchema);
        title = SpiPreconditions.cleanNullable(title);
        description = SpiPreconditions.cleanNullable(description);
        annotations = ToolContract.normalizeAnnotations(annotations);
        SpiPreconditions.requireMeta(_meta);
    }
}
