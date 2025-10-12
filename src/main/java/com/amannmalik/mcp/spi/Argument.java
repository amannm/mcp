package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

public record Argument(String name, String value) {
    public Argument {
        ValidationUtil.requireAllNonNull("name and value are required", name, value);
        name = ValidationUtil.requireClean(name);
        value = ValidationUtil.requireClean(value);
    }
}
