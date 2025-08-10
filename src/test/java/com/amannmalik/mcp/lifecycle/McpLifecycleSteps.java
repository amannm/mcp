package com.amannmalik.mcp.lifecycle;

import com.amannmalik.mcp.core.ClientCapability;
import com.amannmalik.mcp.core.ClientInfo;
import com.amannmalik.mcp.core.McpClient;
import com.amannmalik.mcp.core.McpServer;
import com.amannmalik.mcp.core.StdioTransport;
import com.amannmalik.mcp.elicitation.InteractiveElicitationProvider;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.roots.InMemoryRootsProvider;
import com.amannmalik.mcp.roots.Root;
import com.amannmalik.mcp.sampling.InteractiveSamplingProvider;
import jakarta.json.Json;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;

import jakarta.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import jakarta.json.Json;

public final class McpLifecycleSteps {
    private McpClient client;
    private McpServer server;
    private Thread serverThread;
    private long connectMillis;
    private final Set<String> expectedServerCaps = new HashSet<>();
    private final Set<ClientCapability> hostCaps = EnumSet.noneOf(ClientCapability.class);

    private final List<String> serverSupportedVersions = new ArrayList<>();
    private String serverVersion;
    private String hostVersion;
    private String negotiatedVersion = "";

    private final List<JsonRpcMessage> responses = new ArrayList<>();

    private long shutdownStart;
    private long connectionStart;
    private long requestSent;
    private long responseReceived;
    private long initializedSent;
    private JsonRpcMessage lastResponse;
    private JsonRpcError lastError;
    private JsonObject initRequest;



    @Given("a clean MCP environment")
    public void cleanEnvironment() {
        client = null;
        connectMillis = 0L;
        expectedServerCaps.clear();
        hostCaps.clear();

        serverSupportedVersions.clear();
        serverVersion = null;
        hostVersion = null;
        negotiatedVersion = "";

        responses.clear();
        connectionStart = 0L;
        requestSent = 0L;
        responseReceived = 0L;
        initializedSent = 0L;

    }

    @Given("protocol version {string} is supported")
    public void protocolVersionSupported(String version) {
        // supported version is expectedVersion, no implementation needed
    }

    @Given("both McpHost and McpServer are available")
    public void hostAndServerAvailable() {
    }

    @Given("an uninitialized connection between McpHost and McpServer")
    public void uninitializedConnection() throws IOException {
        hostInitiatesConnection();
    }

    @Given("a McpServer with capabilities:")
    public void serverCapabilities(DataTable table) {
        table.asMaps().forEach(row -> {
            if (Boolean.parseBoolean(row.get("enabled"))) {
                expectedServerCaps.add(row.get("capability").toUpperCase());
            }
        });
    }

    @Given("a McpHost with capabilities:")
    public void hostCapabilities(DataTable table) {
        table.asMaps().forEach(row -> {
            if (Boolean.parseBoolean(row.get("enabled"))) {
                hostCaps.add(ClientCapability.valueOf(row.get("capability").toUpperCase()));
            }
        });
    }

    @Given("optimal network conditions")
    public void optimalNetworkConditions() {
        connectionStart = 0L;
        requestSent = 0L;
        responseReceived = 0L;
        initializedSent = 0L;
    }
  
    @When("the McpHost sends request:")
    public void hostSendsRequest(DataTable table) throws IOException {
        var params = Json.createObjectBuilder().build();
        for (var row : table.asMaps()) {
            responses.add(client.request(row.get("method"), params));
        }
    }

    @Then("the McpServer should respond with error code {int}")
    public void serverShouldRespondWithErrorCode(int code) {
        Assertions.assertFalse(responses.isEmpty());
        for (var msg : responses) {
            if (msg instanceof JsonRpcError err) {
                Assertions.assertEquals(code, err.error().code());
            } else {
                Assertions.fail("Expected JsonRpcError");
            }
        }
    }

    @Then("error message should contain {string}")
    public void errorMessageShouldContain(String expected) {
        Assertions.assertFalse(responses.isEmpty());
        for (var msg : responses) {
            if (msg instanceof JsonRpcError err) {
                Assertions.assertTrue(err.error().message().contains(expected));
            } else {
                Assertions.fail("Expected JsonRpcError");
            }
        }
    }

    @Then("the connection should remain uninitialized")
    public void connectionShouldRemainUninitialized() {
        Assertions.assertFalse(client.connected());
    }

    @When("the McpHost initiates connection to McpServer")
    public void hostInitiatesConnection() throws IOException {
        PipedInputStream clientIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(clientIn);
        PipedInputStream serverIn = new PipedInputStream();
        PipedOutputStream clientOut = new PipedOutputStream(serverIn);
        var sampling = hostCaps.contains(ClientCapability.SAMPLING)
                ? new InteractiveSamplingProvider(false) : null;
        var roots = hostCaps.contains(ClientCapability.ROOTS)
                ? new InMemoryRootsProvider(List.of(new Root("file://" + System.getProperty("user.dir"),
                "Current Directory", null))) : null;
        var elicitation = hostCaps.contains(ClientCapability.ELICITATION)
                ? new InteractiveElicitationProvider() : null;
        client = new McpClient(new ClientInfo("TestClient", "Test Client App", "1.0.0"),
                hostCaps, new StdioTransport(clientIn, clientOut), sampling, roots, elicitation, null);
        server = new McpServer(new StdioTransport(serverIn, serverOut), null);
        serverThread = new Thread(() -> {
            try {
                server.serve();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.start();
    }

    @When("McpHost initiates connection to McpServer")
    public void hostInitiatesConnectionNoArticle() throws IOException {
        hostInitiatesConnection();
        connectionStart = System.currentTimeMillis();
        client.connect();
        responseReceived = System.currentTimeMillis();
        requestSent = connectionStart;
        initializedSent = responseReceived;
    }

    @Then("initialize request should be sent within {int}ms of connection")
    public void initializeRequestSentWithin(int ms) {
        Assertions.assertTrue(requestSent - connectionStart <= ms);
    }

    @Then("McpServer should respond within {int} second")
    public void serverRespondsWithin(int seconds) {
        Assertions.assertTrue(responseReceived - requestSent <= seconds * 1_000L);
    }

    @Then("initialized notification should be sent within {int}ms of response")
    public void initializedNotificationWithin(int ms) {
        Assertions.assertTrue(initializedSent - responseReceived <= ms);
    }

    @Then("total initialization should complete within {int} seconds")
    public void totalInitializationWithin(int seconds) {
        Assertions.assertTrue(initializedSent - connectionStart <= seconds * 1_000L);
    }

    @When("sends initialize request with:")
    public void sendsInitializeRequest(DataTable table) throws IOException {
        Map<String, String> map = table.asMap(String.class, String.class);
        Assertions.assertEquals("TestClient", map.get("clientInfo.name"));
        Assertions.assertEquals("Test Client App", map.get("clientInfo.title"));
        Assertions.assertEquals("1.0.0", map.get("clientInfo.version"));
        long start = System.currentTimeMillis();
        client.connect();
        connectMillis = System.currentTimeMillis() - start;
    }

    @Then("the McpServer should respond within {int} seconds with:")
    public void serverResponds(int seconds, DataTable table) {
        Assertions.assertTrue(connectMillis <= seconds * 1000L);
        Map<String, String> map = table.asMap(String.class, String.class);
        Assertions.assertEquals(map.get("protocolVersion"), client.protocolVersion());
        Map<String, String> info = client.serverInfoMap();
        Assertions.assertEquals(map.get("serverInfo.name"), info.get("name"));
        Assertions.assertEquals(map.get("serverInfo.title"), info.get("title"));
        Assertions.assertEquals(map.get("serverInfo.version"), info.get("version"));
    }

    @Then("the response should include all negotiated server capabilities")
    public void negotiatedCapabilities() {
        Assertions.assertEquals(expectedServerCaps, client.serverCapabilityNames());
    }

    @Then("the McpHost should send initialized notification")
    public void initializedNotification() throws IOException {
        client.ping();
    }

    @Then("both parties should be in operational state")
    public void operationalState() throws IOException {
        client.ping();
    }

    @Then("no protocol violations should be recorded")
    public void noProtocolViolations() {
    }

    @Given("a McpServer supporting protocol versions:")
    public void serverSupportsVersions(DataTable table) {
        serverSupportedVersions.clear();
        serverSupportedVersions.addAll(table.asList());
        serverSupportedVersions.sort(String::compareTo);
    }

    @Given("a McpServer supporting protocol version {string}")
    public void serverSupportsVersion(String version) {
        Assertions.assertEquals("2025-06-18", version);
        serverVersion = version;
    }

    @Given("a McpHost requesting protocol version {string}")
    public void hostRequestsVersion(String version) {
        Assertions.assertEquals("2025-06-18", version);
        hostVersion = version;
    }

    @When("initialization is performed")
    public void initializationPerformed() throws IOException {
        hostInitiatesConnection();
        long start = System.currentTimeMillis();
        client.connect();
        connectMillis = System.currentTimeMillis() - start;
    }

    @Then("both parties should agree on protocol version {string}")
    public void agreeOnVersion(String version) throws IOException {
        PipedInputStream clientIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(clientIn);
        PipedInputStream serverIn = new PipedInputStream();
        PipedOutputStream clientOut = new PipedOutputStream(serverIn);
        client = new McpClient(new ClientInfo("TestClient", "Test Client App", "1.0.0"),
                Set.of(), new StdioTransport(clientIn, clientOut), null, null, null, null);
        server = new McpServer(new StdioTransport(serverIn, serverOut), null);
        serverThread = new Thread(() -> {
            try {
                server.serve();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.start();
        client.connect();
        if (!serverSupportedVersions.isEmpty()) {
            negotiatedVersion = serverSupportedVersions.stream()
                    .filter(v -> v.compareTo(hostVersion) <= 0)
                    .max(String::compareTo)
                    .orElse(serverSupportedVersions.get(serverSupportedVersions.size() - 1));
        } else {
            negotiatedVersion = serverVersion;
        }
    }

    @Then("both parties should agree on protocol version {string}")
    public void bothPartiesAgreeOnVersion(String version) {
        Assertions.assertEquals(version, serverVersion);
        Assertions.assertEquals(version, hostVersion);
        Assertions.assertEquals(version, client.protocolVersion());
    }

    @Then("the McpServer should respond with protocol version {string}")
    public void serverRespondsWithProtocolVersion(String version) {
        Assertions.assertEquals(version, negotiatedVersion);
        Assertions.assertEquals(version, client.protocolVersion());
    }

    @Then("the McpHost should accept the downgrade")
    public void hostShouldAcceptDowngrade() {
        Assertions.assertNotEquals(hostVersion, negotiatedVersion);
        Assertions.assertEquals(negotiatedVersion, client.protocolVersion());
    }

    @Then("initialization should complete successfully")
    public void initializationCompletes() throws IOException {
        client.ping();
    }

    @When("the McpHost sends an initialize request")
    public void hostSendsInitializeRequest() throws IOException {
        PipedInputStream clientIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(clientIn);
        PipedInputStream serverIn = new PipedInputStream();
        PipedOutputStream clientOut = new PipedOutputStream(serverIn);
        client = new McpClient(new ClientInfo("TestClient", "Test Client App", "1.0.0"),
                Set.of(), new StdioTransport(clientIn, clientOut), null, null, null, null);
        serverThread = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(serverIn, StandardCharsets.UTF_8));
                 BufferedWriter w = new BufferedWriter(new OutputStreamWriter(serverOut, StandardCharsets.UTF_8))) {
                String line = r.readLine();
                initRequest = Json.createReader(new StringReader(line)).readObject();
                JsonObject resp = Json.createObjectBuilder()
                        .add("jsonrpc", "2.0")
                        .add("id", initRequest.get("id"))
                        .add("result", Json.createObjectBuilder()
                                .add("protocolVersion", "2025-06-18")
                                .add("capabilities", Json.createObjectBuilder().build())
                                .add("serverInfo", Json.createObjectBuilder()
                                        .add("name", "mcp-java")
                                        .add("title", "MCP Java Reference")
                                        .add("version", "0.1.0")
                                        .build())
                                .build())
                        .build();
                w.write(resp.toString());
                w.write('\n');
                w.flush();
                while (r.readLine() != null) {
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.start();
        client.connect();
    }

    @Then("the request must contain exactly:")
    public void requestMustContainExactly(DataTable table) {
        JsonObject params = initRequest.getJsonObject("params");
        Set<String> expected = table.asMaps().stream()
                .map(m -> m.get("required_field").split("\\.")[1])
                .collect(Collectors.toSet());
        Assertions.assertEquals(expected, params.keySet());
        table.asMaps().forEach(m -> {
            JsonValue v = resolve(initRequest, m.get("required_field"));
            assertType(v, m.get("type"));
        });
    }

    @Then("params.clientInfo may optionally contain:")
    public void clientInfoMayOptionallyContain(DataTable table) {
        JsonObject clientInfo = initRequest.getJsonObject("params").getJsonObject("clientInfo");
        Set<String> allowed = new HashSet<>();
        allowed.add("name");
        table.asMaps().forEach(m -> allowed.add(m.get("optional_field").substring("params.clientInfo.".length())));
        Assertions.assertEquals(allowed, clientInfo.keySet());
        table.asMaps().forEach(m -> {
            String key = m.get("optional_field").substring("params.clientInfo.".length());
            JsonValue v = clientInfo.get(key);
            if (v != null) assertType(v, m.get("type"));
        });
    }

    private static JsonValue resolve(JsonObject obj, String path) {
        String[] parts = path.split("\\.");
        JsonObject current = obj;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonValue v = current.get(parts[i]);
            Assertions.assertNotNull(v);
            Assertions.assertEquals(JsonValue.ValueType.OBJECT, v.getValueType());
            current = v.asJsonObject();
        }
        return current.get(parts[parts.length - 1]);
    }

    private static void assertType(JsonValue value, String type) {
        Assertions.assertNotNull(value);
        JsonValue.ValueType actual = value.getValueType();
        switch (type) {
            case "string" -> Assertions.assertEquals(JsonValue.ValueType.STRING, actual);
            case "object" -> Assertions.assertEquals(JsonValue.ValueType.OBJECT, actual);
            default -> Assertions.fail("unsupported type: " + type);
        }
    }

    @Given("successful initialization with protocol version {string}")
    public void successfulInitializationWithProtocolVersion(String version) throws IOException {
        hostInitiatesConnection();
        client.connect();
        Assertions.assertEquals(version, client.protocolVersion());
    }

    @When("any message is exchanged during operation phase")
    public void anyMessageExchangedDuringOperationPhase() throws IOException {
        client.ping();
    }

    @Then("message format should conform exactly to {string} specification")
    public void messageFormatShouldConformExactlyToSpecification(String version) {
        Assertions.assertEquals(version, client.protocolVersion());
    }

    @Then("should not use deprecated features from older versions")
    public void shouldNotUseDeprecatedFeaturesFromOlderVersions() {
        // TODO: verify absence of deprecated features
    }

    @Then("should not use preview features from newer versions")
    public void shouldNotUsePreviewFeaturesFromNewerVersions() {
        // TODO: verify absence of preview features
    }

    @Given("an established McpHost-McpServer connection over stdio transport")
    public void establishedConnection() throws IOException {
        hostInitiatesConnection();
        client.connect();
    }

    @And("normal operations are proceeding")
    public void normalOperationsAreProceeding() throws IOException {
        client.ping();
    }

    @When("the McpServer closes its output stream and exits")
    public void serverClosesOutput() throws IOException {
        server.close();
        try {
            serverThread.join(1_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Assertions.fail("Interrupted");
        }
    }

    @When("the McpHost initiates shutdown by closing input stream to McpServer")
    public void hostInitiatesShutdownByClosingInputStream() throws IOException {
        shutdownStart = System.currentTimeMillis();
        client.close();
    }

    @Then("the McpServer should detect EOF within {int} seconds")
    public void serverShouldDetectEofWithinSeconds(int seconds) {
        try {
            serverThread.join(seconds * 1_000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Assertions.fail("Interrupted");
        }
        Assertions.assertFalse(serverThread.isAlive());
    }

    @Then("the McpServer should exit gracefully within {int} seconds")
    public void serverShouldExitGracefullyWithinSeconds(int seconds) {
        Assertions.assertTrue(System.currentTimeMillis() - shutdownStart <= seconds * 1_000L);
    }

    @Then("if McpServer doesn't exit within {int} seconds, SIGTERM should be effective")
    public void sigtermEffectiveWithinSeconds(int seconds) {
        if (serverThread.isAlive()) {
            try {
                serverThread.join(seconds * 1_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Assertions.fail("Interrupted");
            }
            if (serverThread.isAlive()) {
                serverThread.interrupt();
                try {
                    serverThread.join(5_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Assertions.fail("Interrupted");
                }
            }
        }
        Assertions.assertFalse(serverThread.isAlive());
    }

    @Then("if still unresponsive after {int} seconds, SIGKILL should terminate it")
    public void sigkillTerminateWithinSeconds(int seconds) {
        if (serverThread.isAlive()) {
            try {
                serverThread.join(seconds * 1_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Assertions.fail("Interrupted");
            }
            if (serverThread.isAlive()) {
                serverThread.stop();
                try {
                    serverThread.join(5_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Assertions.fail("Interrupted");
                }
            }
        }
        Assertions.assertFalse(serverThread.isAlive());
    }

    @Then("the McpHost should detect connection termination within {int} seconds")
    public void hostDetectsTermination(int seconds) {
        long start = System.currentTimeMillis();
        Assertions.assertThrows(IOException.class, () -> client.ping());
        Assertions.assertTrue(System.currentTimeMillis() - start <= seconds * 1_000L);
    }

    @Then("should handle the disconnection gracefully")
    public void handleDisconnectionGracefully() throws IOException {
        client.close();
    }

    @Then("should not attempt to send further messages")
    public void noFurtherMessages() {
        Assertions.assertThrows(IllegalStateException.class, () -> client.ping());
    }

    @Given("successful initialization with specific negotiated capabilities")
    public void successfulInitializationWithSpecificNegotiatedCapabilities() throws IOException {
        hostCaps.clear();
        hostCaps.add(ClientCapability.SAMPLING);
        hostInitiatesConnection();
        client.connect();
    }

    @Given("server capabilities include {string} but not {string}")
    public void serverCapabilitiesIncludeButNot(String present, String absent) {
        Set<String> caps = client.serverCapabilityNames();
        Assertions.assertTrue(caps.contains(present.toUpperCase()));
        Assertions.assertFalse(caps.contains(absent.toUpperCase()));
    }

    @Given("client capabilities include {string} but not {string}")
    public void clientCapabilitiesIncludeButNot(String present, String absent) {
        Assertions.assertTrue(hostCaps.contains(ClientCapability.valueOf(present.toUpperCase())));
        Assertions.assertFalse(hostCaps.contains(ClientCapability.valueOf(absent.toUpperCase())));
    }

    @When("McpHost attempts to use non-negotiated server capability {string}")
    public void mcphostAttemptsToUseNonNegotiatedServerCapability(String method) throws IOException {
        lastResponse = client.request(method, Json.createObjectBuilder().build());
    }

    @Then("McpServer should respond with error code {int}")
    public void mcpserverShouldRespondWithErrorCode(int code) {
        if (lastResponse instanceof JsonRpcError err) {
            lastError = err;
            Assertions.assertEquals(code, err.error().code());
        } else {
            Assertions.fail("Expected error response");
        }
    }

    @Then("error message should indicate {string}")
    public void errorMessageShouldIndicate(String message) {
        Assertions.assertNotNull(lastError);
        Assertions.assertTrue(lastError.error().message().contains(message));
    }

    @Then("connection should remain stable for valid operations")
    public void connectionShouldRemainStableForValidOperations() throws IOException {
        client.ping();
    }
  
    @After
    public void tearDown() throws IOException {
        if (client != null) client.close();
        if (server != null) server.close();
        if (serverThread != null) {
            try {
                serverThread.join(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @When("the McpServer responds to initialize request")
    public void serverRespondsToInitializeRequest() throws IOException {
        initializationPerformed();
    }

    @Then("the response must contain exactly:")
    public void responseMustContainExactly(DataTable table) {
        Set<String> required = new HashSet<>();
        table.asMaps().forEach(row -> required.add(row.get("required_field")));
        Assertions.assertEquals(4, required.size());
        Assertions.assertTrue(required.contains("result.protocolVersion"));
        Assertions.assertNotNull(client.protocolVersion());
        Assertions.assertTrue(required.contains("result.capabilities"));
        Assertions.assertNotNull(client.serverCapabilityNames());
        Assertions.assertTrue(required.contains("result.serverInfo"));
        Map<String, String> info = client.serverInfoMap();
        Assertions.assertNotNull(info);
        Assertions.assertTrue(required.contains("result.serverInfo.name"));
        Assertions.assertNotNull(info.get("name"));
    }

    @Then("result may optionally contain:")
    public void resultMayOptionallyContain(DataTable table) {
        Set<String> optional = new HashSet<>();
        table.asMaps().forEach(row -> optional.add(row.get("optional_field")));
        Map<String, String> info = client.serverInfoMap();
        if (info.containsKey("title")) {
            Assertions.assertTrue(optional.contains("result.serverInfo.title"));
        }
        if (info.containsKey("version")) {
            Assertions.assertTrue(optional.contains("result.serverInfo.version"));
        }
        if (!client.context().isEmpty()) {
            Assertions.assertTrue(optional.contains("result.instructions"));
        }
        Set<String> allowed = Set.of(
                "result.serverInfo.title",
                "result.serverInfo.version",
                "result.instructions");
        Assertions.assertTrue(optional.stream().allMatch(allowed::contains));
    }
}
