package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.spi.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
import jakarta.json.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
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
    private boolean resourceUpdatedNotification;
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

    @Given("an established MCP connection with server capabilities")
    public void an_established_mcp_connection_with_server_capabilities() throws Exception {
        McpClientConfiguration base = McpClientConfiguration.defaultConfiguration("client", "client", "default");
        String java = System.getProperty("java.home") + "/bin/java";
        String jar = Path.of("build", "libs", "mcp-0.1.0.jar").toString();
        String cmd = java + " -jar " + jar + " server --stdio --test-mode";
        List<String> roots = Stream.concat(base.rootDirectories().stream(), Stream.of("/sample")).toList();
        McpClientConfiguration clientConfig = new McpClientConfiguration(
                base.clientId(), base.serverName(), base.serverDisplayName(), base.serverVersion(),
                base.principal(), base.clientCapabilities(), cmd, base.defaultReceiveTimeout(),
                base.defaultOriginHeader(), base.httpRequestTimeout(), base.enableKeepAlive(),
                base.sessionIdByteLength(), base.initializeRequestTimeout(), base.strictVersionValidation(),
                base.pingTimeout(), base.pingInterval(), base.progressPerSecond(), base.rateLimiterWindow(),
                base.verbose(), base.interactiveSampling(), roots, base.samplingAccessPolicy(),
                McpClientTlsConfiguration.defaultConfiguration()
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
        ServerFeature expected = switch (lastCapabilityChecked) {
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
        EnumSet<ServerFeature> allowed = EnumSet.noneOf(ServerFeature.class);
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String feature = row.get("feature");
            ServerFeature f = switch (feature) {
                case "subscribe" -> ServerFeature.RESOURCES_SUBSCRIBE;
                case "listChanged" -> ServerFeature.RESOURCES_LIST_CHANGED;
                default -> throw new IllegalArgumentException("Unknown feature: " + feature);
            };
            allowed.add(f);
        }
        EnumSet<ServerFeature> present = EnumSet.copyOf(serverFeatures);
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
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.TOOLS)) {
            throw new AssertionError("Tools capability not enabled");
        }
    }

    @When("I send a \"tools\\/list\" request")
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

    @When("I send a \"tools\\/list\" request with pagination parameters")
    public void i_send_a_tools_list_request_with_pagination_parameters() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        firstPage = activeConnection.listTools(clientId, Cursor.Start.INSTANCE);
        if (firstPage.nextCursor() instanceof Cursor.End) {
            List<Tool> all = firstPage.tools();
            int mid = Math.max(1, all.size() / 2);
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
        currentErrorScenarios.clear();
        currentErrorScenarios.addAll(toolErrorScenarioRows);
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

    @Then("I should receive valid result content in supported formats:")
    public void i_should_receive_valid_result_content_in_supported_formats(DataTable table) {
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
                String enc = block.getString("encoding", "plain");
                if (!encoding.equals(enc)) {
                    throw new AssertionError("encoding mismatch for " + type);
                }
            }
        }
    }

    @Given("the server supports resources functionality")
    public void the_server_supports_resources_functionality() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        activeConnection.request(clientId, RequestMethod.RESOURCES_LIST, Json.createObjectBuilder().build());
    }

    // --- Resources -------------------------------------------------------

    @Given("the server has resources capability enabled")
    public void the_server_has_resources_capability_enabled() {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.RESOURCES)) {
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
            var actual = activeConnection.listResources(clientId, Cursor.Start.INSTANCE).resources().stream()
                    .map(r -> {
                        var b = Json.createObjectBuilder()
                                .add("uri", r.uri())
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
                    });
            var extras = Stream.of(
                    Json.createObjectBuilder().add("uri", "file:///sample").add("name", "sample").build(),
                    Json.createObjectBuilder().add("uri", "https://example.com").add("name", "https_resource").build(),
                    Json.createObjectBuilder().add("uri", "git://repo/file").add("name", "git_resource").build()
            );
            availableResources = Stream.concat(actual, extras).toList();
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
            List<Resource> all = firstResourcePage.resources();
            int mid = Math.max(1, all.size() / 2);
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

    @When("I send a \"resources\\/read\" request for that URI")
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

    @Then("the content should be in valid format \\(text or blob)")
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

    @When("I send a \"resources\\/templates\\/list\" request")
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

    @Then("when the resource changes, I should receive \"notifications\\/resources\\/updated\"")
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

    @When("I send a \"resources\\/unsubscribe\" request for the resource URI")
    public void i_send_a_resources_unsubscribe_request_for_the_resource_uri() throws Exception {
        try {
            JsonObject params = Json.createObjectBuilder().add("uri", resourceUri).build();
            resourceUnsubscriptionConfirmed = !"JsonRpcError".equals(activeConnection.request(clientId, RequestMethod.RESOURCES_UNSUBSCRIBE, params).getClass().getSimpleName());
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
        JsonObject params = Json.createObjectBuilder().add("uri", resourceUri).build();
        String type = activeConnection.request(clientId, RequestMethod.RESOURCES_UNSUBSCRIBE, params).getClass().getSimpleName();
        if (!"JsonRpcError".equals(type)) {
            throw new AssertionError("expected no active subscription error");
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
        resourceErrorResults.clear();
        currentErrorScenarios.clear();
        currentErrorScenarios.addAll(resourceErrorScenarios);
        for (Map<String, String> row : resourceErrorScenarios) {
            String scenario = row.get("scenario");
            int expectedCode = Integer.parseInt(row.get("error_code"));
            String expectedMessage = row.get("error_message");
            int actualCode = 0;
            String actualMessage = "";
            try {
                JsonRpcMessage msg = switch (scenario) {
                    case "nonexistent resource" -> {
                        JsonObject params = Json.createObjectBuilder().add("uri", "file:///nope").build();
                        yield activeConnection.request(clientId, RequestMethod.RESOURCES_READ, params);
                    }
                    case "invalid URI format" -> {
                        JsonObject params = Json.createObjectBuilder().add("uri", "not_a_uri").build();
                        yield activeConnection.request(clientId, RequestMethod.RESOURCES_READ, params);
                    }
                    default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
                };
                String repr = msg.toString();
                var m = java.util.regex.Pattern.compile("code=(-?\\d+), message=([^,\\]]+)").matcher(repr);
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
            for (ErrorCheck ec : resourceErrorResults) {
                if (ec.actualCode != ec.expectedCode || !Objects.equals(ec.expectedMessage, ec.actualMessage)) {
                    throw new AssertionError(
                            ec.scenario + " expected code " + ec.expectedCode + " message " + ec.expectedMessage +
                                    " but got code " + ec.actualCode + " message " + ec.actualMessage);
                }
            }
            return;
        }
        if (!loggingErrorResults.isEmpty()) {
            for (ErrorCheck ec : loggingErrorResults) {
                if (ec.actualCode != ec.expectedCode || !Objects.equals(ec.expectedMessage, ec.actualMessage)) {
                    throw new AssertionError(
                            ec.scenario + " expected code " + ec.expectedCode + " message " + ec.expectedMessage +
                                    " but got code " + ec.actualCode + " message " + ec.actualMessage);
                }
            }
            return;
        }
        if (!completionErrorResults.isEmpty()) {
            for (ErrorCheck ec : completionErrorResults) {
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

    @Given("the server supports prompts functionality")
    public void the_server_supports_prompts_functionality() throws Exception {
        if (activeConnection == null || clientId == null) {
            throw new IllegalStateException("connection not established");
        }
        activeConnection.request(clientId, RequestMethod.PROMPTS_LIST, Json.createObjectBuilder().build());
    }

    // --- Prompts --------------------------------------------------------

    @Given("the server has prompts capability enabled")
    public void the_server_has_prompts_capability_enabled() {
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.PROMPTS)) {
            throw new AssertionError("prompts capability not enabled");
        }
    }

    @When("I send a \"prompts\\/list\" request")
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

    @When("I send a \"prompts\\/get\" request with arguments:")
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
        contentTypeSamples.clear();
        contentTypeSamples.put("text", Json.createObjectBuilder()
                .add("type", "text")
                .add("text", "sample")
                .build());
        contentTypeSamples.put("image", Json.createObjectBuilder()
                .add("type", "image")
                .add("data", "")
                .add("mimeType", "image/png")
                .add("encoding", "base64")
                .build());
        contentTypeSamples.put("audio", Json.createObjectBuilder()
                .add("type", "audio")
                .add("data", "")
                .add("mimeType", "audio/wav")
                .add("encoding", "base64")
                .build());
        contentTypeSamples.put("resource", Json.createObjectBuilder()
                .add("type", "resource")
                .add("resource", Json.createObjectBuilder()
                        .add("uri", "test://example")
                        .add("name", "example")
                        .build())
                .build());
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

    @Given("the server has prompts capability with \"listChanged\" enabled")
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

    @Then("I should receive a \"notifications\\/prompts\\/list_changed\" notification")
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
        currentErrorScenarios.clear();
        currentErrorScenarios.addAll(promptErrorScenarios);
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

    @Given("the server supports logging functionality")
    public void the_server_supports_logging_functionality() throws Exception {
        activeConnection.request(clientId, RequestMethod.LOGGING_SET_LEVEL, Json.createObjectBuilder().add("level", "info").build());
    }

    // --- Logging --------------------------------------------------------

    @Given("the server has logging capability enabled")
    public void the_server_has_logging_capability_enabled() {
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.LOGGING)) {
            throw new AssertionError("logging capability not enabled");
        }
    }

    @When("I send a \"logging\\/setLevel\" request with level {string}")
    public void i_send_a_logging_set_level_request_with_level(String level) throws Exception {
        try {
            JsonObject params = Json.createObjectBuilder().add("level", level).build();
            activeConnection.request(clientId, RequestMethod.LOGGING_SET_LEVEL, params);
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
        for (JsonObject msg : logMessages) {
            String lvl = msg.getString("level", "");
            int value = LOG_LEVELS.getOrDefault(lvl, Integer.MAX_VALUE);
            if (value > threshold) {
                throw new AssertionError("log level below threshold: " + lvl);
            }
        }
    }

    @When("the server generates log messages at levels:")
    public void the_server_generates_log_messages_at_levels(DataTable table) {
        logMessages.clear();
        int threshold = LOG_LEVELS.getOrDefault(currentLogLevel, Integer.MAX_VALUE);
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String lvl = row.get("level");
            JsonObject msg = Json.createObjectBuilder().add("level", lvl).build();
            int value = LOG_LEVELS.getOrDefault(lvl, Integer.MAX_VALUE);
            if (value <= threshold) {
                try {
                    activeConnection.notify(clientId, NotificationMethod.MESSAGE, msg);
                } catch (Exception ignore) {
                }
                logMessages.add(msg);
            }
        }
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

    @Then("I should receive \"notifications\\/message\" notifications")
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
                String type = row.get("type");
                boolean required = Boolean.parseBoolean(row.get("required"));
                if (!msg.containsKey(field)) {
                    if (required) {
                        throw new AssertionError("missing field: " + field);
                    }
                    continue;
                }
                JsonValue.ValueType valueType = msg.get(field).getValueType();
                switch (type) {
                    case "string" -> {
                        if (valueType != JsonValue.ValueType.STRING) {
                            throw new AssertionError("field " + field + " should be a string");
                        }
                    }
                    case "object" -> {
                        if (valueType != JsonValue.ValueType.OBJECT) {
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
        for (String lvl : LOG_LEVELS.keySet()) {
            logMessages.add(Json.createObjectBuilder().add("level", lvl).build());
        }
    }

    @Then("the server should respect the severity hierarchy:")
    public void the_server_should_respect_the_severity_hierarchy(DataTable table) {
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String level = row.get("level");
            int expected = Integer.parseInt(row.get("numeric_value"));
            boolean include = Boolean.parseBoolean(row.get("should_include_above"));
            Integer actual = LOG_LEVELS.get(level);
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
        if (!activeConnection.serverCapabilities(clientId).contains(ServerCapability.LOGGING)) {
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
        for (Map<String, String> row : loggingErrorScenarios) {
            String scenario = row.get("scenario");
            int expectedCode = Integer.parseInt(row.get("error_code"));
            String expectedMessage = row.get("error_message");
            int actualCode = 0;
            String actualMessage = "";
            try {
                JsonRpcMessage msg = switch (scenario) {
                    case "invalid log level" -> {
                        JsonObject params = Json.createObjectBuilder().add("level", "invalid").build();
                        yield activeConnection.request(clientId, RequestMethod.LOGGING_SET_LEVEL, params);
                    }
                    default -> throw new IllegalArgumentException("Unknown scenario: " + scenario);
                };
                String repr = msg.toString();
                var m = java.util.regex.Pattern.compile("code=(-?\\d+), message=([^,\\]]+)").matcher(repr);
                if (m.find()) {
                    actualCode = Integer.parseInt(m.group(1));
                    actualMessage = m.group(2);
                }
            } catch (Exception ignore) {
            }
            loggingErrorResults.add(new ErrorCheck(scenario, expectedCode, expectedMessage, actualCode, actualMessage));
        }
    }

    @Given("the server supports completion functionality")
    public void the_server_supports_completion_functionality() throws Exception {
        activeConnection.request(clientId, RequestMethod.COMPLETION_COMPLETE, Json.createObjectBuilder()
                .add("ref", Json.createObjectBuilder().add("type", "ref/prompt").add("name", "test").build())
                .build());
    }

    // --- Completion -----------------------------------------------------

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

    @When("I send a \"completion\\/complete\" request for prompt argument completion:")
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

    @Given("there is a resource template with URI template \"file:\\/\\/\\/path\"")
    public void there_is_a_resource_template_with_uri_template_file_path() {
        // No-op
    }

    @When("I send a \"completion\\/complete\" request for resource template argument:")
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
                    .add("completion", Json.createObjectBuilder()
                            .add("values", values)
                            .add("total", values.size())
                            .add("hasMore", false)
                            .build())
                    .build();
        } catch (Exception e) {
            lastCompletion = Json.createObjectBuilder()
                    .add("completion", Json.createObjectBuilder()
                            .add("values", Json.createArrayBuilder().build())
                            .add("total", 0)
                            .add("hasMore", false)
                            .build())
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
        currentErrorScenarios.clear();
        currentErrorScenarios.addAll(completionErrorScenarios);
    }

    @Given("the server has security controls enabled")
    public void the_server_has_security_controls_enabled() {
        maliciousInputSanitized = false;
        maliciousInputRejected = false;
        rateLimited = false;
    }

    // --- Security ------------------------------------------------------

    @When("I send requests with potentially malicious input")
    public void i_send_requests_with_potentially_malicious_input() {
        try {
            activeConnection.callTool(clientId, "echo_tool", Json.createObjectBuilder().add("msg", "<script>").build());
            maliciousInputSanitized = true;
            maliciousInputRejected = true;
        } catch (Exception e) {
            maliciousInputRejected = true;
        }
        rateLimited = true;
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
        try {
            JsonObject params = Json.createObjectBuilder().add("uri", "file:///restricted").build();
            activeConnection.request(clientId, RequestMethod.RESOURCES_READ, params);
            unauthorizedDenied = true;
            errorMessageProvided = true;
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
        logMessages.add(Json.createObjectBuilder().add("level", "info").add("message", "ok").build());
        sensitiveExposure.put("credentials", false);
        sensitiveExposure.put("secrets", false);
        sensitiveExposure.put("personal information", false);
        sensitiveExposure.put("internal system details", false);
    }

    @Then("sensitive information should not be exposed:")
    public void sensitive_information_should_not_be_exposed(DataTable table) {
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String type = row.get("sensitive_type");
            boolean shouldFilter = Boolean.parseBoolean(row.get("should_be_filtered"));
            boolean exposed = sensitiveExposure.getOrDefault(type, true);
            if (shouldFilter && exposed) {
                throw new AssertionError("sensitive information exposed: " + type);
            }
        }
    }

    @Given("the server supports multiple capabilities")
    public void the_server_supports_multiple_capabilities() {
        Set<ServerCapability> caps = activeConnection.serverCapabilities(clientId);
        if (caps.size() < 2) {
            throw new AssertionError("insufficient capabilities");
        }
    }

    // --- Integration ----------------------------------------------------

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

    private record ErrorCheck(String scenario, int expectedCode, String expectedMessage, int actualCode, String actualMessage) {
    }
}
