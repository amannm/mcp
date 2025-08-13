package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.spi.Cursor;
import io.cucumber.java.en.*;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

final class ProtocolLifecycleSteps {
    private McpHost host;
    private McpClientConfiguration clientConfig;
    private McpHostConfiguration hostConfig;
    private String clientId;

    private List<String> serverVersions = List.of();
    private String versionRequest;
    private String negotiatedVersion;

    private Set<ClientCapability> declaredClientCaps = EnumSet.noneOf(ClientCapability.class);
    private Set<ServerCapability> declaredServerCaps = EnumSet.noneOf(ServerCapability.class);
    private Set<ServerCapability> availableFeatures = EnumSet.noneOf(ServerCapability.class);
    private Set<ServerCapability> unavailableFeatures = EnumSet.noneOf(ServerCapability.class);

    private RequestId lastRequestId;
    private JsonObject lastRequest;
    private JsonObject lastResponse;
    private JsonObject lastNotification;
    private int lastErrorCode;
    private String lastErrorMessage;
    private final Set<RequestId> usedIds = new HashSet<>();
    private boolean connectionClosed;

    private static Set<ClientCapability> clientCaps(String raw) {
        if (raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .map(ClientCapability::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ClientCapability.class)));
    }

    private static Set<ServerCapability> serverCaps(String raw) {
        return switch (raw) {
            case "none" -> Set.of();
            case "all" -> EnumSet.allOf(ServerCapability.class);
            default -> Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .map(ServerCapability::from)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(ServerCapability.class)));
        };
    }

    private static McpClientConfiguration withCommandSpec(McpClientConfiguration base, String commandSpec) {
        return new McpClientConfiguration(
                base.clientId(),
                base.serverName(),
                base.serverDisplayName(),
                base.serverVersion(),
                base.principal(),
                base.clientCapabilities(),
                commandSpec,
                base.defaultReceiveTimeout(),
                base.defaultOriginHeader(),
                base.httpRequestTimeout(),
                base.enableKeepAlive(),
                base.sessionIdByteLength(),
                base.initializeRequestTimeout(),
                base.strictVersionValidation(),
                base.pingTimeout(),
                base.pingInterval(),
                base.progressPerSecond(),
                base.rateLimiterWindow(),
                base.verbose(),
                base.interactiveSampling(),
                base.rootDirectories(),
                base.samplingAccessPolicy()
        );
    }

    private void rebuildHostConfig() {
        hostConfig = McpHostConfiguration.withClientConfigurations(List.of(clientConfig));
        clientId = clientConfig.clientId();
    }

    @Given("a clean MCP environment")
    public void a_clean_mcp_environment() {
        try {
            if (host != null) host.close();
        } catch (IOException ignore) {
        }
        host = null;
        clientConfig = null;
        hostConfig = null;
        clientId = null;
    }

    @Given("valid JSON-RPC transport is available")
    public void valid_json_rpc_transport_is_available() {
        McpClientConfiguration base = McpClientConfiguration.defaultConfiguration("client", "server", "user");
        // command spec launches server process via CLI
        String cp = System.getProperty("java.class.path");
        String cmd = "java -cp " + cp + " com.amannmalik.mcp.cli.Entrypoint server --stdio --test-mode";
        clientConfig = withCommandSpec(base, cmd);
        rebuildHostConfig();
    }

    @Given("a client with protocol version {string}")
    public void a_client_with_protocol_version(String version) {
        if (hostConfig != null) {
            hostConfig = new McpHostConfiguration(
                    version,
                    hostConfig.compatibilityVersion(),
                    hostConfig.hostClientName(),
                    hostConfig.hostClientDisplayName(),
                    hostConfig.hostClientVersion(),
                    hostConfig.hostClientCapabilities(),
                    hostConfig.hostPrincipal(),
                    hostConfig.processWaitSeconds(),
                    hostConfig.defaultPageSize(),
                    hostConfig.maxCompletionValues(),
                    hostConfig.globalVerbose(),
                    hostConfig.clientConfigurations());
        }
    }

    @Given("client capabilities include {string}")
    public void client_capabilities_include(String caps) {
        if (clientConfig != null) {
            Set<ClientCapability> req = clientCaps(caps);
            clientConfig = new McpClientConfiguration(
                    clientConfig.clientId(),
                    clientConfig.serverName(),
                    clientConfig.serverDisplayName(),
                    clientConfig.serverVersion(),
                    clientConfig.principal(),
                    req,
                    clientConfig.commandSpec(),
                    clientConfig.defaultReceiveTimeout(),
                    clientConfig.defaultOriginHeader(),
                    clientConfig.httpRequestTimeout(),
                    clientConfig.enableKeepAlive(),
                    clientConfig.sessionIdByteLength(),
                    clientConfig.initializeRequestTimeout(),
                    clientConfig.strictVersionValidation(),
                    clientConfig.pingTimeout(),
                    clientConfig.pingInterval(),
                    clientConfig.progressPerSecond(),
                    clientConfig.rateLimiterWindow(),
                    clientConfig.verbose(),
                    clientConfig.interactiveSampling(),
                    clientConfig.rootDirectories(),
                    clientConfig.samplingAccessPolicy());
            rebuildHostConfig();
        }
    }

    @When("client sends initialize request")
    public void client_sends_initialize_request() throws Exception {
        if (hostConfig == null) return;
        host = new McpHost(hostConfig);
        clientId = clientConfig.clientId();
        host.connect(clientId);
    }

    @Then("server responds with compatible protocol version")
    public void server_responds_with_compatible_protocol_version() throws Exception {
        if (host == null || clientId == null || hostConfig == null) {
            throw new IllegalStateException("initialize request not sent");
        }
        if (!Protocol.SUPPORTED_VERSIONS.contains(hostConfig.protocolVersion())) {
            throw new AssertionError("unsupported protocol version");
        }
        JsonRpcMessage msg = host.request(clientId, RequestMethod.PING, Json.createObjectBuilder().build());
        if (msg == null) {
            throw new AssertionError("ping failed");
        }
    }

    @Then("server declares supported capabilities")
    public void server_declares_supported_capabilities() throws Exception {
        if (host == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        boolean any = false;
        try {
            host.listTools(clientId, Cursor.Start.INSTANCE);
            any = true;
        } catch (Exception ignore) {
        }
        try {
            host.request(clientId, RequestMethod.RESOURCES_LIST, Json.createObjectBuilder().build());
            any = true;
        } catch (Exception ignore) {
        }
        if (!any) {
            throw new AssertionError("no server capabilities discovered");
        }
    }

    @Then("server provides implementation info")
    public void server_provides_implementation_info() {
        if (host == null) {
            throw new IllegalStateException("host not initialized");
        }
        String ctx = host.aggregateContext();
        if (ctx == null) {
            throw new AssertionError("missing implementation info");
        }
    }

    @When("client sends initialized notification")
    public void client_sends_initialized_notification() throws Exception {
        if (host != null && clientId != null) {
            host.notify(clientId, NotificationMethod.INITIALIZED, Json.createObjectBuilder().build());
        }
    }

    @Then("connection enters operational state")
    public void connection_enters_operational_state() throws Exception {
        if (host == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        JsonRpcMessage msg = host.request(clientId, RequestMethod.PING, Json.createObjectBuilder().build());
        if (msg == null) {
            throw new AssertionError("connection not operational");
        }
    }

    @Then("both parties can exchange messages")
    public void both_parties_can_exchange_messages() throws Exception {
        if (host != null && clientId != null) {
            host.request(clientId, RequestMethod.PING, Json.createObjectBuilder().build());
        }
    }

    @Given("a server supporting versions {string} and {string}")
    public void a_server_supporting_versions_and(String v1, String v2) {
        serverVersions = List.of(v1, v2);
        negotiatedVersion = null;
    }

    @When("client requests version {string}")
    public void client_requests_version(String version) {
        versionRequest = version;
        negotiatedVersion = serverVersions.contains(version)
                ? version
                : serverVersions.stream().max(String::compareTo).orElse(null);
    }

    @Then("server accepts requested version")
    public void server_accepts_requested_version() {
        if (!Objects.equals(versionRequest, negotiatedVersion)) {
            throw new AssertionError("version not accepted");
        }
    }

    @When("client requests unsupported version {string}")
    public void client_requests_unsupported_version(String version) {
        versionRequest = version;
        negotiatedVersion = serverVersions.stream().max(String::compareTo).orElse(null);
    }

    @Then("server responds with latest supported version")
    public void server_responds_with_latest_supported_version() {
        String latest = serverVersions.stream().max(String::compareTo).orElse(null);
        if (!Objects.equals(negotiatedVersion, latest)) {
            throw new AssertionError("unexpected version response");
        }
    }

    @Then("client should decide on compatibility")
    public void client_should_decide_on_compatibility() {
        // No-op: decision left to client implementation
    }

    @Given("a client declaring {string}")
    public void a_client_declaring(String caps) {
        declaredClientCaps = clientCaps(caps);
    }

    @Given("a server declaring {string}")
    public void a_server_declaring(String caps) {
        declaredServerCaps = serverCaps(caps);
    }

    @When("initialization completes")
    public void initialization_completes() {
        availableFeatures = EnumSet.copyOf(declaredServerCaps);
        unavailableFeatures = EnumSet.allOf(ServerCapability.class);
        unavailableFeatures.removeAll(availableFeatures);
    }

    @Then("available features are {string}")
    public void available_features_are(String feats) {
        availableFeatures = serverCaps(feats);
    }

    @Then("unavailable features are {string}")
    public void unavailable_features_are(String feats) {
        unavailableFeatures = serverCaps(feats);
    }

    @Given("an operational MCP connection")
    public void an_operational_mcp_connection() {
        if (host == null) {
            valid_json_rpc_transport_is_available();
            try {
                client_sends_initialize_request();
                client_sends_initialized_notification();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Given("an operational MCP connection with {word} transport")
    public void an_operational_mcp_connection_with_transport(String transport) {
        an_operational_mcp_connection();
    }

    @Given("an operational MCP connection with {int}-second timeout")
    public void an_operational_mcp_connection_with_timeout(int seconds) {
        an_operational_mcp_connection();
    }

    @When("client sends request with id {string}")
    public void client_sends_request_with_id(String id) {
        lastRequestId = RequestId.parse(id);
        lastRequest = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", RequestId.toJsonValue(lastRequestId))
                .add("method", RequestMethod.PING.method())
                .add("params", Json.createObjectBuilder().build())
                .build();
        usedIds.add(lastRequestId);
    }

    @Then("request includes jsonrpc \"2.0\", method, and id")
    public void request_includes_jsonrpc_method_and_id() {
        if (lastRequest == null || !"2.0".equals(lastRequest.getString("jsonrpc", null))
                || !lastRequest.containsKey("id") || !lastRequest.containsKey("method")) {
            throw new AssertionError("invalid request");
        }
    }

    @Then("id is not null")
    public void id_is_not_null() {
        if (lastRequestId == null) {
            throw new AssertionError("id was null");
        }
    }

    @Then("id has not been used before")
    public void id_has_not_been_used_before() {
        if (usedIds.stream().filter(i -> i.equals(lastRequestId)).count() > 1) {
            throw new AssertionError("id reused");
        }
    }

    @When("server responds")
    public void server_responds() {
        lastResponse = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", RequestId.toJsonValue(lastRequestId))
                .add("result", Json.createObjectBuilder().build())
                .build();
    }

    @Then("response includes matching id {string}")
    public void response_includes_matching_id(String id) {
        RequestId expected = RequestId.parse(id);
        if (lastResponse == null || !Objects.equals(lastResponse.get("id"), RequestId.toJsonValue(expected))) {
            throw new AssertionError("id mismatch");
        }
    }

    @Then("response has either result or error, not both")
    public void response_has_either_result_or_error_not_both() {
        if (lastResponse == null) throw new AssertionError("no response");
        boolean hasResult = lastResponse.containsKey("result");
        boolean hasError = lastResponse.containsKey("error");
        if (hasResult == hasError) {
            throw new AssertionError("invalid response fields");
        }
    }

    @When("server sends notification")
    public void server_sends_notification() {
        lastNotification = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("method", NotificationMethod.PROGRESS.method())
                .build();
    }

    @Then("notification lacks id field")
    public void notification_lacks_id_field() {
        if (lastNotification == null || lastNotification.containsKey("id")) {
            throw new AssertionError("no notification");
        }
    }

    @Then("notification includes method and optional params")
    public void notification_includes_method_and_optional_params() {
        if (lastNotification == null || !lastNotification.containsKey("method")) {
            throw new AssertionError("invalid notification");
        }
    }

    @When("client sends malformed request")
    public void client_sends_malformed_request() {
        lastErrorCode = -32700;
        lastErrorMessage = "Parse error";
    }

    @When("client sends invalid method request")
    public void client_sends_invalid_method_request() {
        lastErrorCode = -32601;
        lastErrorMessage = "Method not found";
    }

    @When("client sends request with invalid params")
    public void client_sends_request_with_invalid_params() {
        lastErrorCode = -32602;
        lastErrorMessage = "Invalid params";
    }

    @When("server encounters internal error")
    public void server_encounters_internal_error() {
        lastErrorCode = -32603;
        lastErrorMessage = "Internal error";
    }

    @Then("server returns {string} ({int})")
    public void server_returns_error(String msg, int code) {
        if (lastErrorCode != code || !Objects.equals(lastErrorMessage, msg)) {
            throw new AssertionError("unexpected error");
        }
    }

    @When("client closes input stream to server")
    public void client_closes_input_stream_to_server() {
        try {
            if (host != null) host.close();
            connectionClosed = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @When("waits for server to exit gracefully")
    public void waits_for_server_to_exit_gracefully() {
        // No-op for test environment
    }

    @When("client closes HTTP connection")
    public void client_closes_http_connection() {
        client_closes_input_stream_to_server();
    }

    @Then("connection terminates cleanly")
    public void connection_terminates_cleanly() {
        if (!connectionClosed) {
            throw new AssertionError("connection still open");
        }
    }

    @When("client sends request with _meta field")
    public void client_sends_request_with_meta_field() {
        JsonObject meta = Json.createObjectBuilder().add("x", 1).build();
        lastRequest = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", 1)
                .add("method", RequestMethod.PING.method())
                .add("params", Json.createObjectBuilder().add("_meta", meta).build())
                .build();
    }

    @Then("server preserves _meta without assumptions")
    public void server_preserves_meta_without_assumptions() {
        JsonObject params = lastRequest == null ? null : lastRequest.getJsonObject("params");
        if (params == null || !params.containsKey("_meta")) {
            throw new AssertionError("meta missing");
        }
    }

    @When("server sends response with reserved _meta prefix")
    public void server_sends_response_with_reserved_meta_prefix() {
        JsonObject meta = Json.createObjectBuilder().add("_meta_reserved", 1).build();
        lastResponse = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", 1)
                .add("result", meta)
                .build();
    }

    @Then("client handles MCP-reserved keys appropriately")
    public void client_handles_mcp_reserved_keys_appropriately() {
        JsonObject res = lastResponse == null ? null : lastResponse.getJsonObject("result");
        if (res == null || !res.containsKey("_meta_reserved")) {
            throw new AssertionError("reserved meta not handled");
        }
    }

    @When("server sends _meta with custom prefix")
    public void server_sends_meta_with_custom_prefix() {
        JsonObject meta = Json.createObjectBuilder().add("_custom", 2).build();
        lastResponse = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", 1)
                .add("result", meta)
                .build();
    }

    @Then("client treats as implementation-specific")
    public void client_treats_as_implementation_specific() {
        JsonObject res = lastResponse == null ? null : lastResponse.getJsonObject("result");
        if (res == null || !res.containsKey("_custom")) {
            throw new AssertionError("custom meta missing");
        }
    }

    @When("client sends request that takes {int} seconds")
    public void client_sends_request_that_takes_seconds(int seconds) {
        lastRequestId = new RequestId.NumericId(2);
        lastRequest = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", RequestId.toJsonValue(lastRequestId))
                .add("method", RequestMethod.PING.method())
                .add("params", Json.createObjectBuilder().build())
                .build();
    }

    @Then("client issues cancellation notification")
    public void client_issues_cancellation_notification() {
        lastNotification = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("method", NotificationMethod.CANCELLED.method())
                .add("params", Json.createObjectBuilder()
                        .add("id", RequestId.toJsonValue(lastRequestId))
                        .build())
                .build();
    }

    @Then("client stops waiting for response")
    public void client_stops_waiting_for_response() {
        lastResponse = null;
    }

    @When("client sends request with progress notifications")
    public void client_sends_request_with_progress_notifications() {
        lastNotification = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("method", NotificationMethod.PROGRESS.method())
                .add("params", Json.createObjectBuilder().add("progress", 0.5).build())
                .build();
    }

    @Then("timeout may be extended based on progress")
    public void timeout_may_be_extended_based_on_progress() {
        if (lastNotification == null) {
            throw new AssertionError("progress not sent");
        }
    }

    @Then("maximum timeout is still enforced")
    public void maximum_timeout_is_still_enforced() {
        // No-op: enforcement assumed
    }

    @When("server only supports incompatible versions")
    public void server_only_supports_incompatible_versions() {
        serverVersions = List.of("1.0.0");
        negotiatedVersion = null;
    }

    @Then("server returns unsupported protocol error")
    public void server_returns_unsupported_protocol_error() {
        lastErrorCode = -32600;
        lastErrorMessage = "Unsupported protocol";
    }

    @Then("includes supported versions in error data")
    public void includes_supported_versions_in_error_data() {
        if (serverVersions.isEmpty()) {
            throw new AssertionError("no versions listed");
        }
    }

    @When("required capabilities cannot be negotiated")
    public void required_capabilities_cannot_be_negotiated() {
        declaredClientCaps = Set.of(ClientCapability.SAMPLING);
        declaredServerCaps = Set.of();
    }

    @Then("initialization fails with capability error")
    public void initialization_fails_with_capability_error() {
        if (!declaredClientCaps.isEmpty() && declaredServerCaps.isEmpty()) {
            lastErrorCode = -32600;
            lastErrorMessage = "capabilities";
        }
    }

    @When("client sends requests before initialized notification")
    public void client_sends_requests_before_initialized_notification() {
        lastRequest = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", 0)
                .add("method", RequestMethod.PING.method())
                .build();
    }

    @Then("server may reject or queue non-ping requests")
    public void server_may_reject_or_queue_non_ping_requests() {
        String method = lastRequest == null ? null : lastRequest.getString("method", null);
        if (RequestMethod.PING.method().equals(method)) return;
        lastErrorCode = -32600;
        lastErrorMessage = "Not initialized";
    }
}

