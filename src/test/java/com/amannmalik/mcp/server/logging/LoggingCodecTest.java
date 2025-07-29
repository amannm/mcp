package com.amannmalik.mcp.server.logging;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoggingCodecTest {
    @Test
    void roundTripNotification() {
        LoggingNotification note = new LoggingNotification(
                LoggingLevel.WARNING,
                "test",
                Json.createValue("message"));
        JsonObject obj = LoggingCodec.toJsonObject(note);
        assertEquals("warning", obj.getString("level"));
        assertEquals("test", obj.getString("logger"));
        assertEquals("message", obj.getString("data"));

        LoggingNotification parsed = LoggingCodec.toLoggingNotification(obj);
        assertEquals(note, parsed);
    }

    @Test
    void roundTripSetLevel() {
        SetLevelRequest req = new SetLevelRequest(LoggingLevel.ERROR);
        JsonObject obj = LoggingCodec.toJsonObject(req);
        assertEquals("error", obj.getString("level"));
        SetLevelRequest parsed = LoggingCodec.toSetLevelRequest(obj);
        assertEquals(req, parsed);
    }
}
