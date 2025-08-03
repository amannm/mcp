package com.amannmalik.mcp.logging;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoggingCodecTest {
    @Test
    void rejectsNullObject() {
        var ex = assertThrows(IllegalArgumentException.class, () -> LoggingCodec.toSetLevelRequest(null));
        assertEquals("object required", ex.getMessage());
    }
}
