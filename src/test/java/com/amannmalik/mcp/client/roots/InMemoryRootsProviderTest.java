package com.amannmalik.mcp.client.roots;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRootsProviderTest {
    @Test
    void listAddRemoveAndNotify() throws Exception {
        Root r1 = new Root("file:///a", "A");
        InMemoryRootsProvider provider = new InMemoryRootsProvider(List.of(r1));
        assertEquals(List.of(r1), provider.list());

        AtomicInteger called = new AtomicInteger();
        RootsSubscription sub = provider.subscribe(called::incrementAndGet);

        Root r2 = new Root("file:///b", "B");
        provider.add(r2);
        assertEquals(2, provider.list().size());
        assertEquals(1, called.get());

        provider.remove("file:///a");
        assertEquals(List.of(r2), provider.list());
        assertEquals(2, called.get());

        sub.close();
        provider.add(new Root("file:///c", "C"));
        assertEquals(2, called.get());
    }
}
