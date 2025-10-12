package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.SpiPreconditions;

import java.util.Map;

public record Context(Map<String, String> arguments) {
    public Context {
        arguments = SpiPreconditions.cleanMap(arguments);
    }

    @Override
    public Map<String, String> arguments() {
        return SpiPreconditions.copyMap(arguments);
    }
}
