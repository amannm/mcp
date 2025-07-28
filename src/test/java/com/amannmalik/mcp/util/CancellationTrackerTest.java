package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.RequestId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CancellationTrackerTest {
    @Test
    void cancelAndRelease() {
        CancellationTracker tracker = new CancellationTracker();
        RequestId id = new RequestId.NumericId(1);
        tracker.register(id);
        tracker.cancel(id, "stop");
        assertTrue(tracker.isCancelled(id));
        assertEquals("stop", tracker.reason(id));
        tracker.release(id);
        assertFalse(tracker.isCancelled(id));
        assertDoesNotThrow(() -> tracker.register(id));
    }
}
