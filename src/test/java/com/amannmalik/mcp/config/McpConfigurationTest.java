package com.amannmalik.mcp.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class McpConfigurationTest {
    @TempDir
    Path dir;

    @Test
    void environmentOverride() throws IOException {
        Path file = dir.resolve("cfg.yml");
        Files.writeString(file, "environments:\n  dev:\n    server:\n      transport:\n        port: 1234\n");
        McpConfiguration cfg = McpConfiguration.load(file, "dev");
        assertEquals(1234, cfg.server().transport().port());
    }

    @Test
    void reloadUpdatesCurrent() throws IOException {
        Path file = dir.resolve("r.yml");
        Files.writeString(file, "server:\n  transport:\n    port: 1\n");
        McpConfiguration.reload(file, "");
        assertEquals(1, McpConfiguration.current().server().transport().port());
        Files.writeString(file, "server:\n  transport:\n    port: 9\n");
        McpConfiguration.reload(file, "");
        assertEquals(9, McpConfiguration.current().server().transport().port());
    }

    @Test
    void invalidPortFails() throws IOException {
        Path file = dir.resolve("bad.yml");
        Files.writeString(file, "server:\n  transport:\n    port: -1\n");
        assertThrows(IllegalArgumentException.class, () -> McpConfiguration.load(file, ""));
    }
}
