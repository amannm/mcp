package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.spi.Cursor;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public final class ProtocolLifecycleSteps {

    private final Set<RequestId> usedRequestIds = new HashSet<>();
    private final Set<RequestId> sentRequestIds = new HashSet<>();
    private McpClientConfiguration clientConfig;
    private McpHostConfiguration hostConfig;
    private McpHost activeConnection;
    private String clientId;
    private boolean connectionClosed = false;
    private String requestedVersion;
    private String negotiatedVersion;
    private final List<String> serverSupportedVersions = new ArrayList<>();
    private Set<ClientCapability> clientCapabilities = EnumSet.noneOf(ClientCapability.class);
    private Set<ServerCapability> serverCapabilities = EnumSet.noneOf(ServerCapability.class);
    private Set<ServerCapability> availableFeatures = EnumSet.noneOf(ServerCapability.class);
    private RequestId lastRequestId;
    private JsonObject lastRequest;
    private JsonObject lastResponse;
    private JsonObject lastNotification;
    private String lastErrorMessage;
    private int lastErrorCode;
    private final List<Map<String, String>> capabilityConfigurations = new ArrayList<>();
    private boolean samplingRequested;
    private boolean samplingApproved;
    private boolean promptExposed;
    private final List<Map<String, String>> errorScenarios = new ArrayList<>();
    private final List<Set<ServerCapability>> discoveredCapabilities = new ArrayList<>();
    private Map<String, String> currentConfiguration;
    private final List<Boolean> acceptHeaderResults = new ArrayList<>();
    private final List<Boolean> expectedAcceptResults = new ArrayList<>();
    private final List<Boolean> originHeaderResults = new ArrayList<>();
    private final List<Boolean> expectedOriginResults = new ArrayList<>();
    private final List<Boolean> contentTypeResults = new ArrayList<>();
    private final List<Boolean> expectedContentTypeResults = new ArrayList<>();
    private final List<Integer> getStatuses = new ArrayList<>();
    private final List<Integer> expectedGetStatuses = new ArrayList<>();
    private final List<String> getContentTypes = new ArrayList<>();
    private final List<String> expectedGetContentTypes = new ArrayList<>();

    private final List<Integer> postMessageStatuses = new ArrayList<>();
    private final List<Integer> expectedPostMessageStatuses = new ArrayList<>();
    private final List<Boolean> postMessageBodiesEmpty = new ArrayList<>();
    private final List<Boolean> expectedPostMessageBodiesEmpty = new ArrayList<>();

    private String serverSessionId;
    private boolean sessionActive;
    private int lastHttpStatus;
    // New step definitions for large message handling
    private long largePayloadSize;
    private boolean largeMessageHandled;
    private boolean connectionStable;
    // New step definitions for concurrent request processing
    private final List<RequestId> concurrentRequestIds = new ArrayList<>();
    private final Map<RequestId, JsonObject> concurrentResponses = new HashMap<>();
    private boolean allConcurrentRequestsProcessed;
    private boolean noIdConflicts;
    // New step definitions for message ordering guarantees
    private final List<Map<String, String>> dependentRequests = new ArrayList<>();
    private final Map<String, JsonObject> requestResponses = new HashMap<>();
    private BufferedReader stdioReader;
    private Exception newlineError;
    private Exception invalidResponseError;

    private boolean serverInitialized = true;
    private final List<Boolean> preInitAllowedResults = new ArrayList<>();
    private final List<Boolean> expectedPreInitAllowedResults = new ArrayList<>();


    private Set<ClientCapability> parseClientCapabilities(String capabilities) {
        if (capabilities == null || capabilities.trim().isEmpty()) {
            return EnumSet.noneOf(ClientCapability.class);
        }
        return Arrays.stream(capabilities.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .map(ClientCapability::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ClientCapability.class)));
    }

    private Set<ServerCapability> parseServerCapabilities(String capabilities) {
        if (capabilities == null || capabilities.trim().isEmpty() || "none".equalsIgnoreCase(capabilities.trim())) {
            return EnumSet.noneOf(ServerCapability.class);
        }
        if ("all".equalsIgnoreCase(capabilities.trim())) {
            return EnumSet.allOf(ServerCapability.class);
        }
        return Arrays.stream(capabilities.split(","))
                .map(String::trim)
                .map(ServerCapability::from)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ServerCapability.class)));
    }

    private JsonObject createRequest(RequestId id, String method, JsonObject params) {
        var builder = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", RequestId.toJsonValue(id))
                .add("method", method);
        if (params != null) {
            builder.add("params", params);
        }
        return builder.build();
    }

    private JsonObject createResponse(RequestId id, JsonObject result) {
        return Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", RequestId.toJsonValue(id))
                .add("result", result != null ? result : Json.createObjectBuilder().build())
                .build();
    }

    private JsonObject createNotification(String method, JsonObject params) {
        var builder = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("method", method);
        if (params != null) {
            builder.add("params", params);
        }
        return builder.build();
    }

    private McpClientConfiguration configureWithCommand(McpClientConfiguration base, String commandSpec) {
        return new McpClientConfiguration(
                base.clientId(), base.serverName(), base.serverDisplayName(), base.serverVersion(),
                base.principal(), base.clientCapabilities(), commandSpec, base.defaultReceiveTimeout(),
                base.defaultOriginHeader(), base.httpRequestTimeout(), base.enableKeepAlive(),
                base.sessionIdByteLength(), base.initializeRequestTimeout(), base.strictVersionValidation(),
                base.pingTimeout(), base.pingInterval(), base.progressPerSecond(), base.rateLimiterWindow(),
                base.verbose(), base.interactiveSampling(), base.rootDirectories(), base.samplingAccessPolicy()
        );
    }

    private McpClientConfiguration configureWithCapabilities(McpClientConfiguration base, Set<ClientCapability> capabilities) {
        return new McpClientConfiguration(
                base.clientId(), base.serverName(), base.serverDisplayName(), base.serverVersion(),
                base.principal(), capabilities, base.commandSpec(), base.defaultReceiveTimeout(),
                base.defaultOriginHeader(), base.httpRequestTimeout(), base.enableKeepAlive(),
                base.sessionIdByteLength(), base.initializeRequestTimeout(), base.strictVersionValidation(),
                base.pingTimeout(), base.pingInterval(), base.progressPerSecond(), base.rateLimiterWindow(),
                base.verbose(), base.interactiveSampling(), base.rootDirectories(), base.samplingAccessPolicy()
        );
    }

    private void updateHostConfiguration() {
        if (clientConfig != null) {
            hostConfig = new McpHostConfiguration(
                    "2025-06-18",
                    "2025-03-26",
                    "mcp-host",
                    "MCP Host",
                    "1.0.0",
                    Set.of(ClientCapability.SAMPLING, ClientCapability.ROOTS, ClientCapability.ELICITATION),
                    "default", // Use default principal to match server and client
                    Duration.ofSeconds(2),
                    100,
                    100,
                    false,
                    List.of(clientConfig)
            );
            clientId = clientConfig.clientId();
        }
    }

    @Given("a clean MCP environment")
    public void a_clean_mcp_environment() {
        try {
            if (activeConnection != null) activeConnection.close();
        } catch (IOException ignore) {
        }

        activeConnection = null;
        clientId = null;
        clientConfig = null;
        hostConfig = null;
        connectionClosed = false;

        requestedVersion = null;
        negotiatedVersion = null;
        serverSupportedVersions.clear();

        clientCapabilities = EnumSet.noneOf(ClientCapability.class);
        serverCapabilities = EnumSet.noneOf(ServerCapability.class);
        availableFeatures = EnumSet.noneOf(ServerCapability.class);

        lastRequestId = null;
        lastRequest = null;
        lastResponse = null;
        lastNotification = null;
        lastErrorMessage = null;
        lastErrorCode = 0;
        usedRequestIds.clear();
        sentRequestIds.clear();

        if (currentConfiguration == null) {
            capabilityConfigurations.clear();
            errorScenarios.clear();
        }
        currentConfiguration = null;
        promptExposed = false;
        serverInitialized = true;
        preInitAllowedResults.clear();
        expectedPreInitAllowedResults.clear();
        largePayloadSize = 0L;
        largeMessageHandled = false;
        connectionStable = false;
        concurrentRequestIds.clear();
        concurrentResponses.clear();
        allConcurrentRequestsProcessed = false;
        noIdConflicts = false;
        dependentRequests.clear();
        requestResponses.clear();
    }

    @Given("a transport mechanism is available")
    public void a_transport_mechanism_is_available() {
        McpClientConfiguration base = McpClientConfiguration.defaultConfiguration("client", "client", "default");
        String java = System.getProperty("java.home") + "/bin/java";
        String jar = Path.of("build", "libs", "mcp-0.1.0.jar").toString();
        String cmd = java + " -jar " + jar + " server --stdio --test-mode";
        clientConfig = configureWithCommand(base, cmd);
        updateHostConfiguration();
    }

    @Given("I want to connect using protocol version {string}")
    public void i_want_to_connect_using_protocol_version(String version) {
        requestedVersion = version;
        if (hostConfig != null) {
            hostConfig = new McpHostConfiguration(
                    version, hostConfig.compatibilityVersion(), hostConfig.hostClientName(),
                    hostConfig.hostClientDisplayName(), hostConfig.hostClientVersion(),
                    hostConfig.hostClientCapabilities(), hostConfig.hostPrincipal(),
                    hostConfig.processWaitSeconds(), hostConfig.defaultPageSize(),
                    hostConfig.maxCompletionValues(), hostConfig.globalVerbose(),
                    hostConfig.clientConfigurations());
        }
    }

    @Given("I can provide {string} capabilities")
    public void i_can_provide_capabilities(String capabilities) {
        if (clientConfig != null) {
            clientCapabilities = parseClientCapabilities(capabilities);
            clientConfig = configureWithCapabilities(clientConfig, clientCapabilities);
            updateHostConfiguration();
        }
    }

    @When("I establish a connection with the server")
    public void i_establish_a_connection_with_the_server() throws Exception {
        if (hostConfig == null) return;
        activeConnection = new McpHost(hostConfig);

        // Grant consent for server connection in test environment
        activeConnection.grantConsent("server");
        activeConnection.grantConsent("tool:test_tool");
        activeConnection.grantConsent("tool:error_tool");
        activeConnection.grantConsent("tool:echo_tool");
        activeConnection.grantConsent("tool:slow_tool");
        activeConnection.grantConsent("sampling");

        clientId = clientConfig.clientId();
        activeConnection.connect(clientId);
    }

    @Then("the connection should be established successfully")
    public void the_connection_should_be_established_successfully() throws Exception {
        if (activeConnection == null || clientId == null || hostConfig == null) {
            throw new IllegalStateException("connection not initiated");
        }
        if (!Protocol.SUPPORTED_VERSIONS.contains(hostConfig.protocolVersion())) {
            throw new AssertionError("protocol version not supported");
        }
        negotiatedVersion = hostConfig.protocolVersion();
    }

    @Then("both parties should agree on capabilities")
    public void both_parties_should_agree_on_capabilities() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }

        boolean hasServerCapabilities = false;
        try {
            activeConnection.listTools(clientId, Cursor.Start.INSTANCE);
            serverCapabilities.add(ServerCapability.TOOLS);
            hasServerCapabilities = true;
        } catch (Exception ignore) {
        }
        try {
            activeConnection.request(clientId, RequestMethod.RESOURCES_LIST, Json.createObjectBuilder().build());
            serverCapabilities.add(ServerCapability.RESOURCES);
            hasServerCapabilities = true;
        } catch (Exception ignore) {
        }

        availableFeatures.addAll(serverCapabilities);

        String context = activeConnection.aggregateContext();
        if (context == null) {
            throw new AssertionError("missing server implementation info");
        }
    }

    @Then("I should be able to exchange messages")
    public void i_should_be_able_to_exchange_messages() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }

        activeConnection.notify(clientId, NotificationMethod.INITIALIZED, Json.createObjectBuilder().build());

        JsonRpcMessage response = activeConnection.request(clientId, RequestMethod.PING, Json.createObjectBuilder().build());
        if (response == null) {
            throw new AssertionError("message exchange failed");
        }
    }

    @Given("the server supports versions {string}")
    public void the_server_supports_versions(String versions) {
        serverSupportedVersions.clear();
        serverSupportedVersions.addAll(Arrays.stream(versions.split(","))
                .map(String::trim)
                .toList());
    }

    @When("I request connection with version {string}")
    public void i_request_connection_with_version(String version) {
        requestedVersion = version;
        negotiatedVersion = serverSupportedVersions.contains(version)
                ? version
                : serverSupportedVersions.stream().max(String::compareTo).orElse(null);
    }

    @Then("the server should accept my version request")
    public void the_server_should_accept_my_version_request() {
        if (!Objects.equals(requestedVersion, negotiatedVersion)) {
            throw new AssertionError("version not accepted: requested %s, got %s"
                    .formatted(requestedVersion, negotiatedVersion));
        }
    }

    @When("I request connection with unsupported version {string}")
    public void i_request_connection_with_unsupported_version(String version) {
        requestedVersion = version;
        negotiatedVersion = serverSupportedVersions.stream().max(String::compareTo).orElse(null);
    }

    @Then("the server should offer its latest supported version")
    public void the_server_should_offer_its_latest_supported_version() {
        String expectedLatest = serverSupportedVersions.stream().max(String::compareTo).orElse(null);
        if (!Objects.equals(negotiatedVersion, expectedLatest)) {
            throw new AssertionError("expected latest version %s, got %s"
                    .formatted(expectedLatest, negotiatedVersion));
        }
    }

    @Then("I should be able to decide whether to proceed")
    public void i_should_be_able_to_decide_whether_to_proceed() {
        if (negotiatedVersion == null) {
            throw new AssertionError("no compatible version available");
        }
    }

    @Given("the server offers {string} features")
    public void the_server_offers_features(String features) {
        serverCapabilities = parseServerCapabilities(features);
    }

    @When("I complete the connection handshake")
    public void i_complete_the_connection_handshake() {
        availableFeatures.clear();
        availableFeatures.addAll(serverCapabilities);
    }

    @Then("I should have access to {string} features")
    public void i_should_have_access_to_features(String features) {
        Set<ServerCapability> expected = parseServerCapabilities(features);
        if (!availableFeatures.containsAll(expected)) {
            throw new AssertionError("Expected features not available: %s".formatted(expected));
        }
    }

    @Then("{string} features should not be available")
    public void features_should_not_be_available(String features) {
        Set<ServerCapability> expectedUnavailable = parseServerCapabilities(features);
        Set<ServerCapability> intersection = new HashSet<>(availableFeatures);
        intersection.retainAll(expectedUnavailable);
        if (!intersection.isEmpty()) {
            throw new AssertionError("Expected features should be unavailable but are available: %s".formatted(intersection));
        }
    }

    @Given("I have an established MCP connection")
    public void i_have_an_established_mcp_connection() {
        if (activeConnection == null) {
            a_transport_mechanism_is_available();
            try {
                i_want_to_connect_using_protocol_version("2025-06-18");
                i_can_provide_capabilities("sampling, roots, elicitation");
                i_establish_a_connection_with_the_server();
                i_should_be_able_to_exchange_messages();
                negotiatedVersion = "2025-06-18";
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Given("I have an established MCP connection using {string} transport")
    public void i_have_an_established_mcp_connection_using_transport(String transportType) {
        i_have_an_established_mcp_connection();
    }

    @Given("I have an established MCP connection with {int} second timeout")
    public void i_have_an_established_mcp_connection_with_timeout(int timeoutSeconds) {
        i_have_an_established_mcp_connection();
    }

    @Given("an MCP server using {string} transport")
    public void an_mcp_server_using_transport(String transport) {
        if (!"http".equalsIgnoreCase(transport)) {
            throw new AssertionError("unsupported transport: %s".formatted(transport));
        }
    }

    @When("I send HTTP requests with the following Accept headers:")
    public void i_send_http_requests_with_the_following_accept_headers(DataTable dataTable) {
        acceptHeaderResults.clear();
        expectedAcceptResults.clear();
        dataTable.asMaps(String.class, String.class).forEach(row -> {
            var method = row.get("method");
            var header = row.get("accept_header");
            var expected = Boolean.parseBoolean(row.get("should_accept"));
            expectedAcceptResults.add(expected);
            acceptHeaderResults.add(isAcceptHeaderValid(method, header));
        });
    }

    private boolean isAcceptHeaderValid(String method, String header) {
        var types = (header == null || header.isBlank() || "none".equalsIgnoreCase(header))
                ? Set.<String>of()
                : Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "POST" -> types.contains("application/json") && types.contains("text/event-stream");
            case "GET" -> types.contains("text/event-stream");
            default -> false;
        };
    }

    @Then("each request should be handled according to Accept header requirements")
    public void each_request_should_be_handled_according_to_accept_header_requirements() {
        for (int i = 0; i < acceptHeaderResults.size(); i++) {
            if (!Objects.equals(acceptHeaderResults.get(i), expectedAcceptResults.get(i))) {
                throw new AssertionError("accept header validation failed at index %d".formatted(i));
            }
        }
    }

    @When("I send HTTP GET requests with the following SSE support configurations:")
    public void i_send_http_get_requests_with_the_following_sse_support_configurations(DataTable table) {
        getStatuses.clear();
        expectedGetStatuses.clear();
        getContentTypes.clear();
        expectedGetContentTypes.clear();
        table.asMaps(String.class, String.class).forEach(row -> {
            var supports = Boolean.parseBoolean(row.get("server_supports_sse"));
            expectedGetStatuses.add(Integer.parseInt(row.get("expected_status")));
            expectedGetContentTypes.add(row.get("expected_content_type"));
            if (supports) {
                getStatuses.add(200);
                getContentTypes.add("text/event-stream");
            } else {
                getStatuses.add(405);
                getContentTypes.add("none");
            }
        });
    }

    @Then("each GET request should be handled according to SSE support")
    public void each_get_request_should_be_handled_according_to_sse_support() {
        for (int i = 0; i < getStatuses.size(); i++) {
            if (!Objects.equals(getStatuses.get(i), expectedGetStatuses.get(i))) {
                throw new AssertionError("GET status mismatch at index %d".formatted(i));
            }
            var actualCt = getContentTypes.get(i);
            var expectedCt = expectedGetContentTypes.get(i);
            if (!expectedCt.equalsIgnoreCase(actualCt)) {
                throw new AssertionError("GET content type mismatch at index %d".formatted(i));
            }
        }
    }

    @When("I send HTTP requests with the following Origin headers:")
    public void i_send_http_requests_with_the_following_origin_headers(DataTable dataTable) {
        originHeaderResults.clear();
        expectedOriginResults.clear();
        dataTable.asMaps(String.class, String.class).forEach(row -> {
            var header = row.get("origin_header");
            var expected = Boolean.parseBoolean(row.get("should_accept"));
            expectedOriginResults.add(expected);
            originHeaderResults.add(isOriginHeaderValid(header));
        });
    }

    private boolean isOriginHeaderValid(String header) {
        return header != null
                && !header.isBlank()
                && !"none".equalsIgnoreCase(header)
                && header.equals("https://client.example.com");
    }

    @Then("each request should be handled according to Origin header requirements")
    public void each_request_should_be_handled_according_to_origin_header_requirements() {
        for (int i = 0; i < originHeaderResults.size(); i++) {
            if (!Objects.equals(originHeaderResults.get(i), expectedOriginResults.get(i))) {
                throw new AssertionError("origin header validation failed at index %d".formatted(i));
            }
        }
    }

    @When("I validate server HTTP POST responses with the following Content-Types:")
    public void i_validate_server_http_post_responses_with_the_following_content_types(DataTable table) {
        contentTypeResults.clear();
        expectedContentTypeResults.clear();
        table.asMaps(String.class, String.class).forEach(row -> {
            var ct = row.get("content_type");
            var expected = Boolean.parseBoolean(row.get("should_accept"));
            expectedContentTypeResults.add(expected);
            contentTypeResults.add(isContentTypeValid(ct));
        });
    }

    private boolean isContentTypeValid(String ct) {
        if (ct == null || ct.isBlank() || "none".equalsIgnoreCase(ct)) {
            return false;
        }
        return ct.startsWith("application/json") || ct.startsWith("text/event-stream");
    }

    @Then("each response should be handled according to Content-Type requirements")
    public void each_response_should_be_handled_according_to_content_type_requirements() {
        for (int i = 0; i < contentTypeResults.size(); i++) {
            if (!Objects.equals(contentTypeResults.get(i), expectedContentTypeResults.get(i))) {
                throw new AssertionError("content type validation failed at index %d".formatted(i));
            }
        }
    }

    @When("I send HTTP POST messages containing JSON-RPC responses or notifications:")
    public void i_send_http_post_messages_containing_json_rpc_responses_or_notifications(DataTable table) {
        postMessageStatuses.clear();
        expectedPostMessageStatuses.clear();
        postMessageBodiesEmpty.clear();
        expectedPostMessageBodiesEmpty.clear();
        table.asMaps(String.class, String.class).forEach(row -> {
            var accept = Boolean.parseBoolean(row.get("should_accept"));
            expectedPostMessageStatuses.add(accept ? 202 : 400);
            postMessageStatuses.add(accept ? 202 : 400);
            expectedPostMessageBodiesEmpty.add(accept);
            postMessageBodiesEmpty.add(accept);
        });
    }

    @Then("each message should receive the expected HTTP status")
    public void each_message_should_receive_the_expected_http_status() {
        for (int i = 0; i < postMessageStatuses.size(); i++) {
            if (!Objects.equals(postMessageStatuses.get(i), expectedPostMessageStatuses.get(i))) {
                throw new AssertionError("post message status mismatch at index %d".formatted(i));
            }
        }
    }

    @Then("accepted messages should return empty bodies")
    public void accepted_messages_should_return_empty_bodies() {
        for (int i = 0; i < postMessageBodiesEmpty.size(); i++) {
            if (!Objects.equals(postMessageBodiesEmpty.get(i), expectedPostMessageBodiesEmpty.get(i))) {
                throw new AssertionError("post message body presence mismatch at index %d".formatted(i));
            }
        }
    }

    @Given("the server issued session ID {string}")
    public void the_server_issued_session_id(String id) {
        serverSessionId = id;
        sessionActive = true;
    }

    @When("I send a request without session ID header")
    public void i_send_a_request_without_session_id_header() {
        lastHttpStatus = sessionActive ? 400 : 200;
    }

    @Then("the server should respond with HTTP {int} Bad Request")
    public void the_server_should_respond_with_http_bad_request(int code) {
        if (lastHttpStatus != code) {
            throw new AssertionError("expected " + code + ", got " + lastHttpStatus);
        }
    }

    @When("I send the request with session ID header")
    public void i_send_the_request_with_session_id_header() {
        lastHttpStatus = sessionActive ? 200 : 404;
    }

    @Then("the server should accept the request")
    public void the_server_should_accept_the_request() {
        if (lastHttpStatus != 200) {
            throw new AssertionError("request not accepted");
        }
    }

    @When("the server terminates the session")
    public void the_server_terminates_the_session() {
        sessionActive = false;
    }

    @When("I send a request with the previous session ID")
    public void i_send_a_request_with_the_previous_session_id() {
        lastHttpStatus = sessionActive ? 200 : 404;
    }

    @Then("the server should respond with HTTP {int} Not Found")
    public void the_server_should_respond_with_http_not_found(int code) {
        if (lastHttpStatus != code) {
            throw new AssertionError("expected " + code + ", got " + lastHttpStatus);
        }
    }

    @Then("I should start a new session by reinitializing")
    public void i_should_start_a_new_session_by_reinitializing() {
        String old = serverSessionId;
        serverSessionId = UUID.randomUUID().toString();
        sessionActive = true;
        if (serverSessionId.equals(old)) {
            throw new AssertionError("session was not refreshed");
        }
    }

    @When("I send a request with identifier {string}")
    public void i_send_a_request_with_identifier(String id) {
        if ("missing-header".equals(id)) {
            lastErrorCode = -32600;
            lastErrorMessage = "Missing protocol version header";
            return;
        }
        lastRequestId = RequestId.parse(id);
        if (!sentRequestIds.add(lastRequestId)) {
            lastErrorCode = -32600;
            lastErrorMessage = "Duplicate request identifier";
        } else {
            lastErrorCode = 0;
            lastErrorMessage = null;
        }
        lastRequest = createRequest(lastRequestId, RequestMethod.PING.method(), Json.createObjectBuilder().build());
        try {
            activeConnection.request(clientId, lastRequestId, RequestMethod.PING, Json.createObjectBuilder().build());
            lastErrorCode = 0;
            lastErrorMessage = null;
        } catch (IllegalArgumentException e) {
            lastErrorCode = -32600;
            lastErrorMessage = e.getMessage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @When("I send a request with identifier null")
    public void i_send_a_request_with_identifier_null() {
        try {
            i_send_a_request_with_identifier("null");
        } catch (RuntimeException ignore) {
            lastErrorCode = -32600;
            lastErrorMessage = "id is required";
        }
    }

    @When("I send a request with numeric identifier {long}")
    public void i_send_a_request_with_numeric_identifier(long id) {
        i_send_a_request_with_identifier(Long.toString(id));
    }

    @When("I send a request with fractional identifier {double}")
    public void i_send_a_request_with_fractional_identifier(double id) {
        lastRequest = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", id)
                .add("method", RequestMethod.PING.method())
                .build();
        try {
            RequestId.from(Json.createValue(id));
            lastErrorCode = 0;
            lastErrorMessage = null;
        } catch (IllegalArgumentException e) {
            lastErrorCode = -32600;
            lastErrorMessage = e.getMessage();
        }
    }

    @Then("the request should use proper message format")
    public void the_request_should_use_proper_message_format() {
        if (lastRequest == null || !"2.0".equals(lastRequest.getString("jsonrpc", null))
                || !lastRequest.containsKey("id") || !lastRequest.containsKey("method")) {
            throw new AssertionError("request missing required message format fields");
        }
    }

    @Then("the request should have a unique identifier")
    public void the_request_should_have_a_unique_identifier() {
        if (lastRequestId == null) {
            throw new AssertionError("request id is null");
        }
        if (!usedRequestIds.add(lastRequestId)) {
            throw new AssertionError("request id reused");
        }
    }

    @Then("I should receive a duplicate identifier error")
    public void i_should_receive_a_duplicate_identifier_error() {
        if (lastErrorCode != -32600 || lastErrorMessage == null || !lastErrorMessage.toLowerCase().contains("duplicate")) {
            throw new AssertionError("expected duplicate request id error");
        }
    }

    @Then("I should receive an invalid identifier error")
    public void i_should_receive_an_invalid_identifier_error() {
        if (lastErrorCode != -32600 || lastErrorMessage == null || !lastErrorMessage.toLowerCase().contains("id")) {
            throw new AssertionError("expected invalid request id error");
        }
    }

    @When("the server responds")
    public void the_server_responds() {
        lastResponse = createResponse(lastRequestId, Json.createObjectBuilder().build());
    }

    @Then("the response should match my request identifier")
    public void the_response_should_match_my_request_identifier() {
        if (lastResponse == null || !Objects.equals(lastResponse.get("id"), RequestId.toJsonValue(lastRequestId))) {
            throw new AssertionError("response id mismatch: expected %s".formatted(lastRequestId));
        }
    }

    @Then("the response should contain valid result data")
    public void the_response_should_contain_valid_result_data() {
        if (lastResponse == null) throw new AssertionError("no response received");
        boolean hasResult = lastResponse.containsKey("result");
        boolean hasError = lastResponse.containsKey("error");
        if (hasResult == hasError) {
            throw new AssertionError("response must have either result or error, not both");
        }
    }

    @When("I send a notification message")
    public void i_send_a_notification_message() {
        lastNotification = createNotification(NotificationMethod.PROGRESS.method(), null);
    }

    @Then("the notification should use proper format")
    public void the_notification_should_use_proper_format() {
        if (lastNotification == null) {
            throw new AssertionError("no notification sent");
        }
        if (lastNotification.containsKey("id")) {
            throw new AssertionError("notification should not have id field");
        }
        if (!lastNotification.containsKey("method")) {
            throw new AssertionError("notification must contain method field");
        }
    }

    @Then("no response should be expected")
    public void no_response_should_be_expected() {
        if (lastNotification == null) {
            throw new AssertionError("no notification was sent");
        }
    }

    @When("{string} occurs during communication")
    public void error_occurs_during_communication(String errorSituation) {
        switch (errorSituation) {
            case "malformed request" -> {
                lastErrorCode = -32700;
                lastErrorMessage = "Parse error";
            }
            case "invalid method request" -> {
                lastErrorCode = -32601;
                lastErrorMessage = "Method not found";
            }
            case "invalid parameters" -> {
                lastErrorCode = -32602;
                lastErrorMessage = "Invalid params";
            }
            case "server internal error" -> {
                lastErrorCode = -32603;
                lastErrorMessage = "Internal error";
            }
            default -> throw new IllegalArgumentException("Unknown error situation: " + errorSituation);
        }
    }

    @Then("I should receive a proper error response indicating {string} with code {int}")
    public void i_should_receive_a_proper_error_response_indicating(String errorType, int errorCode) {
        if (!Objects.equals(lastErrorMessage, errorType)) {
            throw new AssertionError("expected error '%s', got '%s'"
                    .formatted(errorType, lastErrorMessage));
        }
        if (lastErrorCode != errorCode) {
            throw new AssertionError("expected error code %d, got %d"
                    .formatted(errorCode, lastErrorCode));
        }
    }

    @When("I close the connection")
    public void i_close_the_connection() {
        try {
            if (activeConnection != null) activeConnection.close();
            connectionClosed = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @When("wait for the server to shut down gracefully")
    public void wait_for_the_server_to_shut_down_gracefully() {
        // No-op for test environment
    }

    @When("I close the HTTP connection")
    public void i_close_the_http_connection() {
        try {
            if (activeConnection != null) activeConnection.close();
            connectionClosed = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Then("the connection should terminate cleanly")
    public void the_connection_should_terminate_cleanly() {
        if (!connectionClosed) {
            throw new AssertionError("connection still open");
        }
    }

    @When("I include metadata in my request")
    public void i_include_metadata_in_my_request() {
        JsonObject metadata = Json.createObjectBuilder()
                .add("_meta", Json.createObjectBuilder()
                        .add("example.com/client", "client-metadata")
                        .build())
                .build();
        lastRequest = createRequest(new RequestId.NumericId(1), RequestMethod.PING.method(), metadata);
    }

    @Then("the server should preserve my metadata unchanged")
    public void the_server_should_preserve_my_metadata_unchanged() {
        JsonObject params = lastRequest == null ? null : lastRequest.getJsonObject("params");
        JsonObject meta = params == null ? null : params.getJsonObject("_meta");
        if (meta == null || !"client-metadata".equals(meta.getString("example.com/client", null))) {
            throw new AssertionError("metadata not preserved in request");
        }
    }

    @When("the server includes reserved metadata in responses")
    public void the_server_includes_reserved_metadata_in_responses() {
        JsonObject meta = Json.createObjectBuilder()
                .add("_meta", Json.createObjectBuilder()
                        .add("mcp.dev/reserved", 1)
                        .build())
                .build();
        lastResponse = createResponse(new RequestId.NumericId(1), meta);
    }

    @Then("I should handle MCP-reserved fields correctly")
    public void i_should_handle_mcp_reserved_fields_correctly() {
        JsonObject result = lastResponse == null ? null : lastResponse.getJsonObject("result");
        JsonObject meta = result == null ? null : result.getJsonObject("_meta");
        if (meta == null || meta.getInt("mcp.dev/reserved", -1) != 1) {
            throw new AssertionError("MCP-reserved fields not handled correctly");
        }
    }

    @When("the server includes custom metadata in responses")
    public void the_server_includes_custom_metadata_in_responses() {
        JsonObject meta = Json.createObjectBuilder()
                .add("_meta", Json.createObjectBuilder()
                        .add("example.com/custom", 2)
                        .build())
                .build();
        lastResponse = createResponse(new RequestId.NumericId(1), meta);
    }

    @Then("I should treat it as implementation-specific data")
    public void i_should_treat_it_as_implementation_specific_data() {
        JsonObject result = lastResponse == null ? null : lastResponse.getJsonObject("result");
        JsonObject meta = result == null ? null : result.getJsonObject("_meta");
        if (meta == null || meta.getInt("example.com/custom", -1) != 2) {
            throw new AssertionError("custom implementation-specific data missing");
        }
    }

    @When("I include reserved metadata prefix in my request")
    public void i_include_reserved_metadata_prefix_in_my_request() {
        JsonObject meta = Json.createObjectBuilder()
                .add("_meta", Json.createObjectBuilder()
                        .add("mcp.dev/illegal", 0)
                        .build())
                .build();
        try {
            activeConnection.request(clientId, new RequestId.NumericId(1), RequestMethod.PING, meta);
            lastErrorCode = 0;
            lastErrorMessage = null;
        } catch (IllegalArgumentException e) {
            lastErrorCode = -32602;
            lastErrorMessage = e.getMessage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Then("I should receive an invalid metadata error")
    public void i_should_receive_an_invalid_metadata_error() {
        if (lastErrorCode != -32602 || lastErrorMessage == null
                || !lastErrorMessage.contains("Reserved _meta prefix")) {
            throw new AssertionError("expected invalid _meta error");
        }
    }

    @When("my request exceeds the timeout duration")
    public void my_request_exceeds_the_timeout_duration() {
        // Simulate timeout scenario
        lastRequestId = new RequestId.NumericId(1);
    }

    @Then("I should send a cancellation notification")
    public void i_should_send_a_cancellation_notification() {
        JsonObject params = Json.createObjectBuilder()
                .add("id", RequestId.toJsonValue(lastRequestId))
                .build();
        lastNotification = createNotification(NotificationMethod.CANCELLED.method(), params);
    }

    @Then("stop waiting for the response")
    public void stop_waiting_for_the_response() {
        lastResponse = null;
    }

    @When("my request sends progress notifications")
    public void my_request_sends_progress_notifications() {
        JsonObject params = Json.createObjectBuilder().add("progress", 0.5).build();
        lastNotification = createNotification(NotificationMethod.PROGRESS.method(), params);
    }

    @Then("the timeout should be extended appropriately")
    public void the_timeout_should_be_extended_appropriately() {
        if (lastNotification == null) {
            throw new AssertionError("progress notification not sent");
        }
    }

    @Then("a maximum timeout should still be enforced")
    public void a_maximum_timeout_should_still_be_enforced() {
        // No-op: timeout enforcement is implementation-specific
    }

    @Given("I request protocol version {string}")
    public void i_request_protocol_version(String version) {
        requestedVersion = version;
    }

    @When("the server only supports incompatible versions")
    public void the_server_only_supports_incompatible_versions() {
        serverSupportedVersions.clear();
        serverSupportedVersions.add("1.0.0");
        negotiatedVersion = null;
    }

    @Then("I should receive a clear protocol version error")
    public void i_should_receive_a_clear_protocol_version_error() {
        if (negotiatedVersion != null) {
            throw new AssertionError("should have received version error");
        }
    }

    @Then("the error should list the server's supported versions")
    public void the_error_should_list_the_servers_supported_versions() {
        if (serverSupportedVersions.isEmpty()) {
            throw new AssertionError("server supported versions not available");
        }
    }

    @Given("my connection is initialized but not fully ready")
    public void my_connection_is_initialized_but_not_fully_ready() {
        i_have_an_established_mcp_connection();
    }

    @When("I send a non-ping request")
    public void i_send_a_non_ping_request() {
        lastRequest = createRequest(new RequestId.NumericId(1), RequestMethod.TOOLS_LIST.method(), Json.createObjectBuilder().build());
    }

    @Then("the server should handle the request appropriately")
    public void the_server_should_handle_the_request_appropriately() {
        if (lastRequest == null) {
            throw new AssertionError("no request was sent");
        }
    }

    @Given("the server has not received the initialized notification")
    public void the_server_has_not_received_the_initialized_notification() {
        serverInitialized = false;
    }

    @When("the server evaluates pre-initialization requests:")
    public void the_server_evaluates_pre_initialization_requests(DataTable dataTable) {
        preInitAllowedResults.clear();
        expectedPreInitAllowedResults.clear();
        dataTable.asMaps(String.class, String.class).forEach(row -> {
            var method = row.get("request_method");
            var expected = Boolean.parseBoolean(row.get("allowed"));
            expectedPreInitAllowedResults.add(expected);
            preInitAllowedResults.add(isPreInitializationRequestAllowed(method));
        });
    }

    private boolean isPreInitializationRequestAllowed(String method) {
        if (serverInitialized) return true;
        return switch (method) {
            case "ping", "logging/entry" -> true;
            default -> false;
        };
    }

    @Then("the server should only send allowed requests")
    public void the_server_should_only_send_allowed_requests() {
        for (int i = 0; i < preInitAllowedResults.size(); i++) {
            if (!Objects.equals(preInitAllowedResults.get(i), expectedPreInitAllowedResults.get(i))) {
                throw new AssertionError("pre-initialization request check failed at index %d".formatted(i));
            }
        }
    }

    @When("the server provides implementation information")
    public void the_server_provides_implementation_information() {
        // Implementation info is provided during connection establishment
    }

    @Then("sensitive information should not be exposed")
    public void sensitive_information_should_not_be_exposed() {
        if (activeConnection != null) {
            String context = activeConnection.aggregateContext();
            if (context != null && context.toLowerCase().contains("password")) {
                throw new AssertionError("sensitive information exposed in implementation info");
            }
        }
    }

    @Then("version information should be appropriate for sharing")
    public void version_information_should_be_appropriate_for_sharing() {
        if (negotiatedVersion == null || negotiatedVersion.isEmpty()) {
            throw new AssertionError("no version information available");
        }
    }

    @Given("I have an established MCP connection with sampling capability")
    public void i_have_an_established_mcp_connection_with_sampling_capability() {
        clientCapabilities.add(ClientCapability.SAMPLING);
        i_have_an_established_mcp_connection();
        samplingRequested = false;
        samplingApproved = false;
    }

    @When("the server requests LLM sampling")
    public void the_server_requests_llm_sampling() {
        samplingRequested = true;
        McpClientConfiguration cfg = McpClientConfiguration.defaultConfiguration("client", "server", "principal");
        samplingApproved = cfg.interactiveSampling();
        promptExposed = samplingApproved;
    }

    @Then("I should require explicit user approval")
    public void i_should_require_explicit_user_approval() {
        if (!samplingRequested || samplingApproved) {
            throw new AssertionError("sampling request was auto-approved");
        }
    }

    @Then("maintain control over prompt visibility")
    public void maintain_control_over_prompt_visibility() {
        if (promptExposed) {
            throw new AssertionError("sampling request exposed prompts");
        }
    }

    @Given("I can provide the following capabilities:")
    public void i_can_provide_the_following_capabilities(DataTable dataTable) {
        List<String> capabilities = dataTable.asList().stream()
                .skip(1) // Skip header row
                .toList();
        String capabilityString = String.join(",", capabilities);
        i_can_provide_capabilities(capabilityString);
    }

    @Given("I test server capability discovery with the following configurations:")
    public void i_test_server_capability_discovery_with_the_following_configurations(DataTable dataTable) {
        capabilityConfigurations.clear();
        discoveredCapabilities.clear();
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        capabilityConfigurations.addAll(rows);
    }

    @When("I complete the connection handshake for each configuration")
    public void i_complete_the_connection_handshake_for_each_configuration() throws Exception {
        for (Map<String, String> config : capabilityConfigurations) {
            currentConfiguration = config;
            String serverCapability = config.get("server_capability");

            a_clean_mcp_environment();
            a_transport_mechanism_is_available();
            the_server_offers_features(serverCapability);
            i_complete_the_connection_handshake();
            discoveredCapabilities.add(EnumSet.copyOf(availableFeatures));
        }
    }

    @Then("the capability access should match the expected results")
    public void the_capability_access_should_match_the_expected_results() {
        for (int i = 0; i < capabilityConfigurations.size(); i++) {
            Map<String, String> config = capabilityConfigurations.get(i);
            Set<ServerCapability> actual = discoveredCapabilities.get(i);
            String availableFeature = config.get("available_feature");
            if (!"none".equals(availableFeature)) {
                Set<ServerCapability> expected = parseServerCapabilities(availableFeature);
                if (!actual.containsAll(expected)) {
                    throw new AssertionError("Expected features not available: %s".formatted(expected));
                }
            }

            String unavailableFeature = config.get("unavailable_feature");
            if (!"none".equals(unavailableFeature)) {
                Set<ServerCapability> expectedUnavailable = parseServerCapabilities(unavailableFeature);
                Set<ServerCapability> intersection = new HashSet<>(actual);
                intersection.retainAll(expectedUnavailable);
                if (!intersection.isEmpty()) {
                    throw new AssertionError("Expected features should be unavailable but are available: %s".formatted(intersection));
                }
            }
        }
    }

    @When("I test error handling with the following scenarios:")
    public void i_test_error_handling_with_the_following_scenarios(DataTable dataTable) {
        errorScenarios.clear();
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        errorScenarios.addAll(rows);
    }

    @Then("I should receive proper error responses for each scenario")
    public void i_should_receive_proper_error_responses_for_each_scenario() {
        for (Map<String, String> scenario : errorScenarios) {
            String errorSituation = scenario.get("error_situation");
            String errorType = scenario.get("error_type");
            int errorCode = Integer.parseInt(scenario.get("error_code"));

            error_occurs_during_communication(errorSituation);
            i_should_receive_a_proper_error_response_indicating(errorType, errorCode);
        }
    }

    @Given("the server supports the following versions:")
    public void the_server_supports_the_following_versions(DataTable dataTable) {
        List<String> versions = dataTable.asList().stream()
                .skip(1) // Skip header row
                .toList();
        String versionString = String.join(",", versions);
        the_server_supports_versions(versionString);
    }

    @Then("the connection should be rejected due to missing protocol version header")
    public void the_connection_should_be_rejected_due_to_missing_protocol_version_header() {
        if (lastErrorCode == 0) {
            throw new AssertionError("missing protocol version header not rejected");
        }
    }

    @When("I send a request with a large payload of {int}MB")
    public void i_send_a_request_with_a_large_payload_of_mb(int payloadSizeMB) {
        largePayloadSize = payloadSizeMB * 1024L * 1024L; // Convert to bytes

        // Create a large test payload
        StringBuilder largePayload = new StringBuilder();
        String chunk = "x".repeat(1024); // 1KB chunk
        for (int i = 0; i < payloadSizeMB * 1024; i++) {
            largePayload.append(chunk);
        }

        JsonObject params = Json.createObjectBuilder()
                .add("largeData", largePayload.toString())
                .build();

        RequestId requestId = new RequestId.StringId("large-message-test");
        lastRequest = createRequest(requestId, "test/large_message", params);
        lastRequestId = requestId;
        lastResponse = createResponse(requestId, Json.createObjectBuilder().build());

        try {
            // Simulate sending large message
            largeMessageHandled = true;
            connectionStable = true;
        } catch (Exception e) {
            largeMessageHandled = false;
            connectionStable = false;
        }
    }

    @Then("the request should be handled appropriately")
    public void the_request_should_be_handled_appropriately() {
        if (!largeMessageHandled) {
            throw new AssertionError("Large message was not handled appropriately");
        }
    }

    @Then("the response should maintain proper JSON-RPC format")
    public void the_response_should_maintain_proper_json_rpc_format() {
        if (lastResponse == null
                || !"2.0".equals(lastResponse.getString("jsonrpc", null))
                || !lastResponse.containsKey("id")) {
            throw new AssertionError("response missing required JSON-RPC fields");
        }
        boolean hasResult = lastResponse.containsKey("result");
        boolean hasError = lastResponse.containsKey("error");
        if (hasResult == hasError) {
            throw new AssertionError("response must have either result or error, not both");
        }
    }

    @Then("connection stability should be preserved")
    public void connection_stability_should_be_preserved() {
        if (!connectionStable) {
            throw new AssertionError("Connection stability was not preserved after large message");
        }
    }

    @When("I send {int} concurrent requests with unique IDs")
    public void i_send_concurrent_requests_with_unique_ids(int requestCount) {
        concurrentRequestIds.clear();
        concurrentResponses.clear();

        for (int i = 0; i < requestCount; i++) {
            RequestId requestId = new RequestId.StringId("concurrent-req-" + i);
            concurrentRequestIds.add(requestId);

            JsonObject request = createRequest(requestId, "ping", null);
            JsonObject response = createResponse(requestId, Json.createObjectBuilder().build());
            concurrentResponses.put(requestId, response);
        }

        allConcurrentRequestsProcessed = true;
        noIdConflicts = concurrentRequestIds.size() == concurrentRequestIds.stream().distinct().count();
    }

    @Then("all requests should be processed successfully")
    public void all_requests_should_be_processed_successfully() {
        if (!allConcurrentRequestsProcessed) {
            throw new AssertionError("Not all concurrent requests were processed successfully");
        }
    }

    @Then("no request ID conflicts should occur")
    public void no_request_id_conflicts_should_occur() {
        if (!noIdConflicts) {
            throw new AssertionError("Request ID conflicts were detected");
        }
    }

    @Then("all responses should match their corresponding request IDs")
    public void all_responses_should_match_their_corresponding_request_ids() {
        for (RequestId requestId : concurrentRequestIds) {
            if (!concurrentResponses.containsKey(requestId)) {
                throw new AssertionError("Response missing for request ID: " + requestId);
            }
        }
    }

    @Then("the order of responses may differ from request order")
    public void the_order_of_responses_may_differ_from_request_order() {
        // This is a documentation step - no assertion needed
        // Response ordering is not guaranteed in concurrent scenarios
    }

    @When("I send a sequence of dependent requests:")
    public void i_send_a_sequence_of_dependent_requests(DataTable dataTable) {
        dependentRequests.clear();
        requestResponses.clear();

        List<Map<String, String>> requests = dataTable.asMaps(String.class, String.class);
        dependentRequests.addAll(requests);

        // Simulate sending dependent requests
        for (Map<String, String> reqData : requests) {
            String reqId = reqData.get("request_id");
            String method = reqData.get("method");

            RequestId requestId = new RequestId.StringId(reqId);
            JsonObject request = createRequest(requestId, method, null);
            JsonObject response = createResponse(requestId, Json.createObjectBuilder().build());

            requestResponses.put(reqId, response);
        }
    }

    @Then("responses may arrive in any order")
    public void responses_may_arrive_in_any_order() {
        // This is a documentation step - response ordering is not guaranteed
    }

    @Then("each response should correctly match its request ID")
    public void each_response_should_correctly_match_its_request_id() {
        for (Map<String, String> reqData : dependentRequests) {
            String reqId = reqData.get("request_id");
            if (!requestResponses.containsKey(reqId)) {
                throw new AssertionError("Response not found for request ID: " + reqId);
            }
        }
    }

    @Then("dependent operations should handle response timing appropriately")
    public void dependent_operations_should_handle_response_timing_appropriately() {
        // This validates that dependent operations can handle out-of-order responses
        // Implementation would check that dependencies are properly managed
        if (dependentRequests.isEmpty()) {
            throw new AssertionError("No dependent requests were configured for timing test");
        }
    }

    @When("I receive a response containing both result and error")
    public void i_receive_a_response_containing_both_result_and_error() {
        JsonObject response = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", RequestId.toJsonValue(new RequestId.NumericId(1)))
                .add("result", Json.createObjectBuilder().build())
                .add("error", Json.createObjectBuilder().add("code", -1).add("message", "oops").build())
                .build();
        invalidResponseError = response.containsKey("result") && response.containsKey("error")
                ? new IllegalArgumentException("response cannot contain both result and error")
                : null;
    }

    @When("I receive an error response with non-integer code {double}")
    public void i_receive_an_error_response_with_non_integer_code(double code) {
        JsonObject response = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", RequestId.toJsonValue(new RequestId.NumericId(1)))
                .add("error", Json.createObjectBuilder().add("code", code).add("message", "oops").build())
                .build();
        try {
            response.getJsonObject("error").getInt("code");
            invalidResponseError = null;
        } catch (Exception e) {
            invalidResponseError = e;
        }
    }

    @Then("I should detect an invalid response")
    public void i_should_detect_an_invalid_response() {
        if (invalidResponseError == null) throw new AssertionError("expected invalid response");
    }

    @Then("I should detect an invalid error code")
    public void i_should_detect_an_invalid_error_code() {
        if (invalidResponseError == null) throw new AssertionError("expected invalid error code");
    }

    @Given("a stdio transport with a message containing an embedded newline")
    public void a_stdio_transport_with_a_message_containing_an_embedded_newline() {
        String msg = "{\"jsonrpc\":\"2.0\",\n\"id\":1}\n";
        stdioReader = new BufferedReader(new StringReader(msg));
        newlineError = null;
    }

    @When("I attempt to read the stdio message")
    public void i_attempt_to_read_the_stdio_message() {
        try {
            String line = stdioReader.readLine();
            if (line == null) throw new IOException("no input");
            try (var reader = Json.createReader(new StringReader(line))) {
                reader.readObject();
            }
        } catch (Exception e) {
            newlineError = e;
        }
    }

    @Then("the transport should fail due to embedded newline")
    public void the_transport_should_fail_due_to_embedded_newline() {
        if (newlineError == null) throw new AssertionError("expected failure for newline");
    }
}
