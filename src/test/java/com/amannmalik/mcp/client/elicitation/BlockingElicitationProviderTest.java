package com.amannmalik.mcp.client.elicitation;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockingElicitationProviderTest {
    @Test
    void respondAndElicit() throws Exception {
        BlockingElicitationProvider provider = new BlockingElicitationProvider();
        JsonObject schema = Json.createObjectBuilder().add("type", "object").build();
        ElicitationRequest req = new ElicitationRequest("msg", schema);

        ElicitationResponse resp = new ElicitationResponse(ElicitationAction.ACCEPT,
                Json.createObjectBuilder().add("a", 1).build());
        provider.respond(resp);

        ElicitationResponse result = provider.elicit(req, 10);
        assertEquals(resp, result);
    }

    @Test
    void timeoutReturnsCancel() throws Exception {
        BlockingElicitationProvider provider = new BlockingElicitationProvider();
        JsonObject schema = Json.createObjectBuilder().add("type", "object").build();
        ElicitationRequest req = new ElicitationRequest("msg", schema);

        ElicitationResponse result = provider.elicit(req, 1);
        assertEquals(ElicitationAction.CANCEL, result.action());
        assertNull(result.content());
    }
}
