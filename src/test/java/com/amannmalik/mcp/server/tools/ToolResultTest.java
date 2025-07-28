package com.amannmalik.mcp.server.tools;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolResultTest {
    @Test
    void sanitizesText() {
        JsonArray arr = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("type", "text").add("text", "ok").build())
                .build();
        assertDoesNotThrow(() -> new ToolResult(arr, null, false));
    }

    @Test
    void rejectsControlChars() {
        JsonArray arr = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("type", "text").add("text", "bad\u0001").build())
                .build();
        assertThrows(IllegalArgumentException.class, () -> new ToolResult(arr, null, false));
    }
}
