package com.amannmalik.mcp.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProgressTrackerTest {
    @Test
    void progressMustIncrease() {
        ProgressTracker tracker = new ProgressTracker();
        ProgressToken token = new ProgressToken.StringToken("t");
        tracker.register(token);
        tracker.update(new ProgressNotification(token, 0.1, 1.0, null));
        assertThrows(IllegalArgumentException.class, () ->
                tracker.update(new ProgressNotification(token, 0.05, 1.0, null)));
    }
}
