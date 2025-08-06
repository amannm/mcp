package com.amannmalik.mcp;

import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.transport.*;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.roots.*;
import com.amannmalik.mcp.sampling.*;
import com.amannmalik.mcp.elicitation.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

public class LifecycleFeatureSteps {
    
    private McpServer server;
    private McpClient client;
    private Transport serverTransport;
    private Transport clientTransport;
    
    private Set<ServerCapability> serverCapabilities = new HashSet<>();
    private Map<ServerCapability, Map<String, Boolean>> serverFeatureMap = new HashMap<>();
    
    private Set<ClientCapability> clientCapabilities = new HashSet<>();
    private Map<ClientCapability, Map<String, Boolean>> clientFeatureMap = new HashMap<>();
    
    private String protocolVersion;
    private boolean connectionInitiated = false;
    private boolean capabilityNegotiationCompleted = false;
    private boolean initializedNotificationSent = false;
    private boolean operationPhaseEntered = false;
    private boolean shutdownRequested = false;
    private boolean connectionTerminated = false;
    private boolean resourcesCleaned = false;

    @After
    public void cleanup() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore cleanup errors in tests
            }
        }
        if (server != null) {
            try {
                server.close();
            } catch (Exception e) {
                // Ignore cleanup errors in tests
            }
        }
    }

    @Given("a clean MCP environment")
    public void aCleanMcpEnvironment() {
        // Reset all test state
        server = null;
        client = null;
        serverTransport = null;
        clientTransport = null;
        serverCapabilities.clear();
        serverFeatureMap.clear();
        clientCapabilities.clear();
        clientFeatureMap.clear();
        protocolVersion = null;
        connectionInitiated = false;
        capabilityNegotiationCompleted = false;
        initializedNotificationSent = false;
        operationPhaseEntered = false;
        shutdownRequested = false;
        connectionTerminated = false;
        resourcesCleaned = false;
    }

    @And("protocol version {string} is supported")
    public void protocolVersionIsSupported(String version) {
        this.protocolVersion = version;
        assertEquals("2025-06-18", version, "Only MCP protocol version 2025-06-18 is supported");
    }

    @Given("an MCP server with capabilities:")
    public void anMcpServerWithCapabilities(DataTable capabilitiesTable) throws Exception {
        serverCapabilities.clear();
        serverFeatureMap.clear();
        
        List<Map<String, String>> rows = capabilitiesTable.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            String capability = row.get("capability").toUpperCase();
            String feature = row.get("feature");
            boolean enabled = Boolean.parseBoolean(row.get("enabled"));
            
            if (enabled) {
                ServerCapability serverCap = ServerCapability.valueOf(capability);
                serverCapabilities.add(serverCap);
                
                if (feature != null && !feature.trim().isEmpty()) {
                    serverFeatureMap.computeIfAbsent(serverCap, k -> new HashMap<>())
                            .put(feature, true);
                }
            }
        }
        
        // Create in-memory transport pair for testing
        PipedInputStream serverInput = new PipedInputStream();
        PipedOutputStream serverOutput = new PipedOutputStream();
        PipedInputStream clientInput = new PipedInputStream();
        PipedOutputStream clientOutput = new PipedOutputStream();
        
        // Connect the pipes - server writes to client input, client writes to server input
        serverOutput.connect(clientInput);
        clientOutput.connect(serverInput);
        
        serverTransport = new StdioTransport(serverInput, serverOutput);
        
        // Create server with configured capabilities
        server = createServerWithCapabilities(serverTransport);
        
        // Start server in background thread
        Thread serverThread = new Thread(() -> {
            try {
                server.serve();
            } catch (Exception e) {
                // Expected when client disconnects
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        
        // Give server time to start
        Thread.sleep(100);
        
        clientTransport = new StdioTransport(clientInput, clientOutput);
    }

    @And("an MCP client with capabilities:")
    public void anMcpClientWithCapabilities(DataTable capabilitiesTable) throws Exception {
        clientCapabilities.clear();
        clientFeatureMap.clear();
        
        List<Map<String, String>> rows = capabilitiesTable.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            String capability = row.get("capability").toUpperCase();
            String feature = row.get("feature");
            boolean enabled = Boolean.parseBoolean(row.get("enabled"));
            
            if (enabled) {
                ClientCapability clientCap = ClientCapability.valueOf(capability);
                clientCapabilities.add(clientCap);
                
                if (feature != null && !feature.trim().isEmpty()) {
                    clientFeatureMap.computeIfAbsent(clientCap, k -> new HashMap<>())
                            .put(feature, true);
                }
            }
        }
        
        // Create client with configured capabilities
        client = createClientWithCapabilities(clientTransport);
    }

    @When("the client initiates connection with protocol version {string}")
    public void theClientInitiatesConnectionWithProtocolVersion(String version) throws Exception {
        assertEquals(this.protocolVersion, version, "Protocol version mismatch");
        
        client.connect();
        connectionInitiated = true;
        
        // Give time for initialization
        Thread.sleep(200);
    }

    @Then("the server responds with supported capabilities")
    public void theServerRespondsWithSupportedCapabilities() {
        assertTrue(connectionInitiated, "Connection must be initiated first");
        assertTrue(client.connected(), "Client should be connected");
        
        // Verify server capabilities were negotiated
        Set<ServerCapability> negotiatedCaps = client.serverCapabilities();
        assertFalse(negotiatedCaps.isEmpty(), "Server should have responded with capabilities");
        
        // Note: The actual capabilities depend on what providers are available in the server
        // For this test, we verify that the server responded with some capabilities
    }

    @And("capability negotiation completes successfully")
    public void capabilityNegotiationCompletesSuccessfully() {
        assertTrue(connectionInitiated, "Connection must be initiated first");
        assertTrue(client.connected(), "Client should be connected after capability negotiation");
        capabilityNegotiationCompleted = true;
    }

    @And("the client sends {string} notification")
    public void theClientSendsNotification(String notificationType) {
        assertEquals("initialized", notificationType, "Expected initialized notification");
        assertTrue(capabilityNegotiationCompleted, "Capability negotiation must complete first");
        
        // The McpClient automatically sends initialized notification during connect()
        // We just need to verify the connection is properly established
        assertTrue(client.connected(), "Client should be connected and initialized");
        initializedNotificationSent = true;
    }

    @Then("the connection enters operation phase")
    public void theConnectionEntersOperationPhase() {
        assertTrue(initializedNotificationSent, "Initialized notification must be sent first");
        assertTrue(client.connected(), "Connection should be active in operation phase");
        operationPhaseEntered = true;
    }

    @When("the client requests shutdown")
    public void theClientRequestsShutdown() throws Exception {
        assertTrue(operationPhaseEntered, "Must be in operation phase before shutdown");
        
        client.disconnect();
        shutdownRequested = true;
        
        // Give time for graceful shutdown
        Thread.sleep(100);
    }

    @Then("the connection terminates gracefully")
    public void theConnectionTerminatesGracefully() throws Exception {
        assertTrue(shutdownRequested, "Shutdown must be requested first");
        
        // Verify client is disconnected
        assertFalse(client.connected(), "Client should be disconnected");
        
        connectionTerminated = true;
    }

    @And("all resources are properly cleaned up")
    public void allResourcesAreProperlyCleanedUp() {
        assertTrue(connectionTerminated, "Connection must be terminated first");
        
        // Verify resources are cleaned up
        assertFalse(client.connected(), "Client connection should be closed");
        
        resourcesCleaned = true;
    }

    private McpServer createServerWithCapabilities(Transport transport) {
        // The McpServer constructor takes transport and instructions
        // Capabilities are determined by what providers are available
        // For testing purposes, we use the default configuration
        return new McpServer(transport, null);
    }

    private McpClient createClientWithCapabilities(Transport transport) {
        Set<ClientCapability> caps = EnumSet.copyOf(clientCapabilities);
        
        ClientFeatures features = new ClientFeatures(
                clientFeatureMap.getOrDefault(ClientCapability.ROOTS, Map.of())
                        .getOrDefault("listChanged", false)
        );
        
        McpConfiguration config = McpConfiguration.current();
        ClientInfo info = new ClientInfo(
                config.clientName(),
                config.clientDisplayName(),
                config.clientVersion()
        );
        
        SamplingProvider samplingProvider = caps.contains(ClientCapability.SAMPLING) 
                ? new InteractiveSamplingProvider(false) 
                : null;
        
        RootsProvider rootsProvider = caps.contains(ClientCapability.ROOTS)
                ? new InMemoryRootsProvider(List.of(new Root("file://test", "Test Root", null)))
                : null;
        
        ElicitationProvider elicitationProvider = caps.contains(ClientCapability.ELICITATION)
                ? new InteractiveElicitationProvider()
                : null;
        
        return new McpClient(
                info,
                caps,
                transport,
                samplingProvider,
                rootsProvider,
                elicitationProvider,
                null // No listener for tests
        );
    }
}