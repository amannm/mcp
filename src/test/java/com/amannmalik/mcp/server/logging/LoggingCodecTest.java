package com.amannmalik.mcp.server.logging;

import jakarta.json.JsonValue;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoggingCodecTest {
    @Test
    void notificationRoundTrip() {
        LoggingNotification n = new LoggingNotification(LoggingLevel.ERROR, "test", JsonValue.TRUE);
        JsonObject json = LoggingCodec.toJsonObject(n);
        LoggingNotification parsed = LoggingCodec.toLoggingNotification(json);
        assertEquals(n, parsed);
    }

    @Test
    void setLevelRoundTrip() {
        SetLevelRequest req = new SetLevelRequest(LoggingLevel.WARNING);
        JsonObject json = LoggingCodec.toJsonObject(req);
        SetLevelRequest parsed = LoggingCodec.toSetLevelRequest(json);
        assertEquals(req, parsed);
    }
}
