package com.amannmalik.mcp;

import com.amannmalik.mcp.client.McpClient;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.jsonrpc.JsonRpcCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.RequestId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpConformanceTest {

    private static final String JAVA_BIN = System.getProperty("java.home") +
            File.separator + "bin" + File.separator + "java";

    private ExecutorService executor;

    @BeforeAll
    void setup() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @AfterAll
    void cleanup() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void testRealisticUsage() throws Exception {
        ProcessBuilder serverBuilder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--stdio", "-v"
        );
        Process serverProcess = serverBuilder.start();
        try {
            boolean finished = false;
            long endTime = System.currentTimeMillis() + Duration.ofMillis(500).toMillis();
            while (System.currentTimeMillis() < endTime) {
                if (serverProcess.isAlive()) {
                    finished = true;
                    break;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    fail("Interrupted while waiting: " + "Server should start within 500ms");
                }
            }
            if (!finished) {
                fail("Server should start within 500ms");
            }
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(serverProcess.getErrorStream()));
            String errorLine = null;
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 1000) {
                if (errorReader.ready()) {
                    errorLine = errorReader.readLine();
                    if (errorLine != null && errorLine.contains("Exception")) {
                        fail("Server failed to start: " + errorLine);
                    }
                }
                Thread.sleep(10);
            }
            StdioTransport clientTransport = new StdioTransport(
                    serverProcess.getInputStream(),
                    serverProcess.getOutputStream()
            );

            McpClient client = new McpClient(
                    new ClientInfo("test-client", "Test Client", "1.0"),
                    EnumSet.allOf(ClientCapability.class),
                    clientTransport
            );
            CompletableFuture<Void> connectTask = CompletableFuture.runAsync(() -> {
                try {
                    client.connect();
                } catch (Exception e) {
                    throw new RuntimeException("Client connection failed", e);
                }
            });

            try {
                connectTask.get(2, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                fail("Client connection timed out after 2 seconds");
            }
            Thread.sleep(1500);
            assertTrue(serverProcess.isAlive(), "Server process should be alive before protocol tests");

            var expected = EnumSet.of(
                    ServerCapability.RESOURCES,
                    ServerCapability.TOOLS,
                    ServerCapability.PROMPTS,
                    ServerCapability.LOGGING,
                    ServerCapability.COMPLETIONS
            );
            assertEquals(expected, client.serverCapabilities(), "Server capabilities should match");
            assertDoesNotThrow(() -> client.ping(), "ping should succeed");

            // TODO: Compactly test all features in various realistic MCP usage scenarios...

            CompletableFuture<Void> disconnectTask = CompletableFuture.runAsync(() -> {
                try {
                    client.disconnect();
                } catch (Exception e) {
                    throw new RuntimeException("Client disconnect failed", e);
                }
            });
            try {
                disconnectTask.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                fail("Client disconnect timed out after 1 second");
            }
        } finally {
            if (serverProcess.isAlive()) {
                serverProcess.destroyForcibly();
                boolean terminated = serverProcess.waitFor(2, TimeUnit.SECONDS);
                if (!terminated) {
                    fail("Server process failed to terminate within 2 seconds");
                }
            }
        }
    }

    @Test
    void testRejectRequestBeforeInitialization() throws Exception {
        ProcessBuilder serverBuilder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--stdio", "-v"
        );
        Process serverProcess = serverBuilder.start();
        try {
            long endTime = System.currentTimeMillis() + 500;
            while (!serverProcess.isAlive() && System.currentTimeMillis() < endTime) {
                Thread.sleep(10);
            }
            StdioTransport transport = new StdioTransport(
                    serverProcess.getInputStream(),
                    serverProcess.getOutputStream()
            );
            JsonRpcRequest req = new JsonRpcRequest(
                    new RequestId.NumericId(1), "roots/list", null);
            transport.send(JsonRpcCodec.toJsonObject(req));
            JsonRpcMessage msg = JsonRpcCodec.fromJsonObject(transport.receive());
            assertTrue(msg instanceof JsonRpcError);
            assertEquals(-32000, ((JsonRpcError) msg).error().code());
        } finally {
            if (serverProcess.isAlive()) {
                serverProcess.destroyForcibly();
                serverProcess.waitFor(2, TimeUnit.SECONDS);
            }
        }
    }
}