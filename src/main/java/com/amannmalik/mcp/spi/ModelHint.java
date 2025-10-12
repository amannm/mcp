package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.SpiPreconditions;

public record ModelHint(String name) {

    public ModelHint {
        if (name != null) {
            name = SpiPreconditions.requireClean(name);
        }
    }

}
