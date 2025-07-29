package com.amannmalik.mcp;

import com.amannmalik.mcp.client.McpClient;
import com.amannmalik.mcp.client.elicitation.BlockingElicitationProvider;
import com.amannmalik.mcp.client.elicitation.ElicitationAction;
import com.amannmalik.mcp.client.elicitation.ElicitationResponse;
import com.amannmalik.mcp.jsonrpc.JsonRpcCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.util.ProgressNotification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
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

            BlockingElicitationProvider elicitation = new BlockingElicitationProvider();
            elicitation.respond(new ElicitationResponse(ElicitationAction.CANCEL, null));
            McpClient client = new McpClient(
                    new ClientInfo("test-client", "Test Client", "1.0"),
                    EnumSet.allOf(ClientCapability.class),
                    clientTransport,
                    null,
                    null,
                    elicitation
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

            var progressEvents = new CopyOnWriteArrayList<ProgressNotification>();
            client.setProgressListener(progressEvents::add);

            JsonObject meta = Json.createObjectBuilder()
                    .add("progressToken", "tok")
                    .build();
            JsonObject params = Json.createObjectBuilder()
                    .add("_meta", meta)
                    .build();
            JsonRpcMessage m = client.request("resources/list", params);
            assertInstanceOf(JsonRpcResponse.class, m, "resources/list should succeed");
            var list = ((JsonRpcResponse) m).result().getJsonArray("resources");
            assertEquals(1, list.size(), "default resources count");
            assertEquals("test://example", list.getJsonObject(0).getString("uri"));
            Thread.sleep(100);
            assertEquals(2, progressEvents.size(), "progress notifications expected");

            m = client.request("resources/read", Json.createObjectBuilder()
                    .add("uri", "test://example")
                    .build());
            assertInstanceOf(JsonRpcResponse.class, m);
            var block = ((JsonRpcResponse) m).result()
                    .getJsonArray("contents").getJsonObject(0);
            assertEquals("hello", block.getString("text"));

            m = client.request("resources/templates/list", Json.createObjectBuilder().build());
            assertInstanceOf(JsonRpcResponse.class, m);
            var templates = ((JsonRpcResponse) m).result().getJsonArray("resourceTemplates");
            assertEquals(1, templates.size());

            m = client.request("tools/list", Json.createObjectBuilder().build());
            assertInstanceOf(JsonRpcResponse.class, m);
            var tools = ((JsonRpcResponse) m).result().getJsonArray("tools");
            assertEquals(1, tools.size());
            assertEquals("test_tool", tools.getJsonObject(0).getString("name"));

            m = client.request("tools/call", Json.createObjectBuilder()
                    .add("name", "test_tool")
                    .build());
            assertInstanceOf(JsonRpcResponse.class, m);
            var content = ((JsonRpcResponse) m).result()
                    .getJsonArray("content").getJsonObject(0);
            assertEquals("ok", content.getString("text"));

            m = client.request("prompts/list", Json.createObjectBuilder().build());
            assertInstanceOf(JsonRpcResponse.class, m);
            var prompts = ((JsonRpcResponse) m).result().getJsonArray("prompts");
            assertEquals(1, prompts.size());

            m = client.request("prompts/get", Json.createObjectBuilder()
                    .add("name", "test_prompt")
                    .add("arguments", Json.createObjectBuilder().add("test_arg", "v").build())
                    .build());
            assertInstanceOf(JsonRpcResponse.class, m);
            var messages = ((JsonRpcResponse) m).result().getJsonArray("messages");
            assertEquals("hello", messages.getJsonObject(0).getJsonObject("content").getString("text"));

            m = client.request("completion/complete", Json.createObjectBuilder()
                    .add("ref", Json.createObjectBuilder()
                            .add("type", "ref/prompt")
                            .add("name", "test_prompt")
                            .build())
                    .add("argument", Json.createObjectBuilder()
                            .add("name", "test_arg")
                            .add("value", "")
                            .build())
                    .build());
            assertInstanceOf(JsonRpcResponse.class, m);
            var values = ((JsonRpcResponse) m).result()
                    .getJsonObject("completion")
                    .getJsonArray("values");
            assertEquals(List.of("test_completion"), values.getValuesAs(jakarta.json.JsonString.class).stream().map(jakarta.json.JsonString::getString).toList());

            m = client.request("logging/setLevel", Json.createObjectBuilder().add("level", "debug").build());
            assertInstanceOf(JsonRpcResponse.class, m);

            m = client.request("resources/read", Json.createObjectBuilder().add("uri", "bad://uri").build());
            assertInstanceOf(JsonRpcError.class, m);
            assertEquals(-32002, ((JsonRpcError) m).error().code());

            m = client.request("tools/call", Json.createObjectBuilder().add("name", "nope").build());
            assertInstanceOf(JsonRpcError.class, m);
            assertEquals(-32602, ((JsonRpcError) m).error().code());

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
            assertInstanceOf(JsonRpcError.class, msg);
            assertEquals(-32000, ((JsonRpcError) msg).error().code());
        } finally {
            if (serverProcess.isAlive()) {
                serverProcess.destroyForcibly();
                serverProcess.waitFor(2, TimeUnit.SECONDS);
            }
        }
    }
}