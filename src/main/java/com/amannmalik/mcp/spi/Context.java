package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

import java.util.Map;

public record Context(Map<String, String> arguments) {
    public Context {
        arguments = ValidationUtil.requireCleanMap(arguments);
    }

    @Override
    public Map<String, String> arguments() {
        return ValidationUtil.copyMap(arguments);
    }
}
