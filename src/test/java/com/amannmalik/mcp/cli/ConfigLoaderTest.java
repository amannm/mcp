package com.amannmalik.mcp.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class ConfigLoaderTest {
    @Test
    void loadsYamlHostConfig() throws Exception {
        Path p = Files.createTempFile("mcp", ".yaml");
        Files.writeString(p, "mode: host\nclients:\n  a: cmd\n");
        var cfg = ConfigLoader.load(p);
        if (cfg instanceof HostConfig h) {
            assertEquals(Map.of("a", "cmd"), h.clients());
        } else {
            fail("wrong type");
        }
    }

    @Test
    void loadsServerAuthString() throws Exception {
        Path p = Files.createTempFile("mcp", ".yaml");
        Files.writeString(p, "mode: server\nauthorizationServers: https://auth\n");
        var cfg = ConfigLoader.load(p);
        if (cfg instanceof ServerConfig s) {
            assertEquals(List.of("https://auth"), s.authorizationServers());
        } else {
            fail("wrong type");
        }
    }
}
