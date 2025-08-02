package com.amannmalik.mcp;

import com.amannmalik.mcp.client.McpClient;
import com.amannmalik.mcp.client.elicitation.*;
import com.amannmalik.mcp.client.roots.InMemoryRootsProvider;
import com.amannmalik.mcp.client.roots.Root;
import com.amannmalik.mcp.client.sampling.CreateMessageResponse;
import com.amannmalik.mcp.client.sampling.SamplingProvider;
import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.security.OriginValidator;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import jakarta.json.Json;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public final class McpConformanceSteps {
    private static final String JAVA_BIN = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

    private Process serverProcess;
    private StreamableHttpTransport serverTransport;
    private CompletableFuture<Void> serverTask;
    private McpClient client;
    private final Map<String, JsonRpcMessage> responses = new ConcurrentHashMap<>();

    @Before
    public void setup() throws Exception {
        // Initial setup without transport-specific configuration
        // Transport setup happens in @Given step
    }

    private void setupTestConfiguration(String transport) {
        String configFile = "http".equals(transport) ? "/mcp-test-config-http.yaml" : "/mcp-test-config.yaml";
        String testConfigPath = getClass().getResource(configFile).getPath();
        System.setProperty("test.config.path", testConfigPath);
        System.out.println("DEBUG: Using transport: " + transport + ", config: " + configFile);
    }

    @After
    public void cleanup() throws Exception {
        if (client != null) client.disconnect();
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroyForcibly();
            serverProcess.waitFor(2, TimeUnit.SECONDS);
        }
        if (serverTask != null && !serverTask.isDone()) {
            serverTask.cancel(true);
        }
        if (serverTransport != null) serverTransport.close();
    }

    @Given("a running MCP server using {word} transport")
    public void setupTransport(String transport) throws Exception {
        System.setProperty("mcp.test.transport", transport);
        setupTestConfiguration(transport);
        System.out.println("DEBUG: Creating transport: " + transport);
        client = createClient(createTransport(transport));
        client.connect();
        System.out.println("DEBUG: Client connected successfully with " + transport + " transport");
    }

    @Then("capabilities should be advertised and ping succeeds")
    public void verifyCapabilitiesAndPing() {
        var expected = EnumSet.of(
                ServerCapability.RESOURCES, ServerCapability.TOOLS,
                ServerCapability.PROMPTS, ServerCapability.LOGGING,
                ServerCapability.COMPLETIONS
        );
        assertEquals(expected, client.serverCapabilities());
        assertTrue(client.toolsListChangedSupported());
        assertDoesNotThrow(() -> client.ping());
    }

    @When("testing core functionality")
    public void testCoreFunctionality(DataTable table) throws Exception {
        for (var row : table.asMaps()) {
            String operation = row.get("operation");
            String parameter = row.get("parameter");
            String expected = row.get("expected_result");
            JsonRpcMessage response = executeOperation(operation, parameter);
            verifyResult(operation, response, expected, parameter);
        }
    }

    @When("testing error conditions")
    public void testErrorConditions(DataTable table) throws Exception {
        for (var row : table.asMaps()) {
            String operation = row.get("operation");
            String parameter = row.get("parameter");
            int expectedCode = Integer.parseInt(row.get("expected_error_code"));
            JsonRpcMessage response = executeOperation(operation, parameter);
            assertInstanceOf(JsonRpcError.class, response);
            assertEquals(expectedCode, ((JsonRpcError) response).error().code());
        }
    }

    @When("the client disconnects")
    public void disconnect() throws Exception {
        client.disconnect();
        if (serverTask != null) serverTask.cancel(true);
        if (serverTransport != null) serverTransport.close();
    }

    @Then("the server terminates cleanly")
    public void verifyTermination() throws Exception {
        if (serverProcess != null) {
            if (serverProcess.isAlive()) {
                serverProcess.waitFor(2, TimeUnit.SECONDS);
                if (serverProcess.isAlive()) serverProcess.destroy();
            }
            assertFalse(serverProcess.isAlive());
        } else if (serverTask != null) {
            if (!serverTask.isCancelled()) {
                serverTask.get(2, TimeUnit.SECONDS);
            }
            assertTrue(serverTask.isDone());
        }
    }

    private String getTransportType() {
        return System.getProperty("mcp.test.transport", "stdio");
    }

    private Transport createTransport(String type) throws Exception {
        System.out.println("DEBUG: createTransport called with type: " + type);
        if ("http".equals(type)) {
            System.out.println("DEBUG: Creating HTTP transport");
            return createHttpTransport();
        } else {
            System.out.println("DEBUG: Creating stdio transport");
            return createStdioTransport();
        }
    }

    private Transport createHttpTransport() throws Exception {
        System.out.println("DEBUG: Starting StreamableHttpTransport server...");
        serverTransport = new StreamableHttpTransport(0,
                new OriginValidator(Set.of("http://localhost", "http://127.0.0.1")),
                null,
                "https://example.com/.well-known/oauth-protected-resource",
                List.of("https://auth.example.com"));
        System.out.println("DEBUG: StreamableHttpTransport server started on port: " + serverTransport.port());
        serverTask = CompletableFuture.runAsync(() -> {
            try (var server = new McpServer(serverTransport, null)) {
                System.out.println("DEBUG: McpServer starting to serve...");
                server.serve();
            } catch (Exception e) {
                System.err.println("DEBUG: McpServer error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, runnable -> Thread.ofVirtual().start(runnable));
        String clientUrl = "http://127.0.0.1:" + serverTransport.port() + "/";
        System.out.println("DEBUG: Creating HTTP client transport with URL: " + clientUrl);
        return new StreamableHttpClientTransport(URI.create(clientUrl));
    }

    private Transport createStdioTransport() throws Exception {
        var args = new ArrayList<String>();
        args.add(JAVA_BIN);
        args.addAll(List.of("-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--stdio", "--test-mode", "-v"));

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(false);
        serverProcess = pb.start();

        Thread errorReader = new Thread(() -> {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(serverProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println("Server error: " + line);
                    fail(line);
                }
            } catch (Exception e) {
                System.err.println("Error reading server stderr: " + e.getMessage());
                fail(e.getMessage());
            }
        });
        errorReader.setDaemon(true);
        errorReader.start();

        long end = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < end) {
            if (serverProcess.isAlive()) break;
            Thread.sleep(50);
        }

        if (!serverProcess.isAlive()) {
            System.err.println("Server process exit code: " + serverProcess.exitValue());
        }
        assertTrue(serverProcess.isAlive(), "server failed to start");

        return new StdioTransport(serverProcess.getInputStream(), serverProcess.getOutputStream());
    }

    private McpClient createClient(Transport transport) {
        BlockingElicitationProvider elicitation = new BlockingElicitationProvider();
        elicitation.respond(new ElicitResult(ElicitationAction.CANCEL, null, null));

        SamplingProvider sampling = (_, _) -> new CreateMessageResponse(
                Role.ASSISTANT, new ContentBlock.Text("ok", null, null),
                "mock-model", "endTurn", null);

        InMemoryRootsProvider rootsProvider = new InMemoryRootsProvider(
                List.of(new Root("file:///tmp", "Test Root", null)));

        return new McpClient(
                new ClientInfo("test-client", "Test Client", "1.0"),
                EnumSet.allOf(ClientCapability.class), transport,
                sampling, rootsProvider, elicitation);
    }

    private JsonRpcMessage executeOperation(String operation, String parameter) throws Exception {
        return switch (operation) {
            case "list_resources" -> client.request("resources/list",
                    Json.createObjectBuilder().add("_meta",
                            Json.createObjectBuilder().add("progressToken", "tok")).build());
            case "resource_metadata" -> client.request("resources/list", Json.createObjectBuilder().build());
            case "read_resource" -> client.request("resources/read",
                    Json.createObjectBuilder().add("uri", parameter).build());
            case "list_templates" -> client.request("resources/templates/list", Json.createObjectBuilder().build());
            case "list_tools" -> client.request("tools/list", Json.createObjectBuilder().build());
            case "list_tools_schema" -> client.request("tools/list", Json.createObjectBuilder().build());
            case "call_tool" -> client.request("tools/call",
                    Json.createObjectBuilder().add("name", parameter).build());
            case "list_prompts" -> client.request("prompts/list", Json.createObjectBuilder().build());
            case "get_prompt" -> client.request("prompts/get",
                    Json.createObjectBuilder().add("name", parameter)
                            .add("arguments", Json.createObjectBuilder().add("test_arg", "v")).build());
            case "list_prompt_name" -> client.request("prompts/list", Json.createObjectBuilder().build());
            case "list_prompt_arg_required" -> client.request("prompts/list", Json.createObjectBuilder().build());
            case "get_prompt_text" -> client.request("prompts/get",
                    Json.createObjectBuilder().add("name", parameter)
                            .add("arguments", Json.createObjectBuilder().add("test_arg", "v")).build());
            case "get_prompt_role" -> client.request("prompts/get",
                    Json.createObjectBuilder().add("name", parameter)
                            .add("arguments", Json.createObjectBuilder().add("test_arg", "v")).build());
            case "get_prompt_invalid" -> client.request("prompts/get",
                    Json.createObjectBuilder().add("name", parameter).build());
            case "get_prompt_missing_arg" -> client.request("prompts/get",
                    Json.createObjectBuilder().add("name", parameter).build());
            case "request_completion" -> client.request("completion/complete",
                    Json.createObjectBuilder()
                            .add("ref", Json.createObjectBuilder().add("type", "ref/prompt").add("name", "test_prompt"))
                            .add("argument", Json.createObjectBuilder().add("name", "test_arg").add("value", "")).build());
            case "set_log_level" -> client.request("logging/setLevel",
                    Json.createObjectBuilder().add("level", parameter).build());
            case "subscribe_resource" -> client.request("resources/subscribe",
                    Json.createObjectBuilder().add("uri", parameter).build());
            case "unsubscribe_resource" -> client.request("resources/unsubscribe",
                    Json.createObjectBuilder().add("uri", parameter).build());
            case "read_invalid_uri" -> client.request("resources/read",
                    Json.createObjectBuilder().add("uri", parameter).build());
            case "call_unknown_tool" -> client.request("tools/call",
                    Json.createObjectBuilder().add("name", parameter).build());
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

    private void verifyResult(String operation, JsonRpcMessage response, String expected, String parameter) {
        assertInstanceOf(JsonRpcResponse.class, response);
        var result = ((JsonRpcResponse) response).result();

        switch (operation) {
            case "list_resources" -> {
                var resources = result.getJsonArray("resources");
                assertEquals(1, resources.size());
                assertEquals(expected, resources.getJsonObject(0).getString("uri"));
            }
            case "resource_metadata" -> {
                var resource = result.getJsonArray("resources").getJsonObject(0);
                assertEquals(parameter, resource.getString("name"));
                assertEquals(expected, resource.getString("mimeType"));
            }
            case "read_resource" -> {
                var content = result.getJsonArray("contents").getJsonObject(0);
                assertEquals(expected, content.getString("text"));
            }
            case "list_templates" -> {
                var templates = result.getJsonArray("resourceTemplates");
                assertEquals(Integer.parseInt(expected), templates.size());
            }
            case "list_tools" -> {
                var tools = result.getJsonArray("tools");
                assertEquals(1, tools.size());
                assertEquals(expected, tools.getJsonObject(0).getString("name"));
            }
            case "list_tools_schema" -> {
                var tools = result.getJsonArray("tools");
                assertEquals(1, tools.size());
                var tool = tools.getJsonObject(0);
                assertEquals(expected, tool.getString("name"));
                assertEquals("object", tool.getJsonObject("inputSchema").getString("type"));
            }
            case "call_tool" -> {
                var content = result.getJsonArray("content").getJsonObject(0);
                assertEquals(expected, content.getString("text"));
            }
            case "list_prompts" -> {
                var prompts = result.getJsonArray("prompts");
                assertEquals(Integer.parseInt(expected), prompts.size());
            }
            case "get_prompt" -> {
                var messages = result.getJsonArray("messages");
                assertEquals(expected, messages.getJsonObject(0).getJsonObject("content").getString("text"));
            }
            case "list_prompt_name" -> {
                var prompts = result.getJsonArray("prompts");
                assertEquals(expected, prompts.getJsonObject(0).getString("name"));
            }
            case "list_prompt_arg_required" -> {
                var prompts = result.getJsonArray("prompts");
                var args = prompts.getJsonObject(0).getJsonArray("arguments");
                assertEquals(Boolean.parseBoolean(expected), args.getJsonObject(0).getBoolean("required"));
            }
            case "get_prompt_text" -> {
                var messages = result.getJsonArray("messages");
                assertEquals(expected, messages.getJsonObject(0).getJsonObject("content").getString("text"));
            }
            case "get_prompt_role" -> {
                var messages = result.getJsonArray("messages");
                assertEquals(expected, messages.getJsonObject(0).getString("role"));
            }
            case "request_completion" -> {
                var values = result.getJsonObject("completion").getJsonArray("values");
                assertEquals(expected, values.getJsonString(0).getString());
            }
            case "set_log_level", "subscribe_resource", "unsubscribe_resource" -> assertTrue(true);
        }
    }
}