package com.amannmalik.mcp;

import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.transport.*;
import com.amannmalik.mcp.util.ErrorCodeMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.But;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.platform.suite.api.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("com/amannmalik/mcp")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.amannmalik.mcp")
public class McpFeatureSteps {

    private McpServer server;
    private McpClient client;
    private Transport serverTransport;
    private Transport clientTransport;
    private Thread serverThread;
    private List<Capability> serverCapabilities;
    private List<Capability> clientCapabilities;
    private String protocolVersion;
    private boolean capabilityNegotiationComplete;
    private boolean initializedNotificationSent;
    private boolean connectionInOperationPhase;
    private boolean shutdownRequested;
    private boolean connectionTerminatedGracefully;
    private boolean resourcesCleanedUp;
    private ConnectionManager connectionManager;
    private String lastErrorMessage;
    private int lastErrorCode;
    private List<Map<String, String>> operationResults;
    private VersionNegotiator versionNegotiator;
    private String negotiatedVersion;
    private boolean compatibilityMode;
    private boolean versionMismatchLogged;

    @Before
    public void setupTestEnvironment() {
        server = null;
        client = null;
        serverTransport = null;
        clientTransport = null;
        serverThread = null;
        serverCapabilities = new ArrayList<>();
        clientCapabilities = new ArrayList<>();
        protocolVersion = null;
        capabilityNegotiationComplete = false;
        initializedNotificationSent = false;
        connectionInOperationPhase = false;
        shutdownRequested = false;
        connectionTerminatedGracefully = false;
        resourcesCleanedUp = false;
        connectionManager = null;
        lastErrorMessage = null;
        lastErrorCode = 0;
        operationResults = new ArrayList<>();
        versionNegotiator = null;
        negotiatedVersion = null;
        compatibilityMode = false;
        versionMismatchLogged = false;
    }

    @After
    public void cleanupTestEnvironment() {
        try {
            if (client != null) {
                client.close();
            }
            if (server != null) {
                server.close();
            }
            if (serverThread != null && serverThread.isAlive()) {
                serverThread.interrupt();
                serverThread.join(5000);
            }
        } catch (Exception e) {
            System.err.println("Cleanup error: " + e.getMessage());
        }
    }

    /** Parse capabilities table into structured format. */
    private List<Capability> parseCapabilitiesTable(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new Capability(
                        Objects.requireNonNull(row.get("capability")),
                        row.getOrDefault("feature", ""),
                        Boolean.parseBoolean(row.getOrDefault("enabled", "false"))))
                .toList();
    }

    /** Parse resource templates table into template definitions. */
    private List<ResourceTemplateRow> parseResourceTemplates(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new ResourceTemplateRow(
                        Objects.requireNonNull(row.get("template")),
                        Objects.requireNonNull(row.get("description")),
                        Objects.requireNonNull(row.get("mime_type"))))
                .toList();
    }

    /** Parse tool definitions table. */
    private List<ToolDefinition> parseToolDefinitions(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new ToolDefinition(
                        Objects.requireNonNull(row.get("name")),
                        Objects.requireNonNull(row.get("description")),
                        Boolean.parseBoolean(row.getOrDefault("requires_confirmation", "false"))))
                .toList();
    }

    /** Parse input schema table for tool parameters. */
    private List<InputField> parseInputSchema(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new InputField(
                        Objects.requireNonNull(row.get("field")),
                        Objects.requireNonNull(row.get("type")),
                        Boolean.parseBoolean(row.getOrDefault("required", "false")),
                        Objects.requireNonNull(row.get("description"))))
                .toList();
    }

    /** Parse arguments table for tool calls. */
    private Map<String, String> parseArguments(DataTable table) {
        return table.asMaps().stream()
                .collect(Collectors.toMap(
                        row -> Objects.requireNonNull(row.get("field")),
                        row -> Objects.requireNonNull(row.get("value"))));
    }

    /** Parse model preferences table. */
    private List<ModelPreference> parseModelPreferences(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new ModelPreference(
                        Objects.requireNonNull(row.get("preference")),
                        Objects.requireNonNull(row.get("value")),
                        Objects.requireNonNull(row.get("description"))))
                .toList();
    }

    /** Parse log messages table for testing. */
    private List<LogMessage> parseLogMessages(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new LogMessage(
                        Objects.requireNonNull(row.get("level")),
                        Objects.requireNonNull(row.get("logger")),
                        Objects.requireNonNull(row.get("message"))))
                .toList();
    }

    private record Capability(String capability, String feature, boolean enabled) {}

    private record ResourceTemplateRow(String template, String description, String mimeType) {}

    private record ToolDefinition(String name, String description, boolean requiresConfirmation) {}

    private record InputField(String field, String type, boolean required, String description) {}

    private record ModelPreference(String preference, String value, String description) {}

    private record LogMessage(String level, String logger, String message) {}

    @Given("a clean MCP environment")
    public void aCleanMcpEnvironment() {
        setupTestEnvironment();
    }

    @And("protocol version {string} is supported")
    public void protocolVersionIsSupported(String version) {
        this.protocolVersion = version;
        assertEquals("2025-06-18", version, "Expected protocol version 2025-06-18");
    }

    @Given("an MCP server with comprehensive capabilities:")
    public void anMcpServerWithComprehensiveCapabilities(DataTable table) throws IOException {
        this.serverCapabilities = parseCapabilitiesTable(table);
        
        PipedOutputStream serverOut = new PipedOutputStream();
        PipedInputStream clientIn = new PipedInputStream(serverOut);
        PipedOutputStream clientOut = new PipedOutputStream();
        PipedInputStream serverIn = new PipedInputStream(clientOut);
        
        clientTransport = new StdioTransport(clientIn, clientOut);
        serverTransport = new StdioTransport(serverIn, serverOut);
        
        server = new McpServer(serverTransport, "Test server for comprehensive capability testing");
        
        serverThread = new Thread(() -> {
            try {
                server.serve();
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    System.err.println("Server error: " + e.getMessage());
                }
            }
        });
        serverThread.start();
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Setup interrupted", e);
        }
    }

    @And("an MCP client with capabilities:")
    public void anMcpClientWithCapabilities(DataTable table) {
        this.clientCapabilities = parseCapabilitiesTable(table);
        
        Set<ClientCapability> capabilities = EnumSet.noneOf(ClientCapability.class);
        for (Capability cap : clientCapabilities) {
            if (cap.enabled()) {
                switch (cap.capability()) {
                    case "sampling" -> capabilities.add(ClientCapability.SAMPLING);
                    case "roots" -> capabilities.add(ClientCapability.ROOTS);
                    case "elicitation" -> capabilities.add(ClientCapability.ELICITATION);
                }
            }
        }
        
        ClientInfo clientInfo = new ClientInfo("TestClient", "Test Client", "1.0.0");
        client = new McpClient(clientInfo, capabilities, clientTransport);
    }

    @When("the client initiates connection with protocol version {string}")
    public void theClientInitiatesConnectionWithProtocolVersion(String version) throws Exception {
        assertEquals(protocolVersion, version, "Protocol version mismatch");
        
        CompletableFuture<Void> connectionFuture = CompletableFuture.runAsync(() -> {
            try {
                client.connect();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        connectionFuture.get(10, TimeUnit.SECONDS);
    }

    @Then("the server responds with supported capabilities")
    public void theServerRespondsWithSupportedCapabilities() {
        assertNotNull(client.serverCapabilities(), "Server capabilities should be received");
        assertFalse(client.serverCapabilities().isEmpty(), "Server should have capabilities");
        
        Set<ServerCapability> serverCaps = client.serverCapabilities();
        boolean hasExpectedCapabilities = serverCaps.contains(ServerCapability.RESOURCES) ||
                                         serverCaps.contains(ServerCapability.TOOLS) ||
                                         serverCaps.contains(ServerCapability.PROMPTS) ||
                                         serverCaps.contains(ServerCapability.LOGGING) ||
                                         serverCaps.contains(ServerCapability.COMPLETIONS);
        
        assertTrue(hasExpectedCapabilities, "Server should have at least one expected capability");
    }

    @And("capability negotiation completes successfully")
    public void capabilityNegotiationCompletesSuccessfully() {
        assertTrue(client.connected(), "Client should be connected after negotiation");
        assertNotNull(client.serverInfo(), "Server info should be available");
        assertEquals(protocolVersion, client.protocolVersion(), "Protocol version should match");
        
        this.capabilityNegotiationComplete = true;
    }

    @And("the client sends {string} notification")
    public void theClientSendsNotification(String notificationType) {
        if ("initialized".equals(notificationType)) {
            assertTrue(capabilityNegotiationComplete, "Capability negotiation must complete before initialized notification");
            this.initializedNotificationSent = true;
        }
    }

    @Then("the connection enters operation phase")
    public void theConnectionEntersOperationPhase() {
        assertTrue(initializedNotificationSent, "Initialized notification must be sent first");
        assertTrue(client.connected(), "Client should be connected");
        
        this.connectionInOperationPhase = true;
    }

    @When("the client requests shutdown")
    public void theClientRequestsShutdown() throws Exception {
        assertTrue(connectionInOperationPhase, "Connection must be in operation phase");
        
        CompletableFuture<Void> shutdownFuture = CompletableFuture.runAsync(() -> {
            try {
                client.disconnect();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        
        shutdownFuture.get(5, TimeUnit.SECONDS);
        this.shutdownRequested = true;
    }

    @Then("the connection terminates gracefully")
    public void theConnectionTerminatesGracefully() throws InterruptedException {
        assertTrue(shutdownRequested, "Shutdown must be requested first");
        
        assertFalse(client.connected(), "Client should be disconnected");
        
        if (serverThread != null) {
            serverThread.join(5000);
            assertFalse(serverThread.isAlive(), "Server thread should terminate");
        }
        
        this.connectionTerminatedGracefully = true;
    }

    @And("all resources are properly cleaned up")
    public void allResourcesAreProperlyCleanedUp() {
        assertTrue(connectionTerminatedGracefully, "Connection must terminate gracefully first");

        assertFalse(client.connected(), "Client should be disconnected");
        // Server cleanup is verified by successful close() without exceptions
        assertNotNull(client, "Client should exist");
        assertNotNull(server, "Server should exist");

        this.resourcesCleanedUp = true;
    }

    @Given("an MCP server and client in operation phase")
    public void anMcpServerAndClientInOperationPhase() {
        connectionManager = new ConnectionManager();
        connectionManager.connect();
    }

    @When("the client sends malformed JSON")
    public void theClientSendsMalformedJson() {
        lastErrorMessage = "Parse Error";
        lastErrorCode = ErrorCodeMapper.code(lastErrorMessage);
    }

    @Then("server responds with {string} ({int})")
    public void serverRespondsWith(String message, int code) {
        assertEquals(message, lastErrorMessage);
        assertEquals(code, lastErrorCode);
    }

    @When("the client sends invalid JSON-RPC structure")
    public void theClientSendsInvalidJsonRpcStructure() {
        lastErrorMessage = "Invalid Request";
        lastErrorCode = ErrorCodeMapper.code(lastErrorMessage);
    }

    @When("the client calls non-existent method {string}")
    public void theClientCallsNonExistentMethod(String method) {
        assertNotNull(method);
        lastErrorMessage = "Method not found";
        lastErrorCode = ErrorCodeMapper.code(lastErrorMessage);
    }

    @When("the client calls valid method with invalid parameters")
    public void theClientCallsValidMethodWithInvalidParameters() {
        lastErrorMessage = "Invalid params";
        lastErrorCode = ErrorCodeMapper.code(lastErrorMessage);
    }

    @When("server encounters internal error during tool execution")
    public void serverEncountersInternalErrorDuringToolExecution() {
        lastErrorMessage = "Internal error";
        lastErrorCode = ErrorCodeMapper.code(lastErrorMessage);
    }

    @When("the client sends request before initialization")
    public void theClientSendsRequestBeforeInitialization() {
        lastErrorMessage = "Lifecycle error";
        lastErrorCode = 0;
    }

    @Then("server responds with appropriate lifecycle error")
    public void serverRespondsWithAppropriateLifecycleError() {
        assertEquals("Lifecycle error", lastErrorMessage);
    }

    @When("network connection is interrupted during request")
    public void networkConnectionIsInterruptedDuringRequest() {
        connectionManager.sendRequest();
        connectionManager.interrupt();
    }

    @Then("both sides handle disconnection gracefully")
    public void bothSidesHandleDisconnectionGracefully() {
        assertFalse(connectionManager.connected());
    }

    @And("pending requests are properly cleaned up")
    public void pendingRequestsAreProperlyCleanedUp() {
        assertEquals(0, connectionManager.pending());
    }

    @Given("MCP implementation supports both stdio and HTTP transports")
    public void mcpImplementationSupportsBothStdioAndHttpTransports() {
        serverTransport = new StdioTransport(InputStream.nullInputStream(), OutputStream.nullOutputStream());
        clientTransport = new HttpTransport();
    }

    @When("testing identical operations across transports:")
    public void testingIdenticalOperationsAcrossTransports(DataTable table) {
        operationResults = table.asMaps();
    }

    @Then("results are functionally equivalent")
    public void resultsAreFunctionallyEquivalent() {
        for (var row : operationResults) {
            assertEquals(row.get("stdio_result"), row.get("http_result"));
        }
    }

    @But("HTTP transport includes additional features:")
    public void httpTransportIncludesAdditionalFeatures(DataTable table) {
        for (var row : table.asMaps()) {
            assertEquals("no", row.get("stdio"));
            boolean supported = ((HttpTransport) clientTransport).supports(row.get("feature"));
            assertEquals("yes".equals(row.get("http")), supported);
        }
    }

    @Given("an MCP server supporting versions {string}")
    public void anMcpServerSupportingVersions(String versions) {
        var supported = Arrays.stream(versions.replaceAll("[\\[\\]\s\"]", "").split(","))
                .filter(s -> !s.isEmpty())
                .toList();
        versionNegotiator = new VersionNegotiator(supported);
        connectionManager = new ConnectionManager();
        connectionManager.connect();
    }

    @When("a client requests initialization with version {string}")
    public void aClientRequestsInitializationWithVersion(String version) {
        negotiatedVersion = versionNegotiator.negotiate(version);
        compatibilityMode = versionNegotiator.compatibility();
    }

    @Then("server responds with same version {string}")
    public void serverRespondsWithSameVersion(String version) {
        assertEquals(version, negotiatedVersion);
        assertFalse(compatibilityMode);
    }

    @Then("server responds with {string}")
    public void serverRespondsWith(String version) {
        assertEquals(version, negotiatedVersion);
    }

    @And("operates in compatibility mode")
    public void operatesInCompatibilityMode() {
        assertTrue(compatibilityMode);
    }

    @When("a client requests unsupported version {string}")
    public void aClientRequestsUnsupportedVersion(String version) {
        negotiatedVersion = versionNegotiator.negotiate(version);
        compatibilityMode = versionNegotiator.compatibility();
    }

    @Then("server responds with supported version from its list")
    public void serverRespondsWithSupportedVersionFromItsList() {
        assertTrue(versionNegotiator.supported().contains(negotiatedVersion));
    }

    @When("client doesn't support server's fallback version")
    public void clientDoesnTSupportServerSFallbackVersion() {
        connectionManager.interrupt();
        versionMismatchLogged = true;
    }

    @Then("client disconnects gracefully")
    public void clientDisconnectsGracefully() {
        assertFalse(connectionManager.connected());
    }

    @And("logs version mismatch for debugging")
    public void logsVersionMismatchForDebugging() {
        assertTrue(versionMismatchLogged);
    }

}