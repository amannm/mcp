package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.CallToolRequestAbstractEntityCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public record CallToolRequest(String name,
                              JsonObject arguments,
                              JsonObject _meta) {
    static final CallToolRequestAbstractEntityCodec CODEC = new CallToolRequestAbstractEntityCodec();

    public CallToolRequest {
        if (name == null) throw new IllegalArgumentException("name required");
        name = ValidationUtil.requireClean(name);
        arguments = arguments == null ? JsonValue.EMPTY_JSON_OBJECT : arguments;
        ValidationUtil.requireMeta(_meta);
    }

}
