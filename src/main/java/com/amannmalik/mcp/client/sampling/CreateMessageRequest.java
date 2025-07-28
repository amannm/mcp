package com.amannmalik.mcp.client.sampling;

import java.util.List;

/** Parameters for requesting a new message from a model. */
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
