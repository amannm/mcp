package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.CreateMessageRequestContract;
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
        messages = CreateMessageRequestContract.sanitizeMessages(messages);
        systemPrompt = CreateMessageRequestContract.sanitizeSystemPrompt(systemPrompt);
        stopSequences = CreateMessageRequestContract.sanitizeStopSequences(stopSequences);
        maxTokens = CreateMessageRequestContract.normalizeMaxTokens(maxTokens);
        ValidationUtil.requireMeta(_meta);
    }

    @Override
    public List<SamplingMessage> messages() {
        return ValidationUtil.copyList(messages);
    }

    @Override
    public List<String> stopSequences() {
        return ValidationUtil.copyList(stopSequences);
    }

    public enum IncludeContext {NONE, THIS_SERVER, ALL_SERVERS}
}
