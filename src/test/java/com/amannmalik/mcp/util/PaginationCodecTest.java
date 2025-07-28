package com.amannmalik.mcp.util;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaginationCodecTest {
    @Test
    void roundTripRequest() {
        PaginatedRequest req = new PaginatedRequest("c1");
        JsonObject json = PaginationCodec.toJsonObject(req);
        PaginatedRequest parsed = PaginationCodec.toPaginatedRequest(json);
        assertEquals(req, parsed);
    }

    @Test
    void roundTripResult() {
        PaginatedResult res = new PaginatedResult("c2");
        JsonObject json = PaginationCodec.toJsonObject(res);
        PaginatedResult parsed = PaginationCodec.toPaginatedResult(json);
        assertEquals(res, parsed);
    }
}
