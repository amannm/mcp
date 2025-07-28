package com.amannmalik.mcp.host;

import com.amannmalik.mcp.client.McpClient;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.security.ConsentManager;
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
        ConsentManager consents = new ConsentManager();
        Principal principal = new Principal("u", Set.of());
        HostProcess host = new HostProcess(allowAll, consents, principal);
        host.grantConsent("fake");
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
        ConsentManager consents = new ConsentManager();
        Principal principal = new Principal("u", Set.of());
        HostProcess host = new HostProcess(denyAll, consents, principal);
        host.grantConsent("fake");
        assertThrows(SecurityException.class, () -> host.register("x", new FakeClient()));
    }

    @Test
    void requiresUserConsent() {
        SecurityPolicy allowAll = c -> true;
        ConsentManager consents = new ConsentManager();
        Principal principal = new Principal("u", Set.of());
        HostProcess host = new HostProcess(allowAll, consents, principal);
        FakeClient client = new FakeClient();
        assertThrows(SecurityException.class, () -> host.register("one", client));
        host.grantConsent("fake");
        assertDoesNotThrow(() -> host.register("one", client));
    }
}
