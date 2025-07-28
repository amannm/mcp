package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.client.SimpleMcpClient;
import com.amannmalik.mcp.security.PrivacyBoundaryEnforcer;
import com.amannmalik.mcp.transport.StdioTransport;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResourceServerAccessTest {
    private StdioTransport clientTransport;
    private StdioTransport serverTransport;
    private ResourceServer server;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws IOException {
        Resource r1 = new Resource("file:///a.txt", "a.txt", null, null, "text/plain", null,
                new ResourceAnnotations(Set.of(Audience.USER), null, Instant.now()));
        Resource r2 = new Resource("file:///b.txt", "b.txt", null, null, "text/plain", null,
                new ResourceAnnotations(Set.of(Audience.ASSISTANT), null, Instant.now()));
        Map<String, ResourceBlock> contents = Map.of(
                r1.uri(), new ResourceBlock.Text(r1.uri(), r1.name(), null, r1.mimeType(), "a", r1.annotations()),
                r2.uri(), new ResourceBlock.Text(r2.uri(), r2.name(), null, r2.mimeType(), "b", r2.annotations())
        );
        InMemoryResourceProvider provider = new InMemoryResourceProvider(java.util.List.of(r1, r2), contents, java.util.List.of());
        PrivacyBoundaryEnforcer enforcer = new PrivacyBoundaryEnforcer();
        enforcer.allow("u", Audience.USER);

        PipedInputStream clientIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(clientIn);
        PipedInputStream serverIn = new PipedInputStream();
        PipedOutputStream clientOut = new PipedOutputStream(serverIn);
        clientTransport = new StdioTransport(clientIn, clientOut);
        serverTransport = new StdioTransport(serverIn, serverOut);
        server = new ResourceServer(provider, serverTransport, enforcer, new Principal("u", Set.of()));
        serverThread = new Thread(() -> {
            try { server.serve(); } catch (IOException ignored) {}
        });
        serverThread.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        clientTransport.close();
        server.close();
        serverThread.join();
    }

    @Test
    void enforcesAudience() throws Exception {
        SimpleMcpClient client = new SimpleMcpClient(new ClientInfo("c", "C", "1"),
                EnumSet.of(ClientCapability.EXPERIMENTAL), clientTransport);
        client.connect();
        JsonRpcMessage listMsg = client.request("resources/list", Json.createObjectBuilder().build());
        JsonArray resources = ((JsonRpcResponse) listMsg).result().getJsonArray("resources");
        assertEquals(1, resources.size());

        JsonRpcMessage err = client.request("resources/read", Json.createObjectBuilder().add("uri", "file:///b.txt").build());
        assertTrue(err instanceof JsonRpcError);
        client.disconnect();
    }
}
