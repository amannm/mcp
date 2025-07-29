package com.amannmalik.mcp.client.sampling;

import jakarta.json.JsonObject;

import java.util.List;

public record CreateMessageRequest(
        List<SamplingMessage> messages,
        ModelPreferences modelPreferences,
        String systemPrompt,
        IncludeContext includeContext,
        Double temperature,
        int maxTokens,
        List<String> stopSequences,
        JsonObject metadata
) {
    public enum IncludeContext {NONE, THIS_SERVER, ALL_SERVERS}

    public CreateMessageRequest {
        messages = messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
        stopSequences = stopSequences == null || stopSequences.isEmpty() ? List.of() : List.copyOf(stopSequences);
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be > 0");
        }
    }
}
