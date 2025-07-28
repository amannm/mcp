package com.amannmalik.mcp.util;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressCodecTest {
    @Test
    void roundTrip() {
        ProgressNotification n = new ProgressNotification(
                new ProgressToken.StringToken("tok"),
                0.5,
                1.0,
                "half"
        );
        JsonObject json = ProgressCodec.toJsonObject(n);
        ProgressNotification parsed = ProgressCodec.toProgressNotification(json);
        assertEquals(n, parsed);
    }
}
