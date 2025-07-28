package com.amannmalik.mcp.server.resources;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileSystemResourceProviderTest {
    @Test
    void listAndRead() throws Exception {
        Path dir = Files.createTempDirectory("fsprov");
        Path file = Files.writeString(dir.resolve("a.txt"), "hi");
        FileSystemResourceProvider p = new FileSystemResourceProvider(dir);
        ResourceList list = p.list(null);
        assertEquals(1, list.resources().size());
        Resource r = list.resources().getFirst();
        assertEquals(file.toUri().toString(), r.uri());
        ResourceBlock block = p.read(r.uri());
        assertTrue(block instanceof ResourceBlock.Text);
        assertEquals("hi", ((ResourceBlock.Text) block).text());
    }
}
