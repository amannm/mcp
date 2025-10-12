package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.spi.internal.SpiPreconditions;

public record ModelHint(String name) {

    public ModelHint {
        if (name != null) {
            name = SpiPreconditions.requireClean(name);
        }
    }

}
