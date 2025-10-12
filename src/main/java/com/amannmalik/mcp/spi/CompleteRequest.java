package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record CompleteRequest(
        Ref ref,
        Argument argument,
        Context context,
        JsonObject _meta) {
    public CompleteRequest {
        ValidationUtil.requireAllNonNull("ref and argument are required", ref, argument);
        ValidationUtil.requireMeta(_meta);
    }
}
