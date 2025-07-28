package com.amannmalik.mcp.server.resources;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemResourceProviderSubscriptionTest {
    @Test
    void subscribeReceivesUpdates() throws Exception {
        Path dir = Files.createTempDirectory("fsprovsub");
        Path file = Files.writeString(dir.resolve("a.txt"), "hi");
        FileSystemResourceProvider p = new FileSystemResourceProvider(dir);
        CountDownLatch latch = new CountDownLatch(1);
        ResourceSubscription sub = p.subscribe(file.toUri().toString(), u -> latch.countDown());
        Files.writeString(file, "bye");
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        sub.close();
    }
}
