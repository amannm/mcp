package com.amannmalik.mcp.jsonrpc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdTrackerTest {
    @Test
    void rejectsDuplicateIds() {
        var tracker = new IdTracker();
        tracker.register(new RequestId.NumericId(1));
        assertThrows(IllegalArgumentException.class, () -> tracker.register(new RequestId.NumericId(1)));
    }
}
