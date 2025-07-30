package com.amannmalik.mcp.client.sampling;

import com.amannmalik.mcp.validation.InputSanitizer;

/**
 * Hint for model selection. The value is sanitized to remove control
 * characters, ensuring safe transmission over JSON-RPC transports.
 */
public record ModelHint(String name) {
    public ModelHint {
        if (name != null) {
            name = InputSanitizer.requireClean(name);
        }
    }
}
