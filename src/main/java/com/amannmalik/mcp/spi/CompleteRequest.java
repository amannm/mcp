package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record CompleteRequest(
        Ref ref,
        Argument argument,
        Context context,
        JsonObject _meta) {
    public CompleteRequest {
        if (ref == null || argument == null) {
            throw new IllegalArgumentException("ref and argument are required");
        }
        ValidationUtil.requireMeta(_meta);
    }
}
