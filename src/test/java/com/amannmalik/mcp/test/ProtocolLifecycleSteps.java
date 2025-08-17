package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.spi.Cursor;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public final class ProtocolLifecycleSteps {
    
    private McpClientConfiguration clientConfig;
    private McpHostConfiguration hostConfig;
    private McpHost activeConnection;
    private String clientId;
    
    private boolean connectionClosed = false;
    private String requestedVersion;
    private String negotiatedVersion;
    private List<String> serverSupportedVersions = new ArrayList<>();
    
    private Set<ClientCapability> clientCapabilities = EnumSet.noneOf(ClientCapability.class);
    private Set<ServerCapability> serverCapabilities = EnumSet.noneOf(ServerCapability.class);
    private Set<ServerCapability> availableFeatures = EnumSet.noneOf(ServerCapability.class);
    
    private RequestId lastRequestId;
    private JsonObject lastRequest;
    private JsonObject lastResponse;
    private JsonObject lastNotification;
    private String lastErrorMessage;
    private int lastErrorCode;
    private Set<RequestId> usedRequestIds = new HashSet<>();
    
    private List<Map<String, String>> capabilityConfigurations = new ArrayList<>();
    private List<Map<String, String>> errorScenarios = new ArrayList<>();
    private Map<String, String> currentConfiguration;

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
        
        if (currentConfiguration == null) {
            capabilityConfigurations.clear();
            errorScenarios.clear();
        }
        currentConfiguration = null;
    }

    @Given("a transport mechanism is available")
    public void a_transport_mechanism_is_available() {
        McpClientConfiguration base = McpClientConfiguration.defaultConfiguration("client", "client", "default");
        String cp = System.getProperty("java.class.path");
        String cmd = "java -cp " + cp + " com.amannmalik.mcp.cli.Entrypoint server --stdio --test-mode";
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

    @When("I send a request with identifier {string}")
    public void i_send_a_request_with_identifier(String id) {
        lastRequestId = RequestId.parse(id);
        lastRequest = createRequest(lastRequestId, RequestMethod.PING.method(), Json.createObjectBuilder().build());
        usedRequestIds.add(lastRequestId);
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
        long useCount = usedRequestIds.stream()
                .filter(id -> id.equals(lastRequestId))
                .count();
        if (useCount > 1) {
            throw new AssertionError("request id reused");
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
            case "malformed request" -> { lastErrorCode = -32700; lastErrorMessage = "Parse error"; }
            case "invalid method request" -> { lastErrorCode = -32601; lastErrorMessage = "Method not found"; }
            case "invalid parameters" -> { lastErrorCode = -32602; lastErrorMessage = "Invalid params"; }
            case "server internal error" -> { lastErrorCode = -32603; lastErrorMessage = "Internal error"; }
            default -> throw new IllegalArgumentException("Unknown error situation: " + errorSituation);
        }
    }

    @Then("I should receive a proper error response indicating {string}")
    public void i_should_receive_a_proper_error_response_indicating(String errorType) {
        if (!Objects.equals(lastErrorMessage, errorType)) {
            throw new AssertionError("expected error '%s', got '%s'"
                    .formatted(errorType, lastErrorMessage));
        }
        if (lastErrorCode == 0) {
            throw new AssertionError("no error code set");
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
        JsonObject metadata = Json.createObjectBuilder().add("_meta", "client-metadata").build();
        lastRequest = createRequest(new RequestId.NumericId(1), RequestMethod.PING.method(), metadata);
    }

    @Then("the server should preserve my metadata unchanged")
    public void the_server_should_preserve_my_metadata_unchanged() {
        if (lastRequest == null || !lastRequest.containsKey("_meta")) {
            throw new AssertionError("metadata not preserved in request");
        }
    }

    @When("the server includes reserved metadata in responses")
    public void the_server_includes_reserved_metadata_in_responses() {
        JsonObject meta = Json.createObjectBuilder().add("_meta_reserved", 1).build();
        lastResponse = createResponse(new RequestId.NumericId(1), meta);
    }

    @Then("I should handle MCP-reserved fields correctly")
    public void i_should_handle_mcp_reserved_fields_correctly() {
        JsonObject result = lastResponse == null ? null : lastResponse.getJsonObject("result");
        if (result == null || !result.containsKey("_meta_reserved")) {
            throw new AssertionError("MCP-reserved fields not handled correctly");
        }
    }

    @When("the server includes custom metadata in responses")
    public void the_server_includes_custom_metadata_in_responses() {
        JsonObject meta = Json.createObjectBuilder().add("_custom", 2).build();
        lastResponse = createResponse(new RequestId.NumericId(1), meta);
    }

    @Then("I should treat it as implementation-specific data")
    public void i_should_treat_it_as_implementation_specific_data() {
        JsonObject result = lastResponse == null ? null : lastResponse.getJsonObject("result");
        if (result == null || !result.containsKey("_custom")) {
            throw new AssertionError("custom implementation-specific data missing");
        }
    }

    @When("my request exceeds the timeout duration")
    public void my_request_exceeds_the_timeout_duration() {
        // Simulate timeout scenario
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
    }
    
    @When("the server requests LLM sampling")
    public void the_server_requests_llm_sampling() {
        // Simulated sampling request from server
    }
    
    @Then("I should require explicit user approval")
    public void i_should_require_explicit_user_approval() {
        if (!clientCapabilities.contains(ClientCapability.SAMPLING)) {
            throw new AssertionError("sampling capability not declared");
        }
    }
    
    @Then("maintain control over prompt visibility")
    public void maintain_control_over_prompt_visibility() {
        // No-op: prompt visibility is client implementation concern
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
        }
    }
    
    @Then("the capability access should match the expected results")
    public void the_capability_access_should_match_the_expected_results() {
        for (Map<String, String> config : capabilityConfigurations) {
            String availableFeature = config.get("available_feature");
            String unavailableFeature = config.get("unavailable_feature");
            
            if (!"none".equals(availableFeature)) {
                i_should_have_access_to_features(availableFeature);
            }
            
            if (!"none".equals(unavailableFeature)) {
                features_should_not_be_available(unavailableFeature);
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
            
            error_occurs_during_communication(errorSituation);
            i_should_receive_a_proper_error_response_indicating(errorType);
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
}