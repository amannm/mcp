package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

public record SamplingMessage(Role role, MessageContent content) {
    public SamplingMessage {
        ValidationUtil.requireAllNonNull("role and content are required", role, content);
    }
}
