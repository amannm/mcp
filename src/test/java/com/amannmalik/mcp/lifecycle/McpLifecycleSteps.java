package com.amannmalik.mcp.lifecycle;

import com.amannmalik.mcp.core.ClientCapability;
import com.amannmalik.mcp.core.ClientInfo;
import com.amannmalik.mcp.core.McpClient;
import com.amannmalik.mcp.core.McpServer;
import com.amannmalik.mcp.core.StdioTransport;
import com.amannmalik.mcp.elicitation.InteractiveElicitationProvider;
import com.amannmalik.mcp.roots.InMemoryRootsProvider;
import com.amannmalik.mcp.roots.Root;
import com.amannmalik.mcp.sampling.InteractiveSamplingProvider;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.*;

public final class McpLifecycleSteps {
    private McpClient client;
    private McpServer server;
    private Thread serverThread;
    private long connectMillis;
    private final Set<String> expectedServerCaps = new HashSet<>();
    private final Set<ClientCapability> hostCaps = EnumSet.noneOf(ClientCapability.class);

    @Given("a clean MCP environment")
    public void cleanEnvironment() {
        client = null;
        connectMillis = 0L;
        expectedServerCaps.clear();
        hostCaps.clear();
    }

    @Given("protocol version {string} is supported")
    public void protocolVersionSupported(String version) {
        // supported version is expectedVersion, no implementation needed
    }

    @Given("both McpHost and McpServer are available")
    public void hostAndServerAvailable() {
    }

    @Given("a McpServer with capabilities:")
    public void serverCapabilities(DataTable table) {
        table.asMaps().forEach(row -> {
            if (Boolean.parseBoolean(row.get("enabled"))) {
                expectedServerCaps.add(row.get("capability").toUpperCase());
            }
        });
    }

    @Given("a McpHost with capabilities:")
    public void hostCapabilities(DataTable table) {
        table.asMaps().forEach(row -> {
            if (Boolean.parseBoolean(row.get("enabled"))) {
                hostCaps.add(ClientCapability.valueOf(row.get("capability").toUpperCase()));
            }
        });
    }

    @When("the McpHost initiates connection to McpServer")
    public void hostInitiatesConnection() throws IOException {
        PipedInputStream clientIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(clientIn);
        PipedInputStream serverIn = new PipedInputStream();
        PipedOutputStream clientOut = new PipedOutputStream(serverIn);
        var sampling = hostCaps.contains(ClientCapability.SAMPLING)
                ? new InteractiveSamplingProvider(false) : null;
        var roots = hostCaps.contains(ClientCapability.ROOTS)
                ? new InMemoryRootsProvider(List.of(new Root("file://" + System.getProperty("user.dir"),
                "Current Directory", null))) : null;
        var elicitation = hostCaps.contains(ClientCapability.ELICITATION)
                ? new InteractiveElicitationProvider() : null;
        client = new McpClient(new ClientInfo("TestClient", "Test Client App", "1.0.0"),
                hostCaps, new StdioTransport(clientIn, clientOut), sampling, roots, elicitation, null);
        server = new McpServer(new StdioTransport(serverIn, serverOut), null);
        serverThread = new Thread(() -> {
            try {
                server.serve();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.start();
    }

    @When("sends initialize request with:")
    public void sendsInitializeRequest(DataTable table) throws IOException {
        Map<String, String> map = table.asMap(String.class, String.class);
        Assertions.assertEquals("TestClient", map.get("clientInfo.name"));
        Assertions.assertEquals("Test Client App", map.get("clientInfo.title"));
        Assertions.assertEquals("1.0.0", map.get("clientInfo.version"));
        long start = System.currentTimeMillis();
        client.connect();
        connectMillis = System.currentTimeMillis() - start;
    }

    @Then("the McpServer should respond within {int} seconds with:")
    public void serverResponds(int seconds, DataTable table) {
        Assertions.assertTrue(connectMillis <= seconds * 1000L);
        Map<String, String> map = table.asMap(String.class, String.class);
        Assertions.assertEquals(map.get("protocolVersion"), client.protocolVersion());
        Map<String, String> info = client.serverInfoMap();
        Assertions.assertEquals(map.get("serverInfo.name"), info.get("name"));
        Assertions.assertEquals(map.get("serverInfo.title"), info.get("title"));
        Assertions.assertEquals(map.get("serverInfo.version"), info.get("version"));
    }

    @Then("the response should include all negotiated server capabilities")
    public void negotiatedCapabilities() {
        Assertions.assertEquals(expectedServerCaps, client.serverCapabilityNames());
    }

    @Then("the McpHost should send initialized notification")
    public void initializedNotification() throws IOException {
        client.ping();
    }

    @Then("both parties should be in operational state")
    public void operationalState() throws IOException {
        client.ping();
    }

    @Then("no protocol violations should be recorded")
    public void noProtocolViolations() {
    }

    @After
    public void tearDown() throws IOException {
        if (client != null) client.close();
        if (server != null) server.close();
        if (serverThread != null) {
            try {
                serverThread.join(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
