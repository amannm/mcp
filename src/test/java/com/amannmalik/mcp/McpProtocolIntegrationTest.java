package com.amannmalik.mcp;

import com.amannmalik.mcp.client.DefaultMcpClient;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.transport.StdioTransport;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

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
    void testHappyPath() throws Exception {
        ProcessBuilder serverBuilder = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--stdio", "-v"
        );

        Process serverProcess = serverBuilder.start();

        try {
            assertEventually(() -> serverProcess.isAlive(), Duration.ofMillis(500), "Server should start within 500ms");
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

            DefaultMcpClient client = new DefaultMcpClient(
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

            testServerCapabilities(client);
            assertDoesNotThrow(client::ping, "ping should succeed");

            JsonRpcMessage resourcesResponse = testProtocolOperationExpectingSuccess(() -> client.request("resources/list", Json.createObjectBuilder().build()), "resources/list", 5000);
            JsonRpcMessage toolsResponse = testProtocolOperationExpectingSuccess(() -> client.request("tools/list", Json.createObjectBuilder().build()), "tools/list", 5000);
            JsonRpcMessage promptsResponse = testProtocolOperationExpectingSuccess(() -> client.request("prompts/list", Json.createObjectBuilder().build()), "prompts/list", 5000);

            validateListResponse(resourcesResponse, "resources");
            validateListResponse(toolsResponse, "tools");
            validateListResponse(promptsResponse, "prompts");

            testResourceFeatures(client);
            testToolFeatures(client);
            testPromptFeatures(client);
            testLoggingFeatures(client);
            testCompletionFeatures(client);
            testProgressTracking(client);
            testCancellation(client);
            testErrorHandling(client);

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

    private JsonRpcMessage testProtocolOperationWithTimeout(Callable<JsonRpcMessage> operation, String operationName, long timeoutMs) {
        CompletableFuture<JsonRpcMessage> task = CompletableFuture.supplyAsync(() -> {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new RuntimeException(operationName + " operation failed", e);
            }
        });
        try {
            return task.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            fail(operationName + " operation timed out after " + timeoutMs + "ms");
        } catch (ExecutionException e) {
            fail(operationName + " operation failed: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(operationName + " operation was interrupted");
        }
        throw new IllegalStateException();
    }

    private JsonRpcMessage testProtocolOperationExpectingSuccess(Callable<JsonRpcMessage> operation, String operationName, long timeoutMs) {
        JsonRpcMessage response = testProtocolOperationWithTimeout(operation, operationName, timeoutMs);
        assertInstanceOf(JsonRpcResponse.class, response, operationName + " should return JsonRpcResponse");
        return response;
    }

    private void validateListResponse(JsonRpcMessage response, String expectedArrayKey) {
        assertInstanceOf(JsonRpcResponse.class, response);
        JsonRpcResponse resp = (JsonRpcResponse) response;
        JsonObject result = resp.result();
        assertTrue(result.containsKey(expectedArrayKey), "Response should contain " + expectedArrayKey + " array");
        assertInstanceOf(JsonArray.class, result.get(expectedArrayKey), expectedArrayKey + " should be an array");
    }

    private void testResourceFeatures(DefaultMcpClient client) {
        JsonRpcMessage templatesResponse = testProtocolOperationWithTimeout(() -> client.request("resources/templates/list", Json.createObjectBuilder().build()), "resources/templates/list", 3000);
        JsonRpcMessage readResponse = testProtocolOperationWithTimeout(() -> client.request("resources/read", Json.createObjectBuilder().add("uri", "test://example").build()), "resources/read", 3000);
        JsonObject result = ((JsonRpcResponse) readResponse).result();
        assertTrue(result.containsKey("contents"), "Resource read should return contents");
    }

    private void testToolFeatures(DefaultMcpClient client) {
        JsonRpcMessage toolCallResponse = testProtocolOperationWithTimeout(() -> client.request("tools/call",
                Json.createObjectBuilder()
                        .add("name", "test_tool")
                        .add("arguments", Json.createObjectBuilder().build())
                        .build()), "tools/call", 3000);

        JsonObject result = ((JsonRpcResponse) toolCallResponse).result();
        assertTrue(result.containsKey("content"), "Tool call should return content");
    }

    private void testPromptFeatures(DefaultMcpClient client) {
        JsonRpcMessage promptGetResponse = testProtocolOperationWithTimeout(() -> client.request("prompts/get",
                Json.createObjectBuilder()
                        .add("name", "test_prompt")
                        .add("arguments", Json.createObjectBuilder().build())
                        .build()), "prompts/get", 3000);
        JsonObject result = ((JsonRpcResponse) promptGetResponse).result();
        assertTrue(result.containsKey("messages"), "Prompt get should return messages");
    }

    private void testLoggingFeatures(DefaultMcpClient client) {
        JsonRpcMessage logLevelResponse = testProtocolOperationWithTimeout(() -> client.request("logging/setLevel",
                Json.createObjectBuilder().add("level", "info").build()), "logging/setLevel", 3000);
    }

    private void testCompletionFeatures(DefaultMcpClient client) {
        JsonRpcMessage completionResponse = testProtocolOperationWithTimeout(() -> client.request("completion/complete",
                Json.createObjectBuilder()
                        .add("ref", Json.createObjectBuilder()
                                .add("type", "ref/prompt")
                                .add("name", "test_prompt")
                                .build())
                        .add("argument", Json.createObjectBuilder()
                                .add("name", "test_arg")
                                .add("value", "test")
                                .build())
                        .build()), "completion/complete", 3000);
        JsonObject result = ((JsonRpcResponse) completionResponse).result();
        assertTrue(result.containsKey("completion"), "Completion should return completion object");

    }

    private void testProgressTracking(DefaultMcpClient client) throws IOException {
        JsonObject paramsWithProgress = Json.createObjectBuilder()
                .add("_meta", Json.createObjectBuilder()
                        .add("progressToken", "test-progress-123")
                        .build())
                .build();

        JsonRpcMessage response = client.request("ping", paramsWithProgress);
        assertInstanceOf(JsonRpcResponse.class, response);
    }

    private void testCancellation(DefaultMcpClient client) throws IOException, InterruptedException {
        client.notify("notifications/cancelled",
                Json.createObjectBuilder()
                        .add("requestId", "123")
                        .add("reason", "Test cancellation")
                        .build());
        Thread.sleep(100);
    }

    private void testServerCapabilities(DefaultMcpClient client) {
        var expected = EnumSet.of(
                ServerCapability.RESOURCES,
                ServerCapability.TOOLS,
                ServerCapability.PROMPTS,
                ServerCapability.LOGGING,
                ServerCapability.COMPLETIONS
        );
        assertEquals(expected, client.serverCapabilities(), "Server capabilities should match");
    }

    private void testErrorHandling(DefaultMcpClient client) {
        JsonRpcMessage unknown = testProtocolOperationWithTimeout(
                () -> client.request("unknown/method", Json.createObjectBuilder().build()),
                "unknown/method", 3000);
        assertInstanceOf(JsonRpcError.class, unknown, "Unknown method should return error");
        assertEquals(JsonRpcErrorCode.METHOD_NOT_FOUND.code(),
                ((JsonRpcError) unknown).error().code());

        JsonRpcMessage badParams = testProtocolOperationWithTimeout(
                () -> client.request("resources/read", Json.createObjectBuilder().build()),
                "resources/read missing", 3000);
        assertInstanceOf(JsonRpcError.class, badParams, "Invalid params should return error");
        assertEquals(JsonRpcErrorCode.INVALID_PARAMS.code(),
                ((JsonRpcError) badParams).error().code());
    }

    private int findAvailablePort() {
        try (var socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            return 8080;
        }
    }

    private void assertEventually(Supplier<Boolean> condition, Duration timeout, String message) {
        long endTime = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < endTime) {
            if (condition.get()) {
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
}