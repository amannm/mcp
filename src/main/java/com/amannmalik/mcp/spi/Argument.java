package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.spi.internal.SpiPreconditions;

public record Argument(String name, String value) {

    public Argument {
        SpiPreconditions.requireAllNonNull("name and value are required", name, value);
        name = SpiPreconditions.requireClean(name);
        value = SpiPreconditions.requireClean(value);
    }

}
