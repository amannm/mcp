package com.amannmalik.mcp.client;

import com.amannmalik.mcp.client.elicitation.ElicitationAction;
import com.amannmalik.mcp.client.elicitation.ElicitationResponse;
import jakarta.json.Json;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ElicitationResponseTest {
    @Test
    void rejectNestedContent() {
        var content = Json.createObjectBuilder()
                .add("foo", Json.createObjectBuilder().add("bar", 1).build())
                .build();
        assertThrows(IllegalArgumentException.class,
                () -> new ElicitationResponse(ElicitationAction.ACCEPT, content));
    }
}
