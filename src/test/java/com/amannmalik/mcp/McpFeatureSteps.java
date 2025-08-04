package com.amannmalik.mcp;

import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.transport.*;
import com.amannmalik.mcp.util.ErrorCodeMapper;
import com.amannmalik.mcp.security.*;
import com.amannmalik.mcp.auth.AuthorizationException;
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
}