package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.RequestId;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CancellationCodecTest {
    @Test
    void roundTrip() {
        CancelledNotification n = new CancelledNotification(
                new RequestId.NumericId(1),
                "done"
        );
        JsonObject json = CancellationCodec.toJsonObject(n);
        CancelledNotification parsed = CancellationCodec.toCancelledNotification(json);
        assertEquals(n, parsed);
    }
}
