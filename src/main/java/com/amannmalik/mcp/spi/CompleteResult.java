package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record CompleteResult(Completion completion, JsonObject _meta) implements Result {
    public CompleteResult {
        ValidationUtil.requireNonNull(completion, "completion required");
        ValidationUtil.requireMeta(_meta);
    }
}
