package com.amannmalik.mcp.client.elicitation;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElicitationCodecTest {
    @Test
    void requestRoundTrip() {
        JsonObject schema = Json.createObjectBuilder()
                .add("type", "object")
                .add("properties", Json.createObjectBuilder()
                        .add("name", Json.createObjectBuilder().add("type", "string")))
                .add("required", Json.createArrayBuilder().add("name"))
                .build();
        ElicitationRequest req = new ElicitationRequest("Provide name", schema);
        JsonObject json = ElicitationCodec.toJsonObject(req);
        ElicitationRequest parsed = ElicitationCodec.toRequest(json);
        assertEquals(req, parsed);
    }

    @Test
    void responseRoundTrip() {
        JsonObject content = Json.createObjectBuilder().add("name", "a").build();
        ElicitationResponse resp = new ElicitationResponse(ElicitationAction.ACCEPT, content);
        JsonObject json = ElicitationCodec.toJsonObject(resp);
        ElicitationResponse parsed = ElicitationCodec.toResponse(json);
        assertEquals(resp.action(), parsed.action());
        assertEquals(resp.content(), parsed.content());
    }
}
