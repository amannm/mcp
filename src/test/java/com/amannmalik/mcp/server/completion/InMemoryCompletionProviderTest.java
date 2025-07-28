package com.amannmalik.mcp.server.completion;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCompletionProviderTest {
    @Test
    void completesWithContext() throws Exception {
        InMemoryCompletionProvider provider = new InMemoryCompletionProvider();
        provider.add(new CompleteRequest.Ref.PromptRef("code"), "language", Map.of(), List.of("java", "python"));
        provider.add(new CompleteRequest.Ref.PromptRef("code"), "framework", Map.of("language", "python"), List.of("flask"));
        CompleteRequest req = new CompleteRequest(
                new CompleteRequest.Ref.PromptRef("code"),
                new CompleteRequest.Argument("framework", "f"),
                new CompleteRequest.Context(Map.of("language", "python"))
        );
        CompleteResult result = provider.complete(req);
        assertEquals(List.of("flask"), result.completion().values());
        assertEquals(1, result.completion().total());
        assertFalse(result.completion().hasMore());
    }
}
