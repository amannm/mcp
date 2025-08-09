package com.amannmalik.mcp;

import com.amannmalik.mcp.core.McpHost;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import io.cucumber.java.en.*;
import io.cucumber.java.After;
import io.cucumber.datatable.DataTable;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.junit.jupiter.api.Assertions;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class McpLifecycleSteps {
    private McpHost host;
    private JsonObject capabilities;
    private JsonRpcMessage lastResponse;
    private final String clientId = "test-server";

    @Given("a clean MCP environment")
    public void cleanMcpEnvironment() throws IOException {
        if (host != null) host.close();
        capabilities = Json.createObjectBuilder().build();
    }

    @Given("protocol version {string} is supported")
    public void protocolVersionSupported(String version) {
        Objects.requireNonNull(version);
    }

    @Given("a client with capabilities:")
    public void aClientWithCapabilities(DataTable table) {
        Map<String, JsonObjectBuilder> temp = new LinkedHashMap<>();
        for (Map<String, String> row : table.asMaps()) {
            String capability = Objects.requireNonNull(row.get("capability"));
            String sub = row.get("subcapability");
            String value = row.get("value");
            JsonObjectBuilder capBuilder = temp.computeIfAbsent(capability, k -> Json.createObjectBuilder());
            if (sub != null && !sub.isBlank()) {
                capBuilder.add(sub, Boolean.parseBoolean(value));
            }
        }
        JsonObjectBuilder caps = Json.createObjectBuilder();
        temp.forEach((key, builder) -> caps.add(key, builder.build()));
        capabilities = caps.build();
    }

    @When("the client sends an initialize request with:")
    public void theClientSendsAnInitializeRequestWith(DataTable table) throws IOException {
        startHost();
        Map<String, String> params = table.asMap();
        JsonObject clientInfo = Json.createObjectBuilder()
                .add("name", Objects.requireNonNull(params.get("clientInfo.name")))
                .add("version", Objects.requireNonNull(params.get("clientInfo.version")))
                .build();
        JsonObject initParams = Json.createObjectBuilder()
                .add("protocolVersion", Objects.requireNonNull(params.get("protocolVersion")))
                .add("capabilities", capabilities)
                .add("clientInfo", clientInfo)
                .build();
        
        lastResponse = host.request(clientId, "initialize", initParams);
    }

    @Then("the server should respond with:")
    public void theServerShouldRespondWith(DataTable table) {
        if (lastResponse instanceof JsonRpcResponse response) {
            JsonObject result = response.result();
            Map<String, String> expected = table.asMap();
            Assertions.assertEquals(expected.get("protocolVersion"), result.getString("protocolVersion"));
            JsonObject serverInfo = result.getJsonObject("serverInfo");
            Assertions.assertEquals(expected.get("serverInfo.name"), serverInfo.getString("name"));
        } else {
            Assertions.fail("Expected successful response, got: " + lastResponse);
        }
    }

    @Then("the response should include server capabilities")
    public void theResponseShouldIncludeServerCapabilities() {
        if (lastResponse instanceof JsonRpcResponse response) {
            JsonObject result = response.result();
            JsonObject caps = result.getJsonObject("capabilities");
            Assertions.assertTrue(caps.containsKey("server"));
        } else {
            Assertions.fail("Expected successful response, got: " + lastResponse);
        }
    }

    @Then("the client should send an initialized notification")
    public void theClientShouldSendAnInitializedNotification() throws IOException {
        host.notify(clientId, "notifications/initialized", Json.createObjectBuilder().build());
    }

    @Then("the connection should be in operational state")
    public void theConnectionShouldBeInOperationalState() throws IOException {
        JsonRpcMessage resp = host.request(clientId, "ping", Json.createObjectBuilder().build());
        if (resp instanceof JsonRpcResponse response) {
            Assertions.assertNotNull(response.result());
        } else {
            Assertions.fail("Expected successful ping response, got: " + resp);
        }
    }

    @After
    public void tearDown() throws IOException {
        if (host != null) host.close();
    }

    private void startHost() throws IOException {
        if (host != null) host.close();
        String classpath = System.getProperty("java.class.path");
        String serverCommand = "java -cp " + classpath + " com.amannmalik.mcp.Entrypoint server --stdio --test-mode";
        Map<String, String> clientSpecs = Map.of(clientId, serverCommand);
        host = McpHost.forCli(clientSpecs, false);
        
        // Grant necessary consents for testing
        host.grantConsent(clientId);
    }
}

