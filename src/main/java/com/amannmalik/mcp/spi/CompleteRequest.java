package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.SpiPreconditions;
import jakarta.json.JsonObject;

public record CompleteRequest(
        Ref ref,
        Argument argument,
        Context context,
        JsonObject _meta) {
    public CompleteRequest {
        SpiPreconditions.requireAllNonNull("ref and argument are required", ref, argument);
        SpiPreconditions.requireMeta(_meta);
    }
}
