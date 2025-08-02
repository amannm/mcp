package com.amannmalik.mcp.jsonrpc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class RequestIdTest {
    @Test
    void parse() {
        RequestId numeric = RequestId.parse("7");
        assertTrue(numeric instanceof RequestId.NumericId);
        assertEquals("7", numeric.toString());

        RequestId string = RequestId.parse("\"x\"");
        assertTrue(string instanceof RequestId.StringId);
        assertEquals("x", string.toString());

        RequestId nullId = RequestId.parse("null");
        assertSame(RequestId.NullId.INSTANCE, nullId);
        assertEquals("null", nullId.toString());
    }
}
