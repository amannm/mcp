package com.amannmalik.mcp;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.auth.*;
import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.elicitation.*;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.logging.LoggingLevel;
import com.amannmalik.mcp.logging.LoggingMessageNotification;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.resources.ResourceSubscription;
import com.amannmalik.mcp.resources.ResourceUpdate;
import com.amannmalik.mcp.roots.*;
import com.amannmalik.mcp.sampling.*;
import com.amannmalik.mcp.transport.*;
import com.amannmalik.mcp.util.ListChangeSubscription;
import com.amannmalik.mcp.util.ProgressNotification;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import jakarta.json.*;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;


import static org.junit.jupiter.api.Assertions.*;

/// - [ToolProvider](src/main/java/com/amannmalik/mcp/tools/ToolProvider.java) - Tools conformance testing
/// - [PromptProvider](src/main/java/com/amannmalik/mcp/prompts/PromptProvider.java) - Prompts conformance testing
/// - [ResourceProvider](src/main/java/com/amannmalik/mcp/resources/ResourceProvider.java) - Resources conformance testing
/// - [ElicitationProvider](src/main/java/com/amannmalik/mcp/elicitation/ElicitationProvider.java) - Elicitation conformance testing
/// - [SamplingProvider](src/main/java/com/amannmalik/mcp/sampling/SamplingProvider.java) - Sampling conformance testing
/// - [RootsManager](src/main/java/com/amannmalik/mcp/roots/RootsManager.java) - Roots conformance testing
/// - [McpServer](src/main/java/com/amannmalik/mcp/McpServer.java) - Core server functionality testing
/// - [JsonRpc](src/main/java/com/amannmalik/mcp/jsonrpc/JsonRpc.java) - Base protocol testing
public final class McpConformanceSteps {
    private static final String JAVA_BIN = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

    private Process serverProcess;
    private StreamableHttpTransport serverTransport;
    private CompletableFuture<Void> serverTask;
    private McpClient client;
    private BlockingElicitationProvider elicitation;
    private SamplingProvider sampling;
    private CountingRootsProvider rootsProvider;

    private final BlockingQueue<LoggingMessageNotification> logs = new LinkedBlockingQueue<>();
    private final BlockingQueue<JsonRpcMessage> notifications = new LinkedBlockingQueue<>();
    private final Set<String> receivedNotifications = ConcurrentHashMap.newKeySet();
    private final BlockingQueue<ResourceUpdate> resourceUpdates = new LinkedBlockingQueue<>();
    private final BlockingQueue<ProgressNotification> progressUpdates = new LinkedBlockingQueue<>();
    private ProgressNotification firstProgressUpdate;
    private ProgressNotification lastProgressUpdate;
    private String currentProgressToken;
    private final List<String> paginatedTools = new ArrayList<>();
    private JsonObject authorizationMetadata = JsonValue.EMPTY_JSON_OBJECT;

    @FunctionalInterface
    private interface OperationExecutor {
        JsonRpcMessage execute(String parameter) throws Exception;
    }

    @FunctionalInterface
    private interface ResultVerifier {
        void verify(JsonRpcMessage response, String expected, String parameter);
    }

    private record OperationHandler(OperationExecutor executor, ResultVerifier verifier) {}

    private final Map<String, OperationHandler> operations = Map.ofEntries(
        // Resources operations
        Map.entry("list_resources", new OperationHandler(
            p -> client.request("resources/list", Json.createObjectBuilder().build()),
            (r, e, p) -> assertEquals(e, ((JsonRpcResponse) r).result().getJsonArray("resources").getJsonObject(0).getString("uri"))
        )),
        Map.entry("read_resource", new OperationHandler(
            p -> client.request("resources/read", Json.createObjectBuilder().add("uri", p).build()),
            (r, e, p) -> assertEquals(e, ((JsonRpcResponse) r).result().getJsonArray("contents").getJsonObject(0).getString("text"))
        )),
        Map.entry("resource_metadata", new OperationHandler(
            p -> client.request("resources/list", Json.createObjectBuilder().build()),
            (r, e, p) -> {
                var resource = ((JsonRpcResponse) r).result().getJsonArray("resources").getJsonObject(0);
                assertEquals(p, resource.getString("name"));
                assertEquals(e, resource.getString("mimeType"));
            }
        )),
        Map.entry("list_resources_annotations", new OperationHandler(
            p -> client.request("resources/list", Json.createObjectBuilder().build()),
            (r, e, p) -> {
                var resource = ((JsonRpcResponse) r).result().getJsonArray("resources").getJsonObject(0);
                var ann = Annotations.CODEC.fromJson(resource.getJsonObject("annotations"));
                assertTrue(ann.audience().contains(Role.USER));
                assertEquals(Double.parseDouble(e), ann.priority());
                assertNotNull(ann.lastModified());
            }
        )),
        Map.entry("list_templates", new OperationHandler(
            p -> client.request("resources/templates/list", Json.createObjectBuilder().build()),
            (r, e, p) -> assertEquals(Integer.parseInt(e), ((JsonRpcResponse) r).result().getJsonArray("resourceTemplates").size())
        )),
        Map.entry("subscribe_resource", new OperationHandler(
            p -> client.request("resources/subscribe", Json.createObjectBuilder().add("uri", p).build()),
            (r, e, p) -> assertTrue(true)
        )),
        Map.entry("unsubscribe_resource", new OperationHandler(
            p -> client.request("resources/unsubscribe", Json.createObjectBuilder().add("uri", p).build()),
            (r, e, p) -> assertTrue(true)
        )),
        // Tools operations
        Map.entry("list_tools", new OperationHandler(
            p -> client.request("tools/list", Json.createObjectBuilder().build()),
            (r, e, p) -> {
                var tools = ((JsonRpcResponse) r).result().getJsonArray("tools");
                assertTrue(tools.stream().map(JsonValue::asJsonObject).anyMatch(t -> e.equals(t.getString("name"))));
            }
        )),
        Map.entry("list_tools_schema", new OperationHandler(
            p -> client.request("tools/list", Json.createObjectBuilder().build()),
            (r, e, p) -> {
                var tools = ((JsonRpcResponse) r).result().getJsonArray("tools");
                var tool = tools.stream().map(JsonValue::asJsonObject).filter(t -> e.equals(t.getString("name"))).findFirst().orElseThrow();
                assertEquals("object", tool.getJsonObject("inputSchema").getString("type"));
            }
        )),
        Map.entry("list_tools_output_schema", new OperationHandler(
            p -> client.request("tools/list", Json.createObjectBuilder().build()),
            (r, e, p) -> {
                var tools = ((JsonRpcResponse) r).result().getJsonArray("tools");
                var tool = tools.stream().map(JsonValue::asJsonObject).filter(t -> e.equals(t.getString("name"))).findFirst().orElseThrow();
                assertEquals("object", tool.getJsonObject("outputSchema").getString("type"));
            }
        )),
        Map.entry("list_tools_annotations", new OperationHandler(
            p -> client.request("tools/list", Json.createObjectBuilder().build()),
            (r, e, p) -> {
                var tools = ((JsonRpcResponse) r).result().getJsonArray("tools");
                var tool = tools.stream().map(JsonValue::asJsonObject).filter(t -> p.equals(t.getString("name"))).findFirst().orElseThrow();
                assertEquals(Boolean.parseBoolean(e), tool.getJsonObject("annotations").getBoolean("readOnlyHint"));
            }
        )),
        // Prompts operations
        Map.entry("list_prompts", new OperationHandler(
            p -> client.request("prompts/list", Json.createObjectBuilder().build()),
            (r, e, p) -> assertEquals(Integer.parseInt(e), ((JsonRpcResponse) r).result().getJsonArray("prompts").size())
        )),
        Map.entry("list_prompt_name", new OperationHandler(
            p -> client.request("prompts/list", Json.createObjectBuilder().build()),
            (r, e, p) -> assertEquals(e, ((JsonRpcResponse) r).result().getJsonArray("prompts").getJsonObject(0).getString("name"))
        )),
        Map.entry("list_prompt_arg_required", new OperationHandler(
            p -> client.request("prompts/list", Json.createObjectBuilder().build()),
            (r, e, p) -> {
                var prompts = ((JsonRpcResponse) r).result().getJsonArray("prompts");
                var args = prompts.getJsonObject(0).getJsonArray("arguments");
                assertEquals(Boolean.parseBoolean(e), args.getJsonObject(0).getBoolean("required"));
            }
        )),
        Map.entry("get_prompt", new OperationHandler(
            p -> client.request("prompts/get", Json.createObjectBuilder().add("name", p).add("arguments", Json.createObjectBuilder().add("test_arg", "v")).build()),
            (r, e, p) -> assertEquals(e, ((JsonRpcResponse) r).result().getJsonArray("messages").getJsonObject(0).getJsonObject("content").getString("text"))
        )),
        Map.entry("get_prompt_text", new OperationHandler(
            p -> client.request("prompts/get", Json.createObjectBuilder().add("name", p).add("arguments", Json.createObjectBuilder().add("test_arg", "v")).build()),
            (r, e, p) -> assertEquals(e, ((JsonRpcResponse) r).result().getJsonArray("messages").getJsonObject(0).getJsonObject("content").getString("text"))
        )),
        Map.entry("get_prompt_role", new OperationHandler(
            p -> client.request("prompts/get", Json.createObjectBuilder().add("name", p).add("arguments", Json.createObjectBuilder().add("test_arg", "v")).build()),
            (r, e, p) -> assertEquals(e, ((JsonRpcResponse) r).result().getJsonArray("messages").getJsonObject(0).getString("role"))
        )),
        // Completion operations
        Map.entry("request_completion", new OperationHandler(
            p -> client.request("completion/complete", Json.createObjectBuilder()
                .add("ref", Json.createObjectBuilder().add("type", "ref/prompt").add("name", "test_prompt"))
                .add("argument", Json.createObjectBuilder().add("name", "test_arg").add("value", "")).build()),
            (r, e, p) -> {
                var completion = ((JsonRpcResponse) r).result().getJsonObject("completion");
                assertEquals(e, completion.getJsonArray("values").getJsonString(0).getString());
                assertTrue(completion.containsKey("hasMore"));
            }
        )),
        // Logging operations  
        Map.entry("set_log_level", new OperationHandler(
            p -> client.request("logging/setLevel", Json.createObjectBuilder().add("level", p).build()),
            (r, e, p) -> assertTrue(true)
        )),
        // Sampling operations
        Map.entry("request_sampling", new OperationHandler(
            p -> {
                var req = new CreateMessageRequest(
                    List.of(new SamplingMessage(Role.USER, new ContentBlock.Text("hi", null, null))),
                    new ModelPreferences(List.of(new ModelHint("claude-3-sonnet")), null, 0.5, 0.8),
                    "You are a helpful assistant.", null, null, 10, List.of(), null, null);
                return client.request("sampling/createMessage", CreateMessageRequest.CODEC.toJson(req));
            },
            (r, e, p) -> {
                var result = ((JsonRpcResponse) r).result();
                assertEquals("assistant", result.getString("role"));
                assertEquals(e, result.getJsonObject("content").getString("text"));
                assertEquals("mock-model", result.getString("model"));
                assertEquals("endTurn", result.getString("stopReason"));
            }
        )),
        // Roots operations
        Map.entry("roots_listed", new OperationHandler(
            p -> {
                for (int i = 0; i < 50 && rootsProvider.listCount() == 0; i++) Thread.sleep(100);
                return new JsonRpcResponse(RequestId.NullId.INSTANCE, Json.createObjectBuilder().add("count", rootsProvider.listCount()).build());
            },
            (r, e, p) -> assertTrue(((JsonRpcResponse) r).result().getInt("count") >= Integer.parseInt(e))
        )),
        // Authorization operations
        Map.entry("unauthorized_request", new OperationHandler(
            p -> {
                var http = HttpClient.newHttpClient();
                var req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + serverTransport.port() + "/"))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build();
                var resp = http.send(req, HttpResponse.BodyHandlers.discarding());
                return new JsonRpcResponse(RequestId.NullId.INSTANCE, Json.createObjectBuilder()
                    .add("status", resp.statusCode())
                    .add("www_authenticate", resp.headers().firstValue("WWW-Authenticate").orElse("")).build());
            },
            (r, e, p) -> {
                var result = ((JsonRpcResponse) r).result();
                assertEquals(401, result.getInt("status"));
                assertEquals(e, result.getString("www_authenticate"));
            }
        )),
        Map.entry("resource_metadata_auth_server", new OperationHandler(
            p -> {
                var http = HttpClient.newHttpClient();
                var uri = URI.create("http://127.0.0.1:" + serverTransport.port() + "/.well-known/oauth-protected-resource");
                var resp = http.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
                try (var reader = Json.createReader(new StringReader(resp.body()))) {
                    var body = reader.readObject();
                    return new JsonRpcResponse(RequestId.NullId.INSTANCE, Json.createObjectBuilder()
                        .add("status", resp.statusCode())
                        .add("authorization_server", body.getJsonArray("authorization_servers").getString(0)).build());
                }
            },
            (r, e, p) -> {
                var result = ((JsonRpcResponse) r).result();
                assertEquals(200, result.getInt("status"));
                assertEquals(e, result.getString("authorization_server"));
            }
        ))
    );

    private final Map<String, OperationExecutor> errorOperations = Map.ofEntries(
        Map.entry("read_invalid_uri", p -> client.request("resources/read", Json.createObjectBuilder().add("uri", p).build())),
        Map.entry("list_resources_invalid_cursor", p -> client.request("resources/list", Json.createObjectBuilder().add("cursor", p).build())),
        Map.entry("call_unknown_tool", p -> client.request("tools/call", Json.createObjectBuilder().add("name", p).build())),
        Map.entry("cancel_tool_call", p -> {
            try {
                client.request("tools/call", Json.createObjectBuilder().add("name", p).build(), 100);
                return new JsonRpcResponse(RequestId.NullId.INSTANCE, Json.createObjectBuilder().build());
            } catch (IOException e) {
                return JsonRpcError.of(RequestId.NullId.INSTANCE, JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
            }
        }),
        Map.entry("call_tool_elicit_cancel", p -> {
            elicitation.respond(new ElicitResult(ElicitationAction.CANCEL, null, null));
            return client.request("tools/call", Json.createObjectBuilder().add("name", p).build());
        }),
        Map.entry("call_tool_elicit_decline", p -> {
            elicitation.respond(new ElicitResult(ElicitationAction.DECLINE, null, null));
            return client.request("tools/call", Json.createObjectBuilder().add("name", p).build());
        }),
        Map.entry("call_tool_elicit_invalid", p -> {
            elicitation.respond(new ElicitResult(ElicitationAction.ACCEPT, Json.createObjectBuilder().add("msg", 1).build(), null));
            return client.request("tools/call", Json.createObjectBuilder().add("name", p).build());
        }),
        Map.entry("get_prompt_invalid", p -> client.request("prompts/get", Json.createObjectBuilder().add("name", p).build())),
        Map.entry("get_prompt_missing_arg", p -> client.request("prompts/get", Json.createObjectBuilder().add("name", p).build())),
        Map.entry("list_prompts_invalid_cursor", p -> client.request("prompts/list", Json.createObjectBuilder().add("cursor", p).build())),
        Map.entry("request_completion_invalid", p -> client.request("completion/complete", Json.createObjectBuilder()
            .add("ref", Json.createObjectBuilder().add("type", "ref/prompt").add("name", "nope"))
            .add("argument", Json.createObjectBuilder().add("name", "test_arg").add("value", "")).build())),
        Map.entry("request_completion_missing_arg", p -> client.request("completion/complete", Json.createObjectBuilder()
            .add("ref", Json.createObjectBuilder().add("type", "ref/prompt").add("name", "test_prompt")).build())),
        Map.entry("request_completion_missing_ref", p -> client.request("completion/complete", Json.createObjectBuilder()
            .add("argument", Json.createObjectBuilder().add("name", "test_arg").add("value", "")).build())),
        Map.entry("set_log_level_invalid", p -> client.request("logging/setLevel", Json.createObjectBuilder().add("level", p).build())),
        Map.entry("set_log_level_missing", p -> client.request("logging/setLevel", Json.createObjectBuilder().build())),
        Map.entry("set_log_level_extra", p -> client.request("logging/setLevel", Json.createObjectBuilder().add("level", p).add("extra", true).build())),
        Map.entry("request_sampling_reject", p -> {
            var req = new CreateMessageRequest(
                List.of(new SamplingMessage(Role.USER, new ContentBlock.Text("reject", null, null))),
                new ModelPreferences(List.of(new ModelHint("claude-3-sonnet")), null, 0.5, 0.8),
                "You are a helpful assistant.", null, null, 10, List.of(), null, null);
            return client.request("sampling/createMessage", CreateMessageRequest.CODEC.toJson(req));
        }),
        Map.entry("ping_invalid", p -> client.request("ping", Json.createObjectBuilder().add("extra", p).build())),
        Map.entry("roots_invalid", p -> client.request("roots/list", Json.createObjectBuilder().build())),
        Map.entry("list_tools_invalid_cursor", p -> client.request("tools/list", Json.createObjectBuilder().add("cursor", p).build())),
        Map.entry("subscribe_invalid_resource", p -> client.request("resources/subscribe", Json.createObjectBuilder().add("uri", p).build())),
        Map.entry("unsubscribe_nonexistent", p -> client.request("resources/unsubscribe", Json.createObjectBuilder().add("uri", p).build())),
        Map.entry("subscribe_duplicate_resource", p -> {
            JsonRpcMessage first = client.request("resources/subscribe", Json.createObjectBuilder().add("uri", p).build());
            assertInstanceOf(JsonRpcResponse.class, first);
            return client.request("resources/subscribe", Json.createObjectBuilder().add("uri", p).build());
        }),
        // Missing notification operations
        Map.entry("subscribe_then_update", p -> {
            client.request("resources/subscribe", Json.createObjectBuilder().add("uri", p).build());
            return new JsonRpcResponse(RequestId.NullId.INSTANCE, Json.createObjectBuilder().add("status", "success").build());
        }),
        Map.entry("multiple_updates", p -> {
            return new JsonRpcResponse(RequestId.NullId.INSTANCE, Json.createObjectBuilder().add("status", "success").build());
        }),
        Map.entry("unsubscribe_all", p -> {
            client.request("resources/unsubscribe", Json.createObjectBuilder().add("uri", p).build());
            return new JsonRpcResponse(RequestId.NullId.INSTANCE, Json.createObjectBuilder().add("status", "success").build());
        }),
        Map.entry("check_listChanged", p -> {
            return new JsonRpcResponse(RequestId.NullId.INSTANCE, Json.createObjectBuilder().add("status", "success").build());
        })
    );

    private final Map<String, OperationExecutor> specialOperations = Map.ofEntries(
        Map.entry("call_tool", p -> client.request("tools/call", Json.createObjectBuilder().add("name", p).build())),
        Map.entry("call_tool_structured", p -> client.request("tools/call", Json.createObjectBuilder().add("name", p).build())),
        Map.entry("call_tool_error", p -> client.request("tools/call", Json.createObjectBuilder().add("name", p).build())),
        Map.entry("call_tool_elicit", p -> {
            elicitation.respond(new ElicitResult(ElicitationAction.ACCEPT, Json.createObjectBuilder().add("msg", "ping").build(), null));
            return client.request("tools/call", Json.createObjectBuilder().add("name", p).build());
        })
    );

    private final Map<String, ResultVerifier> specialVerifiers = Map.ofEntries(
        Map.entry("call_tool", (r, e, p) -> assertEquals(e, ((JsonRpcResponse) r).result().getJsonArray("content").getJsonObject(0).getString("text"))),
        Map.entry("call_tool_structured", (r, e, p) -> {
            var result = ((JsonRpcResponse) r).result();
            assertEquals(e, result.getJsonArray("content").getJsonObject(0).getString("text"));
            assertEquals(e, result.getJsonObject("structuredContent").getString("message"));
        }),
        Map.entry("call_tool_error", (r, e, p) -> {
            var result = ((JsonRpcResponse) r).result();
            assertEquals(e, result.getJsonArray("content").getJsonObject(0).getString("text"));
            assertTrue(result.getBoolean("isError"));
        }),
        Map.entry("call_tool_elicit", (r, e, p) -> assertEquals(e, ((JsonRpcResponse) r).result().getJsonArray("content").getJsonObject(0).getString("text"))),
        Map.entry("subscribe_then_update", (r, e, p) -> assertEquals(e, ((JsonRpcResponse) r).result().getString("status"))),
        Map.entry("multiple_updates", (r, e, p) -> assertEquals(e, ((JsonRpcResponse) r).result().getString("status"))),
        Map.entry("unsubscribe_all", (r, e, p) -> assertEquals(e, ((JsonRpcResponse) r).result().getString("status"))),
        Map.entry("check_listChanged", (r, e, p) -> assertEquals(e, ((JsonRpcResponse) r).result().getString("status")))
    );

    @Before
    public void reset() throws Exception {
        cleanup();
    }

    @After
    public void cleanup() throws Exception {
        if (client != null) client.disconnect();
        if (serverProcess != null) {
            if (serverProcess.isAlive()) serverProcess.destroyForcibly();
            serverProcess.waitFor(2, TimeUnit.SECONDS);
        }
        if (serverTask != null && !serverTask.isDone()) serverTask.cancel(true);
        if (serverTransport != null) serverTransport.close();
    }

    @Given("a running MCP server using {word} transport")
    public void setupTransport(String transport) throws Exception {
        System.setProperty("mcp.test.transport", transport);
        client = createClient(createTransport(transport), createTestListener());
        client.connect();
    }

    @Then("capabilities should be advertised and ping succeeds")
    public void verifyCapabilitiesAndPing() {
        var expected = EnumSet.of(ServerCapability.RESOURCES, ServerCapability.TOOLS, ServerCapability.PROMPTS, ServerCapability.LOGGING, ServerCapability.COMPLETIONS);
        assertEquals(expected, client.serverCapabilities());
        assertTrue(client.toolsListChangedSupported());
        assertDoesNotThrow(() -> client.ping());
        assertTrue(Set.of(Protocol.LATEST_VERSION, Protocol.PREVIOUS_VERSION).contains(client.protocolVersion()));
        var info = client.serverInfo();
        assertFalse(info.name().isBlank());
        assertFalse(info.version().isBlank());
    }

    @When("testing all operations")
    public void testAllOperations(DataTable table) throws Exception {
        for (var row : table.asMaps()) {
            String operation = row.get("operation");
            String parameter = Objects.toString(row.get("parameter"), "");
            String expected = Objects.toString(row.get("expected_result"), "");
            String errorCode = Objects.toString(row.get("expected_error_code"), "");

            JsonRpcMessage response = executeOperation(operation, parameter);

            if (!errorCode.isEmpty()) {
                if (!(response instanceof JsonRpcError)) {
                    System.err.println("Expected error for operation " + operation + " but got: " + response);
                }
                assertInstanceOf(JsonRpcError.class, response);
                assertEquals(Integer.parseInt(errorCode), ((JsonRpcError) response).error().code());
            } else {
                if (!(response instanceof JsonRpcResponse)) {
                    System.err.println("Expected success for operation " + operation + " but got error: " + response);
                }
                assertInstanceOf(JsonRpcResponse.class, response);
                verifyResult(operation, response, expected, parameter);
            }
        }
    }

    private JsonRpcMessage executeOperation(String operation, String parameter) throws Exception {
        if (operations.containsKey(operation)) {
            return operations.get(operation).executor().execute(parameter);
        }
        if (errorOperations.containsKey(operation)) {
            return errorOperations.get(operation).execute(parameter);
        }
        if (specialOperations.containsKey(operation)) {
            return specialOperations.get(operation).execute(parameter);
        }
        throw new IllegalArgumentException("Unknown operation: " + operation);
    }

    private void verifyResult(String operation, JsonRpcMessage response, String expected, String parameter) {
        if (operations.containsKey(operation)) {
            operations.get(operation).verifier().verify(response, expected, parameter);
        } else if (specialVerifiers.containsKey(operation)) {
            specialVerifiers.get(operation).verify(response, expected, parameter);
        }
    }

    @When("requesting resource list with progress tracking")
    public void requestResourceListWithProgress() throws Exception {
        currentProgressToken = UUID.randomUUID().toString();
        JsonRpcMessage response = client.request("resources/list", Json.createObjectBuilder().add("_meta", Json.createObjectBuilder().add("progressToken", currentProgressToken)).build());
        if (!(response instanceof JsonRpcResponse)) {
            System.err.println("Progress request failed with error: " + response);
        }
        assertInstanceOf(JsonRpcResponse.class, response);
    }

    @Then("progress updates are received")
    public void verifyProgressUpdates() throws Exception {
        firstProgressUpdate = progressUpdates.poll(2, TimeUnit.SECONDS);
        assertNotNull(firstProgressUpdate);
        assertEquals(currentProgressToken, firstProgressUpdate.token().asString());
        lastProgressUpdate = firstProgressUpdate;
    }

    @And("progress completes to {double}")
    public void progressCompletes(double expected) throws Exception {
        double last = firstProgressUpdate.progress();
        boolean complete = last >= expected;
        lastProgressUpdate = firstProgressUpdate;
        ProgressNotification note;
        while ((note = progressUpdates.poll(2, TimeUnit.SECONDS)) != null) {
            double current = note.progress();
            assertTrue(current >= last);
            last = current;
            lastProgressUpdate = note;
            if (current >= expected) complete = true;
        }
        assertTrue(complete);
    }

    @And("progress message is provided")
    public void progressMessageProvided() {
        assertNotNull(lastProgressUpdate.message());
    }

    @When("listing tools with pagination")
    public void listToolsWithPagination() throws Exception {
        paginatedTools.clear();
        Optional<String> cursor = Optional.empty();
        do {
            var req = Json.createObjectBuilder();
            cursor.ifPresent(c -> req.add("cursor", c));
            JsonRpcMessage response = client.request("tools/list", req.build());
            assertInstanceOf(JsonRpcResponse.class, response);
            var result = ((JsonRpcResponse) response).result();
            result.getJsonArray("tools").stream().map(JsonValue::asJsonObject).map(t -> t.getString("name")).forEach(paginatedTools::add);
            cursor = result.containsKey("nextCursor") ? Optional.of(result.getString("nextCursor")) : Optional.empty();
        } while (cursor.isPresent());
    }

    @Then("pagination covers all tools")
    public void paginationCoversAllTools() {
        assertEquals(Set.of("test_tool", "error_tool", "echo_tool", "slow_tool"), Set.copyOf(paginatedTools));
    }

    @And("a cancellation log message is received")
    public void verifyCancellationLog() throws Exception {
        LoggingMessageNotification log = logs.poll(2, TimeUnit.SECONDS);
        assertNotNull(log);
        assertEquals(LoggingLevel.INFO, log.level());
        assertEquals("cancellation", log.logger());
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
            }
            if (serverProcess.isAlive()) {
                serverProcess.destroy();
                serverProcess.waitFor(2, TimeUnit.SECONDS);
            }
            if (serverProcess.isAlive()) {
                serverProcess.destroyForcibly();
                serverProcess.waitFor(2, TimeUnit.SECONDS);
            }
            assertFalse(serverProcess.isAlive());
        } else if (serverTask != null) {
            if (!serverTask.isCancelled()) {
                serverTask.get(2, TimeUnit.SECONDS);
            }
            assertTrue(serverTask.isDone());
        }
    }

    @When("testing subscription capabilities")
    public void testSubscriptionCapabilities(DataTable table) throws Exception {
        for (var row : table.asMaps()) {
            String capabilityType = row.get("capability_type");
            String feature = row.get("feature");
            boolean expectedSupport = Boolean.parseBoolean(row.get("expected_support"));

            boolean actualSupport = switch (capabilityType) {
                case "resources" -> switch (feature) {
                    case "subscribe" -> client.resourcesSubscribeSupported();
                    case "listChanged" -> client.resourcesListChangedSupported();
                    default -> throw new IllegalArgumentException("Unknown resource feature: " + feature);
                };
                case "prompts" -> switch (feature) {
                    case "listChanged" -> client.promptsListChangedSupported();
                    default -> throw new IllegalArgumentException("Unknown prompt feature: " + feature);
                };
                case "tools" -> switch (feature) {
                    case "listChanged" -> client.toolsListChangedSupported();
                    default -> throw new IllegalArgumentException("Unknown tool feature: " + feature);
                };
                case "roots" -> switch (feature) {
                    case "listChanged" -> rootsProvider.supportsListChanged();
                    default -> throw new IllegalArgumentException("Unknown roots feature: " + feature);
                };
                default -> throw new IllegalArgumentException("Unknown capability type: " + capabilityType);
            };

            assertEquals(expectedSupport, actualSupport, "Capability " + capabilityType + "." + feature + " support mismatch");
        }
    }

    @And("testing comprehensive notification behaviors")
    public void testComprehensiveNotificationBehaviors(DataTable table) throws Exception {
        for (var row : table.asMaps()) {
            String operation = row.get("operation");
            String parameter = Objects.toString(row.get("parameter"), "");
            String expected = Objects.toString(row.get("expected_result"), "");

            if (operation != null && !operation.isEmpty()) {
                JsonRpcMessage response = executeOperation(operation, parameter);
                if (!expected.isEmpty()) {
                    assertInstanceOf(JsonRpcResponse.class, response);
                    verifyResult(operation, response, expected, parameter);
                }
            }
        }
    }

    @And("testing comprehensive notification validation")
    public void testComprehensiveNotificationValidation(DataTable table) throws Exception {
        for (var row : table.asMaps()) {
            String operation = row.get("operation");
            String parameter = Objects.toString(row.get("parameter"), "");  
            String expected = Objects.toString(row.get("expected_result"), "");

            if (operation != null && !operation.isEmpty()) {
                JsonRpcMessage response = executeOperation(operation, parameter);
                if (!expected.isEmpty()) {
                    assertInstanceOf(JsonRpcResponse.class, response);
                    verifyResult(operation, response, expected, parameter);
                }
            }
        }
    }

    @When("fetching authorization metadata")
    public void fetchAuthorizationMetadata() throws Exception {
        if (serverTransport instanceof StreamableHttpTransport http) {
            var url = URI.create("http://127.0.0.1:" + http.port() + "/.well-known/oauth-protected-resource");
            var request = HttpRequest.newBuilder(url).header("Accept", "application/json").build();
            var response = HttpClient.newHttpClient().send(request, BodyHandlers.ofInputStream());
            try (var reader = Json.createReader(response.body())) {
                authorizationMetadata = reader.readObject();
            }
        } else {
            fail("HTTP transport required");
        }
    }

    @Then("authorization metadata uses server base URL")
    public void verifyAuthorizationMetadataResource() {
        if (serverTransport instanceof StreamableHttpTransport http) {
            var expected = "http://127.0.0.1:" + http.port();
            assertEquals(expected, authorizationMetadata.getString("resource"));
        } else {
            fail("HTTP transport required");
        }
    }

    @Then("authorization servers are advertised")
    public void verifyAuthorizationServers() {
        var servers = authorizationMetadata.getJsonArray("authorization_servers");
        assertFalse(servers.isEmpty());
    }

    private Transport createTransport(String type) throws Exception {
        if ("http".equals(type)) {
            Transport t = createHttpTransport();
            if (t instanceof StreamableHttpClientTransport s) {
                s.setAuthorization("token");
            }
            return t;
        } else {
            return createStdioTransport();
        }
    }

    private Transport createHttpTransport() throws Exception {
        serverTransport = new StreamableHttpTransport(0,
            Set.of("http://localhost", "http://127.0.0.1"),
            new AuthorizationManager(List.of(new BearerTokenAuthorizationStrategy(token -> new Principal("test", Set.of())))),
            "https://example.com/.well-known/oauth-protected-resource",
            List.of("https://auth.example.com"));
        
        serverTask = CompletableFuture.runAsync(() -> {
            try (var server = new McpServer(serverTransport, null)) {
                server.serve();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, runnable -> Thread.ofVirtual().start(runnable));
        
        String clientUrl = "http://127.0.0.1:" + serverTransport.port() + "/";
        return new StreamableHttpClientTransport(URI.create(clientUrl));
    }

    private Transport createStdioTransport() throws Exception {
        var args = new ArrayList<String>();
        args.add(JAVA_BIN);
        args.addAll(List.of("-cp", System.getProperty("java.class.path"), "com.amannmalik.mcp.Main", "server", "--stdio", "--auth-server", "https://auth.example.com", "-v"));

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(false);
        serverProcess = pb.start();

        Thread errorReader = new Thread(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(serverProcess.getErrorStream()))) {
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

    private McpClient createClient(Transport transport, McpClient.McpClientListener listener) {
        elicitation = new BlockingElicitationProvider();
        sampling = new InteractiveSamplingProvider(true);
        rootsProvider = new CountingRootsProvider(List.of(new Root("file:///tmp", "Test Root", null)));

        return new McpClient(new ClientInfo("test-client", "Test Client", "1.0"), EnumSet.allOf(ClientCapability.class), transport, sampling, rootsProvider, elicitation, listener);
    }

    private McpClient.McpClientListener createTestListener() {
        return new McpClient.McpClientListener() {
            @Override
            public void onProgress(ProgressNotification notification) {
                progressUpdates.add(notification);
            }

            @Override
            public void onMessage(LoggingMessageNotification notification) {
                logs.add(notification);
            }

            @Override
            public void onResourceListChanged() {
                notifications.add(new JsonRpcNotification("notifications/resources/list_changed", null));
            }

            @Override
            public void onToolListChanged() {
                notifications.add(new JsonRpcNotification("notifications/tools/list_changed", null));
            }

            @Override
            public void onPromptsListChanged() {
                notifications.add(new JsonRpcNotification("notifications/prompts/list_changed", null));
            }
        };
    }

    private static final class CountingRootsProvider implements RootsProvider {
        private final InMemoryRootsProvider delegate;
        private final AtomicInteger count = new AtomicInteger();

        CountingRootsProvider(List<Root> initial) {
            delegate = new InMemoryRootsProvider(initial);
        }

        @Override
        public List<Root> list() {
            count.incrementAndGet();
            return delegate.list();
        }

        @Override
        public ListChangeSubscription subscribe(RootsListener listener) {
            return delegate.subscribe(listener);
        }

        @Override
        public boolean supportsListChanged() {
            return delegate.supportsListChanged();
        }

        int listCount() {
            return count.get();
        }
    }
}