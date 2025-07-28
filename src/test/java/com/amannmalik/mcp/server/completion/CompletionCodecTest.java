package com.amannmalik.mcp.server.completion;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompletionCodecTest {
    @Test
    void requestRoundTrip() {
        CompleteRequest req = new CompleteRequest(
                new CompleteRequest.Ref.PromptRef("code"),
                new CompleteRequest.Argument("lang", "j"),
                new CompleteRequest.Context(Map.of("framework", "spring"))
        );
        JsonObject json = CompletionCodec.toJsonObject(req);
        CompleteRequest parsed = CompletionCodec.toCompleteRequest(json);
        assertEquals(req, parsed);
    }

    @Test
    void resultRoundTrip() {
        CompleteResult res = new CompleteResult(new CompleteResult.Completion(List.of("java", "javascript"), 5, true));
        JsonObject json = CompletionCodec.toJsonObject(res);
        CompleteResult parsed = CompletionCodec.toCompleteResult(json);
        assertEquals(res, parsed);
    }
}
