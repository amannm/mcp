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

    @Test
    void requestAndResponse() {
        ListRootsRequest req = new ListRootsRequest();
        JsonObject reqJson = RootsCodec.toJsonObject(req);
        assertEquals(0, reqJson.size());
        assertNotNull(RootsCodec.toListRootsRequest(reqJson));

        ListRootsResponse resp = new ListRootsResponse(List.of(new Root("file:///x", "X")));
        JsonObject respJson = RootsCodec.toJsonObject(resp);
        ListRootsResponse parsed = RootsCodec.toListRootsResponse(respJson);
        assertEquals(resp.roots(), parsed.roots());
    }

    @Test
    void notificationRoundTrip() {
        RootsListChangedNotification n = new RootsListChangedNotification();
        JsonObject json = RootsCodec.toJsonObject(n);
        assertEquals(0, json.size());
        assertNotNull(RootsCodec.toRootsListChangedNotification(json));
    }
}
