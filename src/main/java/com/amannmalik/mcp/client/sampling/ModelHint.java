package com.amannmalik.mcp.client.sampling;

import com.amannmalik.mcp.validation.InputSanitizer;

public record ModelHint(String name) {
    public ModelHint {
        if (name != null) {
            name = InputSanitizer.requireClean(name);
        }
    }
}
