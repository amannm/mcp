package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.ContextJsonCodec;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;

import java.util.Map;

public record Context(Map<String, String> arguments) {

    static final JsonCodec<Context> CODEC = new ContextJsonCodec();

    public Context(Map<String, String> arguments) {
        this.arguments = ValidationUtil.requireCleanMap(arguments);
    }

    @Override
    public Map<String, String> arguments() {
        return Map.copyOf(arguments);
    }

}
