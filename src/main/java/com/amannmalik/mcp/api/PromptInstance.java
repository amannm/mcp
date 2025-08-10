package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.PromptInstanceAbstractEntityCodec;

import java.util.List;

public record PromptInstance(String description, List<PromptMessage> messages) {
    static final JsonCodec<PromptInstance> CODEC = new PromptInstanceAbstractEntityCodec();

    public PromptInstance {
        messages = messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
    }

}
