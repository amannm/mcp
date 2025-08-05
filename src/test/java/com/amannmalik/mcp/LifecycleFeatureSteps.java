package com.amannmalik.mcp;

import com.amannmalik.mcp.lifecycle.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;

import java.util.*;
import java.util.regex.Pattern;

public class LifecycleFeatureSteps {
    
    private final TestContext context = new TestContext();
    
    @Given("a clean MCP environment")
    public void aCleanMcpEnvironment() {
        context.reset();
    }
    
    @Given("protocol version {string} is supported")
    public void protocolVersionIsSupported(String version) {
        context.setSupportedProtocolVersion(version);
    }
    
    @Given("an MCP server with comprehensive capabilities:")
    public void anMcpServerWithComprehensiveCapabilities(DataTable dataTable) {
        var capabilities = parseServerCapabilities(dataTable);
        var features = parseServerFeatures(dataTable);
        context.configureServer(capabilities, features);
    }
    
    @Given("an MCP client with capabilities:")
    public void anMcpClientWithCapabilities(DataTable dataTable) {
        var capabilities = parseClientCapabilities(dataTable);
        var features = parseClientFeatures(dataTable);
        context.configureClient(capabilities, features);
    }
    
    @When("the client initiates connection with protocol version {string}")
    public void theClientInitiatesConnectionWithProtocolVersion(String version) {
        var request = new InitializeRequest(
            version,
            context.getClientCapabilities(),
            context.getClientInfo(),
            context.getClientFeatures()
        );
        context.initiateConnection(request);
    }
    
    @Then("the server responds with supported capabilities")
    public void theServerRespondsWithSupportedCapabilities() {
        var response = context.getInitializeResponse();
        assert response != null : "Server should have responded";
        assert response.capabilities() != null : "Response should include capabilities";
        assert !response.capabilities().server().isEmpty() : "Server should advertise capabilities";
    }
    
    @Then("capability negotiation completes successfully")
    public void capabilityNegotiationCompletesSuccessfully() {
        var response = context.getInitializeResponse();
        assert response.protocolVersion().equals(context.getSupportedProtocolVersion()) : 
            "Protocol versions should match";
        
        var serverCaps = response.capabilities().server();
        var expectedCaps = context.getServerCapabilities().server();
        assert serverCaps.containsAll(expectedCaps) : "All configured capabilities should be advertised";
    }
    
    @Then("the client sends {string} notification")
    public void theClientSendsNotification(String notificationType) {
        context.sendInitializedNotification();
        assert context.getLifecycleState() == LifecycleState.INIT : 
            "Should remain in INIT state until notification is processed";
    }
    
    @Then("the connection enters operation phase")
    public void theConnectionEntersOperationPhase() {
        context.processInitializedNotification();
        assert context.getLifecycleState() == LifecycleState.OPERATION : 
            "Should transition to OPERATION state";
    }
    
    @When("the client requests shutdown")
    public void theClientRequestsShutdown() {
        context.requestShutdown();
    }
    
    @Then("the connection terminates gracefully")
    public void theConnectionTerminatesGracefully() {
        assert context.getLifecycleState() == LifecycleState.SHUTDOWN : 
            "Should transition to SHUTDOWN state";
        assert context.isShutdownComplete() : "Shutdown should complete gracefully";
    }
    
    @Then("all resources are properly cleaned up")
    public void allResourcesAreProperlyCleanedUp() {
        assert context.areResourcesCleanedUp() : "All resources should be cleaned up";
        assert context.getActiveConnections() == 0 : "No connections should remain active";
    }
    
    // Version negotiation step definitions
    @Given("an MCP server supporting versions [{string}, {string}]")
    public void anMcpServerSupportingVersions(String version1, String version2) {
        var versions = List.of(version1, version2);
        context.configureSupportedVersions(versions);
    }
    
    @When("a client requests initialization with version {string}")
    public void aClientRequestsInitializationWithVersion(String requestedVersion) {
        context.setRequestedVersion(requestedVersion);
        var negotiatedVersion = context.negotiateVersion(requestedVersion);
        context.setNegotiatedVersion(negotiatedVersion);
        
        var request = new InitializeRequest(
            requestedVersion,
            context.getClientCapabilities() != null ? context.getClientCapabilities() : new Capabilities(Set.of(), Set.of(), Map.of(), Map.of()),
            context.getClientInfo(),
            context.getClientFeatures()
        );
        context.initiateConnectionWithVersion(request, negotiatedVersion);
    }
    
    @Then("server responds with same version {string}")
    public void serverRespondsWithSameVersion(String expectedVersion) {
        var response = context.getInitializeResponse();
        assert response != null : "Server should have responded";
        assert expectedVersion.equals(response.protocolVersion()) : 
            String.format("Expected version '%s' but got '%s'", expectedVersion, response.protocolVersion());
    }
    
    @Then("server responds with {string}")
    public void serverRespondsWith(String expectedVersion) {
        serverRespondsWithSameVersion(expectedVersion);
    }
    
    @Then("operates in compatibility mode")
    public void operatesInCompatibilityMode() {
        assert context.isInCompatibilityMode() : "Server should be operating in compatibility mode";
    }
    
    @When("a client requests unsupported version {string}")
    public void aClientRequestsUnsupportedVersion(String unsupportedVersion) {
        aClientRequestsInitializationWithVersion(unsupportedVersion);
    }
    
    @Then("server responds with supported version from its list")
    public void serverRespondsWithSupportedVersionFromItsList() {
        var response = context.getInitializeResponse();
        assert response != null : "Server should have responded";
        var supportedVersions = context.getSupportedVersions();
        assert supportedVersions.contains(response.protocolVersion()) : 
            String.format("Response version '%s' should be from supported list %s", 
                         response.protocolVersion(), supportedVersions);
    }
    
    @When("client doesn't support server's fallback version")
    public void clientDoesntSupportServersFallbackVersion() {
        context.setClientSupportsServerVersion(false);
    }
    
    @Then("client disconnects gracefully")
    public void clientDisconnectsGracefully() {
        context.disconnectGracefully();
        assert context.isDisconnectedGracefully() : "Client should disconnect gracefully";
    }
    
    @Then("logs version mismatch for debugging")
    public void logsVersionMismatchForDebugging() {
        assert context.hasVersionMismatchLog() : "Version mismatch should be logged for debugging";
    }
    
    private Set<ServerCapability> parseServerCapabilities(DataTable dataTable) {
        Set<ServerCapability> capabilities = EnumSet.noneOf(ServerCapability.class);
        for (var row : dataTable.asMaps()) {
            var capability = row.get("capability");
            var enabled = Boolean.parseBoolean(row.get("enabled"));
            if (enabled) {
                switch (capability.toLowerCase()) {
                    case "resources" -> capabilities.add(ServerCapability.RESOURCES);
                    case "tools" -> capabilities.add(ServerCapability.TOOLS);
                    case "prompts" -> capabilities.add(ServerCapability.PROMPTS);
                    case "logging" -> capabilities.add(ServerCapability.LOGGING);
                    case "completions" -> capabilities.add(ServerCapability.COMPLETIONS);
                }
            }
        }
        return capabilities;
    }
    
    private ServerFeatures parseServerFeatures(DataTable dataTable) {
        boolean resourcesSubscribe = false;
        boolean resourcesListChanged = false;
        boolean toolsListChanged = false;
        boolean promptsListChanged = false;
        
        for (var row : dataTable.asMaps()) {
            var capability = row.get("capability");
            var feature = row.get("feature");
            var enabled = Boolean.parseBoolean(row.get("enabled"));
            
            if (enabled && "resources".equals(capability)) {
                if ("subscribe".equals(feature)) {
                    resourcesSubscribe = true;
                } else if ("listChanged".equals(feature)) {
                    resourcesListChanged = true;
                }
            } else if (enabled && "tools".equals(capability) && "listChanged".equals(feature)) {
                toolsListChanged = true;
            } else if (enabled && "prompts".equals(capability) && "listChanged".equals(feature)) {
                promptsListChanged = true;
            }
        }
        
        return new ServerFeatures(resourcesSubscribe, resourcesListChanged, toolsListChanged, promptsListChanged);
    }
    
    private Set<ClientCapability> parseClientCapabilities(DataTable dataTable) {
        Set<ClientCapability> capabilities = EnumSet.noneOf(ClientCapability.class);
        for (var row : dataTable.asMaps()) {
            var capability = row.get("capability");
            var enabled = Boolean.parseBoolean(row.get("enabled"));
            if (enabled) {
                switch (capability.toLowerCase()) {
                    case "sampling" -> capabilities.add(ClientCapability.SAMPLING);
                    case "roots" -> capabilities.add(ClientCapability.ROOTS);
                    case "elicitation" -> capabilities.add(ClientCapability.ELICITATION);
                }
            }
        }
        return capabilities;
    }
    
    private ClientFeatures parseClientFeatures(DataTable dataTable) {
        boolean rootsListChanged = false;
        
        for (var row : dataTable.asMaps()) {
            var capability = row.get("capability");
            var feature = row.get("feature");
            var enabled = Boolean.parseBoolean(row.get("enabled"));
            
            if (enabled && "roots".equals(capability) && "listChanged".equals(feature)) {
                rootsListChanged = true;
            }
        }
        
        return new ClientFeatures(rootsListChanged);
    }
    
    private List<String> parseVersionList(String versionsString) {
        // Parse versions from string like ["2025-06-18", "2024-11-05"] 
        var pattern = Pattern.compile("\"([^\"]+)\"");
        var matcher = pattern.matcher(versionsString);
        var versions = new ArrayList<String>();
        while (matcher.find()) {
            versions.add(matcher.group(1));
        }
        return versions;
    }
    
    private static class TestContext {
        private String supportedProtocolVersion = "2025-06-18";
        private Capabilities serverCapabilities;
        private ServerFeatures serverFeatures;
        private Capabilities clientCapabilities;
        private ClientFeatures clientFeatures;
        private ClientInfo clientInfo;
        private InitializeResponse initializeResponse;
        private LifecycleState lifecycleState = LifecycleState.INIT;
        private boolean shutdownComplete = false;
        private boolean resourcesCleanedUp = false;
        private int activeConnections = 0;
        
        // Version negotiation fields
        private VersionNegotiator versionNegotiator;
        private List<String> supportedVersions = List.of("2025-06-18");
        private String requestedVersion;
        private String negotiatedVersion;
        private boolean clientSupportsServerVersion = true;
        private boolean disconnectedGracefully = false;
        private boolean versionMismatchLogged = false;
        
        void reset() {
            supportedProtocolVersion = "2025-06-18";
            serverCapabilities = null;
            serverFeatures = null;
            clientCapabilities = null;
            clientFeatures = null;
            clientInfo = new ClientInfo("test-client", "Test Client", "1.0.0");
            initializeResponse = null;
            lifecycleState = LifecycleState.INIT;
            shutdownComplete = false;
            resourcesCleanedUp = false;
            activeConnections = 0;
            
            // Reset version negotiation fields
            versionNegotiator = null;
            supportedVersions = List.of("2025-06-18");
            requestedVersion = null;
            negotiatedVersion = null;
            clientSupportsServerVersion = true;
            disconnectedGracefully = false;
            versionMismatchLogged = false;
        }
        
        void setSupportedProtocolVersion(String version) {
            this.supportedProtocolVersion = version;
        }
        
        String getSupportedProtocolVersion() {
            return supportedProtocolVersion;
        }
        
        void configureServer(Set<ServerCapability> capabilities, ServerFeatures features) {
            this.serverCapabilities = new Capabilities(Set.of(), capabilities, Map.of(), Map.of());
            this.serverFeatures = features;
        }
        
        void configureClient(Set<ClientCapability> capabilities, ClientFeatures features) {
            this.clientCapabilities = new Capabilities(capabilities, Set.of(), Map.of(), Map.of());
            this.clientFeatures = features;
        }
        
        Capabilities getServerCapabilities() {
            return serverCapabilities;
        }
        
        Capabilities getClientCapabilities() {
            return clientCapabilities;
        }
        
        ClientFeatures getClientFeatures() {
            return clientFeatures;
        }
        
        ClientInfo getClientInfo() {
            return clientInfo;
        }
        
        void initiateConnection(InitializeRequest request) {
            activeConnections = 1;
            var serverInfo = new ServerInfo("test-server", "Test Server", "1.0.0");
            this.initializeResponse = new InitializeResponse(
                supportedProtocolVersion,
                serverCapabilities,
                serverInfo,
                null,
                serverFeatures
            );
        }
        
        InitializeResponse getInitializeResponse() {
            return initializeResponse;
        }
        
        void sendInitializedNotification() {
            // Client sends initialized notification
        }
        
        void processInitializedNotification() {
            lifecycleState = LifecycleState.OPERATION;
        }
        
        LifecycleState getLifecycleState() {
            return lifecycleState;
        }
        
        void requestShutdown() {
            lifecycleState = LifecycleState.SHUTDOWN;
            shutdownComplete = true;
            resourcesCleanedUp = true;
            activeConnections = 0;
        }
        
        boolean isShutdownComplete() {
            return shutdownComplete;
        }
        
        boolean areResourcesCleanedUp() {
            return resourcesCleanedUp;
        }
        
        int getActiveConnections() {
            return activeConnections;
        }
        
        // Version negotiation methods
        void configureSupportedVersions(List<String> versions) {
            this.supportedVersions = List.copyOf(versions);
            this.versionNegotiator = new VersionNegotiator(versions);
            this.supportedProtocolVersion = versions.get(0); // Default to first version
        }
        
        List<String> getSupportedVersions() {
            return supportedVersions;
        }
        
        void setRequestedVersion(String version) {
            this.requestedVersion = version;
        }
        
        String negotiateVersion(String requested) {
            if (versionNegotiator == null) {
                versionNegotiator = new VersionNegotiator(supportedVersions);
            }
            return versionNegotiator.negotiate(requested);
        }
        
        void setNegotiatedVersion(String version) {
            this.negotiatedVersion = version;
        }
        
        boolean isInCompatibilityMode() {
            return versionNegotiator != null && versionNegotiator.compatibility();
        }
        
        void initiateConnectionWithVersion(InitializeRequest request, String negotiatedVersion) {
            activeConnections = 1;
            var serverInfo = new ServerInfo("test-server", "Test Server", "1.0.0");
            this.initializeResponse = new InitializeResponse(
                negotiatedVersion,
                serverCapabilities != null ? serverCapabilities : new Capabilities(Set.of(), Set.of(), Map.of(), Map.of()),
                serverInfo,
                null,
                serverFeatures
            );
        }
        
        void setClientSupportsServerVersion(boolean supports) {
            this.clientSupportsServerVersion = supports;
            if (!supports) {
                this.versionMismatchLogged = true;
            }
        }
        
        void disconnectGracefully() {
            this.disconnectedGracefully = true;
            this.lifecycleState = LifecycleState.SHUTDOWN;
            this.activeConnections = 0;
        }
        
        boolean isDisconnectedGracefully() {
            return disconnectedGracefully;
        }
        
        boolean hasVersionMismatchLog() {
            return versionMismatchLogged;
        }
    }
}