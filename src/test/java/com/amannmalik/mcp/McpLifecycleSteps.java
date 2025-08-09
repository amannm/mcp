package com.amannmalik.mcp;

import com.amannmalik.mcp.core.JsonRpcEndpoint;
import com.amannmalik.mcp.core.McpClient;
import com.amannmalik.mcp.core.McpServer;
import com.amannmalik.mcp.elicitation.ElicitationProvider;
import com.amannmalik.mcp.elicitation.InteractiveElicitationProvider;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.roots.InMemoryRootsProvider;
import com.amannmalik.mcp.roots.Root;
import com.amannmalik.mcp.roots.RootsProvider;
import com.amannmalik.mcp.sampling.InteractiveSamplingProvider;
import com.amannmalik.mcp.sampling.SamplingProvider;
import com.amannmalik.mcp.transport.StdioTransport;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class McpLifecycleSteps {
    private McpServer server;
    private McpClient host;
    private Thread serverThread;
    private StdioTransport clientTransport;
    private ClientInfo clientInfo;
    private final Set<ServerCapability> expectedServerCaps = EnumSet.noneOf(ServerCapability.class);
    private boolean resourcesSubscribe;
    private boolean resourcesListChanged;
    private boolean promptsListChanged;
    private boolean toolsListChanged;
    private final Set<ClientCapability> hostCaps = EnumSet.noneOf(ClientCapability.class);
    private long start;
    private long end;

    @Given("a clean MCP environment")
    public void cleanEnvironment() {
        closeQuietly();
        expectedServerCaps.clear();
        hostCaps.clear();
        resourcesSubscribe = false;
        resourcesListChanged = false;
        promptsListChanged = false;
        toolsListChanged = false;
    }

    @Given("protocol version {string} is supported")
    public void protocolVersionSupported(String version) {
        Assertions.assertTrue(Set.of(Protocol.LATEST_VERSION, Protocol.PREVIOUS_VERSION).contains(version));
    }

    @Given("both McpHost and McpServer are available")
    public void bothAvailable() {
        Assertions.assertNotNull(McpClient.class);
        Assertions.assertNotNull(McpServer.class);
    }

    @Given("a McpServer with capabilities:")
    public void serverWithCapabilities(DataTable table) {
        for (var row : table.asMaps()) {
            if (!Boolean.parseBoolean(row.get("enabled"))) continue;
            var cap = ServerCapability.valueOf(row.get("capability").toUpperCase());
            expectedServerCaps.add(cap);
            var sub = row.get("subcapability");
            if (sub == null) continue;
            switch (cap) {
                case RESOURCES -> {
                    if ("subscribe".equals(sub)) resourcesSubscribe = true;
                    if ("listChanged".equals(sub)) resourcesListChanged = true;
                }
                case TOOLS -> {
                    if ("listChanged".equals(sub)) toolsListChanged = true;
                }
                case PROMPTS -> {
                    if ("listChanged".equals(sub)) promptsListChanged = true;
                }
                default -> {
                }
            }
        }
    }

    @Given("a McpHost with capabilities:")
    public void hostWithCapabilities(DataTable table) {
        for (var row : table.asMaps()) {
            if (!Boolean.parseBoolean(row.get("enabled"))) continue;
            hostCaps.add(ClientCapability.valueOf(row.get("capability").toUpperCase()));
        }
    }

    @When("the McpHost initiates connection to McpServer")
    public void initiateConnection() throws IOException {
        var serverIn = new PipedInputStream();
        var clientOut = new PipedOutputStream(serverIn);
        var clientIn = new PipedInputStream();
        var serverOut = new PipedOutputStream(clientIn);
        var serverTransport = new StdioTransport(serverIn, serverOut);
        clientTransport = new StdioTransport(clientIn, clientOut);
        server = new McpServer(serverTransport, null);
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
    public void sendInitialize(DataTable table) throws IOException {
        Map<String, String> fields = table.asMaps().stream()
                .collect(Collectors.toMap(r -> r.get("field"), r -> r.get("value")));
        Assertions.assertEquals(Protocol.LATEST_VERSION, fields.get("protocolVersion"));
        clientInfo = new ClientInfo(
                fields.get("clientInfo.name"),
                fields.get("clientInfo.title"),
                fields.get("clientInfo.version"));
        SamplingProvider sampling = hostCaps.contains(ClientCapability.SAMPLING)
                ? new InteractiveSamplingProvider(true) : null;
        RootsProvider roots = hostCaps.contains(ClientCapability.ROOTS)
                ? new InMemoryRootsProvider(List.<Root>of()) : null;
        ElicitationProvider elicitation = hostCaps.contains(ClientCapability.ELICITATION)
                ? new InteractiveElicitationProvider() : null;
        host = new McpClient(clientInfo, hostCaps, clientTransport, sampling, roots, elicitation, null);
        start = System.currentTimeMillis();
        host.connect();
        end = System.currentTimeMillis();
    }

    @Then("the McpServer should respond within {int} seconds with:")
    public void serverResponds(int seconds, DataTable table) {
        Assertions.assertTrue(end - start <= seconds * 1_000L);
        Map<String, String> expected = table.asMaps().stream()
                .collect(Collectors.toMap(r -> r.get("field"), r -> r.get("value")));
        Assertions.assertEquals(expected.get("protocolVersion"), getField(host, "protocolVersion"));
        var info = (ServerInfo) getField(host, "serverInfo");
        Assertions.assertEquals(expected.get("serverInfo.name"), info.name());
        Assertions.assertEquals(expected.get("serverInfo.title"), info.title());
        Assertions.assertEquals(expected.get("serverInfo.version"), info.version());
    }

    @Then("the response should include all negotiated server capabilities")
    public void responseIncludesCaps() {
        Assertions.assertEquals(expectedServerCaps, host.serverCapabilities());
        var features = (ServerFeatures) getField(host, "serverFeatures");
        Assertions.assertEquals(resourcesSubscribe, features.resourcesSubscribe());
        Assertions.assertEquals(resourcesListChanged, features.resourcesListChanged());
        Assertions.assertEquals(promptsListChanged, features.promptsListChanged());
        Assertions.assertEquals(toolsListChanged, features.toolsListChanged());
    }

    @Then("the McpHost should send initialized notification")
    public void hostSentInitialized() {
        Assertions.assertEquals(LifecycleState.OPERATION, serverState());
    }

    @Then("both parties should be in operational state")
    public void bothOperational() {
        Assertions.assertEquals(LifecycleState.OPERATION, serverState());
        Assertions.assertTrue(host.connected());
    }

    @Then("no protocol violations should be recorded")
    public void noProtocolViolations() {
        Assertions.assertTrue(pending(server).isEmpty());
        Assertions.assertTrue(pending(host).isEmpty());
    }

    @After
    public void after() {
        closeQuietly();
    }

    private void closeQuietly() {
        try {
            if (host != null) host.close();
        } catch (IOException ignore) {
        }
        try {
            if (server != null) server.close();
        } catch (IOException ignore) {
        }
        try {
            if (serverThread != null) serverThread.join(100);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
        server = null;
        host = null;
        serverThread = null;
        clientTransport = null;
    }

    private static Object getField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LifecycleState serverState() {
        try {
            Method m = McpServer.class.getDeclaredMethod("state");
            m.setAccessible(true);
            return (LifecycleState) m.invoke(server);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<?, ?> pending(JsonRpcEndpoint ep) {
        try {
            Field f = JsonRpcEndpoint.class.getDeclaredField("pending");
            f.setAccessible(true);
            return (Map<?, ?>) f.get(ep);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
