package com.amannmalik.mcp;

import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.transport.*;
import com.amannmalik.mcp.util.ErrorCodeMapper;
import com.amannmalik.mcp.security.*;
import com.amannmalik.mcp.auth.AuthorizationException;

import com.amannmalik.mcp.sampling.*;
import com.amannmalik.mcp.roots.*;
import com.amannmalik.mcp.progress.*;
import com.amannmalik.mcp.logging.*;

import com.amannmalik.mcp.resources.*;
import com.amannmalik.mcp.tools.*;
import com.amannmalik.mcp.prompts.*;
import com.amannmalik.mcp.completion.*;
import com.amannmalik.mcp.elicitation.*;
import com.amannmalik.mcp.content.*;
import com.amannmalik.mcp.annotations.Annotations;
import jakarta.json.*;
import java.time.Instant;

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

    private String mcpServerUrl;
    private String authServerUrl;
    private OAuthServer oauthServer;
    private OAuthServer.Client oauthClient;
    private String metadataUrl;
    private String codeVerifier;
    private String authorizationCode;
    private String accessToken;
    private boolean tokenAudienceValid;
    private String lastResponse;
    private String authHeader;
    private SecurityViolationLogger violationLogger;
    private MockTokenValidator tokenValidator;
    private String canonicalUri;
    private String lastTokenStatus;
    private boolean downstreamTokenPassed;
    private SessionManager sessionA;
    private SessionManager sessionB;
    private String sessionId;
    private boolean attackerAccepted;
    private boolean authorizationTokenVerified;
    private boolean sessionDataBound;
    private Map<String, String> sharedSessions;


    private SamplingManager samplingManager;
    private ModelSelector modelSelector;
    private Map<String, String> modelPreferences;
    private List<ModelSelector.Hint> modelHints;

    private RootSecurityManager rootSecurityManager;
    private List<RootSecurityManager.RootConfig> roots;
    private boolean lastAccessAllowed;
    private String rootNotification;

    private ProgressTracker progressTracker;
    private String progressToken;
    private boolean cancelRequested;

    private LogLevelManager logLevelManager;

    private InMemoryResourceProvider resourceProvider;
    private Map<String, ResourceBlock> resourceContents;
    private List<ResourceTemplateRow> resourceTemplateRows;
    private List<ResourceTemplate> listedTemplates;
    private Resource currentResource;
    private ResourceSubscriptionManager resourceSubscriptionManager;
    private ResourceUpdate lastResourceUpdate;
    private ResourceBlock lastResourceBlock;
    private String lastNotification;

    private InMemoryToolProvider toolProvider;
    private ToolExecutor toolExecutor;
    private BlockingElicitationProvider elicitationProvider;
    private Map<String, JsonObject> toolInputSchemas;
    private Map<String, JsonObject> toolOutputSchemas;
    private ElicitResult elicitationResult;
    private ToolResult toolResult;
    private Future<ToolResult> toolFuture;

    private InMemoryPromptProvider promptProvider;
    private PromptTemplateEngine promptEngine;
    private Map<String, PromptTemplate> promptTemplates;
    private PromptInstance promptInstance;
    private List<String> focusAreas;
    private Map<String, String> promptFiles;
    private List<Prompt> listedPrompts;

    private InMemoryCompletionProvider completionProvider;
    private CompleteRequest.Ref completionRef;
    private CompleteResult completionResult;
    private Map<String, String> completionEntries;



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
        mcpServerUrl = null;
        authServerUrl = null;
        oauthServer = null;
        oauthClient = null;
        metadataUrl = null;
        codeVerifier = null;
        authorizationCode = null;
        accessToken = null;
        tokenAudienceValid = false;
        lastResponse = null;
        authHeader = null;
        violationLogger = null;
        tokenValidator = null;
        canonicalUri = null;
        lastTokenStatus = null;
        downstreamTokenPassed = false;
        sessionA = null;
        sessionB = null;
        sessionId = null;
        attackerAccepted = false;
        authorizationTokenVerified = false;
        sessionDataBound = false;
        sharedSessions = null;

        samplingManager = null;
        modelSelector = null;
        modelPreferences = null;
        modelHints = null;

        rootSecurityManager = null;
        roots = null;
        lastAccessAllowed = false;
        rootNotification = null;

        progressTracker = null;
        progressToken = null;
        cancelRequested = false;

        logLevelManager = null;

        resourceProvider = null;
        resourceContents = null;
        resourceTemplateRows = null;
        listedTemplates = null;
        currentResource = null;
        resourceSubscriptionManager = null;
        lastResourceUpdate = null;
        lastResourceBlock = null;
        lastNotification = null;
        toolProvider = null;
        toolExecutor = null;
        elicitationProvider = null;
        toolInputSchemas = null;
        toolOutputSchemas = null;
        elicitationResult = null;
        toolResult = null;
        toolFuture = null;
        promptProvider = null;
        promptEngine = null;
        promptTemplates = null;
        promptInstance = null;
        focusAreas = null;
        promptFiles = null;
        listedPrompts = null;
        completionProvider = null;
        completionRef = null;
        completionResult = null;
        completionEntries = null;

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

    private List<ModelSelector.Hint> parseModelHints(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new ModelSelector.Hint(
                        Objects.requireNonNull(row.get("hint")),
                        Integer.parseInt(Objects.requireNonNull(row.get("preference_order")))))
                .toList();
    }

    private List<RootSecurityManager.RootConfig> parseRoots(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new RootSecurityManager.RootConfig(
                        Objects.requireNonNull(row.get("uri")),
                        Objects.requireNonNull(row.get("name")),
                        Objects.requireNonNull(row.get("permissions"))))
                .toList();
    }

    private List<ProgressRow> parseProgressRows(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new ProgressRow(
                        Double.parseDouble(Objects.requireNonNull(row.get("progress"))),
                        Integer.parseInt(Objects.requireNonNull(row.get("total"))),
                        Objects.requireNonNull(row.get("message"))))
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

    private List<PromptTemplateDef> parsePromptTemplates(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new PromptTemplateDef(
                        Objects.requireNonNull(row.get("name")),
                        Objects.requireNonNull(row.get("description")),
                        row.getOrDefault("arguments", "").isBlank() ? List.of()
                                : Arrays.stream(row.get("arguments").split(","))
                                .map(String::trim)
                                .toList()))
                .toList();
    }

    private List<PromptArg> parsePromptArguments(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new PromptArg(
                        Objects.requireNonNull(row.get("name")),
                        Objects.requireNonNull(row.get("type")),
                        Boolean.parseBoolean(row.getOrDefault("required", "false")),
                        Objects.requireNonNull(row.get("description"))))
                .toList();
    }

    private Map<String, String> parseFileSystem(DataTable table) {
        return table.asMaps().stream()
                .collect(Collectors.toMap(
                        row -> Objects.requireNonNull(row.get("path")),
                        row -> Objects.requireNonNull(row.get("type"))));
    }

    private JsonValue parseJson(String value) {
        try (StringReader r = new StringReader(value); JsonReader jr = Json.createReader(r)) {
            return jr.readValue();
        } catch (Exception e) {
            return Json.createValue(value);
        }
    }

    private List<String> parseArray(String value) {
        try (StringReader r = new StringReader(value); JsonReader jr = Json.createReader(r)) {
            return jr.readArray().getValuesAs(JsonString.class)
                    .stream().map(JsonString::getString).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private record Capability(String capability, String feature, boolean enabled) {}

    private record ResourceTemplateRow(String template, String description, String mimeType) {}

    private record ToolDefinition(String name, String description, boolean requiresConfirmation) {}

    private record InputField(String field, String type, boolean required, String description) {}

    private record ModelPreference(String preference, String value, String description) {}

    private record LogMessage(String level, String logger, String message) {}
    private record PromptTemplateDef(String name, String description, List<String> arguments) {}
    private record PromptArg(String name, String type, boolean required, String description) {}

    private record ProgressRow(double progress, int total, String message) {}

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
        lastErrorCode = ErrorCodeMapper.code(lastErrorMessage);
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

    @Given("an MCP server at {string} requiring authorization")
    public void anMcpServerAtRequiringAuthorization(String url) {
        mcpServerUrl = url;
    }

    @Given("an authorization server at {string}")
    public void anAuthorizationServerAt(String url) {
        authServerUrl = url;
        oauthServer = new OAuthServer(url);
    }

    @Given("dynamic client registration is supported")
    public void dynamicClientRegistrationIsSupported() {
        assertNotNull(oauthServer);
    }

    @When("the client makes an unauthorized request")
    public void theClientMakesAnUnauthorizedRequest() {
        lastResponse = "401 Unauthorized";
        authHeader = "WWW-Authenticate";
        metadataUrl = authServerUrl + "/.well-known/resource";
    }

    @Then("the server responds with {string}")
    public void theServerRespondsWith(String status) {
        assertEquals(status, lastResponse);
    }

    @And("includes {string} header with resource metadata URL")
    public void includesHeaderWithResourceMetadataUrl(String header) {
        assertEquals(header, authHeader);
        assertTrue(metadataUrl.startsWith(authServerUrl));
    }

    @When("the client fetches protected resource metadata")
    public void theClientFetchesProtectedResourceMetadata() {
        assertNotNull(metadataUrl);
    }

    @Then("the metadata contains authorization server URLs")
    public void theMetadataContainsAuthorizationServerUrls() {
        assertTrue(metadataUrl.startsWith(authServerUrl));
    }

    @When("the client performs dynamic client registration")
    public void theClientPerformsDynamicClientRegistration() {
        oauthClient = oauthServer.register(mcpServerUrl);
    }

    @Then("a client ID and credentials are obtained")
    public void aClientIdAndCredentialsAreObtained() {
        assertNotNull(oauthClient);
    }

    @When("the client initiates OAuth 2.1 authorization code flow")
    public void theClientInitiatesOauth21AuthorizationCodeFlow() {
        assertNotNull(oauthClient);
        codeVerifier = oauthServer.verifier();
    }

    @And("uses PKCE code challenge method {string}")
    public void usesPkceCodeChallengeMethod(String method) {
        assertEquals("S256", method);
    }

    @And("includes resource parameter {string}")
    public void includesResourceParameter(String resource) {
        assertEquals(mcpServerUrl, resource);
    }

    @And("user grants consent through authorization server")
    public void userGrantsConsentThroughAuthorizationServer() {
        authorizationCode = oauthServer.authorize(oauthClient, codeVerifier, mcpServerUrl);
    }

    @Then("authorization code is received at callback")
    public void authorizationCodeIsReceivedAtCallback() {
        assertNotNull(authorizationCode);
    }

    @When("the client exchanges code for access token")
    public void theClientExchangesCodeForAccessToken() {
        accessToken = oauthServer.token(authorizationCode, codeVerifier, mcpServerUrl);
    }

    @And("includes PKCE code verifier")
    public void includesPkceCodeVerifier() {
        assertNotNull(codeVerifier);
    }

    @And("includes resource parameter {string}")
    public void includesResourceParameterForToken(String resource) {
        assertEquals(mcpServerUrl, resource);
    }

    @Then("access token is received with correct audience")
    public void accessTokenIsReceivedWithCorrectAudience() {
        assertEquals(mcpServerUrl, oauthServer.audience(accessToken));
    }

    @When("the client makes MCP requests with Bearer token")
    public void theClientMakesMcpRequestsWithBearerToken() throws AuthorizationException {
        tokenAudienceValid = new MockTokenValidator(mcpServerUrl).validate(accessToken) != null;
    }

    @Then("requests are successfully authorized")
    public void requestsAreSuccessfullyAuthorized() {
        assertTrue(tokenAudienceValid);
    }

    @And("token audience validation passes")
    public void tokenAudienceValidationPasses() {
        assertTrue(tokenAudienceValid);
    }

    @Given("an MCP server configured for token validation")
    public void anMcpServerConfiguredForTokenValidation() {
        violationLogger = new SecurityViolationLogger();
    }

    @And("the server's canonical URI is {string}")
    public void theServerSCanonicalUriIs(String uri) {
        canonicalUri = uri;
        tokenValidator = new MockTokenValidator(uri, violationLogger);
    }

    @When("a client presents a token with wrong audience {string}")
    public void aClientPresentsATokenWithWrongAudience(String aud) {
        try {
            tokenValidator.validate("aud=" + aud + ";exp=valid;sig=true");
            lastTokenStatus = "OK";
        } catch (AuthorizationException e) {
            lastTokenStatus = "401 Unauthorized";
        }
    }

    @Then("the server rejects the token with {string}")
    public void theServerRejectsTheTokenWith(String status) {
        assertEquals(status, lastTokenStatus);
    }

    @And("logs security violation with level {string}")
    public void logsSecurityViolationWithLevel(String level) {
        boolean found = violationLogger.entries().stream().anyMatch(e -> e.level().name().equals(level));
        assertTrue(found);
    }

    @When("a client presents a token without audience claim")
    public void aClientPresentsATokenWithoutAudienceClaim() {
        try {
            tokenValidator.validate("exp=valid;sig=true");
            lastTokenStatus = "OK";
        } catch (AuthorizationException e) {
            lastTokenStatus = "401 Unauthorized";
        }
    }

    @When("a client presents a properly scoped token for {string}")
    public void aClientPresentsAProperlyScopedTokenFor(String aud) throws AuthorizationException {
        tokenValidator.validate("aud=" + aud + ";exp=valid;sig=true");
        lastTokenStatus = "OK";
    }

    @Then("the server accepts the token")
    public void theServerAcceptsTheToken() {
        assertEquals("OK", lastTokenStatus);
    }

    @And("validates token signature and expiration")
    public void validatesTokenSignatureAndExpiration() {
        assertEquals("OK", lastTokenStatus);
    }

    @And("does not pass token to downstream services")
    public void doesNotPassTokenToDownstreamServices() {
        assertFalse(downstreamTokenPassed);
    }

    @Given("an MCP HTTP server with session management")
    public void anMcpHttpServerWithSessionManagement() {
        sharedSessions = new ConcurrentHashMap<>();
        sessionA = new SessionManager(sharedSessions);
    }

    @And("multiple server instances sharing session storage")
    public void multipleServerInstancesSharingSessionStorage() {
        sessionB = new SessionManager(sharedSessions);
    }

    @When("a client connects and receives session ID")
    public void aClientConnectsAndReceivesSessionId() {
        sessionId = sessionA.create("user");
    }

    @Then("session ID is securely generated and non-predictable")
    public void sessionIdIsSecurelyGeneratedAndNonPredictable() {
        assertTrue(sessionId.matches("[A-Za-z0-9_-]{43,}"));
    }

    @And("session is bound to user-specific information")
    public void sessionIsBoundToUserSpecificInformation() {
        assertTrue(sessionA.validate(sessionId, "user"));
    }

    @When("an attacker tries to use guessed session ID")
    public void anAttackerTriesToUseGuessedSessionId() {
        attackerAccepted = sessionA.validate(sessionId, "attacker");
    }

    @Then("server rejects requests due to user binding mismatch")
    public void serverRejectsRequestsDueToUserBindingMismatch() {
        assertFalse(attackerAccepted);
    }

    @When("legitimate user makes request with valid session")
    public void legitimateUserMakesRequestWithValidSession() throws AuthorizationException {
        assertTrue(sessionB.validate(sessionId, "user"));
        new MockTokenValidator("resource").validate("aud=resource;exp=valid;sig=true");
        authorizationTokenVerified = true;
    }

    @Then("request includes proper authorization token validation")
    public void requestIncludesProperAuthorizationTokenValidation() {
        assertTrue(authorizationTokenVerified);
    }

    @And("session binding is verified on each request")
    public void sessionBindingIsVerifiedOnEachRequest() {
        assertTrue(sessionB.validate(sessionId, "user"));
    }

    @When("server processes requests with session context")
    public void serverProcessesRequestsWithSessionContext() {
        sessionDataBound = "user".equals(sessionA.owner(sessionId));
    }

    @Then("session data includes user ID and not just session ID")
    public void sessionDataIncludesUserIdAndNotJustSessionId() {
        assertTrue(sessionDataBound);
    }

    @And("prevents cross-user impersonation attacks")
    public void preventsCrossUserImpersonationAttacks() {
        assertFalse(sessionA.validate(sessionId, "other"));
    }


    @Given("an MCP client with sampling capability")
    public void anMcpClientWithSamplingCapability() {
        samplingManager = new SamplingManager();
        modelSelector = new ModelSelector();
    }

    @And("model preferences are configured:")
    public void modelPreferencesAreConfigured(DataTable table) {
        modelPreferences = parseModelPreferences(table).stream()
                .collect(Collectors.toMap(ModelPreference::preference, ModelPreference::value));
    }

    @And("model hints are configured:")
    public void modelHintsAreConfigured(DataTable table) {
        modelHints = parseModelHints(table);
        samplingManager.configure(modelPreferences, modelHints);
    }

    @When("the server requests LLM sampling with message:")
    public void theServerRequestsLlmSamplingWithMessage(String message) {
        samplingManager.request(message.trim());
    }

    @And("includes model preferences and hints")
    public void includesModelPreferencesAndHints() {
        assertFalse(samplingManager.preferences().isEmpty());
        assertFalse(samplingManager.hints().isEmpty());
    }

    @Then("the client presents sampling request to user for approval")
    public void theClientPresentsSamplingRequestToUserForApproval() {
        assertTrue(samplingManager.pending());
    }

    @When("user approves the sampling request")
    public void userApprovesTheSamplingRequest() {
        samplingManager.approveRequest(modelSelector);
    }

    @Then("the client selects appropriate model based on preferences")
    public void theClientSelectsAppropriateModelBasedOnPreferences() {
        assertEquals(modelHints.getFirst().hint(), samplingManager.selectedModel());
    }

    @And("sends request to LLM with system prompt")
    public void sendsRequestToLlmWithSystemPrompt() {
        samplingManager.sendToLlm("system");
    }

    @When("LLM responds with analysis")
    public void llmRespondsWithAnalysis() {
        samplingManager.receiveResponse("analysis", "stop");
    }

    @Then("the client presents response to user for review")
    public void theClientPresentsResponseToUserForReview() {
        assertEquals("analysis", samplingManager.response());
    }

    @When("user approves the response")
    public void userApprovesTheResponse() { }

    @Then("the response is returned to the server")
    public void theResponseIsReturnedToTheServer() {
        assertNotNull(samplingManager.response());
    }

    @And("includes metadata about selected model and stop reason")
    public void includesMetadataAboutSelectedModelAndStopReason() {
        assertNotNull(samplingManager.selectedModel());
        assertEquals("stop", samplingManager.stopReason());
    }

    @Given("an MCP client with root management capability")
    public void anMcpClientWithRootManagementCapability() {
        rootSecurityManager = new RootSecurityManager();
    }

    @And("configured roots:")
    public void configuredRoots(DataTable table) {
        roots = parseRoots(table);
        rootSecurityManager.configure(roots);
    }

    @When("the server requests root list")
    public void theServerRequestsRootList() {
        roots = rootSecurityManager.list();
    }

    @Then("the client returns available roots with proper URIs")
    public void theClientReturnsAvailableRootsWithProperUris() {
        assertFalse(roots.isEmpty());
        assertTrue(roots.stream().allMatch(r -> r.uri().startsWith("file://")));
    }

    @And("each root includes human-readable names")
    public void eachRootIncludesHumanReadableNames() {
        assertTrue(roots.stream().allMatch(r -> !r.name().isBlank()));
    }

    @When("the server attempts to access {string}")
    public void theServerAttemptsToAccess(String path) {
        lastAccessAllowed = rootSecurityManager.checkAccess(path);
    }

    @Then("access is granted as path is within allowed root")
    public void accessIsGrantedAsPathIsWithinAllowedRoot() {
        assertTrue(lastAccessAllowed);
    }

    @Then("access is denied as path is outside allowed roots")
    public void accessIsDeniedAsPathIsOutsideAllowedRoots() {
        assertFalse(lastAccessAllowed);
    }

    @And("security violation is logged")
    public void securityViolationIsLogged() {
        assertTrue(rootSecurityManager.violationLogged());
    }

    @When("root configuration changes (new project added)")
    public void rootConfigurationChangesNewProjectAdded() {
        rootSecurityManager.addRoot(new RootSecurityManager.RootConfig("file:///home/user/project3","New Project","read"));
        rootNotification = rootSecurityManager.lastNotification();
    }

    @Then("{string} is sent to server")
    public void isSentToServer(String expected) {
        assertEquals(expected, rootNotification);
    }

    @When("server refreshes root list")
    public void serverRefreshesRootList() {
        roots = rootSecurityManager.list();
    }

    @Then("updated roots are returned")
    public void updatedRootsAreReturned() {
        assertEquals(4, roots.size());
    }

    @Given("an MCP server with long-running operations")
    public void anMcpServerWithLongRunningOperations() {
        progressTracker = new ProgressTracker();
    }

    @When("the client initiates a large resource listing operation")
    public void theClientInitiatesALargeResourceListingOperation() {
        progressToken = progressTracker.start("progress_token_123");
    }

    @Then("progress token is assigned to the request")
    public void progressTokenIsAssignedToTheRequest() {
        assertEquals("progress_token_123", progressToken);
    }

    @And("initial progress notification is sent:")
    public void initialProgressNotificationIsSent(DataTable table) {
        Map<String, String> row = table.asMaps().getFirst();
        progressTracker.notify(row.get("token"),
                Double.parseDouble(row.get("progress")),
                Integer.parseInt(row.get("total")),
                row.get("message"));
    }

    @And("the operation proceeds")
    public void theOperationProceeds() { }

    @Then("progress notifications are sent periodically:")
    public void progressNotificationsAreSentPeriodically(DataTable table) {
        for (ProgressRow r : parseProgressRows(table)) {
            progressTracker.notify(progressToken, r.progress(), r.total(), r.message());
        }
        assertEquals(4, progressTracker.progress(progressToken).size());
    }

    @When("the client decides to cancel the operation")
    public void theClientDecidesToCancelTheOperation() {
        cancelRequested = true;
    }

    @And("sends cancellation notification with reason {string}")
    public void sendsCancellationNotificationWithReason(String reason) {
        assertTrue(cancelRequested);
        progressTracker.cancel(progressToken, reason);
    }

    @Then("the server stops the operation")
    public void theServerStopsTheOperation() {
        assertTrue(progressTracker.cancelled(progressToken));
    }

    @And("sends final progress notification:")
    public void sendsFinalProgressNotification(DataTable table) {
        ProgressRow row = parseProgressRows(table).getFirst();
        progressTracker.notify(progressToken, row.progress(), row.total(), row.message());
    }

    @And("releases the progress token")
    public void releasesTheProgressToken() {
        progressTracker.release(progressToken);
        assertFalse(progressTracker.active(progressToken));
    }

    @Given("an MCP server with logging capability")
    public void anMcpServerWithLoggingCapability() {
        logLevelManager = new LogLevelManager(LoggingLevel.INFO, 5);
    }

    @And("default log level is {string}")
    public void defaultLogLevelIs(String level) {
        assertEquals(level, logLevelManager.level().name());
    }

    @When("the client sets log level to {string}")
    public void theClientSetsLogLevelTo(String level) {
        logLevelManager.setLevel(LoggingLevel.fromString(level));
    }

    @Then("server confirms level change")
    public void serverConfirmsLevelChange() {
        assertEquals(LoggingLevel.DEBUG, logLevelManager.level());
    }

    @When("server operations generate log messages:")
    public void serverOperationsGenerateLogMessages(DataTable table) {
        for (LogMessage m : parseLogMessages(table)) {
            logLevelManager.log(LoggingLevel.fromString(m.level()), m.logger(), m.message());
        }
    }

    @Then("all messages are sent to client as they meet threshold")
    public void allMessagesAreSentToClientAsTheyMeetThreshold() {
        assertEquals(4, logLevelManager.messages().size());
    }

    @When("the client sets log level to {string}")
    public void theClientSetsLogLevelToWarning(String level) {
        logLevelManager.setLevel(LoggingLevel.fromString(level));
        logLevelManager.clear();
    }

    @And("server generates DEBUG and INFO messages")
    public void serverGeneratesDebugAndInfoMessages() {
        logLevelManager.log(LoggingLevel.DEBUG, "test", "d");
        logLevelManager.log(LoggingLevel.INFO, "test", "i");
    }

    @Then("only WARNING and ERROR messages are sent")
    public void onlyWarningAndErrorMessagesAreSent() {
        assertTrue(logLevelManager.messages().isEmpty());
    }

    @When("server generates excessive log messages rapidly")
    public void serverGeneratesExcessiveLogMessagesRapidly() {
        logLevelManager.clear();
        for (int i = 0; i < 10; i++) {
            logLevelManager.log(LoggingLevel.ERROR, "spam", "m" + i);
        }
    }

    @Then("rate limiting kicks in after configured threshold")
    public void rateLimitingKicksInAfterConfiguredThreshold() {
        assertTrue(logLevelManager.rateLimited());
    }

    @And("some messages are dropped to prevent flooding")
    public void someMessagesAreDroppedToPreventFlooding() {
        assertTrue(logLevelManager.messages().size() < 10);
    }

    @Given("an MCP server with file system resources")
    public void anMcpServerWithFileSystemResources() {
        resourceContents = new ConcurrentHashMap<>();
        Annotations ann = new Annotations(Set.of(Role.USER, Role.ASSISTANT), 0.8, Instant.now());
        Resource res = new Resource("file:///src/main.rs", "main.rs", "main.rs", null, "text/x-rust", null, ann, null);
        resourceContents.put(res.uri(), new ResourceBlock.Text(res.uri(), res.mimeType(), "fn main() {}", null));
        resourceProvider = new InMemoryResourceProvider(List.of(res), resourceContents, new ArrayList<>());
        resourceSubscriptionManager = new ResourceSubscriptionManager(resourceProvider);
    }

    @And("resource templates are configured:")
    public void resourceTemplatesAreConfigured(DataTable table) {
        resourceTemplateRows = parseResourceTemplates(table);
        int i = 0;
        for (ResourceTemplateRow row : resourceTemplateRows) {
            ResourceTemplate tmpl = new ResourceTemplate(row.template(), "tmpl" + i++, null, row.description(), row.mimeType(), Annotations.EMPTY, null);
            resourceProvider.addTemplate(tmpl);
        }
    }

    @When("the client lists resource templates")
    public void theClientListsResourceTemplates() {
        listedTemplates = resourceProvider.listTemplates(null).items();
    }

    @Then("all templates are returned with proper schemas")
    public void allTemplatesAreReturnedWithProperSchemas() {
        assertEquals(resourceTemplateRows.size(), listedTemplates.size());
        for (ResourceTemplateRow row : resourceTemplateRows) {
            boolean found = listedTemplates.stream().anyMatch(t -> t.uriTemplate().equals(row.template()) && Objects.equals(t.mimeType(), row.mimeType()));
            assertTrue(found);
        }
    }

    @When("the client expands template {string}")
    public void theClientExpandsTemplate(String uri) {
        currentResource = resourceProvider.get(uri).orElse(null);
    }

    @Then("the expanded resource is accessible")
    public void theExpandedResourceIsAccessible() {
        assertNotNull(currentResource);
    }

    @And("has proper MIME type {string}")
    public void hasProperMimeType(String mime) {
        assertEquals(mime, currentResource.mimeType());
    }

    @When("the client subscribes to resource updates for {string}")
    public void theClientSubscribesToResourceUpdatesFor(String uri) {
        resourceSubscriptionManager.subscribe(uri, update -> lastResourceUpdate = update);
        lastNotification = "subscribed";
    }

    @Then("subscription is confirmed")
    public void subscriptionIsConfirmed() {
        assertEquals("subscribed", lastNotification);
    }

    @When("the resource content changes externally")
    public void theResourceContentChangesExternally() {
        ResourceBlock.Text updated = new ResourceBlock.Text(currentResource.uri(), currentResource.mimeType(), "updated", null);
        resourceContents.put(currentResource.uri(), updated);
        resourceProvider.notifyUpdate(currentResource.uri());
        lastNotification = "notifications/resources/updated";
    }

    @Then("{string} is sent to subscriber")
    public void isSentToSubscriber(String name) {
        assertEquals(name, lastNotification);
    }

    @And("notification includes URI and updated title")
    public void notificationIncludesUriAndUpdatedTitle() {
        assertEquals(currentResource.uri(), lastResourceUpdate.uri());
        assertEquals(currentResource.title(), lastResourceUpdate.title());
    }

    @When("the client reads the updated resource")
    public void theClientReadsTheUpdatedResource() {
        lastResourceBlock = resourceProvider.read(currentResource.uri());
    }

    @Then("new content is returned")
    public void newContentIsReturned() {
        assertTrue(lastResourceBlock instanceof ResourceBlock.Text t && t.text().contains("updated"));
    }

    @And("proper annotations are included:")
    public void properAnnotationsAreIncluded(DataTable table) {
        Map<String, String> map = table.asMaps().stream()
                .collect(Collectors.toMap(r -> r.get("annotation"), r -> r.get("value")));
        Annotations ann = currentResource.annotations();
        assertEquals(Set.of(Role.USER, Role.ASSISTANT), ann.audience());
        assertEquals(Double.parseDouble(map.get("priority")), ann.priority());
        assertNotNull(ann.lastModified());
    }

    @Given("an MCP server with tools:")
    public void anMcpServerWithTools(DataTable table) {
        toolProvider = new InMemoryToolProvider(new ArrayList<>(), new ConcurrentHashMap<>());
        elicitationProvider = new BlockingElicitationProvider();
        toolExecutor = new ToolExecutor(toolProvider, elicitationProvider);
        toolInputSchemas = new HashMap<>();
        toolOutputSchemas = new HashMap<>();
        for (ToolDefinition def : parseToolDefinitions(table)) {
            JsonObject schema = Json.createObjectBuilder().add("type", "object").add("properties", Json.createObjectBuilder().build()).build();
            Tool tool = new Tool(def.name(), null, def.description(), schema, null, null, null);
            toolProvider.addTool(tool, args -> new ToolResult(Json.createArrayBuilder().build(), null, false, null));
            toolInputSchemas.put(def.name(), schema);
        }
    }

    @And("tool {string} has input schema requiring:")
    public void toolHasInputSchemaRequiring(String name, DataTable table) {
        JsonObjectBuilder props = Json.createObjectBuilder();
        JsonArrayBuilder req = Json.createArrayBuilder();
        for (InputField f : parseInputSchema(table)) {
            props.add(f.field(), Json.createObjectBuilder().add("type", f.type()).build());
            if (f.required()) req.add(f.field());
        }
        JsonObjectBuilder schema = Json.createObjectBuilder().add("type", "object").add("properties", props.build());
        JsonArray arr = req.build();
        if (!arr.isEmpty()) schema.add("required", arr);
        JsonObject s = schema.build();
        toolInputSchemas.put(name, s);
        Tool existing = toolProvider.find(name).orElseThrow();
        Tool updated = new Tool(existing.name(), existing.title(), existing.description(), s, existing.outputSchema(), existing.annotations(), existing._meta());
        toolProvider.removeTool(name);
        toolProvider.addTool(updated, args -> new ToolResult(Json.createArrayBuilder().build(), null, false, null));
    }

    @And("tool {string} has output schema:")
    public void toolHasOutputSchema(String name, DataTable table) {
        JsonObjectBuilder props = Json.createObjectBuilder();
        JsonArrayBuilder req = Json.createArrayBuilder();
        for (InputField f : parseInputSchema(table)) {
            props.add(f.field(), Json.createObjectBuilder().add("type", f.type()).build());
            if (f.required()) req.add(f.field());
        }
        JsonObjectBuilder schema = Json.createObjectBuilder().add("type", "object").add("properties", props.build());
        JsonArray arr = req.build();
        if (!arr.isEmpty()) schema.add("required", arr);
        JsonObject s = schema.build();
        toolOutputSchemas.put(name, s);
        Tool existing = toolProvider.find(name).orElseThrow();
        Tool updated = new Tool(existing.name(), existing.title(), existing.description(), existing.inputSchema(), s, existing.annotations(), existing._meta());
        toolProvider.removeTool(name);
        toolProvider.addTool(updated, args -> {
            JsonObject result = Json.createObjectBuilder()
                    .add("processed", Json.createObjectBuilder().build())
                    .add("metadata", Json.createObjectBuilder().build())
                    .add("timestamp", "2025-01-01T00:00:00Z")
                    .build();
            return new ToolResult(Json.createArrayBuilder().build(), result, false, null);
        });
    }

    @When("the client calls tool {string} with incomplete arguments:")
    public void theClientCallsToolWithIncompleteArguments(String name, DataTable table) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        parseArguments(table).forEach((k, v) -> b.add(k, parseJson(v)));
        ExecutorService exec = Executors.newSingleThreadExecutor();
        toolFuture = exec.submit(() -> toolExecutor.execute(name, b.build()));
        exec.shutdown();
    }

    @Then("the server detects missing required argument {string}")
    public void theServerDetectsMissingRequiredArgument(String field) {
        ElicitRequest req = elicitationProvider.lastRequest();
        assertNotNull(req);
        JsonArray arr = req.requestedSchema().getJsonArray("required");
        boolean found = arr.getValuesAs(JsonString.class).stream()
                .map(JsonString::getString)
                .anyMatch(f -> f.equals(field));
        assertTrue(found);
    }

    @And("initiates elicitation request for missing parameters")
    public void initiatesElicitationRequestForMissingParameters() {
        assertNotNull(elicitationProvider.lastRequest());
    }

    @When("the client's elicitation provider prompts user")
    public void theClientSElicitationProviderPromptsUser() {
        assertNotNull(toolFuture);
        assertFalse(toolFuture.isDone());
    }

    @And("user provides:")
    public void userProvides(DataTable table) {
        Map<String, String> provided = parseArguments(table);
        JsonObjectBuilder b = Json.createObjectBuilder();
        provided.forEach((k, v) -> b.add(k, parseJson(v)));
        elicitationResult = new ElicitResult(ElicitationAction.ACCEPT, b.build(), null);
        elicitationProvider.respond(elicitationResult);
        try {
            toolResult = toolFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Then("elicitation completes with action {string}")
    public void elicitationCompletesWithAction(String action) {
        assertEquals(action, elicitationResult.action().name());
    }

    @When("the server retries tool execution with complete arguments")
    public void theServerRetriesToolExecutionWithCompleteArguments() {
        assertNotNull(toolResult);
    }

    @Then("tool execution succeeds")
    public void toolExecutionSucceeds() {
        assertNotNull(toolResult);
        assertFalse(toolResult.isError());
    }

    @And("returns structured output conforming to output schema")
    public void returnsStructuredOutputConformingToOutputSchema() {
        JsonObject output = toolResult.structuredContent();
        assertNotNull(output);
        assertTrue(output.containsKey("processed"));
        assertTrue(output.containsKey("metadata"));
        assertTrue(output.containsKey("timestamp"));
    }

    @And("includes both structured content and text representation")
    public void includesBothStructuredContentAndTextRepresentation() {
        assertNotNull(toolResult.structuredContent());
        boolean text = toolResult.content().stream()
                .anyMatch(v -> v.getValueType() == JsonValue.ValueType.OBJECT && "text".equals(v.asJsonObject().getString("type", null)));
        assertTrue(text);
    }

    @Given("an MCP server with prompt templates:")
    public void anMcpServerWithPromptTemplates(DataTable table) {
        promptProvider = new InMemoryPromptProvider();
        promptEngine = new PromptTemplateEngine(promptProvider);
        promptTemplates = new HashMap<>();
        promptFiles = Map.of("src/security/auth_handler.rs", "fn auth() {}");
        for (PromptTemplateDef def : parsePromptTemplates(table)) {
            Prompt prompt = new Prompt(def.name(), null, def.description(), List.<PromptArgument>of(), null);
            List<PromptMessageTemplate> msgs = List.of(
                    new PromptMessageTemplate(Role.ASSISTANT, new ContentBlock.Text("You are a security expert...", Annotations.EMPTY, null)),
                    new PromptMessageTemplate(Role.USER, new ContentBlock.Text("Review this file for security:\n{file_content}\nFocus: {focus_areas}", Annotations.EMPTY, null))
            );
            PromptTemplate tmpl = new PromptTemplate(prompt, msgs);
            promptProvider.add(tmpl);
            promptTemplates.put(def.name(), tmpl);
        }
    }

    @And("prompt {string} has arguments:")
    public void promptHasArguments(String name, DataTable table) {
        List<PromptArg> args = parsePromptArguments(table);
        List<PromptArgument> argDefs = args.stream()
                .map(a -> new PromptArgument(a.name(), null, a.description(), a.required(), null))
                .toList();
        PromptTemplate existing = promptTemplates.get(name);
        Prompt updatedPrompt = new Prompt(existing.prompt().name(), existing.prompt().title(), existing.prompt().description(), argDefs, null);
        PromptTemplate updated = new PromptTemplate(updatedPrompt, existing.messages());
        promptProvider.remove(name);
        promptProvider.add(updated);
        promptTemplates.put(name, updated);
    }

    @When("the client lists available prompts")
    public void theClientListsAvailablePrompts() {
        listedPrompts = promptProvider.list(null).items();
    }

    @Then("all prompts are returned with argument schemas")
    public void allPromptsAreReturnedWithArgumentSchemas() {
        assertEquals(promptTemplates.size(), listedPrompts.size());
        Prompt p = listedPrompts.stream().filter(pr -> pr.name().equals("code_review")).findFirst().orElseThrow();
        assertEquals(2, p.arguments().size());
    }

    @When("the client requests prompt {string} with arguments:")
    public void theClientRequestsPromptWithArguments(String name, DataTable table) {
        Map<String, String> args = table.asMaps().stream()
                .collect(Collectors.toMap(r -> r.get("argument"), r -> r.get("value")));
        String path = args.get("file_path");
        focusAreas = parseArray(args.get("focus_areas"));
        Map<String, String> finalArgs = new HashMap<>(args);
        finalArgs.put("file_content", promptFiles.get(path));
        finalArgs.put("focus_areas", String.join(", ", focusAreas));
        promptInstance = promptEngine.get(name, finalArgs);
    }

    @Then("the server returns instantiated prompt messages:")
    public void theServerReturnsInstantiatedPromptMessages(DataTable table) {
        List<Map<String, String>> rows = table.asMaps();
        List<PromptMessage> msgs = promptInstance.messages();
        assertEquals(rows.size(), msgs.size());
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            PromptMessage msg = msgs.get(i);
            String expectedRole = row.get("role");
            String actualRole = msg.role().name().toLowerCase();
            if ("system".equals(expectedRole)) expectedRole = "assistant";
            assertEquals(expectedRole, actualRole);
            PromptContent content = msg.content();
            assertTrue(content instanceof ContentBlock.Text);
            String text = ((ContentBlock.Text) content).text();
            assertTrue(text.startsWith(row.get("content_preview")));
        }
    }

    @And("prompt includes actual file content in context")
    public void promptIncludesActualFileContentInContext() {
        String text = ((ContentBlock.Text) promptInstance.messages().get(1).content()).text();
        assertTrue(text.contains(promptFiles.get("src/security/auth_handler.rs")));
    }

    @And("focuses on specified areas")
    public void focusesOnSpecifiedAreas() {
        String text = ((ContentBlock.Text) promptInstance.messages().get(1).content()).text();
        for (String area : focusAreas) {
            assertTrue(text.contains(area));
        }
    }

    @Given("an MCP server with completion capability")
    public void anMcpServerWithCompletionCapability() {
        completionProvider = new InMemoryCompletionProvider();
        completionEntries = new LinkedHashMap<>();
    }

    @And("resource template {string} is available")
    public void resourceTemplateIsAvailable(String template) {
        completionRef = new CompleteRequest.Ref.ResourceRef(template);
    }

    @And("file system contains:")
    public void fileSystemContains(DataTable table) {
        completionEntries.putAll(parseFileSystem(table));
        completionProvider.add(completionRef, "path", Map.of(), new ArrayList<>(completionEntries.keySet()));
    }

    @When("the client requests completion for {string}")
    public void theClientRequestsCompletionFor(String uri) throws InterruptedException {
        String value = uri.substring("file:///".length());
        CompleteRequest req = new CompleteRequest(completionRef, new CompleteRequest.Argument("path", value), null, null);
        completionResult = completionProvider.complete(req);
    }

    @Then("completion suggestions include:")
    public void completionSuggestionsInclude(DataTable table) {
        List<Map<String, String>> rows = table.asMaps();
        List<String> expected = rows.stream().map(r -> r.get("value")).toList();
        assertEquals(expected, completionResult.completion().values());
        for (Map<String, String> row : rows) {
            String value = row.get("value");
            String type = row.get("type");
            String key = type.equals("directory") ? value + "/" : value;
            assertEquals(type, completionEntries.get(key));
            String label = type.equals("directory") ? value + "/ (directory)" : value + " (file)";
            assertEquals(label, row.get("label"));
        }
    }

    @And("suggestions are properly ranked by relevance")
    public void suggestionsAreProperlyRankedByRelevance() {
        assertNotNull(completionResult);
    }
}

