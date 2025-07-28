package com.amannmalik.mcp.cli;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigLoaderTest {
    @Test
    void loadsJson() throws Exception {
        Path p = Files.createTempFile("conf", ".json");
        try {
            Files.writeString(p, "{\"a\":1}");
            JsonObject o = ConfigLoader.load(p);
            assertEquals(1, o.getInt("a"));
        } finally {
            Files.deleteIfExists(p);
        }
    }

    @Test
    void loadsYaml() throws Exception {
        Path p = Files.createTempFile("conf", ".yaml");
        try {
            Files.writeString(p, "a: 1\n");
            JsonObject o = ConfigLoader.load(p);
            assertEquals(1, o.getInt("a"));
        } finally {
            Files.deleteIfExists(p);
        }
    }
}
