package com.amannmalik.mcp.client.sampling;

import java.util.List;


public record CreateMessageRequest(
        List<SamplingMessage> messages,
        ModelPreferences modelPreferences,
        String systemPrompt,
        Integer maxTokens
) {
    public CreateMessageRequest {
        messages = messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
    }
}
