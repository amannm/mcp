package com.amannmalik.mcp.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {
    @Test
    void jsonServer() throws Exception {
        Path p = Files.createTempFile("cfg", ".json");
        Files.writeString(p, "{\"mode\":\"server\",\"transport\":\"http\",\"port\":1234}");
        CliConfig cfg = ConfigLoader.load(p);
        assertTrue(cfg instanceof ServerConfig);
        ServerConfig sc = (ServerConfig) cfg;
        assertEquals(TransportType.HTTP, sc.transport());
        assertEquals(1234, sc.port());
    }

    @Test
    void yamlClient() throws Exception {
        Path p = Files.createTempFile("cfg", ".yaml");
        Files.writeString(p, "mode: client\ntransport: stdio\ncommand: echo");
        CliConfig cfg = ConfigLoader.load(p);
        assertTrue(cfg instanceof ClientConfig);
        ClientConfig cc = (ClientConfig) cfg;
        assertEquals(TransportType.STDIO, cc.transport());
        assertEquals("echo", cc.command());
    }
}
