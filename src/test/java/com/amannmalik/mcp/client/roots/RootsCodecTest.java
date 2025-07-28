package com.amannmalik.mcp.client.roots;

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RootsCodecTest {
    @Test
    void rootRoundTrip() {
        Root root = new Root("file:///a", "A");
        JsonObject json = RootsCodec.toJsonObject(root);
        Root parsed = RootsCodec.toRoot(json);
        assertEquals(root, parsed);
    }

    @Test
    void listRoundTrip() {
        List<Root> list = List.of(new Root("file:///a", "A"), new Root("file:///b", "B"));
        JsonObject json = RootsCodec.toJsonObject(list);
        List<Root> parsed = RootsCodec.toRoots(json);
        assertEquals(list, parsed);
    }
}
