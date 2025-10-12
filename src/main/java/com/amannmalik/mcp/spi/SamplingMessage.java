package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.SpiPreconditions;

public record SamplingMessage(Role role, MessageContent content) {
    public SamplingMessage {
        SpiPreconditions.requireAllNonNull("role and content are required", role, content);
    }
}
