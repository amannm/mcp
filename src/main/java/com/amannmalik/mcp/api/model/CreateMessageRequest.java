package com.amannmalik.mcp.api.model;

import com.amannmalik.mcp.sampling.ModelPreferences;
import com.amannmalik.mcp.util.ValidationUtil;
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
        JsonObject metadata,
        JsonObject _meta
) {

    public CreateMessageRequest {
        messages = messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
        systemPrompt = ValidationUtil.cleanNullable(systemPrompt);
        if (stopSequences == null || stopSequences.isEmpty()) {
            stopSequences = List.of();
        } else {
            stopSequences = stopSequences.stream().map(ValidationUtil::requireClean).toList();
        }
        maxTokens = ValidationUtil.requirePositive(maxTokens, "maxTokens");
        ValidationUtil.requireMeta(_meta);
    }

    public enum IncludeContext {NONE, THIS_SERVER, ALL_SERVERS}

}
