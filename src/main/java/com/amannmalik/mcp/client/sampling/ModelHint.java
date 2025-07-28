package com.amannmalik.mcp.client.sampling;

/** Advisory model identifier hint. */
public record ModelHint(String name) {
    public ModelHint {
        if (name == null) throw new IllegalArgumentException("name is required");
    }
}
