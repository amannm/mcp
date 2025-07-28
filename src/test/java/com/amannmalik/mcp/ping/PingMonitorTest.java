package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.client.SimpleMcpClient;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.StdioTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class PingMonitorTest {
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
    void aliveAndDead() throws Exception {
        SimpleMcpClient client = new SimpleMcpClient(
                new ClientInfo("client", "Client", "1"),
                EnumSet.noneOf(com.amannmalik.mcp.lifecycle.ClientCapability.class),
                clientTransport);
        client.connect();
        assertTrue(PingMonitor.isAlive(client, 100));
        server.close();
        serverThread.join();
        assertFalse(PingMonitor.isAlive(client, 100));
    }

    private static class TestServer extends McpServer {
        TestServer(StdioTransport transport) {
            super(EnumSet.noneOf(com.amannmalik.mcp.lifecycle.ServerCapability.class), transport);
        }
    }
}
