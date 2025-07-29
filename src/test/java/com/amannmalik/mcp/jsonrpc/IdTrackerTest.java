package com.amannmalik.mcp.jsonrpc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdTrackerTest {
    @Test
    void testRegisterAndRelease() {
        IdTracker tracker = new IdTracker();
        RequestId id = new RequestId.NumericId(1);
        tracker.register(id);
        assertThrows(IllegalArgumentException.class, () -> tracker.register(id));
        tracker.release(id);
        assertDoesNotThrow(() -> tracker.register(id));
    }
}
