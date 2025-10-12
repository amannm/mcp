package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

import java.util.List;

public record PromptInstance(String description, List<PromptMessage> messages) {
    public PromptInstance {
        messages = ValidationUtil.immutableList(messages);
    }

    @Override
    public List<PromptMessage> messages() {
        return ValidationUtil.copyList(messages);
    }
}
