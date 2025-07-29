package com.amannmalik.mcp;

import com.amannmalik.mcp.client.sampling.*;
import com.amannmalik.mcp.prompts.Role;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SamplingCodecTest {

    @Test
    void testContentRoundTrip() {
        Annotations ann = new Annotations(Set.of(Role.USER), 0.5, Instant.parse("2025-01-01T00:00:00Z"));
        JsonObject meta = Json.createObjectBuilder().add("a", 1).build();
        MessageContent.Text content = new MessageContent.Text("hi", ann, meta);
        CreateMessageResponse resp = new CreateMessageResponse(Role.ASSISTANT, content, "model", null);
        CreateMessageResponse out = SamplingCodec.toCreateMessageResponse(SamplingCodec.toJsonObject(resp));
        assertEquals(resp, out);
    }

    @Test
    void testResponseModelRequired() {
        MessageContent.Text content = new MessageContent.Text("hi", null, null);
        CreateMessageResponse resp = new CreateMessageResponse(Role.ASSISTANT, content, "m", null);
        JsonObject obj = SamplingCodec.toJsonObject(resp);
        assertEquals("m", obj.getString("model"));
        JsonObject noModel = Json.createObjectBuilder()
                .add("role", obj.getString("role"))
                .add("content", obj.getJsonObject("content"))
                .build();
        assertThrows(IllegalArgumentException.class, () -> SamplingCodec.toCreateMessageResponse(noModel));
    }
}
