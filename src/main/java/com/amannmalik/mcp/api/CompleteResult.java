package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.CompleteResultJsonCodec;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record CompleteResult(Completion completion, JsonObject _meta) {
    static final JsonCodec<CompleteResult> CODEC = new CompleteResultJsonCodec();

    public static final int MAX_VALUES =
            McpConfiguration.current().maxCompletionValues();

    public CompleteResult {
        if (completion == null) throw new IllegalArgumentException("completion required");
        ValidationUtil.requireMeta(_meta);
    }

}
