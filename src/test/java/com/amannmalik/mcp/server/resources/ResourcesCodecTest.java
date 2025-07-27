package com.amannmalik.mcp.server.resources;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourcesCodecTest {
    @Test
    void resourceRoundTrip() {
        ResourceAnnotations ann = new ResourceAnnotations(EnumSet.of(Audience.USER), 0.5, Instant.parse("2025-01-01T00:00:00Z"));
        Resource res = new Resource("file:///a.txt", "a.txt", "A", "", "text/plain", 12L, ann);
        JsonObject json = ResourcesCodec.toJsonObject(res);
        Resource parsed = ResourcesCodec.toResource(json);
        assertEquals(res, parsed);
    }

    @Test
    void contentRoundTrip() {
        ResourceAnnotations ann = new ResourceAnnotations(EnumSet.of(Audience.ASSISTANT), null, null);
        ResourceBlock.Text block = new ResourceBlock.Text("file:///b.txt", "b.txt", null, "text/plain", "hi", ann);
        JsonObject json = ResourcesCodec.toJsonObject(block);
        ResourceBlock parsed = ResourcesCodec.toResourceBlock(json);
        assertEquals(block, parsed);
    }
}
