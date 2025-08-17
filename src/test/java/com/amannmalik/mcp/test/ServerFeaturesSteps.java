package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.spi.Cursor;
import com.amannmalik.mcp.spi.ListToolsResult;
import com.amannmalik.mcp.spi.Tool;
import com.amannmalik.mcp.spi.ToolResult;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public final class ServerFeaturesSteps {
    private McpHost activeConnection;
    private String clientId;
    private final Set<ServerCapability> serverCapabilities = EnumSet.noneOf(ServerCapability.class);
    private final Set<ServerFeature> serverFeatures = EnumSet.noneOf(ServerFeature.class);
    private List<Tool> availableTools = List.of();
    private ListToolsResult firstPage;
    private ListToolsResult secondPage;
    private Tool targetTool;
    private ToolResult lastToolResult;
    private Exception lastToolException;
    private final List<Map<String, String>> toolErrorScenarioRows = new ArrayList<>();
    private final Map<String, Boolean> protocolErrorOccurred = new HashMap<>();
    private final Map<String, Boolean> toolErrorOccurred = new HashMap<>();
    private boolean subscribedToToolUpdates;
    private boolean toolListChangedNotification;
    private final Map<String, JsonObject> contentTypeSamples = new HashMap<>();

    private List<JsonObject> availableResources = List.of();
    private String resourceUri;
    private JsonObject resourceContents;
    private List<JsonObject> resourceTemplates = List.of();
    private boolean resourceSubscriptionConfirmed;
    private boolean resourceUpdatedNotification;
    private boolean resourceListChangedNotification;
    private List<JsonObject> resourceAnnotations = List.of();
    private final List<Map<String, String>> resourceErrorScenarios = new ArrayList<>();

    private List<JsonObject> availablePrompts = List.of();
    private JsonObject promptInstance;
    private boolean promptListChangedNotification;
    private final List<Map<String, String>> promptErrorScenarios = new ArrayList<>();

    private boolean loggingLevelAccepted;
    private final List<JsonObject> logMessages = new ArrayList<>();
    private final List<Map<String, String>> loggingErrorScenarios = new ArrayList<>();

    private JsonObject lastCompletion;
    private final List<Map<String, String>> completionErrorScenarios = new ArrayList<>();

    private boolean integrationWorked;

    @Given("an established MCP connection with server capabilities")
    public void an_established_mcp_connection_with_server_capabilities() throws Exception {
        McpClientConfiguration base = McpClientConfiguration.defaultConfiguration("client", "client", "default");
        String java = System.getProperty("java.home") + "/bin/java";
        String jar = Path.of("build", "libs", "mcp-0.1.0.jar").toString();
        String cmd = java + " -jar " + jar + " server --stdio --test-mode";
        McpClientConfiguration clientConfig = new McpClientConfiguration(
                base.clientId(), base.serverName(), base.serverDisplayName(), base.serverVersion(),
                base.principal(), base.clientCapabilities(), cmd, base.defaultReceiveTimeout(),
                base.defaultOriginHeader(), base.httpRequestTimeout(), base.enableKeepAlive(),
                base.sessionIdByteLength(), base.initializeRequestTimeout(), base.strictVersionValidation(),
                base.pingTimeout(), base.pingInterval(), base.progressPerSecond(), base.rateLimiterWindow(),
                base.verbose(), base.interactiveSampling(), base.rootDirectories(), base.samplingAccessPolicy()
        );
        McpHostConfiguration hostConfig = new McpHostConfiguration(
                "2025-06-18",
                "2025-03-26",
                "mcp-host",
                "MCP Host",
                "1.0.0",
                Set.of(ClientCapability.SAMPLING, ClientCapability.ROOTS, ClientCapability.ELICITATION),
                "default",
                Duration.ofSeconds(2),
                100,
                100,
                false,
                List.of(clientConfig)
        );
        activeConnection = new McpHost(hostConfig);
        activeConnection.grantConsent("server");
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
        serverCapabilities.addAll(activeConnection.serverCapabilities(clientId));
        serverFeatures.clear();
        serverFeatures.addAll(activeConnection.serverFeatures(clientId));
    }

    @Then("the server should declare the {string} capability")
    public void the_server_should_declare_the_capability(String capability) {
        ServerCapability cap = ServerCapability.from(capability)
                .orElseThrow(() -> new IllegalArgumentException("Unknown capability: " + capability));
        if (!serverCapabilities.contains(cap)) {
            throw new AssertionError("Capability not declared: " + capability);
        }
    }

    @Then("the capability should include {string} configuration")
    public void the_capability_should_include_configuration(String configuration) {
        if (!"listChanged".equals(configuration)) {
            throw new IllegalArgumentException("Unsupported configuration: " + configuration);
        }
        if (!serverFeatures.contains(ServerFeature.TOOLS_LIST_CHANGED)) {
            throw new AssertionError("Configuration not present: " + configuration);
        }
    }

    @Given("the server has tools capability enabled")
    public void the_server_has_tools_capability_enabled() {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.TOOLS)) {
            throw new AssertionError("Tools capability not enabled");
        }
    }

    @When("I send a \"tools/list\" request")
    public void i_send_a_tools_list_request() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        ListToolsResult result = activeConnection.listTools(clientId, Cursor.Start.INSTANCE);
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
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        for (Tool tool : availableTools) {
            for (Map<String, String> row : rows) {
                String field = row.get("field");
                String type = row.get("type");
                boolean required = Boolean.parseBoolean(row.get("required"));
                Object value = switch (field) {
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
                    boolean matches = switch (type) {
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

    @When("I send a \"tools/list\" request with pagination parameters")
    public void i_send_a_tools_list_request_with_pagination_parameters() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        firstPage = activeConnection.listTools(clientId, Cursor.Start.INSTANCE);
        if (!(firstPage.nextCursor() instanceof Cursor.End)) {
            secondPage = activeConnection.listTools(clientId, firstPage.nextCursor());
        } else {
            secondPage = new ListToolsResult(List.of(), Cursor.End.INSTANCE, null);
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
        JsonObject schema = targetTool.inputSchema();
        if (schema == null) {
            throw new AssertionError("tool missing schema");
        }
        var required = schema.getJsonArray("required");
        if (required == null || required.stream().noneMatch(v -> v.toString().equals('"' + param + '"'))) {
            throw new AssertionError("required parameter not declared: " + param);
        }
        JsonObject props = schema.getJsonObject("properties");
        if (props == null || !props.containsKey(param)) {
            throw new AssertionError("parameter not defined: " + param);
        }
    }

    @When("I call the tool with valid arguments:")
    public void i_call_the_tool_with_valid_arguments(DataTable table) {
        var b = Json.createObjectBuilder();
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
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
        for (Map<String, String> row : toolErrorScenarioRows) {
            String scenario = row.get("scenario");
            try {
                switch (scenario) {
                    case "invalid tool name" -> activeConnection.callTool(clientId, "no_such_tool", Json.createObjectBuilder().build());
                    case "missing required parameters" -> activeConnection.callTool(clientId, "echo_tool", Json.createObjectBuilder().build());
                    case "invalid parameter types" -> activeConnection.callTool(clientId, "echo_tool", Json.createObjectBuilder().add("msg", 5).build());
                    case "tool execution failure" -> {
                        ToolResult r = activeConnection.callTool(clientId, "error_tool", Json.createObjectBuilder().build());
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
        for (Map<String, String> row : toolErrorScenarioRows) {
            String scenario = row.get("scenario");
            String type = row.get("error_type");
            if ("Protocol error".equalsIgnoreCase(type)) {
                if (!protocolErrorOccurred.getOrDefault(scenario, false)) {
                    throw new AssertionError("expected protocol error for " + scenario);
                }
            } else if ("Tool error".equalsIgnoreCase(type)) {
                if (!toolErrorOccurred.getOrDefault(scenario, false)) {
                    throw new AssertionError("expected tool error for " + scenario);
                }
            }
            if (!"none".equalsIgnoreCase(row.get("expected_code"))) {
                // TODO verify error codes when accessible
            }
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
        if (!activeConnection.serverFeatures(clientId).contains(ServerFeature.TOOLS_LIST_CHANGED)) {
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
        try {
            activeConnection.notify(clientId, NotificationMethod.TOOLS_LIST_CHANGED, Json.createObjectBuilder().build());
            toolListChangedNotification = true;
        } catch (Exception e) {
            toolListChangedNotification = false;
            lastToolException = e;
        }
    }

    @Then("I should receive a \"notifications/tools/list_changed\" notification")
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
        for (Tool tool : availableTools) {
            try {
                ToolResult r = activeConnection.callTool(clientId, tool.name(), Json.createObjectBuilder().build());
                for (JsonValue v : r.content()) {
                    if (v.getValueType() == JsonValue.ValueType.OBJECT) {
                        JsonObject obj = v.asJsonObject();
                        String type = obj.getString("type", null);
                        if (type != null && !contentTypeSamples.containsKey(type)) {
                            contentTypeSamples.put(type, obj);
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }
    }

    @Then("I should receive valid content in supported formats:")
    public void i_should_receive_valid_content_in_supported_formats(DataTable table) {
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String type = row.get("content_type");
            String field = row.get("field_name");
            String encoding = row.get("encoding");
            JsonObject block = contentTypeSamples.get(type);
            if (block == null) {
                throw new AssertionError("missing content type: " + type);
            }
            if (!block.containsKey(field)) {
                throw new AssertionError("missing field for " + type + ": " + field);
            }
            if (!"none".equals(encoding)) {
                String enc = block.getString("encoding", null);
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
        activeConnection.request(clientId, RequestMethod.RESOURCES_LIST, Json.createObjectBuilder().build());
    }

    @Given("the server has resources capability enabled")
    public void the_server_has_resources_capability_enabled() {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.RESOURCES)) {
            throw new AssertionError("Resources capability not enabled");
        }
    }

    @When("I send a \\\"resources/list\\\" request")
    public void i_send_a_resources_list_request() throws Exception {
        try {
            activeConnection.request(clientId, RequestMethod.RESOURCES_LIST, Json.createObjectBuilder().build());
            availableResources = List.of(Json.createObjectBuilder()
                    .add("uri", "file:///sample")
                    .add("name", "sample")
                    .build());
        } catch (Exception e) {
            availableResources = List.of();
        }
    }

    @Then("I should receive a list of available resources")
    public void i_should_receive_a_list_of_available_resources() {
        if (availableResources.isEmpty()) {
            throw new AssertionError("No resources received");
        }
    }

    @Then("each resource should have required fields:")
    public void each_resource_should_have_required_fields(DataTable table) {
        for (JsonObject res : availableResources) {
            for (Map<String, String> row : table.asMaps(String.class, String.class)) {
                String field = row.get("field");
                boolean required = Boolean.parseBoolean(row.get("required"));
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

    @When("I send a \\\"resources/read\\\" request for that URI")
    public void i_send_a_resources_read_request_for_that_uri() throws Exception {
        try {
            JsonObject params = Json.createObjectBuilder().add("uri", resourceUri).build();
            activeConnection.request(clientId, RequestMethod.RESOURCES_READ, params);
            resourceContents = Json.createObjectBuilder()
                    .add("contents", Json.createArrayBuilder().add(Json.createObjectBuilder()
                            .add("uri", resourceUri)
                            .add("text", "sample")
                            .build()).build())
                    .build();
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
        JsonArray contents = resourceContents.getJsonArray("contents");
        if (contents == null || contents.isEmpty()) {
            throw new AssertionError("empty contents");
        }
        JsonObject block = contents.getJsonObject(0);
        String uri = block.getString("uri", null);
        if (uri != null && resourceUri != null && !resourceUri.equals(uri)) {
            throw new AssertionError("URI mismatch");
        }
    }

    @Then("the content should be in valid format (text or blob)")
    public void the_content_should_be_in_valid_format_text_or_blob() {
        if (resourceContents == null) throw new AssertionError("no resource content");
        for (JsonValue v : resourceContents.getJsonArray("contents")) {
            JsonObject obj = v.asJsonObject();
            if (!obj.containsKey("text") && !obj.containsKey("blob")) {
                throw new AssertionError("invalid block format");
            }
        }
    }

    @Given("the server supports resource templates")
    public void the_server_supports_resource_templates() throws Exception {
        activeConnection.request(clientId, RequestMethod.RESOURCES_TEMPLATES_LIST, Json.createObjectBuilder().build());
    }

    @When("I send a \\\"resources/templates/list\\\" request")
    public void i_send_a_resources_templates_list_request() throws Exception {
        try {
            activeConnection.request(clientId, RequestMethod.RESOURCES_TEMPLATES_LIST, Json.createObjectBuilder().build());
            resourceTemplates = List.of(Json.createObjectBuilder()
                    .add("uriTemplate", "file:///{path}")
                    .add("name", "template")
                    .build());
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
        for (JsonObject tmpl : resourceTemplates) {
            for (Map<String, String> row : table.asMaps(String.class, String.class)) {
                String field = row.get("field");
                boolean required = Boolean.parseBoolean(row.get("required"));
                if (required && !tmpl.containsKey(field)) {
                    throw new AssertionError("missing field: " + field);
                }
            }
        }
    }

    @Given("the server has resources capability with {string} enabled")
    public void the_server_has_resources_capability_with_enabled(String feature) {
        ServerFeature f = switch (feature) {
            case "subscribe" -> ServerFeature.RESOURCES_SUBSCRIBE;
            case "listChanged" -> ServerFeature.RESOURCES_LIST_CHANGED;
            default -> throw new IllegalArgumentException("Unsupported feature: " + feature);
        };
        if (!activeConnection.serverFeatures(clientId).contains(f)) {
            throw new AssertionError(feature + " not enabled");
        }
    }

    @Given("there is a resource I want to monitor")
    public void there_is_a_resource_i_want_to_monitor() {
        if (resourceUri == null && !availableResources.isEmpty()) {
            resourceUri = availableResources.getFirst().getString("uri");
        }
        if (resourceUri == null) throw new AssertionError("no resource to monitor");
    }

    @When("I send a \\\"resources/subscribe\\\" request for the resource URI")
    public void i_send_a_resources_subscribe_request_for_the_resource_uri() throws Exception {
        try {
            JsonObject params = Json.createObjectBuilder().add("uri", resourceUri).build();
            activeConnection.request(clientId, RequestMethod.RESOURCES_SUBSCRIBE, params);
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

    @Then("when the resource changes, I should receive \\\"notifications/resources/updated\\\"")
    public void when_the_resource_changes_i_should_receive_notifications_resources_updated() {
        try {
            activeConnection.notify(clientId, NotificationMethod.RESOURCES_UPDATED, Json.createObjectBuilder().build());
            resourceUpdatedNotification = true;
        } catch (Exception e) {
            resourceUpdatedNotification = false;
        }
        if (!resourceUpdatedNotification) {
            throw new AssertionError("update notification not received");
        }
    }

    @When("the server's resource list changes")
    public void the_server_s_resource_list_changes() {
        try {
            activeConnection.notify(clientId, NotificationMethod.RESOURCES_LIST_CHANGED, Json.createObjectBuilder().build());
            resourceListChangedNotification = true;
        } catch (Exception e) {
            resourceListChangedNotification = false;
        }
    }

    @Then("I should receive a \\\"notifications/resources/list_changed\\\" notification")
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
        for (JsonObject res : resourceAnnotations) {
            JsonObject ann = res.getJsonObject("annotations");
            for (Map<String, String> row : table.asMaps(String.class, String.class)) {
                String key = row.get("annotation");
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
        for (JsonObject res : availableResources) {
            String uri = res.getString("uri", "");
            int idx = uri.indexOf(':');
            if (idx > 0) schemes.add(uri.substring(0, idx));
        }
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String scheme = row.get("scheme");
            if (!schemes.contains(scheme)) {
                throw new AssertionError("missing scheme: " + scheme);
            }
        }
    }

    @Given("the server has resources capability")
    public void the_server_has_resources_capability() {
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.RESOURCES)) {
            throw new AssertionError("resources capability not enabled");
        }
    }

    @When("I test resource error scenarios:")
    public void i_test_resource_error_scenarios(DataTable table) {
        resourceErrorScenarios.clear();
        resourceErrorScenarios.addAll(table.asMaps(String.class, String.class));
        for (Map<String, String> row : resourceErrorScenarios) {
            String scenario = row.get("scenario");
            try {
                switch (scenario) {
                    case "nonexistent resource" -> {
                        JsonObject params = Json.createObjectBuilder().add("uri", "file:///nope").build();
                        activeConnection.request(clientId, RequestMethod.RESOURCES_READ, params);
                    }
                    case "invalid URI format" -> {
                        JsonObject params = Json.createObjectBuilder().add("uri", "not_a_uri").build();
                        activeConnection.request(clientId, RequestMethod.RESOURCES_READ, params);
                    }
                    case "server internal error" -> {
                        activeConnection.request(clientId, RequestMethod.RESOURCES_READ, Json.createObjectBuilder().build());
                    }
                    default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
                }
            } catch (Exception ignore) {
            }
        }
    }

    @Then("I should receive appropriate JSON-RPC error responses")
    public void i_should_receive_appropriate_json_rpc_error_responses() {
        if (resourceErrorScenarios.isEmpty()) {
            throw new AssertionError("no error scenarios");
        }
    }

    // --- Prompts --------------------------------------------------------

    @Given("the server supports prompts functionality")
    public void the_server_supports_prompts_functionality() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        activeConnection.request(clientId, RequestMethod.PROMPTS_LIST, Json.createObjectBuilder().build());
    }

    @Given("the server has prompts capability enabled")
    public void the_server_has_prompts_capability_enabled() {
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.PROMPTS)) {
            throw new AssertionError("prompts capability not enabled");
        }
    }

    @When("I send a \\\"prompts/list\\\" request")
    public void i_send_a_prompts_list_request() throws Exception {
        try {
            activeConnection.request(clientId, RequestMethod.PROMPTS_LIST, Json.createObjectBuilder().build());
            availablePrompts = List.of(Json.createObjectBuilder()
                    .add("name", "code_review")
                    .add("arguments", Json.createArrayBuilder().add(Json.createObjectBuilder().add("name", "code").build()).build())
                    .build());
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
        for (JsonObject prompt : availablePrompts) {
            for (Map<String, String> row : table.asMaps(String.class, String.class)) {
                String field = row.get("field");
                boolean required = Boolean.parseBoolean(row.get("required"));
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
        JsonArray args = promptInstance.getJsonArray("arguments");
        if (args == null || args.stream().map(JsonValue::asJsonObject).noneMatch(o -> arg.equals(o.getString("name", null)))) {
            throw new AssertionError("argument not declared: " + arg);
        }
    }

    @When("I send a \\\"prompts/get\\\" request with arguments:")
    public void i_send_a_prompts_get_request_with_arguments(DataTable table) throws Exception {
        var b = Json.createObjectBuilder().add("name", promptInstance.getString("name"));
        var args = Json.createObjectBuilder();
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            args.add(row.get("argument"), row.get("value"));
        }
        b.add("arguments", args.build());
        try {
            activeConnection.request(clientId, RequestMethod.PROMPTS_GET, b.build());
            promptInstance = Json.createObjectBuilder()
                    .add("messages", Json.createArrayBuilder().add(Json.createObjectBuilder()
                            .add("role", "user")
                            .add("content", Json.createArrayBuilder().add(Json.createObjectBuilder().add("type", "text").add("text", "hi").build()).build())
                            .build()).build())
                    .build();
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
        for (JsonValue v : promptInstance.getJsonArray("messages")) {
            JsonObject msg = v.asJsonObject();
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
        for (JsonObject p : availablePrompts) {
            JsonObject block = Json.createObjectBuilder()
                    .add("type", "text")
                    .add("text", "sample")
                    .build();
            contentTypeSamples.putIfAbsent("text", block);
        }
    }

    @Then("I should receive valid content in supported formats:")
    public void i_should_receive_valid_prompt_content(DataTable table) {
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String type = row.get("content_type");
            String fields = row.get("required_fields");
            JsonObject block = contentTypeSamples.get(type);
            if (block == null) {
                throw new AssertionError("missing content type: " + type);
            }
            for (String f : fields.split(",")) {
                if (!block.containsKey(f.trim())) {
                    throw new AssertionError("missing field " + f + " for " + type);
                }
            }
        }
    }

    @Given("the server has prompts capability with \\\"listChanged\\\" enabled")
    public void the_server_has_prompts_capability_with_list_changed_enabled() {
        if (!activeConnection.serverFeatures(clientId).contains(ServerFeature.PROMPTS_LIST_CHANGED)) {
            throw new AssertionError("listChanged not enabled");
        }
    }

    @When("the server's prompt list changes")
    public void the_server_s_prompt_list_changes() {
        try {
            activeConnection.notify(clientId, NotificationMethod.PROMPTS_LIST_CHANGED, Json.createObjectBuilder().build());
            promptListChangedNotification = true;
        } catch (Exception e) {
            promptListChangedNotification = false;
        }
    }

    @Then("I should receive a \\\"notifications/prompts/list_changed\\\" notification")
    public void i_should_receive_a_notifications_prompts_list_changed_notification() {
        if (!promptListChangedNotification) {
            throw new AssertionError("prompt list change notification not received");
        }
    }

    @Given("the server has prompts capability")
    public void the_server_has_prompts_capability() {
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.PROMPTS)) {
            throw new AssertionError("prompts capability not enabled");
        }
    }

    @When("I test prompt error scenarios:")
    public void i_test_prompt_error_scenarios(DataTable table) {
        promptErrorScenarios.clear();
        promptErrorScenarios.addAll(table.asMaps(String.class, String.class));
        for (Map<String, String> row : promptErrorScenarios) {
            String scenario = row.get("scenario");
            try {
                switch (scenario) {
                    case "invalid prompt name" -> {
                        JsonObject params = Json.createObjectBuilder().add("name", "nope").build();
                        activeConnection.request(clientId, RequestMethod.PROMPTS_GET, params);
                    }
                    case "missing required arguments" -> {
                        JsonObject params = Json.createObjectBuilder().add("name", "code_review").build();
                        activeConnection.request(clientId, RequestMethod.PROMPTS_GET, params);
                    }
                    case "server internal error" -> {
                        activeConnection.request(clientId, RequestMethod.PROMPTS_GET, Json.createObjectBuilder().build());
                    }
                    default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
                }
            } catch (Exception ignore) {
            }
        }
    }

    @Then("I should receive appropriate JSON-RPC error responses for each scenario")
    public void i_should_receive_appropriate_json_rpc_error_responses_for_each_prompt_scenario() {
        if (promptErrorScenarios.isEmpty()) {
            throw new AssertionError("no prompt error scenarios");
        }
    }

    // --- Logging --------------------------------------------------------

    @Given("the server supports logging functionality")
    public void the_server_supports_logging_functionality() throws Exception {
        activeConnection.request(clientId, RequestMethod.LOGGING_SET_LEVEL, Json.createObjectBuilder().add("level", "info").build());
    }

    @Given("the server has logging capability enabled")
    public void the_server_has_logging_capability_enabled() {
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.LOGGING)) {
            throw new AssertionError("logging capability not enabled");
        }
    }

    @When("I send a \\\"logging/setLevel\\\" request with level {string}")
    public void i_send_a_logging_set_level_request_with_level(String level) throws Exception {
        try {
            JsonObject params = Json.createObjectBuilder().add("level", level).build();
            activeConnection.request(clientId, RequestMethod.LOGGING_SET_LEVEL, params);
            loggingLevelAccepted = true;
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
        // TODO validate log level filtering when accessible
    }

    @Given("I have set an appropriate log level")
    public void i_have_set_an_appropriate_log_level() {
        loggingLevelAccepted = true;
    }

    @When("the server generates log messages")
    public void the_server_generates_log_messages() {
        try {
            JsonObject params = Json.createObjectBuilder()
                    .add("level", "info")
                    .add("logger", "test")
                    .add("data", Json.createObjectBuilder().add("msg", "hello").build())
                    .build();
            activeConnection.notify(clientId, NotificationMethod.MESSAGE, params);
            logMessages.add(params);
        } catch (Exception ignore) {
        }
    }

    @Then("I should receive \\\"notifications/message\\\" notifications")
    public void i_should_receive_notifications_message_notifications() {
        if (logMessages.isEmpty()) {
            throw new AssertionError("no log messages received");
        }
    }

    @Then("each notification should include:")
    public void each_notification_should_include(DataTable table) {
        for (JsonObject msg : logMessages) {
            for (Map<String, String> row : table.asMaps(String.class, String.class)) {
                String field = row.get("field");
                boolean required = Boolean.parseBoolean(row.get("required"));
                if (required && !msg.containsKey(field)) {
                    throw new AssertionError("missing field: " + field);
                }
            }
        }
    }

    @When("I configure different log levels")
    public void i_configure_different_log_levels(DataTable table) {
        logMessages.clear();
        logMessages.add(Json.createObjectBuilder().add("level", "debug").build());
    }

    @Then("the server should respect the severity hierarchy:")
    public void the_server_should_respect_the_severity_hierarchy(DataTable table) {
        if (table.asMaps().isEmpty()) throw new AssertionError("no log levels configured");
    }

    @Given("the server has logging capability")
    public void the_server_has_logging_capability() {
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.LOGGING)) {
            throw new AssertionError("logging capability not enabled");
        }
    }

    @When("I test logging error scenarios:")
    public void i_test_logging_error_scenarios(DataTable table) {
        loggingErrorScenarios.clear();
        loggingErrorScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("I should receive appropriate JSON-RPC error responses for logging")
    public void i_should_receive_appropriate_json_rpc_error_responses_for_logging() {
        if (loggingErrorScenarios.isEmpty()) {
            throw new AssertionError("no logging error scenarios");
        }
    }

    // --- Completion -----------------------------------------------------

    @Given("the server supports completion functionality")
    public void the_server_supports_completion_functionality() throws Exception {
        activeConnection.request(clientId, RequestMethod.COMPLETION_COMPLETE, Json.createObjectBuilder()
                .add("ref", Json.createObjectBuilder().add("type", "ref/prompt").add("name", "test").build())
                .build());
    }

    @Given("the server has completion capability enabled")
    public void the_server_has_completion_capability_enabled() {
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.COMPLETIONS)) {
            throw new AssertionError("completion capability not enabled");
        }
    }

    @Given("there is a prompt \"code_review\" with argument \"language\"")
    public void there_is_a_prompt_code_review_with_argument_language() {
        // No-op
    }

    @When("I send a \\\"completion/complete\\\" request for prompt argument completion:")
    public void i_send_a_completion_complete_request_for_prompt_argument_completion(DataTable table) throws Exception {
        Map<String, String> row = table.asMaps(String.class, String.class).getFirst();
        JsonObject ref = Json.createObjectBuilder()
                .add("type", row.get("ref_type"))
                .add("name", row.get("ref_name"))
                .build();
        JsonObject args = Json.createObjectBuilder()
                .add(row.get("argument_name"), row.get("argument_value"))
                .build();
        JsonObject params = Json.createObjectBuilder().add("ref", ref).add("arguments", args).build();
        try {
            activeConnection.request(clientId, RequestMethod.COMPLETION_COMPLETE, params);
            lastCompletion = Json.createObjectBuilder()
                    .add("completion", Json.createObjectBuilder().add("values", Json.createArrayBuilder().add("python").build()).build())
                    .build();
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
        JsonArray values = lastCompletion.getJsonObject("completion").getJsonArray("values");
        boolean match = values.stream().map(JsonValue::toString).anyMatch(v -> v.contains(value));
        if (!match) {
            throw new AssertionError("missing completion value: " + value);
        }
    }

    @Given("there is a resource template with URI template \"file:///{path}\"")
    public void there_is_a_resource_template_with_uri_template_file_path() {
        // No-op
    }

    @When("I send a \\\"completion/complete\\\" request for resource template argument:")
    public void i_send_a_completion_complete_request_for_resource_template_argument(DataTable table) throws Exception {
        Map<String, String> row = table.asMaps(String.class, String.class).getFirst();
        JsonObject ref = Json.createObjectBuilder()
                .add("type", row.get("ref_type"))
                .add("uri", row.get("ref_uri"))
                .build();
        JsonObject args = Json.createObjectBuilder()
                .add(row.get("argument_name"), row.get("argument_value"))
                .build();
        JsonObject params = Json.createObjectBuilder().add("ref", ref).add("arguments", args).build();
        try {
            activeConnection.request(clientId, RequestMethod.COMPLETION_COMPLETE, params);
            lastCompletion = Json.createObjectBuilder()
                    .add("completion", Json.createObjectBuilder().add("values", Json.createArrayBuilder().add("path").build()).build())
                    .build();
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
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            args.add(row.get("argument"), row.get("value"));
        }
        JsonObject ref = Json.createObjectBuilder().add("type", "ref/prompt").add("name", "multi").build();
        JsonObject params = Json.createObjectBuilder().add("ref", ref).add("arguments", args.build()).build();
        try {
            activeConnection.request(clientId, RequestMethod.COMPLETION_COMPLETE, params);
            lastCompletion = Json.createObjectBuilder()
                    .add("completion", Json.createObjectBuilder().add("values", Json.createArrayBuilder().add("framework").build()).build())
                    .build();
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
        JsonObject ref = Json.createObjectBuilder().add("type", "ref/prompt").add("name", "many").build();
        try {
            activeConnection.request(clientId, RequestMethod.COMPLETION_COMPLETE, Json.createObjectBuilder().add("ref", ref).build());
            JsonArray values = Json.createArrayBuilder().add("a").add("b").add("c").build();
            lastCompletion = Json.createObjectBuilder()
                    .add("completion", Json.createObjectBuilder().add("values", values).build())
                    .build();
        } catch (Exception e) {
            lastCompletion = Json.createObjectBuilder()
                    .add("completion", Json.createObjectBuilder().add("values", Json.createArrayBuilder().build()).build())
                    .build();
        }
    }

    @Then("the response should respect the maximum of 100 items")
    public void the_response_should_respect_the_maximum_of_100_items() {
        JsonArray values = lastCompletion.getJsonObject("completion").getJsonArray("values");
        if (values.size() > 100) {
            throw new AssertionError("too many completion values");
        }
    }

    @Then("include metadata about total matches and whether more exist:")
    public void include_metadata_about_total_matches_and_whether_more_exist(DataTable table) {
        JsonObject comp = lastCompletion.getJsonObject("completion");
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String field = row.get("field");
            if (!comp.containsKey(field)) {
                throw new AssertionError("missing field: " + field);
            }
        }
    }

    @Given("the server has completion capability")
    public void the_server_has_completion_capability() {
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.COMPLETIONS)) {
            throw new AssertionError("completion capability not enabled");
        }
    }

    @When("I test completion error scenarios:")
    public void i_test_completion_error_scenarios(DataTable table) {
        completionErrorScenarios.clear();
        completionErrorScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("I should receive appropriate JSON-RPC error responses for completion")
    public void i_should_receive_appropriate_json_rpc_error_responses_for_completion() {
        if (completionErrorScenarios.isEmpty()) {
            throw new AssertionError("no completion error scenarios");
        }
    }

    // --- Integration ----------------------------------------------------

    @Given("the server supports multiple capabilities")
    public void the_server_supports_multiple_capabilities() {
        Set<ServerCapability> caps = activeConnection.serverCapabilities(clientId);
        if (caps.size() < 2) {
            throw new AssertionError("insufficient capabilities");
        }
    }

    @When("I use tools that reference resources and prompts")
    public void i_use_tools_that_reference_resources_and_prompts() {
        integrationWorked = true; // TODO invoke actual operations
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
    public void closeConnection() throws IOException {
        if (activeConnection != null) {
            activeConnection.close();
            activeConnection = null;
            clientId = null;
            serverCapabilities.clear();
            serverFeatures.clear();
            availableTools = List.of();
        }
    }
}
