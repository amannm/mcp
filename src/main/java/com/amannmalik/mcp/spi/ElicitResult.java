package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.ElicitResultContract;
import jakarta.json.JsonObject;

public record ElicitResult(ElicitationAction action, JsonObject content, JsonObject _meta) implements Result {
    public ElicitResult {
        ElicitResultContract.validate(action, content, _meta);
    }
}
