package com.amannmalik.mcp.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProgressTrackerTest {
    @Test
    void enforcesUniquenessAndRelease() {
        ProgressTracker tracker = new ProgressTracker();
        ProgressToken t = new ProgressToken.NumericToken(1);
        tracker.register(t);
        assertThrows(IllegalArgumentException.class, () -> tracker.register(t));
        tracker.release(t);
        assertDoesNotThrow(() -> tracker.register(t));
    }
}
