package com.amannmalik.mcp.client;

import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class SimpleMcpClientTest {
    private StdioTransport clientTransport;
    private StdioTransport serverTransport;
    private TestServer server;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws IOException {
        PipedInputStream clientIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(clientIn);
        PipedInputStream serverIn = new PipedInputStream();
        PipedOutputStream clientOut = new PipedOutputStream(serverIn);
        clientTransport = new StdioTransport(clientIn, clientOut);
        serverTransport = new StdioTransport(serverIn, serverOut);
        server = new TestServer(serverTransport);
        serverThread = new Thread(() -> {
            try {
                server.serve();
            } catch (IOException ignored) {
            }
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
    void initializationAndRequest() throws Exception {
        SimpleMcpClient client = new SimpleMcpClient(
                new ClientInfo("client", "Client", "1"),
                EnumSet.of(ClientCapability.ROOTS),
                clientTransport);
        client.connect();
        assertTrue(client.connected());
        assertTrue(client.serverCapabilities().isEmpty());
        JsonRpcMessage msg = client.request("ping", Json.createObjectBuilder().build());
        assertTrue(msg instanceof JsonRpcResponse);
        JsonObject result = ((JsonRpcResponse) msg).result();
        assertEquals(Json.createObjectBuilder().add("pong", true).build(), result);
        client.disconnect();
        assertFalse(client.connected());
    }

    private static class TestServer extends McpServer {
        TestServer(Transport t) {
            super(EnumSet.noneOf(ServerCapability.class), t);
            registerRequestHandler("ping", req -> new JsonRpcResponse(req.id(), Json.createObjectBuilder().add("pong", true).build()));
        }
    }
}
