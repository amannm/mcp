package com.amannmalik.mcp.logging;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SetLevelRequestCodecTest {
    @Test
    void rejectsNullObject() {
        var ex = assertThrows(IllegalArgumentException.class, () -> SetLevelRequest.CODEC.fromJson(null));
        assertEquals("object required", ex.getMessage());
    }
}
