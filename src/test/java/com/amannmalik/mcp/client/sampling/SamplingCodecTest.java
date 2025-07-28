package com.amannmalik.mcp.client.sampling;

import com.amannmalik.mcp.prompts.Role;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SamplingCodecTest {
    @Test
    void requestRoundTrip() {
        CreateMessageRequest req = new CreateMessageRequest(
                List.of(new SamplingMessage(Role.USER, new MessageContent.Text("hi"))),
                new ModelPreferences(List.of(new ModelHint("a")), 0.1, 0.2, 0.3),
                "sys", 10
        );
        JsonObject json = SamplingCodec.toJsonObject(req);
        CreateMessageRequest parsed = SamplingCodec.toCreateMessageRequest(json);
        assertEquals(req, parsed);
    }

    @Test
    void responseRoundTrip() {
        CreateMessageResponse resp = new CreateMessageResponse(
                Role.ASSISTANT,
                new MessageContent.Text("ok"),
                "model-1",
                "endTurn"
        );
        JsonObject json = SamplingCodec.toJsonObject(resp);
        CreateMessageResponse parsed = SamplingCodec.toCreateMessageResponse(json);
        assertEquals(resp, parsed);
    }
}
