package com.amannmalik.mcp.host;

import com.amannmalik.mcp.client.McpClient;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class HostProcessTest {
    private static class FakeClient implements McpClient {
        private final AtomicBoolean connected = new AtomicBoolean();

        @Override
        public ClientInfo info() {
            return new ClientInfo("fake", "Fake", "1");
        }

        @Override
        public void connect() {
            connected.set(true);
        }

        @Override
        public void disconnect() {
            connected.set(false);
        }

        @Override
        public boolean connected() {
            return connected.get();
        }

        @Override
        public String context() {
            return "ctx";
        }
    }

    @Test
    void registersAndAggregates() throws IOException {
        SecurityPolicy allowAll = c -> true;
        HostProcess host = new HostProcess(allowAll);
        FakeClient client = new FakeClient();
        host.register("one", client);
        assertTrue(client.connected());
        assertEquals(Set.of("one"), host.clientIds());
        assertEquals("ctx", host.aggregateContext());
        host.unregister("one");
        assertFalse(client.connected());
    }

    @Test
    void rejectsUnauthorizedClient() {
        SecurityPolicy denyAll = c -> false;
        HostProcess host = new HostProcess(denyAll);
        assertThrows(SecurityException.class, () -> host.register("x", new FakeClient()));
    }
}
