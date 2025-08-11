package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.Immutable;
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
        messages = Immutable.list(messages);
        systemPrompt = ValidationUtil.cleanNullable(systemPrompt);
        stopSequences = Immutable.list(stopSequences == null
                ? null
                : stopSequences.stream().map(ValidationUtil::requireClean).toList());
        maxTokens = ValidationUtil.requirePositive(maxTokens, "maxTokens");
        ValidationUtil.requireMeta(_meta);
    }

    public enum IncludeContext {NONE, THIS_SERVER, ALL_SERVERS}

}
