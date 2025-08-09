package com.amannmalik.mcp;

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
    private Process server;
    private BufferedWriter toServer;
    private BufferedReader fromServer;
    private JsonObject capabilities;
    private JsonObject lastResponse;
    private int nextId;

    @Given("a clean MCP environment")
    public void cleanMcpEnvironment() {
        if (server != null) server.destroy();
        capabilities = Json.createObjectBuilder().build();
        nextId = 0;
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
        temp.forEach((k, v) -> caps.add(k, v.build()));
        capabilities = caps.build();
    }

    @When("the client sends an initialize request with:")
    public void theClientSendsAnInitializeRequestWith(DataTable table) throws IOException {
        startServer();
        Map<String, String> params = table.asMap();
        JsonObject clientInfo = Json.createObjectBuilder()
                .add("name", Objects.requireNonNull(params.get("clientInfo.name")))
                .add("version", Objects.requireNonNull(params.get("clientInfo.version")))
                .build();
        JsonObject init = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", ++nextId)
                .add("method", "initialize")
                .add("params", Json.createObjectBuilder()
                        .add("protocolVersion", Objects.requireNonNull(params.get("protocolVersion")))
                        .add("capabilities", capabilities)
                        .add("clientInfo", clientInfo)
                        .build())
                .build();
        toServer.write(init.toString());
        toServer.write('\n');
        toServer.flush();
        String line = fromServer.readLine();
        lastResponse = Json.createReader(new StringReader(line)).readObject();
    }

    @Then("the server should respond with:")
    public void theServerShouldRespondWith(DataTable table) {
        JsonObject result = lastResponse.getJsonObject("result");
        Map<String, String> expected = table.asMap();
        Assertions.assertEquals(expected.get("protocolVersion"), result.getString("protocolVersion"));
        JsonObject serverInfo = result.getJsonObject("serverInfo");
        Assertions.assertEquals(expected.get("serverInfo.name"), serverInfo.getString("name"));
    }

    @Then("the response should include server capabilities")
    public void theResponseShouldIncludeServerCapabilities() {
        JsonObject result = lastResponse.getJsonObject("result");
        JsonObject caps = result.getJsonObject("capabilities");
        Assertions.assertTrue(caps.containsKey("server"));
    }

    @Then("the client should send an initialized notification")
    public void theClientShouldSendAnInitializedNotification() throws IOException {
        JsonObject note = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("method", "notifications/initialized")
                .build();
        toServer.write(note.toString());
        toServer.write('\n');
        toServer.flush();
    }

    @Then("the connection should be in operational state")
    public void theConnectionShouldBeInOperationalState() throws IOException {
        JsonObject ping = Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", ++nextId)
                .add("method", "ping")
                .add("params", Json.createObjectBuilder().build())
                .build();
        toServer.write(ping.toString());
        toServer.write('\n');
        toServer.flush();
        String line = fromServer.readLine();
        JsonObject resp = Json.createReader(new StringReader(line)).readObject();
        Assertions.assertEquals(nextId, resp.getInt("id"));
    }

    @After
    public void tearDown() throws IOException {
        if (toServer != null) toServer.close();
        if (fromServer != null) fromServer.close();
        if (server != null) server.destroy();
    }

    private void startServer() throws IOException {
        if (server != null) server.destroy();
        String classpath = System.getProperty("java.class.path");
        ProcessBuilder builder = new ProcessBuilder(
                "java", "-cp", classpath,
                "com.amannmalik.mcp.Entrypoint", "server", "--stdio", "--test-mode");
        builder.redirectErrorStream(true);
        server = builder.start();
        toServer = new BufferedWriter(new OutputStreamWriter(server.getOutputStream()));
        fromServer = new BufferedReader(new InputStreamReader(server.getInputStream()));
    }
}

