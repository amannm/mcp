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
import jakarta.json.JsonObject;

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
