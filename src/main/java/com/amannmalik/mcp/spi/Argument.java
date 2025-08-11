package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

public record Argument(String name, String value) {

    public Argument(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("name and value are required");
        }
        this.name = ValidationUtil.requireClean(name);
        this.value = ValidationUtil.requireClean(value);
    }

}
