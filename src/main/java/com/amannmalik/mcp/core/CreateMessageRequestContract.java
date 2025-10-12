package com.amannmalik.mcp.core;

import com.amannmalik.mcp.spi.SamplingMessage;
import com.amannmalik.mcp.util.ValidationUtil;

import java.util.List;

public final class CreateMessageRequestContract {
    private CreateMessageRequestContract() {
    }

    public static List<SamplingMessage> sanitizeMessages(List<SamplingMessage> messages) {
        return ValidationUtil.immutableList(messages);
    }

    public static String sanitizeSystemPrompt(String systemPrompt) {
        return ValidationUtil.cleanNullable(systemPrompt);
    }

    public static List<String> sanitizeStopSequences(List<String> stopSequences) {
        var cleaned = stopSequences == null ? null : stopSequences.stream()
                .map(ValidationUtil::requireClean)
                .toList();
        return ValidationUtil.immutableList(cleaned);
    }

    public static int normalizeMaxTokens(int maxTokens) {
        return ValidationUtil.requirePositive(maxTokens, "maxTokens");
    }
}
