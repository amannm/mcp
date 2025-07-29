package com.amannmalik.mcp;

import com.amannmalik.mcp.client.SimpleMcpClient;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.transport.StdioTransport;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import jakarta.json.Json;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpProtocolIntegrationTest {

    @TempDir
    Path tempDir;

    private static final String JAVA_BIN = System.getProperty("java.home") +
            File.separator + "bin" + File.separator + "java";
    
    private ExecutorService executor;
    private int httpPort;

    @BeforeAll
    void setup() {
        executor = Executors.newCachedThreadPool();
        httpPort = findAvailablePort();
    }

    @AfterAll
    void cleanup() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Complete MCP protocol lifecycle via CLI")
    void testCompleteProtocolLifecycleViaCli() throws Exception {
        ProcessBuilder serverBuilder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--stdio", "-v"
        );
        
        Process serverProcess = serverBuilder.start();
        
        try {
            assertEventually(() -> serverProcess.isAlive(), Duration.ofMillis(500),
                    "Server should start within 500ms");
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
            
            SimpleMcpClient client = new SimpleMcpClient(
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
            testProtocolOperationWithTimeout(() -> client.request("ping", Json.createObjectBuilder().build()), "ping", 10000);
            testProtocolOperationWithTimeout(() -> client.request("resources/list", Json.createObjectBuilder().build()), "resources/list", 5000);
            testProtocolOperationWithTimeout(() -> client.request("tools/list", Json.createObjectBuilder().build()), "tools/list", 5000);
            testProtocolOperationWithTimeout(() -> client.request("prompts/list", Json.createObjectBuilder().build()), "prompts/list", 5000);
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
    
    private void testProtocolOperationWithTimeout(Callable<JsonRpcMessage> operation, String operationName, long timeoutMs) {
        CompletableFuture<JsonRpcMessage> task = CompletableFuture.supplyAsync(() -> {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new RuntimeException(operationName + " operation failed", e);
            }
        });
        try {
            JsonRpcMessage response = task.get(timeoutMs, TimeUnit.MILLISECONDS);
            assertInstanceOf(JsonRpcResponse.class, response, operationName + " should return JsonRpcResponse");
        } catch (TimeoutException e) {
            fail(operationName + " operation timed out after " + timeoutMs + "ms");
        } catch (ExecutionException e) {
            fail(operationName + " operation failed: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(operationName + " operation was interrupted");
        }
    }

    private int findAvailablePort() {
        try (var socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return 8080;
        }
    }

    private void assertEventually(BooleanSupplier condition, Duration timeout, String message) {
        long endTime = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("Interrupted while waiting: " + message);
            }
        }
        fail(message);
    }

    @FunctionalInterface
    interface BooleanSupplier {
        boolean getAsBoolean();
    }
}