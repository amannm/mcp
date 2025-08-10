package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.SetLevelRequestAbstractEntityCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record SetLevelRequest(LoggingLevel level, JsonObject _meta) {
    public static final JsonCodec<SetLevelRequest> CODEC = new SetLevelRequestAbstractEntityCodec();

    public SetLevelRequest {
        if (level == null) throw new IllegalArgumentException("level is required");
        ValidationUtil.requireMeta(_meta);
    }
}
