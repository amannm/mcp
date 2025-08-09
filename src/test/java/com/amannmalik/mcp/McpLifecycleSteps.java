package com.amannmalik.mcp;

import com.amannmalik.mcp.auth.AuthorizationManager;
import com.amannmalik.mcp.auth.AuthorizationStrategy;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.core.McpHost;
import com.amannmalik.mcp.core.McpServer;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.protocol.NotificationMethod;
import com.amannmalik.mcp.protocol.RequestMethod;
import com.amannmalik.mcp.transport.StreamableHttpClientTransport;
import com.amannmalik.mcp.transport.StreamableHttpServerTransport;
import com.amannmalik.mcp.util.CloseUtil;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
import jakarta.json.Json;

import java.io.IOException;
import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public final class McpLifecycleSteps {
    private StreamableHttpServerTransport serverTransport;
    private McpServer server;
    private Thread serverThread;
    private McpHost host;
    private StreamableHttpClientTransport clientTransport;
    private InitializeResponse initializeResponse;
    private long responseTimeMs;
    private boolean initializedSent;
    private boolean pingSuccessful;
    private Set<ServerCapability> expectedServerCaps = Set.of();
    private ServerFeatures expectedServerFeatures = new ServerFeatures(false, false, false, false);
    private Set<ClientCapability> expectedClientCaps = Set.of();
    private boolean rootsListChanged;

    @Given("a clean MCP environment")
    public void cleanEnvironment() throws Exception {
        CloseUtil.closeQuietly(clientTransport);
        CloseUtil.closeQuietly(server);
        CloseUtil.closeQuietly(host);
        if (serverThread != null) serverThread.join(1000);
        clientTransport = null;
        server = null;
        host = null;
        serverThread = null;
        initializeResponse = null;
        initializedSent = false;
        pingSuccessful = false;
        expectedServerCaps = Set.of();
        expectedServerFeatures = new ServerFeatures(false, false, false, false);
        expectedClientCaps = Set.of();
        rootsListChanged = false;
    }

    @Given("protocol version {string} is supported")
    public void protocolVersionSupported(String version) {
        assertEquals(version, Protocol.LATEST_VERSION);
    }

    @Given("both McpHost and McpServer are available")
    public void hostAndServerAvailable() throws Exception {
        AuthorizationStrategy strategy = h -> Optional.of(new Principal("test", Set.of()));
        AuthorizationManager auth = new AuthorizationManager(List.of(strategy));
        serverTransport = new StreamableHttpServerTransport(0, Set.of("http://127.0.0.1"), auth, "", List.of());
        server = new McpServer(serverTransport, "");
        serverThread = new Thread(() -> {
            try {
                server.serve();
            } catch (IOException ignore) {
            }
        });
        serverThread.start();
        host = new McpHost(c -> true, new Principal("host", Set.of()));
    }

    @Given("a McpServer with capabilities:")
    public void serverWithCapabilities(DataTable table) {
        EnumSet<ServerCapability> caps = EnumSet.noneOf(ServerCapability.class);
        boolean resourcesSubscribe = false;
        boolean resourcesListChanged = false;
        boolean toolsListChanged = false;
        boolean promptsListChanged = false;
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String cap = row.get("capability");
            String sub = row.get("subcapability");
            if (!Boolean.parseBoolean(row.get("enabled"))) continue;
            switch (cap) {
                case "resources" -> {
                    caps.add(ServerCapability.RESOURCES);
                    if ("subscribe".equals(sub)) resourcesSubscribe = true;
                    if ("listChanged".equals(sub)) resourcesListChanged = true;
                }
                case "tools" -> {
                    caps.add(ServerCapability.TOOLS);
                    if ("listChanged".equals(sub)) toolsListChanged = true;
                }
                case "prompts" -> {
                    caps.add(ServerCapability.PROMPTS);
                    if ("listChanged".equals(sub)) promptsListChanged = true;
                }
                case "logging" -> caps.add(ServerCapability.LOGGING);
                case "completions" -> caps.add(ServerCapability.COMPLETIONS);
                default -> {
                }
            }
        }
        expectedServerCaps = caps;
        expectedServerFeatures = new ServerFeatures(resourcesSubscribe, resourcesListChanged, toolsListChanged, promptsListChanged);
    }

    @Given("a McpHost with capabilities:")
    public void hostWithCapabilities(DataTable table) {
        EnumSet<ClientCapability> caps = EnumSet.noneOf(ClientCapability.class);
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String cap = row.get("capability");
            String sub = row.get("subcapability");
            if (!Boolean.parseBoolean(row.get("enabled"))) continue;
            switch (cap) {
                case "roots" -> {
                    caps.add(ClientCapability.ROOTS);
                    if ("listChanged".equals(sub)) rootsListChanged = true;
                }
                case "sampling" -> caps.add(ClientCapability.SAMPLING);
                case "elicitation" -> caps.add(ClientCapability.ELICITATION);
                default -> {
                }
            }
        }
        expectedClientCaps = caps;
    }

    @When("the McpHost initiates connection to McpServer")
    public void hostInitiatesConnection() {
        clientTransport = new StreamableHttpClientTransport(URI.create("http://127.0.0.1:" + serverTransport.port()));
    }

    @When("sends initialize request with:")
    public void sendInitializeRequest(DataTable table) throws IOException {
        Map<String, String> row = table.asMap(String.class, String.class);
        String protocolVersion = row.get("protocolVersion");
        ClientInfo info = new ClientInfo(
                row.get("clientInfo.name"),
                row.get("clientInfo.title"),
                row.get("clientInfo.version"));
        Capabilities caps = new Capabilities(expectedClientCaps, Set.of(), Map.of(), Map.of());
        ClientFeatures features = new ClientFeatures(rootsListChanged);
        InitializeRequest init = new InitializeRequest(protocolVersion, caps, info, features);
        JsonRpcRequest req = new JsonRpcRequest(new RequestId.NumericId(1), RequestMethod.INITIALIZE.method(), InitializeRequest.CODEC.toJson(init));
        long start = System.currentTimeMillis();
        clientTransport.send(JsonRpcCodec.CODEC.toJson(req));
        JsonRpcMessage msg = JsonRpcCodec.CODEC.fromJson(clientTransport.receive());
        responseTimeMs = System.currentTimeMillis() - start;
        if (msg instanceof JsonRpcResponse resp) {
            initializeResponse = InitializeResponse.CODEC.fromJson(resp.result());
        } else if (msg instanceof JsonRpcError err) {
            throw new AssertionError(err.error().message());
        } else {
            throw new AssertionError("Unexpected response type");
        }
        JsonRpcNotification note = new JsonRpcNotification(NotificationMethod.INITIALIZED.method(), Json.createObjectBuilder().build());
        clientTransport.send(JsonRpcCodec.CODEC.toJson(note));
        initializedSent = true;
    }

    @Then("the McpServer should respond within {int} seconds with:")
    public void serverResponds(int seconds, DataTable table) {
        assertTrue(responseTimeMs < seconds * 1000L);
        Map<String, String> row = table.asMap(String.class, String.class);
        assertEquals(row.get("protocolVersion"), initializeResponse.protocolVersion());
        ServerInfo info = initializeResponse.serverInfo();
        assertEquals(row.get("serverInfo.name"), info.name());
        assertEquals(row.get("serverInfo.title"), info.title());
        assertEquals(row.get("serverInfo.version"), info.version());
    }

    @Then("the response should include all negotiated server capabilities")
    public void negotiatedCapabilities() {
        assertEquals(expectedServerCaps, initializeResponse.capabilities().server());
        assertEquals(expectedServerFeatures, initializeResponse.features());
    }

    @Then("the McpHost should send initialized notification")
    public void initializedNotification() {
        assertTrue(initializedSent);
    }

    @Then("both parties should be in operational state")
    public void operationalState() throws IOException {
        JsonRpcRequest ping = new JsonRpcRequest(new RequestId.NumericId(2), RequestMethod.PING.method(), Json.createObjectBuilder().build());
        clientTransport.send(JsonRpcCodec.CODEC.toJson(ping));
        JsonRpcMessage msg = JsonRpcCodec.CODEC.fromJson(clientTransport.receive());
        pingSuccessful = msg instanceof JsonRpcResponse;
        assertTrue(pingSuccessful);
    }

    @Then("no protocol violations should be recorded")
    public void noProtocolViolations() {
        assertNotNull(initializeResponse);
        assertTrue(pingSuccessful);
    }

    @After
    public void tearDown() throws Exception {
        cleanEnvironment();
    }
}
