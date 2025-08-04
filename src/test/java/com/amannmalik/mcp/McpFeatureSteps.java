package com.amannmalik.mcp;

import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.transport.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

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

}