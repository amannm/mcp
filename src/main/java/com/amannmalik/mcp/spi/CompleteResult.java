package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record CompleteResult(Completion completion, JsonObject _meta) implements Result {
    public CompleteResult {
        if (completion == null) throw new IllegalArgumentException("completion required");
        ValidationUtil.requireMeta(_meta);
    }
}
