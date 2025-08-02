package com.amannmalik.mcp.util;

import com.amannmalik.mcp.client.roots.Root;
import jakarta.json.Json;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class RootCheckerTest {
    @Test
    void nullUriIsOutsideRoots() throws Exception {
        var dir = Files.createTempDirectory("root");
        var root = new Root(dir.toUri().toString(), null, Json.createObjectBuilder().build());
        assertFalse(RootChecker.withinRoots(null, List.of(root)));
    }
}
