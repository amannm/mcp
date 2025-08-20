package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

public record ModelHint(String name) {

    public ModelHint {
        if (name != null) {
            name = ValidationUtil.requireClean(name);
        }
    }

}
