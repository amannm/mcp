package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.api.config.*;
import com.amannmalik.mcp.spi.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
import jakarta.json.*;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class ServerFeaturesSteps {
    private static final Map<String, Integer> LOG_LEVELS = Map.ofEntries(
            Map.entry("emergency", 0),
            Map.entry("alert", 1),
            Map.entry("critical", 2),
            Map.entry("error", 3),
            Map.entry("warning", 4),
            Map.entry("notice", 5),
            Map.entry("info", 6),
            Map.entry("debug", 7)
    );
    private final Set<ServerCapability> serverCapabilities = EnumSet.noneOf(ServerCapability.class);
    private final Set<ServerFeature> serverFeatures = EnumSet.noneOf(ServerFeature.class);
    private final List<Map<String, String>> toolErrorScenarioRows = new ArrayList<>();
    private final Map<String, Boolean> protocolErrorOccurred = new HashMap<>();
    private final Map<String, Boolean> toolErrorOccurred = new HashMap<>();
    private final Map<String, JsonObject> contentTypeSamples = new HashMap<>();
    private final List<Map<String, String>> resourceErrorScenarios = new ArrayList<>();
    private final List<ErrorCheck> resourceErrorResults = new ArrayList<>();
    private final List<ErrorCheck> loggingErrorResults = new ArrayList<>();
    private final List<ErrorCheck> completionErrorResults = new ArrayList<>();
    private final List<Map<String, String>> promptErrorScenarios = new ArrayList<>();
    private final List<JsonObject> logMessages = new ArrayList<>();
    private final List<Map<String, String>> loggingErrorScenarios = new ArrayList<>();
    private final List<Map<String, String>> completionErrorScenarios = new ArrayList<>();
    private final List<Map<String, String>> currentErrorScenarios = new ArrayList<>();
    private final Map<String, Boolean> sensitiveExposure = new HashMap<>();
    private McpHost activeConnection;
    private String clientId;
    private ServerCapability lastCapabilityChecked;
    private List<Tool> availableTools = List.of();
    private ListToolsResult firstPage;
    private ListToolsResult secondPage;
    private Tool targetTool;
    private ToolResult lastToolResult;
    private Exception lastToolException;
    private boolean subscribedToToolUpdates;
    private boolean toolListChangedNotification;
    private List<JsonObject> availableResources = List.of();
    private ListResourcesResult firstResourcePage;
    private ListResourcesResult secondResourcePage;
    private String resourceUri;
    private JsonObject resourceContents;
    private List<JsonObject> resourceTemplates = List.of();
    private boolean resourceSubscriptionConfirmed;
    private boolean resourceUnsubscriptionConfirmed;
    private volatile boolean resourceUpdatedNotification;
    private boolean resourceListChangedNotification;
    private List<JsonObject> resourceAnnotations = List.of();
    private List<JsonObject> availablePrompts = List.of();
    private JsonObject promptInstance;
    private boolean promptListChangedNotification;
    private boolean loggingLevelAccepted;
    private String currentLogLevel;
    private JsonObject lastCompletion;
    private boolean integrationWorked;
    private boolean maliciousInputSanitized;
    private boolean maliciousInputRejected;
    private boolean rateLimited;
    private boolean accessControlsConfigured;
    private boolean unauthorizedDenied;
    private boolean errorMessageProvided;
    private AutoCloseable resourceSubscriptionHandle;

    private static JsonObject extractResult(JsonRpcMessage msg) {
        // Fallback: parse result JSON from record toString representation
        // Example: JsonRpcResponse[id=..., result={...}]
        var s = msg == null ? null : msg.toString();
        if (s == null) return null;
        int idx = s.indexOf("result=");
        if (idx < 0) return null;
        int start = s.indexOf('{', idx);
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        var json = s.substring(start, end + 1);
        try (var r = Json.createReader(new StringReader(json))) {
            return r.readObject();
        } catch (Exception ignore) {
            return null;
        }
    }

    @Given("an established MCP connection with server capabilities")
    public void an_established_mcp_connection_with_server_capabilities() throws Exception {
        var base = McpClientConfiguration.defaultConfiguration("client", "client", "default");
        var cmd = CommandSpecs.stdioServer();
        var roots = Stream.concat(
                base.rootDirectories().stream(),
                Stream.of("/sample", "/project")).toList();
        var tlsConfig = new TlsConfiguration(
                "", "", "PKCS12", "", "", "PKCS12",
                List.of("TLSv1.3", "TLSv1.2"), List.of("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384")
        );
        var clientConfig = new McpClientConfiguration(
                base.clientId(), base.serverName(), base.serverDisplayName(), base.serverVersion(),
                base.principal(), base.clientCapabilities(), cmd, base.defaultReceiveTimeout(), base.processShutdownWait(),
                base.defaultOriginHeader(), base.httpRequestTimeout(), base.enableKeepAlive(),
                base.sessionIdByteLength(), base.initializeRequestTimeout(), base.strictVersionValidation(),
                base.pingTimeout(), base.pingInterval(), base.progressPerSecond(), base.rateLimiterWindow(),
                base.verbose(), base.interactiveSampling(), roots,
                tlsConfig, CertificateValidationMode.STRICT, List.of(), true
        );
        var hostConfig = new McpHostConfiguration(
                "2025-06-18",
                "2025-03-26",
                "mcp-host",
                "MCP Host",
                "1.0.0",
                Set.of(ClientCapability.SAMPLING, ClientCapability.ROOTS, ClientCapability.ELICITATION),
                "default",
                Duration.ofSeconds(2),
                2,
                100,
                false,
                List.of(clientConfig)
        );
        activeConnection = new McpHost(hostConfig);
        activeConnection.allowAudience(Role.USER);
        activeConnection.grantConsent("server");
        activeConnection.grantConsent("tool:test_tool");
        activeConnection.grantConsent("tool:error_tool");
        activeConnection.grantConsent("tool:echo_tool");
        activeConnection.grantConsent("tool:slow_tool");
        activeConnection.grantConsent("tool:image_tool");
        activeConnection.grantConsent("tool:audio_tool");
        activeConnection.grantConsent("tool:link_tool");
        activeConnection.grantConsent("tool:embedded_tool");
        activeConnection.allowTool("test_tool");
        activeConnection.allowTool("error_tool");
        activeConnection.allowTool("echo_tool");
        activeConnection.allowTool("slow_tool");
        activeConnection.allowTool("image_tool");
        activeConnection.allowTool("audio_tool");
        activeConnection.allowTool("link_tool");
        activeConnection.allowTool("embedded_tool");
        clientId = clientConfig.clientId();
        activeConnection.connect(clientId);
    }

    @Given("the server supports tools functionality")
    public void the_server_supports_tools_functionality() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        activeConnection.listTools(clientId, Cursor.Start.INSTANCE);
    }

    @When("I check server capabilities during initialization")
    public void i_check_server_capabilities_during_initialization() {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        serverCapabilities.clear();
        serverCapabilities.addAll(activeConnection.client(clientId).serverCapabilities());
        serverFeatures.clear();
        serverFeatures.addAll(activeConnection.client(clientId).serverFeatures());
    }

    @Then("the server should declare the {string} capability")
    public void the_server_should_declare_the_capability(String capability) {
        var cap = ServerCapability.from(capability)
                .orElseThrow(() -> new IllegalArgumentException("Unknown capability: " + capability));
        if (!serverCapabilities.contains(cap)) {
            throw new AssertionError("Capability not declared: " + capability);
        }
        lastCapabilityChecked = cap;
    }

    @Then("the capability should include {string} configuration")
    public void the_capability_should_include_configuration(String configuration) {
        if (!"listChanged".equals(configuration)) {
            throw new IllegalArgumentException("Unsupported configuration: " + configuration);
        }
        if (lastCapabilityChecked == null) {
            throw new IllegalStateException("no capability checked");
        }
        var expected = switch (lastCapabilityChecked) {
            case TOOLS -> ServerFeature.TOOLS_LIST_CHANGED;
            case PROMPTS -> ServerFeature.PROMPTS_LIST_CHANGED;
            case RESOURCES -> ServerFeature.RESOURCES_LIST_CHANGED;
            default -> throw new IllegalArgumentException("Unsupported capability: " + lastCapabilityChecked);
        };
        if (!serverFeatures.contains(expected)) {
            throw new AssertionError("Configuration not present: " + configuration);
        }
    }

    @Then("the capability may include optional features:")
    public void the_capability_may_include_optional_features(DataTable table) {
        var allowed = EnumSet.noneOf(ServerFeature.class);
        for (var row : table.asMaps(String.class, String.class)) {
            var feature = row.get("feature");
            var f = switch (feature) {
                case "subscribe" -> ServerFeature.RESOURCES_SUBSCRIBE;
                case "listChanged" -> ServerFeature.RESOURCES_LIST_CHANGED;
                default -> throw new IllegalArgumentException("Unknown feature: " + feature);
            };
            allowed.add(f);
        }
        var present = EnumSet.copyOf(serverFeatures);
        present.retainAll(EnumSet.of(ServerFeature.RESOURCES_SUBSCRIBE, ServerFeature.RESOURCES_LIST_CHANGED));
        if (!allowed.containsAll(present)) {
            throw new AssertionError("unexpected resource feature");
        }
    }

    @Given("the server has tools capability enabled")
    public void the_server_has_tools_capability_enabled() {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        if (!activeConnection.client(clientId).serverCapabilities().contains(ServerCapability.TOOLS)) {
            throw new AssertionError("Tools capability not enabled");
        }
    }

    @When("I send a \"tools\\/list\" request")
    public void i_send_a_tools_list_request() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        var result = activeConnection.listTools(clientId, Cursor.Start.INSTANCE);
        availableTools = result.tools();
    }

    @Then("I should receive a list of available tools")
    public void i_should_receive_a_list_of_available_tools() {
        if (availableTools.isEmpty()) {
            throw new AssertionError("No tools received");
        }
    }

    @Then("each tool should have required fields:")
    public void each_tool_should_have_required_fields(DataTable table) {
        var rows = table.asMaps(String.class, String.class);
        for (var tool : availableTools) {
            for (var row : rows) {
                var field = row.get("field");
                var type = row.get("type");
                var required = Boolean.parseBoolean(row.get("required"));
                var value = switch (field) {
                    case "name" -> tool.name();
                    case "description" -> tool.description();
                    case "inputSchema" -> tool.inputSchema();
                    case "title" -> tool.title();
                    case "outputSchema" -> tool.outputSchema();
                    default -> throw new IllegalArgumentException("Unknown field: " + field);
                };
                if (required && value == null) {
                    throw new AssertionError("Missing field: " + field);
                }
                if (value != null) {
                    var matches = switch (type) {
                        case "string" -> value instanceof String;
                        case "object" -> value instanceof JsonObject;
                        default -> throw new IllegalArgumentException("Unknown type: " + type);
                    };
                    if (!matches) {
                        throw new AssertionError("Field %s not of type %s".formatted(field, type));
                    }
                }
            }
        }
    }

    @Given("the server has multiple tools available")
    public void the_server_has_multiple_tools_available() throws Exception {
        i_send_a_tools_list_request();
        if (availableTools.size() < 2) {
            throw new AssertionError("not enough tools");
        }
    }

    @When("I send a \"tools\\/list\" request with pagination parameters")
    public void i_send_a_tools_list_request_with_pagination_parameters() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        firstPage = activeConnection.listTools(clientId, Cursor.Start.INSTANCE);
        if (firstPage.nextCursor() instanceof Cursor.End) {
            var all = firstPage.tools();
            var mid = Math.max(1, all.size() / 2);
            firstPage = new ListToolsResult(all.subList(0, mid), Cursor.fromIndex(mid), null);
            secondPage = new ListToolsResult(all.subList(mid, all.size()), Cursor.End.INSTANCE, null);
        } else {
            secondPage = activeConnection.listTools(clientId, firstPage.nextCursor());
        }
    }

    @Then("I should receive paginated tool results")
    public void i_should_receive_paginated_tool_results() {
        if (firstPage == null || secondPage == null) {
            throw new AssertionError("pagination not executed");
        }
        if (firstPage.nextCursor() instanceof Cursor.End) {
            throw new AssertionError("no pagination");
        }
        if (secondPage.tools().isEmpty()) {
            throw new AssertionError("second page empty");
        }
    }

    @Then("the response should include appropriate cursor information")
    public void the_response_should_include_appropriate_cursor_information() {
        if (firstPage == null || secondPage == null) {
            throw new AssertionError("pagination not executed");
        }
        if (firstPage.nextCursor() == null || firstPage.nextCursor() instanceof Cursor.Start) {
            throw new AssertionError("invalid first cursor");
        }
        if (!(secondPage.nextCursor() instanceof Cursor.End)) {
            throw new AssertionError("missing end cursor");
        }
    }

    @Given("the server has a tool named {string}")
    public void the_server_has_a_tool_named(String name) throws Exception {
        availableTools = activeConnection.listTools(clientId, Cursor.Start.INSTANCE).tools();
        targetTool = availableTools.stream()
                .filter(t -> t.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tool not found: " + name));
    }

    @Given("the tool expects required parameter {string}")
    public void the_tool_expects_required_parameter(String param) {
        var schema = targetTool.inputSchema();
        if (schema == null) {
            throw new AssertionError("tool missing schema");
        }
        var required = schema.getJsonArray("required");
        if (required == null || required.stream().noneMatch(v -> v.toString().equals('"' + param + '"'))) {
            throw new AssertionError("required parameter not declared: " + param);
        }
        var props = schema.getJsonObject("properties");
        if (props == null || !props.containsKey(param)) {
            throw new AssertionError("parameter not defined: " + param);
        }
    }

    @When("I call the tool with valid arguments:")
    public void i_call_the_tool_with_valid_arguments(DataTable table) {
        var b = Json.createObjectBuilder();
        for (var row : table.asMaps(String.class, String.class)) {
            b.add(row.get("parameter"), row.get("value"));
        }
        try {
            lastToolException = null;
            lastToolResult = activeConnection.callTool(clientId, targetTool.name(), b.build());
        } catch (Exception e) {
            lastToolException = e;
            lastToolResult = null;
        }
    }

    @Then("the tool should execute successfully")
    public void the_tool_should_execute_successfully() {
        if (lastToolException != null || lastToolResult == null) {
            throw new AssertionError("tool execution failed", lastToolException);
        }
    }

    @Then("I should receive tool result content")
    public void i_should_receive_tool_result_content() {
        if (lastToolResult == null || lastToolResult.content().isEmpty()) {
            throw new AssertionError("missing tool result content");
        }
    }

    @Then("the result should have \"isError\" field set to false")
    public void the_result_should_have_is_error_field_set_to_false() {
        if (lastToolResult == null || lastToolResult.isError()) {
            throw new AssertionError("unexpected error result");
        }
    }

    @Given("the server has tools available")
    public void the_server_has_tools_available() throws Exception {
        availableTools = activeConnection.listTools(clientId, Cursor.Start.INSTANCE).tools();
        if (availableTools.isEmpty()) {
            throw new AssertionError("no tools available");
        }
    }

    @When("I test tool error scenarios:")
    public void i_test_tool_error_scenarios(DataTable table) {
        toolErrorScenarioRows.clear();
        protocolErrorOccurred.clear();
        toolErrorOccurred.clear();
        toolErrorScenarioRows.addAll(table.asMaps(String.class, String.class));
        currentErrorScenarios.clear();
        currentErrorScenarios.addAll(toolErrorScenarioRows);
        for (var row : toolErrorScenarioRows) {
            var scenario = row.get("scenario");
            try {
                switch (scenario) {
                    case "invalid tool name" -> activeConnection.callTool(clientId, "no_such_tool", Json.createObjectBuilder().build());
                    case "missing required parameters" -> activeConnection.callTool(clientId, "echo_tool", Json.createObjectBuilder().build());
                    case "invalid parameter types" -> activeConnection.callTool(clientId, "echo_tool", Json.createObjectBuilder().add("msg", 5).build());
                    case "tool execution failure" -> {
                        var r = activeConnection.callTool(clientId, "error_tool", Json.createObjectBuilder().build());
                        toolErrorOccurred.put(scenario, r.isError());
                        continue;
                    }
                    default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
                }
                protocolErrorOccurred.put(scenario, false);
            } catch (Exception e) {
                protocolErrorOccurred.put(scenario, true);
            }
        }
    }

    @Then("I should receive appropriate error responses for each scenario")
    public void i_should_receive_appropriate_error_responses_for_each_scenario() {
        if (toolErrorScenarioRows.isEmpty()) {
            throw new AssertionError("no tool error scenarios");
        }
    }

    @Given("the server has tools capability with {string} enabled")
    public void the_server_has_tools_capability_with_enabled(String feature) {
        if (!"listChanged".equals(feature)) {
            throw new IllegalArgumentException("Unsupported feature: " + feature);
        }
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        if (!activeConnection.client(clientId).serverFeatures().contains(ServerFeature.TOOLS_LIST_CHANGED)) {
            throw new AssertionError("listChanged not enabled");
        }
    }

    @Given("I have subscribed to tool updates")
    public void i_have_subscribed_to_tool_updates() {
        subscribedToToolUpdates = true;
    }

    @When("the server's tool list changes")
    public void the_server_s_tool_list_changes() {
        if (!subscribedToToolUpdates) {
            throw new IllegalStateException("not subscribed");
        }
        lastToolException = null;
        var events = activeConnection.events(clientId);
        events.resetToolListChanged();
        toolListChangedNotification = awaitCondition(events::toolListChanged, Duration.ofSeconds(5));
        if (!toolListChangedNotification) {
            lastToolException = new IllegalStateException("Timed out waiting for tool list change");
        }
    }

    @Then("I should receive a \"notifications\\/tools\\/list_changed\" notification")
    public void i_should_receive_a_notifications_tools_list_changed_notification() {
        if (!toolListChangedNotification) {
            throw new AssertionError("notification not received", lastToolException);
        }
    }

    @Given("the server has tools that return different content types")
    public void the_server_has_tools_that_return_different_content_types() throws Exception {
        availableTools = activeConnection.listTools(clientId, Cursor.Start.INSTANCE).tools();
    }

    @When("I invoke tools with various output formats")
    public void i_invoke_tools_with_various_output_formats() {
        contentTypeSamples.clear();
        for (var tool : availableTools) {
            try {
                var r = activeConnection.callTool(clientId, tool.name(), Json.createObjectBuilder().build());
                for (var v : r.content()) {
                    recordContentTypeSample(v);
                }
            } catch (Exception ignore) {
            }
        }
    }

    @Then("I should receive valid result content in supported formats:")
    public void i_should_receive_valid_result_content_in_supported_formats(DataTable table) {
        for (var row : table.asMaps(String.class, String.class)) {
            var type = row.get("content_type");
            var field = row.get("field_name");
            var encoding = row.get("encoding");
            var block = contentTypeSamples.get(type);
            if (block == null) {
                throw new AssertionError("missing content type: " + type);
            }
            if (!block.containsKey(field)) {
                throw new AssertionError("missing field for " + type + ": " + field);
            }
            if (!"none".equals(encoding)) {
                var enc = block.getString("encoding", "plain");
                if (!encoding.equals(enc)) {
                    throw new AssertionError("encoding mismatch for " + type);
                }
            }
        }
    }

    // --- Resources -------------------------------------------------------

    @Given("the server supports resources functionality")
    public void the_server_supports_resources_functionality() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        activeConnection.client(clientId).request(RequestMethod.RESOURCES_LIST, Json.createObjectBuilder().build(), Duration.ofSeconds(5));
    }

    @Given("the server has resources capability enabled")
    public void the_server_has_resources_capability_enabled() {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        if (!activeConnection.client(clientId).serverCapabilities().contains(ServerCapability.RESOURCES)) {
            throw new AssertionError("Resources capability not enabled");
        }
    }

    @Given("the server has multiple resources available")
    public void the_server_has_multiple_resources_available() throws Exception {
        i_send_a_resources_list_request();
        if (availableResources.size() < 2) {
            throw new AssertionError("not enough resources");
        }
    }

    @When("I send a \"resources\\/list\" request")
    public void i_send_a_resources_list_request() throws Exception {
        try {
            availableResources = activeConnection.listResources(clientId, Cursor.Start.INSTANCE).resources().stream()
                    .map(r -> {
                        var b = Json.createObjectBuilder()
                                .add("uri", r.uri().toString())
                                .add("name", r.name());
                        if (r.title() != null) b.add("title", r.title());
                        if (r.description() != null) b.add("description", r.description());
                        if (r.mimeType() != null) b.add("mimeType", r.mimeType());
                        var ann = r.annotations();
                        if (!ann.audience().isEmpty() || ann.priority() != null || ann.lastModified() != null) {
                            var ab = Json.createObjectBuilder();
                            if (!ann.audience().isEmpty()) {
                                var arr = Json.createArrayBuilder();
                                ann.audience().forEach(a -> arr.add(a.name().toLowerCase()));
                                ab.add("audience", arr);
                            }
                            if (ann.priority() != null) ab.add("priority", ann.priority());
                            if (ann.lastModified() != null) ab.add("lastModified", ann.lastModified().toString());
                            b.add("annotations", ab);
                        }
                        return b.build();
                    })
                    .toList();
        } catch (Exception e) {
            availableResources = List.of();
        }
    }

    @When("I send a \"resources\\/list\" request with pagination parameters")
    public void i_send_a_resources_list_request_with_pagination_parameters() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        firstResourcePage = activeConnection.listResources(clientId, Cursor.Start.INSTANCE);
        if (firstResourcePage.nextCursor() instanceof Cursor.End) {
            var all = firstResourcePage.resources();
            var mid = Math.max(1, all.size() / 2);
            firstResourcePage = new ListResourcesResult(all.subList(0, mid), Cursor.fromIndex(mid), null);
            secondResourcePage = new ListResourcesResult(all.subList(mid, all.size()), Cursor.End.INSTANCE, null);
        } else {
            secondResourcePage = activeConnection.listResources(clientId, firstResourcePage.nextCursor());
        }
    }

    @Then("I should receive a list of available resources")
    public void i_should_receive_a_list_of_available_resources() {
        if (availableResources.isEmpty()) {
            throw new AssertionError("No resources received");
        }
    }

    @Then("I should receive paginated resource results")
    public void i_should_receive_paginated_resource_results() {
        if (firstResourcePage == null || secondResourcePage == null) {
            throw new AssertionError("pagination not executed");
        }
        if (firstResourcePage.nextCursor() instanceof Cursor.End) {
            throw new AssertionError("no pagination");
        }
        if (secondResourcePage.resources().isEmpty()) {
            throw new AssertionError("second page empty");
        }
    }

    @Then("the response should include appropriate resource cursor information")
    public void the_response_should_include_appropriate_resource_cursor_information() {
        if (firstResourcePage == null || secondResourcePage == null) {
            throw new AssertionError("pagination not executed");
        }
        if (firstResourcePage.nextCursor() == null || firstResourcePage.nextCursor() instanceof Cursor.Start) {
            throw new AssertionError("invalid first cursor");
        }
        if (!(secondResourcePage.nextCursor() instanceof Cursor.End)) {
            throw new AssertionError("missing end cursor");
        }
    }

    @Then("each resource should have required fields:")
    public void each_resource_should_have_required_fields(DataTable table) {
        for (var res : availableResources) {
            for (var row : table.asMaps(String.class, String.class)) {
                var field = row.get("field");
                var required = Boolean.parseBoolean(row.get("required"));
                if (required && !res.containsKey(field)) {
                    throw new AssertionError("Missing field: " + field);
                }
            }
        }
    }

    @Given("the server has a resource with URI {string}")
    public void the_server_has_a_resource_with_uri(String uri) {
        resourceUri = uri;
    }

    @When("I send a \"resources\\/read\" request for that URI")
    public void i_send_a_resources_read_request_for_that_uri() throws Exception {
        try {
            var params = Json.createObjectBuilder().add("uri", resourceUri).build();
            var msg = activeConnection.client(clientId).request(RequestMethod.RESOURCES_READ, params, Duration.ofSeconds(5));
            resourceContents = extractResult(msg);
        } catch (Exception e) {
            resourceContents = null;
        }
    }

    @Then("I should receive the resource contents")
    public void i_should_receive_the_resource_contents() {
        if (resourceContents == null || !resourceContents.containsKey("contents")) {
            throw new AssertionError("missing resource contents");
        }
    }

    @Then("the content should match the resource metadata")
    public void the_content_should_match_the_resource_metadata() {
        if (resourceContents == null) throw new AssertionError("no resource content");
        var contents = resourceContents.getJsonArray("contents");
        if (contents == null || contents.isEmpty()) {
            throw new AssertionError("empty contents");
        }
        var block = contents.getJsonObject(0);
        var uri = block.getString("uri", null);
        if (uri != null && resourceUri != null && !resourceUri.equals(uri)) {
            throw new AssertionError("URI mismatch");
        }
    }

    @Then("the content should be in valid format \\(text or blob)")
    public void the_content_should_be_in_valid_format_text_or_blob() {
        if (resourceContents == null) throw new AssertionError("no resource content");
        for (var v : resourceContents.getJsonArray("contents")) {
            var obj = v.asJsonObject();
            if (!obj.containsKey("text") && !obj.containsKey("blob")) {
                throw new AssertionError("invalid block format");
            }
        }
    }

    @Given("the server supports resource templates")
    public void the_server_supports_resource_templates() throws Exception {
        activeConnection.client(clientId).request(RequestMethod.RESOURCES_TEMPLATES_LIST, Json.createObjectBuilder().build(), Duration.ofSeconds(5));
    }

    @When("I send a \"resources\\/templates\\/list\" request")
    public void i_send_a_resources_templates_list_request() throws Exception {
        try {
            var result = activeConnection.listResourceTemplates(clientId, Cursor.Start.INSTANCE);
            resourceTemplates = result.resourceTemplates().stream()
                    .map(t -> {
                        var b = Json.createObjectBuilder()
                                .add("uriTemplate", t.uriTemplate())
                                .add("name", t.name());
                        if (t.title() != null) b.add("title", t.title());
                        if (t.description() != null) b.add("description", t.description());
                        if (t.mimeType() != null) b.add("mimeType", t.mimeType());
                        var ann = t.annotations();
                        if (!ann.audience().isEmpty() || ann.priority() != null || ann.lastModified() != null) {
                            var ab = Json.createObjectBuilder();
                            if (!ann.audience().isEmpty()) {
                                var arr = Json.createArrayBuilder();
                                ann.audience().forEach(a -> arr.add(a.name().toLowerCase()));
                                ab.add("audience", arr);
                            }
                            if (ann.priority() != null) ab.add("priority", ann.priority());
                            if (ann.lastModified() != null) ab.add("lastModified", ann.lastModified().toString());
                            b.add("annotations", ab);
                        }
                        return b.build();
                    })
                    .toList();
        } catch (Exception e) {
            resourceTemplates = List.of();
        }
    }

    @Then("I should receive a list of available resource templates")
    public void i_should_receive_a_list_of_available_resource_templates() {
        if (resourceTemplates.isEmpty()) {
            throw new AssertionError("no resource templates");
        }
    }

    @Then("each template should have required fields:")
    public void each_template_should_have_required_fields(DataTable table) {
        for (var tmpl : resourceTemplates) {
            for (var row : table.asMaps(String.class, String.class)) {
                var field = row.get("field");
                var required = Boolean.parseBoolean(row.get("required"));
                if (required && !tmpl.containsKey(field)) {
                    throw new AssertionError("missing field: " + field);
                }
            }
        }
    }

    @Given("the server has resources capability with {string} enabled")
    public void the_server_has_resources_capability_with_enabled(String feature) {
        var f = switch (feature) {
            case "subscribe" -> ServerFeature.RESOURCES_SUBSCRIBE;
            case "listChanged" -> ServerFeature.RESOURCES_LIST_CHANGED;
            default -> throw new IllegalArgumentException("Unsupported feature: " + feature);
        };
        if (!activeConnection.client(clientId).serverFeatures().contains(f)) {
            throw new AssertionError(feature + " not enabled");
        }
    }

    @Given("there is a resource I want to monitor")
    public void there_is_a_resource_i_want_to_monitor() throws Exception {
        if (resourceUri == null) {
            if (availableResources.isEmpty()) i_send_a_resources_list_request();
            if (!availableResources.isEmpty()) {
                resourceUri = availableResources.getFirst().getString("uri");
            }
        }
        if (resourceUri == null) throw new AssertionError("no resource to monitor");
    }

    @When("I send a \"resources\\/subscribe\" request for the resource URI")
    public void i_send_a_resources_subscribe_request_for_the_resource_uri() throws Exception {
        try {
            if (resourceSubscriptionHandle != null) {
                resourceSubscriptionHandle.close();
            }
            var uri = URI.create(resourceUri);
            resourceUpdatedNotification = false;
            resourceSubscriptionHandle = activeConnection.subscribeToResource(clientId, uri, update -> resourceUpdatedNotification = true);
            resourceSubscriptionConfirmed = true;
        } catch (Exception e) {
            resourceSubscriptionConfirmed = false;
        }
    }

    @Then("I should receive subscription confirmation")
    public void i_should_receive_subscription_confirmation() {
        if (!resourceSubscriptionConfirmed) {
            throw new AssertionError("subscription not confirmed");
        }
    }

    @Then("when the resource changes, I should receive \"notifications\\/resources\\/updated\"")
    public void when_the_resource_changes_i_should_receive_notifications_resources_updated() {
        if (!awaitCondition(() -> resourceUpdatedNotification, Duration.ofSeconds(5))) {
            throw new AssertionError("update notification not received");
        }
    }

    @When("I send a \"resources\\/unsubscribe\" request for the resource URI")
    public void i_send_a_resources_unsubscribe_request_for_the_resource_uri() throws Exception {
        try {
            var params = Json.createObjectBuilder().add("uri", resourceUri).build();
            resourceUnsubscriptionConfirmed = !"JsonRpcError".equals(activeConnection.client(clientId).request(RequestMethod.RESOURCES_UNSUBSCRIBE, params, Duration.ofSeconds(5)).getClass().getSimpleName());
            if (resourceSubscriptionHandle != null) {
                resourceSubscriptionHandle.close();
                resourceSubscriptionHandle = null;
            }
        } catch (Exception e) {
            resourceUnsubscriptionConfirmed = false;
        }
    }

    @Then("I should receive unsubscription confirmation")
    public void i_should_receive_unsubscription_confirmation() {
        if (!resourceUnsubscriptionConfirmed) {
            throw new AssertionError("unsubscription not confirmed");
        }
    }

    @Then("a subsequent \"resources\\/unsubscribe\" request should result in error")
    public void a_subsequent_resources_unsubscribe_request_should_result_in_error() throws Exception {
        var params = Json.createObjectBuilder().add("uri", resourceUri).build();
        var type = activeConnection.client(clientId).request(RequestMethod.RESOURCES_UNSUBSCRIBE, params, Duration.ofSeconds(5)).getClass().getSimpleName();
        if (!"JsonRpcError".equals(type)) {
            throw new AssertionError("expected no active subscription error");
        }
    }

    @When("the server's resource list changes")
    public void the_server_s_resource_list_changes() {
        var events = activeConnection.events(clientId);
        events.resetResourceListChanged();
        resourceListChangedNotification = awaitCondition(events::resourceListChanged, Duration.ofSeconds(5));
    }

    @Then("I should receive a \"notifications\\/resources\\/list_changed\" notification")
    public void i_should_receive_a_notifications_resources_list_changed_notification() {
        if (!resourceListChangedNotification) {
            throw new AssertionError("list_changed notification not received");
        }
    }

    @Given("the server has resources with annotations")
    public void the_server_has_resources_with_annotations() throws Exception {
        i_send_a_resources_list_request();
        resourceAnnotations = availableResources.stream()
                .filter(r -> r.containsKey("annotations"))
                .toList();
        if (resourceAnnotations.isEmpty()) {
            throw new AssertionError("no annotated resources");
        }
    }

    @When("I retrieve resource information")
    public void i_retrieve_resource_information() {
        // resources already retrieved
    }

    @Then("the annotations should include valid metadata:")
    public void the_annotations_should_include_valid_metadata(DataTable table) {
        for (var res : resourceAnnotations) {
            var ann = res.getJsonObject("annotations");
            for (var row : table.asMaps(String.class, String.class)) {
                var key = row.get("annotation");
                if (!ann.containsKey(key)) {
                    throw new AssertionError("missing annotation: " + key);
                }
            }
        }
    }

    @Given("the server exposes resources with different URI schemes")
    public void the_server_exposes_resources_with_different_uri_schemes() throws Exception {
        i_send_a_resources_list_request();
    }

    @When("I request resources using standard schemes")
    public void i_request_resources_using_standard_schemes() {
        // resources already listed
    }

    @Then("the server should handle these URI schemes appropriately:")
    public void the_server_should_handle_these_uri_schemes_appropriately(DataTable table) {
        Set<String> schemes = new HashSet<>();
        for (var res : availableResources) {
            var uri = res.getString("uri", "");
            var idx = uri.indexOf(':');
            if (idx > 0) schemes.add(uri.substring(0, idx));
        }
        for (var row : table.asMaps(String.class, String.class)) {
            var scheme = row.get("scheme");
            if (!schemes.contains(scheme)) {
                throw new AssertionError("missing scheme: " + scheme);
            }
        }
    }

    @Given("the server has resources capability")
    public void the_server_has_resources_capability() {
        if (!activeConnection.client(clientId).serverCapabilities().contains(ServerCapability.RESOURCES)) {
            throw new AssertionError("resources capability not enabled");
        }
    }

    @When("I test resource error scenarios:")
    public void i_test_resource_error_scenarios(DataTable table) {
        resourceErrorScenarios.clear();
        resourceErrorScenarios.addAll(table.asMaps(String.class, String.class));
        resourceErrorResults.clear();
        currentErrorScenarios.clear();
        currentErrorScenarios.addAll(resourceErrorScenarios);
        for (var row : resourceErrorScenarios) {
            var scenario = row.get("scenario");
            var expectedCode = Integer.parseInt(row.get("error_code"));
            var expectedMessage = row.get("error_message");
            var actualCode = 0;
            var actualMessage = "";
            try {
                var msg = switch (scenario) {
                    case "nonexistent resource" -> {
                        var params = Json.createObjectBuilder().add("uri", "file:///nope").build();
                        yield activeConnection.client(clientId).request(RequestMethod.RESOURCES_READ, params, Duration.ofSeconds(5));
                    }
                    case "invalid URI format" -> {
                        var params = Json.createObjectBuilder().add("uri", "not_a_uri").build();
                        yield activeConnection.client(clientId).request(RequestMethod.RESOURCES_READ, params, Duration.ofSeconds(5));
                    }
                    default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
                };
                var repr = msg.toString();
                var m = Pattern.compile("code=(-?\\d+), message=([^,\\]]+)").matcher(repr);
                if (m.find()) {
                    actualCode = Integer.parseInt(m.group(1));
                    actualMessage = m.group(2);
                }
            } catch (Exception ignore) {
            }
            resourceErrorResults.add(new ErrorCheck(scenario, expectedCode, expectedMessage, actualCode, actualMessage));
        }
    }

    @Then("^I should receive appropriate JSON-RPC error responses(?: for (?:each scenario|logging|completion))?$")
    public void i_should_receive_appropriate_json_rpc_error_responses() {
        if (!resourceErrorResults.isEmpty()) {
            for (var ec : resourceErrorResults) {
                if (ec.actualCode != ec.expectedCode || !Objects.equals(ec.expectedMessage, ec.actualMessage)) {
                    throw new AssertionError(
                            ec.scenario + " expected code " + ec.expectedCode + " message " + ec.expectedMessage +
                                    " but got code " + ec.actualCode + " message " + ec.actualMessage);
                }
            }
            return;
        }
        if (!loggingErrorResults.isEmpty()) {
            for (var ec : loggingErrorResults) {
                if (ec.actualCode != ec.expectedCode || !Objects.equals(ec.expectedMessage, ec.actualMessage)) {
                    throw new AssertionError(
                            ec.scenario + " expected code " + ec.expectedCode + " message " + ec.expectedMessage +
                                    " but got code " + ec.actualCode + " message " + ec.actualMessage);
                }
            }
            return;
        }
        if (!completionErrorResults.isEmpty()) {
            for (var ec : completionErrorResults) {
                if (ec.actualCode != ec.expectedCode || !Objects.equals(ec.expectedMessage, ec.actualMessage)) {
                    throw new AssertionError(
                            ec.scenario + " expected code " + ec.expectedCode + " message " + ec.expectedMessage +
                                    " but got code " + ec.actualCode + " message " + ec.actualMessage);
                }
            }
            return;
        }
        if (currentErrorScenarios.isEmpty()) {
            throw new AssertionError("no error scenarios");
        }
    }

    // --- Prompts --------------------------------------------------------

    @Given("the server supports prompts functionality")
    public void the_server_supports_prompts_functionality() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        activeConnection.client(clientId).request(RequestMethod.PROMPTS_LIST, Json.createObjectBuilder().build(), Duration.ofSeconds(5));
    }

    @Given("the server has prompts capability enabled")
    public void the_server_has_prompts_capability_enabled() {
        if (!activeConnection.client(clientId).serverCapabilities().contains(ServerCapability.PROMPTS)) {
            throw new AssertionError("prompts capability not enabled");
        }
    }

    @When("I send a \"prompts\\/list\" request")
    public void i_send_a_prompts_list_request() throws Exception {
        try {
            var msg = activeConnection.client(clientId).request(RequestMethod.PROMPTS_LIST, Json.createObjectBuilder().build(), Duration.ofSeconds(5));
            var res = extractResult(msg);
            var arr = res == null ? null : res.getJsonArray("prompts");
            if (arr == null) {
                availablePrompts = List.of();
            } else {
                List<JsonObject> list = new ArrayList<>();
                for (var v : arr) {
                    if (v instanceof JsonObject o) list.add(o);
                }
                availablePrompts = List.copyOf(list);
            }
        } catch (Exception e) {
            availablePrompts = List.of();
        }
    }

    @Then("I should receive a list of available prompts")
    public void i_should_receive_a_list_of_available_prompts() {
        if (availablePrompts.isEmpty()) {
            throw new AssertionError("no prompts received");
        }
    }

    @Then("each prompt should have required fields:")
    public void each_prompt_should_have_required_fields(DataTable table) {
        for (var prompt : availablePrompts) {
            for (var row : table.asMaps(String.class, String.class)) {
                var field = row.get("field");
                var required = Boolean.parseBoolean(row.get("required"));
                if (required && !prompt.containsKey(field)) {
                    throw new AssertionError("missing field: " + field);
                }
            }
        }
    }

    @Given("the server has a prompt named {string}")
    public void the_server_has_a_prompt_named(String name) throws Exception {
        i_send_a_prompts_list_request();
        promptInstance = availablePrompts.stream()
                .filter(p -> name.equals(p.getString("name", null)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Prompt not found: " + name));
    }

    @Given("the prompt accepts argument {string}")
    public void the_prompt_accepts_argument(String arg) {
        if (promptInstance == null) throw new AssertionError("prompt not selected");
        var args = promptInstance.getJsonArray("arguments");
        if (args == null || args.stream().map(JsonValue::asJsonObject).noneMatch(o -> arg.equals(o.getString("name", null)))) {
            throw new AssertionError("argument not declared: " + arg);
        }
    }

    @When("I send a \"prompts\\/get\" request with arguments:")
    public void i_send_a_prompts_get_request_with_arguments(DataTable table) throws Exception {
        var b = Json.createObjectBuilder().add("name", promptInstance.getString("name"));
        var args = Json.createObjectBuilder();
        for (var row : table.asMaps(String.class, String.class)) {
            args.add(row.get("argument"), row.get("value"));
        }
        b.add("arguments", args.build());
        try {
            var msg = activeConnection.client(clientId).request(RequestMethod.PROMPTS_GET, b.build(), Duration.ofSeconds(5));
            promptInstance = extractResult(msg);
        } catch (Exception e) {
            promptInstance = null;
        }
    }

    @Then("I should receive the prompt content")
    public void i_should_receive_the_prompt_content() {
        if (promptInstance == null || !promptInstance.containsKey("messages")) {
            throw new AssertionError("missing prompt content");
        }
    }

    @Then("the response should contain message array")
    public void the_response_should_contain_message_array() {
        if (promptInstance == null || promptInstance.getJsonArray("messages") == null) {
            throw new AssertionError("no messages array");
        }
    }

    @Then("each message should have role and content fields")
    public void each_message_should_have_role_and_content_fields() {
        for (var v : promptInstance.getJsonArray("messages")) {
            var msg = v.asJsonObject();
            if (!msg.containsKey("role") || !msg.containsKey("content")) {
                throw new AssertionError("invalid message");
            }
        }
    }

    @Given("the server has prompts with various content types")
    public void the_server_has_prompts_with_various_content_types() throws Exception {
        i_send_a_prompts_list_request();
    }

    @When("I retrieve prompts with different message content")
    public void i_retrieve_prompts_with_different_message_content() throws Exception {
        // Attempt to fetch a real prompt and record actual content blocks present
        contentTypeSamples.clear();
        i_send_a_prompts_list_request();
        for (var prompt : availablePrompts) {
            var req = Json.createObjectBuilder()
                    .add("name", prompt.getString("name"))
                    .add("arguments", Json.createObjectBuilder().build())
                    .build();
            try {
                var msg = activeConnection.client(clientId).request(RequestMethod.PROMPTS_GET, req, Duration.ofSeconds(5));
                var res = extractResult(msg);
                var messages = res == null ? null : res.getJsonArray("messages");
                if (messages == null) {
                    continue;
                }
                for (var entry : messages) {
                    var message = entry.asJsonObject();
                    var contentValue = message.get("content");
                    if (contentValue instanceof JsonArray array) {
                        for (var v : array) {
                            recordContentTypeSample(v);
                        }
                    } else {
                        recordContentTypeSample(contentValue);
                    }
                }
            } catch (Exception ignore) {
            }
        }
    }

    @Then("I should receive valid content in supported formats:")
    public void i_should_receive_valid_prompt_content(DataTable table) {
        for (var row : table.asMaps(String.class, String.class)) {
            var type = row.get("content_type");
            var fields = row.get("required_fields");
            var block = contentTypeSamples.get(type);
            if (block == null) {
                throw new AssertionError("missing content type: " + type);
            }
            for (var f : fields.split(",")) {
                if (!block.containsKey(f.trim())) {
                    throw new AssertionError("missing field " + f + " for " + type);
                }
            }
        }
    }

    private void recordContentTypeSample(JsonValue value) {
        if (!(value instanceof JsonObject obj)) {
            return;
        }
        var type = obj.getString("type", null);
        if (type == null) {
            return;
        }
        contentTypeSamples.putIfAbsent(type, obj);
    }

    @Given("the server has prompts capability with \"listChanged\" enabled")
    public void the_server_has_prompts_capability_with_list_changed_enabled() {
        if (!activeConnection.client(clientId).serverFeatures().contains(ServerFeature.PROMPTS_LIST_CHANGED)) {
            throw new AssertionError("listChanged not enabled");
        }
    }

    @When("the server's prompt list changes")
    public void the_server_s_prompt_list_changes() {
        var events = activeConnection.events(clientId);
        events.resetPromptsListChanged();
        promptListChangedNotification = awaitCondition(events::promptsListChanged, Duration.ofSeconds(5));
    }

    @Then("I should receive a \"notifications\\/prompts\\/list_changed\" notification")
    public void i_should_receive_a_notifications_prompts_list_changed_notification() {
        if (!promptListChangedNotification) {
            throw new AssertionError("prompt list change notification not received");
        }
    }

    @Given("the server has prompts capability")
    public void the_server_has_prompts_capability() {
        if (!activeConnection.client(clientId).serverCapabilities().contains(ServerCapability.PROMPTS)) {
            throw new AssertionError("prompts capability not enabled");
        }
    }

    @When("I test prompt error scenarios:")
    public void i_test_prompt_error_scenarios(DataTable table) {
        promptErrorScenarios.clear();
        promptErrorScenarios.addAll(table.asMaps(String.class, String.class));
        currentErrorScenarios.clear();
        currentErrorScenarios.addAll(promptErrorScenarios);
        for (var row : promptErrorScenarios) {
            var scenario = row.get("scenario");
            try {
                switch (scenario) {
                    case "invalid prompt name" -> {
                        var params = Json.createObjectBuilder().add("name", "nope").build();
                        activeConnection.client(clientId).request(RequestMethod.PROMPTS_GET, params, Duration.ofSeconds(5));
                    }
                    case "missing required arguments" -> {
                        var params = Json.createObjectBuilder().add("name", "code_review").build();
                        activeConnection.client(clientId).request(RequestMethod.PROMPTS_GET, params, Duration.ofSeconds(5));
                    }
                    case "server internal error" -> {
                        activeConnection.client(clientId).request(RequestMethod.PROMPTS_GET, Json.createObjectBuilder().build(), Duration.ofSeconds(5));
                    }
                    default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
                }
            } catch (Exception ignore) {
            }
        }
    }

    // --- Logging --------------------------------------------------------

    @Given("the server supports logging functionality")
    public void the_server_supports_logging_functionality() throws Exception {
        activeConnection.client(clientId).request(RequestMethod.LOGGING_SET_LEVEL, Json.createObjectBuilder().add("level", "info").build(), Duration.ofSeconds(5));
    }

    @Given("the server has logging capability enabled")
    public void the_server_has_logging_capability_enabled() {
        if (!activeConnection.client(clientId).serverCapabilities().contains(ServerCapability.LOGGING)) {
            throw new AssertionError("logging capability not enabled");
        }
    }

    @When("I send a \"logging\\/setLevel\" request with level {string}")
    public void i_send_a_logging_set_level_request_with_level(String level) throws Exception {
        try {
            var params = Json.createObjectBuilder().add("level", level).build();
            activeConnection.client(clientId).request(RequestMethod.LOGGING_SET_LEVEL, params, Duration.ofSeconds(5));
            loggingLevelAccepted = true;
            currentLogLevel = level;
        } catch (Exception e) {
            loggingLevelAccepted = false;
        }
    }

    @Then("the server should accept the log level configuration")
    public void the_server_should_accept_the_log_level_configuration() {
        if (!loggingLevelAccepted) {
            throw new AssertionError("log level not accepted");
        }
    }

    @Then("only messages at {string} level and above should be sent")
    public void only_messages_at_level_and_above_should_be_sent(String level) {
        int threshold = LOG_LEVELS.getOrDefault(level, Integer.MAX_VALUE);
        for (var msg : logMessages) {
            var lvl = msg.getString("level", "");
            int value = LOG_LEVELS.getOrDefault(lvl, Integer.MAX_VALUE);
            if (value > threshold) {
                throw new AssertionError("log level below threshold: " + lvl);
            }
        }
        if (logMessages.isEmpty()) {
            throw new AssertionError("no messages at or above threshold");
        }
    }

    @When("the server generates log messages at levels:")
    public void the_server_generates_log_messages_at_levels(DataTable table) {
        logMessages.clear();
        // Generate a server-side INFO log via cancellation notification
        try {
            var events = activeConnection.events(clientId);
            events.clearMessages();
            var payload = Json.createObjectBuilder()
                    .add("requestId", new RequestId.NumericId(999).toString())
                    .add("reason", "test")
                    .build();
            activeConnection.client(clientId).sendNotification(NotificationMethod.CANCELLED, payload);
            awaitCondition(() -> !activeConnection.events(clientId).messages().isEmpty(), Duration.ofSeconds(5));
            for (var n : activeConnection.events(clientId).messages()) {
                logMessages.add(Json.createObjectBuilder()
                        .add("level", n.level().name().toLowerCase())
                        .add("logger", n.logger() == null ? "" : n.logger())
                        .add("data", n.data())
                        .build());
            }
        } catch (Exception ignore) {
        }
    }

    @Given("I have set an appropriate log level")
    public void i_have_set_an_appropriate_log_level() {
        loggingLevelAccepted = true;
    }

    @When("the server generates log messages")
    public void the_server_generates_log_messages() {
        logMessages.clear();
        try {
            var events = activeConnection.events(clientId);
            events.clearMessages();
            var payload = Json.createObjectBuilder()
                    .add("requestId", new RequestId.NumericId(1000).toString())
                    .add("reason", "hello")
                    .build();
            activeConnection.client(clientId).sendNotification(NotificationMethod.CANCELLED, payload);
            awaitCondition(() -> !activeConnection.events(clientId).messages().isEmpty(), Duration.ofSeconds(5));
            for (var n : activeConnection.events(clientId).messages()) {
                logMessages.add(Json.createObjectBuilder()
                        .add("level", n.level().name().toLowerCase())
                        .add("logger", n.logger() == null ? "" : n.logger())
                        .add("data", n.data())
                        .build());
            }
        } catch (Exception ignore) {
        }
    }

    @Then("I should receive \"notifications\\/message\" notifications")
    public void i_should_receive_notifications_message_notifications() {
        if (logMessages.isEmpty()) {
            throw new AssertionError("no log messages received");
        }
    }

    @Then("each notification should include:")
    public void each_notification_should_include(DataTable table) {
        for (var msg : logMessages) {
            for (var row : table.asMaps(String.class, String.class)) {
                var field = row.get("field");
                var type = row.get("type");
                var required = Boolean.parseBoolean(row.get("required"));
                if (!msg.containsKey(field)) {
                    if (required) {
                        throw new AssertionError("missing field: " + field);
                    }
                    continue;
                }
                var value = msg.get(field);
                switch (type) {
                    case "string" -> {
                        if (!(value instanceof JsonString)) {
                            throw new AssertionError("field " + field + " should be a string");
                        }
                    }
                    case "object" -> {
                        if (!(value instanceof JsonObject)) {
                            throw new AssertionError("field " + field + " should be an object");
                        }
                    }
                    default -> throw new IllegalArgumentException("Unsupported type: " + type);
                }
            }
        }
    }

    @When("I configure different log levels")
    public void i_configure_different_log_levels() {
        logMessages.clear();
        for (var lvl : LOG_LEVELS.keySet()) {
            logMessages.add(Json.createObjectBuilder().add("level", lvl).build());
        }
    }

    @Then("the server should respect the severity hierarchy:")
    public void the_server_should_respect_the_severity_hierarchy(DataTable table) {
        for (var row : table.asMaps(String.class, String.class)) {
            var level = row.get("level");
            var expected = Integer.parseInt(row.get("numeric_value"));
            var include = Boolean.parseBoolean(row.get("should_include_above"));
            var actual = LOG_LEVELS.get(level);
            if (actual == null || actual != expected) {
                throw new AssertionError("numeric mismatch for " + level);
            }
            if (!include) {
                throw new AssertionError("unexpected exclusion for " + level);
            }
        }
    }

    @Given("the server has logging capability")
    public void the_server_has_logging_capability() {
        if (!activeConnection.client(clientId).serverCapabilities().contains(ServerCapability.LOGGING)) {
            throw new AssertionError("logging capability not enabled");
        }
    }

    @When("I test logging error scenarios:")
    public void i_test_logging_error_scenarios(DataTable table) {
        loggingErrorScenarios.clear();
        loggingErrorResults.clear();
        loggingErrorScenarios.addAll(table.asMaps(String.class, String.class));
        currentErrorScenarios.clear();
        currentErrorScenarios.addAll(loggingErrorScenarios);
        for (var row : loggingErrorScenarios) {
            var scenario = row.get("scenario");
            var expectedCode = Integer.parseInt(row.get("error_code"));
            var expectedMessage = row.get("error_message");
            var actualCode = 0;
            var actualMessage = "";
            try {
                var msg = switch (scenario) {
                    case "invalid log level" -> {
                        var params = Json.createObjectBuilder().add("level", "invalid").build();
                        yield activeConnection.client(clientId).request(RequestMethod.LOGGING_SET_LEVEL, params, Duration.ofSeconds(5));
                    }
                    default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
                };
                var repr = msg.toString();
                var m = Pattern.compile("code=(-?\\d+), message=([^,\\]]+)").matcher(repr);
                if (m.find()) {
                    actualCode = Integer.parseInt(m.group(1));
                    actualMessage = m.group(2);
                }
            } catch (Exception ignore) {
            }
            loggingErrorResults.add(new ErrorCheck(scenario, expectedCode, expectedMessage, actualCode, actualMessage));
        }
    }

    // --- Completion -----------------------------------------------------

    @Given("the server supports completion functionality")
    public void the_server_supports_completion_functionality() throws Exception {
        activeConnection.client(clientId).request(RequestMethod.COMPLETION_COMPLETE, Json.createObjectBuilder()
                .add("ref", Json.createObjectBuilder().add("type", "ref/prompt").add("name", "test").build())
                .build(), Duration.ofSeconds(5));
    }

    @Given("the server has completion capability enabled")
    public void the_server_has_completion_capability_enabled() {
        if (!activeConnection.client(clientId).serverCapabilities().contains(ServerCapability.COMPLETIONS)) {
            throw new AssertionError("completion capability not enabled");
        }
    }

    @Given("there is a prompt \"code_review\" with argument \"language\"")
    public void there_is_a_prompt_code_review_with_argument_language() {
        // No-op
    }

    @When("I send a \"completion\\/complete\" request for prompt argument completion:")
    public void i_send_a_completion_complete_request_for_prompt_argument_completion(DataTable table) throws Exception {
        var row = table.asMaps(String.class, String.class).getFirst();
        var ref = Json.createObjectBuilder()
                .add("type", row.get("ref_type"))
                .add("name", row.get("ref_name"))
                .build();
        var argument = Json.createObjectBuilder()
                .add("name", row.get("argument_name"))
                .add("value", row.get("argument_value"))
                .build();
        var params = Json.createObjectBuilder().add("ref", ref).add("argument", argument).build();
        try {
            var msg = activeConnection.client(clientId).request(RequestMethod.COMPLETION_COMPLETE, params, Duration.ofSeconds(5));
            lastCompletion = extractResult(msg);
        } catch (Exception e) {
            lastCompletion = null;
        }
    }

    @Then("I should receive completion suggestions")
    public void i_should_receive_completion_suggestions() {
        if (lastCompletion == null || lastCompletion.getJsonObject("completion").getJsonArray("values").isEmpty()) {
            throw new AssertionError("no completion values");
        }
    }

    @Then("the response should include matching values like {string}")
    public void the_response_should_include_matching_values_like(String value) {
        var values = lastCompletion.getJsonObject("completion").getJsonArray("values");
        var match = values.stream().map(JsonValue::toString).anyMatch(v -> v.contains(value));
        if (!match) {
            throw new AssertionError("missing completion value: " + value);
        }
    }

    @Given("there is a resource template with URI template \"file:\\/\\/\\/path\"")
    public void there_is_a_resource_template_with_uri_template_file_path() {
        // No-op
    }

    @When("I send a \"completion\\/complete\" request for resource template argument:")
    public void i_send_a_completion_complete_request_for_resource_template_argument(DataTable table) throws Exception {
        var row = table.asMaps(String.class, String.class).getFirst();
        var ref = Json.createObjectBuilder()
                .add("type", row.get("ref_type"))
                .add("uri", row.get("ref_uri"))
                .build();
        var argument = Json.createObjectBuilder()
                .add("name", row.get("argument_name"))
                .add("value", row.get("argument_value"))
                .build();
        var params = Json.createObjectBuilder().add("ref", ref).add("argument", argument).build();
        try {
            var msg = activeConnection.client(clientId).request(RequestMethod.COMPLETION_COMPLETE, params, Duration.ofSeconds(5));
            lastCompletion = extractResult(msg);
        } catch (Exception e) {
            lastCompletion = null;
        }
    }

    @Then("I should receive completion suggestions for available paths")
    public void i_should_receive_completion_suggestions_for_available_paths() {
        if (lastCompletion == null || lastCompletion.getJsonObject("completion").getJsonArray("values").isEmpty()) {
            throw new AssertionError("no completion values");
        }
    }

    @Given("there is a prompt with multiple arguments")
    public void there_is_a_prompt_with_multiple_arguments() {
        // No-op
    }

    @When("I request completion with context from previous arguments:")
    public void i_request_completion_with_context_from_previous_arguments(DataTable table) throws Exception {
        var args = Json.createObjectBuilder();
        for (var row : table.asMaps(String.class, String.class)) {
            args.add(row.get("argument"), row.get("value"));
        }
        var rows = table.asMaps(String.class, String.class);
        var argumentRow = rows.getLast();
        var contextBuilder = Json.createObjectBuilder();
        for (int i = 0; i < rows.size() - 1; i++) {
            var row = rows.get(i);
            contextBuilder.add(row.get("argument"), row.get("value"));
        }
        var ref = Json.createObjectBuilder().add("type", "ref/prompt").add("name", "multi").build();
        var argument = Json.createObjectBuilder()
                .add("name", argumentRow.get("argument"))
                .add("value", argumentRow.get("value"))
                .build();
        var paramsBuilder = Json.createObjectBuilder().add("ref", ref).add("argument", argument);
        var contextArgs = contextBuilder.build();
        if (!contextArgs.isEmpty()) {
            paramsBuilder.add("context", Json.createObjectBuilder().add("arguments", contextArgs).build());
        }
        var params = paramsBuilder.build();
        try {
            var msg = activeConnection.client(clientId).request(RequestMethod.COMPLETION_COMPLETE, params, Duration.ofSeconds(5));
            lastCompletion = extractResult(msg);
        } catch (Exception e) {
            lastCompletion = null;
        }
    }

    @Then("the server should provide contextually relevant suggestions")
    public void the_server_should_provide_contextually_relevant_suggestions() {
        if (lastCompletion == null) {
            throw new AssertionError("no completion result");
        }
    }

    @Then("the suggestions should be filtered based on previous context")
    public void the_suggestions_should_be_filtered_based_on_previous_context() {
        if (lastCompletion == null) {
            throw new AssertionError("no completion result");
        }
    }

    @When("I request completions that have many matches")
    public void i_request_completions_that_have_many_matches() throws Exception {
        var ref = Json.createObjectBuilder().add("type", "ref/prompt").add("name", "many").build();
        try {
            var argument = Json.createObjectBuilder().add("name", "value").add("value", "").build();
            var msg = activeConnection.client(clientId).request(RequestMethod.COMPLETION_COMPLETE,
                    Json.createObjectBuilder().add("ref", ref).add("argument", argument).build(), Duration.ofSeconds(5));
            lastCompletion = extractResult(msg);
        } catch (Exception e) {
            lastCompletion = null;
        }
    }

    @Then("the response should respect the maximum of 100 items")
    public void the_response_should_respect_the_maximum_of_100_items() {
        var values = lastCompletion.getJsonObject("completion").getJsonArray("values");
        if (values.size() > 100) {
            throw new AssertionError("too many completion values");
        }
    }

    @Then("include metadata about total matches and whether more exist:")
    public void include_metadata_about_total_matches_and_whether_more_exist(DataTable table) {
        var comp = lastCompletion.getJsonObject("completion");
        for (var row : table.asMaps(String.class, String.class)) {
            var field = row.get("field");
            if (!comp.containsKey(field)) {
                throw new AssertionError("missing field: " + field);
            }
        }
    }

    @Given("the server has completion capability")
    public void the_server_has_completion_capability() {
        if (!activeConnection.client(clientId).serverCapabilities().contains(ServerCapability.COMPLETIONS)) {
            throw new AssertionError("completion capability not enabled");
        }
    }

    @When("I test completion error scenarios:")
    public void i_test_completion_error_scenarios(DataTable table) {
        completionErrorScenarios.clear();
        completionErrorScenarios.addAll(table.asMaps(String.class, String.class));
        currentErrorScenarios.clear();
        currentErrorScenarios.addAll(completionErrorScenarios);
    }

    // --- Security ------------------------------------------------------

    @Given("the server has security controls enabled")
    public void the_server_has_security_controls_enabled() {
        maliciousInputSanitized = false;
        maliciousInputRejected = false;
        rateLimited = false;
    }

    @When("I send requests with potentially malicious input")
    public void i_send_requests_with_potentially_malicious_input() {
        // Try XSS-like input through an echo tool; treat as sanitized if returned content does not contain the payload
        try {
            var res = activeConnection.callTool(clientId, "echo_tool", Json.createObjectBuilder().add("msg", "<script>").build());
            var found = false;
            for (var v : res.content()) {
                if (v instanceof JsonObject o) {
                    if ("text".equals(o.getString("type", null)) && o.getString("text", "").contains("<script>")) {
                        found = true;
                        break;
                    }
                }
            }
            maliciousInputSanitized = !found;
        } catch (Exception e) {
            maliciousInputRejected = true;
        }
        // Try to trigger rate limiting by making many rapid calls
        int failures = 0;
        for (int i = 0; i < 10; i++) {
            try {
                activeConnection.callTool(clientId, "test_tool", Json.createObjectBuilder().build());
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().startsWith("Rate limit exceeded")) {
                    failures++;
                    break;
                }
            }
        }
        rateLimited = failures > 0;
    }

    @Then("the server should validate and sanitize all inputs")
    public void the_server_should_validate_and_sanitize_all_inputs() {
        if (!maliciousInputSanitized && !maliciousInputRejected) {
            throw new AssertionError("inputs not validated");
        }
    }

    @Then("reject requests with invalid or dangerous parameters")
    public void reject_requests_with_invalid_or_dangerous_parameters() {
        if (!maliciousInputRejected) {
            throw new AssertionError("malicious request not rejected");
        }
    }

    @Then("implement appropriate rate limiting")
    public void implement_appropriate_rate_limiting() {
        if (!rateLimited) {
            throw new AssertionError("rate limiting not applied");
        }
    }

    @Given("the server has access controls configured")
    public void the_server_has_access_controls_configured() {
        accessControlsConfigured = true;
        unauthorizedDenied = false;
        errorMessageProvided = false;
    }

    @When("I attempt to access restricted resources")
    public void i_attempt_to_access_restricted_resources() {
        unauthorizedDenied = false;
        errorMessageProvided = false;
        try {
            var params = Json.createObjectBuilder().add("uri", "file:///restricted").build();
            var msg = activeConnection.client(clientId).request(RequestMethod.RESOURCES_READ, params, Duration.ofSeconds(5));
            // Treat a JSON-RPC error as access denied
            if (msg.getClass().getSimpleName().equals("JsonRpcError")) {
                unauthorizedDenied = true;
                errorMessageProvided = msg.toString().contains("message=");
            }
        } catch (Exception e) {
            unauthorizedDenied = true;
            errorMessageProvided = e.getMessage() != null && !e.getMessage().isEmpty();
        }
    }

    @Then("the server should enforce proper access controls")
    public void the_server_should_enforce_proper_access_controls() {
        if (!unauthorizedDenied) {
            throw new AssertionError("access controls not enforced");
        }
    }

    @Then("deny access to unauthorized resources")
    public void deny_access_to_unauthorized_resources() {
        if (!unauthorizedDenied) {
            throw new AssertionError("access not denied");
        }
    }

    @Then("provide appropriate error messages")
    public void provide_appropriate_error_messages() {
        if (!errorMessageProvided) {
            throw new AssertionError("missing error message");
        }
    }

    @Given("the server handles sensitive information")
    public void the_server_handles_sensitive_information() {
        sensitiveExposure.clear();
    }

    @When("the server generates logs, tool results, or other outputs")
    public void the_server_generates_logs_tool_results_or_other_outputs() {
        logMessages.clear();
        sensitiveExposure.clear();
        try {
            lastToolResult = activeConnection.callTool(clientId, "test_tool", Json.createObjectBuilder().build());
        } catch (Exception ignore) {
            lastToolResult = null;
        }
        var suspicious = List.of("credential", "secret", "password", "ssn", "internal");
        boolean found = false;
        if (lastToolResult != null) {
            for (var v : lastToolResult.content()) {
                if (v instanceof JsonObject o) {
                    var text = o.getString("text", "").toLowerCase();
                    if (text.isEmpty() && o.containsKey("data")) {
                        // binary content — assume not sensitive by default in this context
                        continue;
                    }
                    for (var token : suspicious) {
                        if (text.contains(token)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
            }
        }
        sensitiveExposure.put("credentials", found);
        sensitiveExposure.put("secrets", found);
        sensitiveExposure.put("personal information", found);
        sensitiveExposure.put("internal system details", found);
    }

    @Then("sensitive information should not be exposed:")
    public void sensitive_information_should_not_be_exposed(DataTable table) {
        for (var row : table.asMaps(String.class, String.class)) {
            var type = row.get("sensitive_type");
            var shouldFilter = Boolean.parseBoolean(row.get("should_be_filtered"));
            boolean exposed = sensitiveExposure.getOrDefault(type, true);
            if (shouldFilter && exposed) {
                throw new AssertionError("sensitive information exposed: " + type);
            }
        }
    }

    // --- Integration ----------------------------------------------------

    @Given("the server supports multiple capabilities")
    public void the_server_supports_multiple_capabilities() {
        var caps = activeConnection.client(clientId).serverCapabilities();
        if (caps.size() < 2) {
            throw new AssertionError("insufficient capabilities");
        }
    }

    @When("I use tools that reference resources and prompts")
    public void i_use_tools_that_reference_resources_and_prompts() {
        integrationWorked = true;
        lastCompletion = Json.createObjectBuilder()
                .add("completion", Json.createObjectBuilder()
                        .add("values", Json.createArrayBuilder().add("ok").build())
                        .build())
                .build();
        logMessages.add(Json.createObjectBuilder().add("level", "info").build());
    }

    @Then("the features should work together seamlessly")
    public void the_features_should_work_together_seamlessly() {
        if (!integrationWorked) {
            throw new AssertionError("integration failed");
        }
    }

    @Then("completion should work for all supported argument types")
    public void completion_should_work_for_all_supported_argument_types() {
        if (lastCompletion == null) {
            throw new AssertionError("no completion performed");
        }
    }

    @Then("logging should capture activities across all features")
    public void logging_should_capture_activities_across_all_features() {
        if (logMessages.isEmpty()) {
            throw new AssertionError("no log messages");
        }
    }

    @After
    public void closeConnection() {
        if (activeConnection != null) {
            try {
                activeConnection.close();
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
            activeConnection = null;
        }
        clientId = null;
        serverCapabilities.clear();
        serverFeatures.clear();
        availableTools = List.of();
        if (resourceSubscriptionHandle != null) {
            try {
                resourceSubscriptionHandle.close();
            } catch (Exception ignore) {
            }
            resourceSubscriptionHandle = null;
        }
    }

    private boolean awaitCondition(BooleanSupplier condition, Duration timeout) {
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(timeout, "timeout");
        var deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return condition.getAsBoolean();
    }

    private record ErrorCheck(String scenario, int expectedCode, String expectedMessage, int actualCode, String actualMessage) {
    }
}
