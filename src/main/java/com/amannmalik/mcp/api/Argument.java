package com.amannmalik.mcp.api;

import com.amannmalik.mcp.completion.ArgumentJsonCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;

public record Argument(String name, String value) {
    static final JsonCodec<Argument> CODEC = new ArgumentJsonCodec();

    public Argument(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("name and value are required");
        }
        this.name = ValidationUtil.requireClean(name);
        this.value = ValidationUtil.requireClean(value);
    }

}
