package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.SpiPreconditions;
import jakarta.json.JsonObject;

public record CompleteResult(Completion completion, JsonObject _meta) implements Result {
    public CompleteResult {
        SpiPreconditions.requireNonNull(completion, "completion required");
        SpiPreconditions.requireMeta(_meta);
    }
}
