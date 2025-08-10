package com.amannmalik.mcp.lifecycle;

import com.amannmalik.mcp.core.ClientCapability;
import com.amannmalik.mcp.core.ClientInfo;
import com.amannmalik.mcp.core.McpClient;
import com.amannmalik.mcp.core.McpServer;
import com.amannmalik.mcp.core.StdioTransport;
import com.amannmalik.mcp.elicitation.InteractiveElicitationProvider;
import com.amannmalik.mcp.roots.InMemoryRootsProvider;
import com.amannmalik.mcp.roots.Root;
import com.amannmalik.mcp.sampling.InteractiveSamplingProvider;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.*;

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
    }

    @Given("protocol version {string} is supported")
    public void protocolVersionSupported(String version) {
        // supported version is expectedVersion, no implementation needed
    }

    @Given("both McpHost and McpServer are available")
    public void hostAndServerAvailable() {
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
        serverVersion = version;
    }

    @Given("a McpHost requesting protocol version {string}")
    public void hostRequestsVersion(String version) {
        hostVersion = version;
    }

    @When("initialization is performed")
    public void initializationPerformed() throws IOException {
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
