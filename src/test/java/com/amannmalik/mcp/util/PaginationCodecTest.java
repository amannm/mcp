package com.amannmalik.mcp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class PaginationCodecTest {
    @Test
    void nullRequest() {
        var r = PaginationCodec.toPaginatedRequest(null);
        assertNull(r.cursor());
        assertNull(r._meta());
    }

    @Test
    void nullResult() {
        var r = PaginationCodec.toPaginatedResult(null);
        assertNull(r.nextCursor());
        assertNull(r._meta());
    }
}

