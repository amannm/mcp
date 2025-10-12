package com.amannmalik.mcp.spi.internal;

import com.amannmalik.mcp.spi.SamplingMessage;

import java.util.List;

public final class CreateMessageRequestContract {
    private CreateMessageRequestContract() {
    }

    public static List<SamplingMessage> sanitizeMessages(List<SamplingMessage> messages) {
        return SpiPreconditions.immutableList(messages);
    }

    public static String sanitizeSystemPrompt(String systemPrompt) {
        return SpiPreconditions.cleanNullable(systemPrompt);
    }

    public static List<String> sanitizeStopSequences(List<String> stopSequences) {
        var cleaned = stopSequences == null ? null : stopSequences.stream()
                .map(SpiPreconditions::requireClean)
                .toList();
        return SpiPreconditions.immutableList(cleaned);
    }

    public static int normalizeMaxTokens(int maxTokens) {
        return SpiPreconditions.requirePositive(maxTokens, "maxTokens");
    }
}
