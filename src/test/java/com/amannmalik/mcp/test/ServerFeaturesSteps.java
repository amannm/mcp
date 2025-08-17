package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.spi.Cursor;
import com.amannmalik.mcp.spi.ListToolsResult;
import com.amannmalik.mcp.spi.Tool;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
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
