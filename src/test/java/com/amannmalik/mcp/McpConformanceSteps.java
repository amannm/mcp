package com.amannmalik.mcp;

import com.amannmalik.mcp.client.McpClient;
import com.amannmalik.mcp.client.elicitation.BlockingElicitationProvider;
import com.amannmalik.mcp.client.elicitation.ElicitResult;
import com.amannmalik.mcp.client.elicitation.ElicitationAction;
import com.amannmalik.mcp.client.roots.InMemoryRootsProvider;
import com.amannmalik.mcp.client.roots.Root;
import com.amannmalik.mcp.client.sampling.CreateMessageResponse;
import com.amannmalik.mcp.client.sampling.MessageContent;
import com.amannmalik.mcp.client.sampling.SamplingProvider;
import com.amannmalik.mcp.client.sampling.SamplingProviderFactory;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.wire.NotificationMethod;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.server.logging.LoggingMessageNotification;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.transport.StreamableHttpClientTransport;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.util.CancellationCodec;
import com.amannmalik.mcp.util.CancelledNotification;
import com.amannmalik.mcp.util.ProgressNotification;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public final class McpConformanceSteps {
    private static final String JAVA_BIN = System.getProperty("java.home") +
            File.separator + "bin" + File.separator + "java";
    
    private static String getJacocoAgent() {
        String agentJar = System.getProperty("jacoco.agent.jar");
        String execFile = System.getProperty("jacoco.exec.file");
        if (agentJar != null && execFile != null) {
            File execFileObj = new File(execFile);
            File jacocoDir = execFileObj.getParentFile();
            jacocoDir.mkdirs();
            String serverExecFile = new File(jacocoDir, "server-" + System.currentTimeMillis() + ".exec").getAbsolutePath();
            return "-javaagent:" + agentJar + "=destfile=" + serverExecFile + ",append=true";
        }
        return null;
    }

    private Process serverProcess;
    private McpClient client;
    private JsonRpcMessage lastMessage;
    private final List<ProgressNotification> progressEvents = new CopyOnWriteArrayList<>();
    private final List<LoggingMessageNotification> logEvents = new CopyOnWriteArrayList<>();

    @Before("@http")
    public void useHttpTransport() {
        System.setProperty("mcp.test.transport", "http");
    }

    @Before("@stdio")
    public void useStdioTransport() {
        System.setProperty("mcp.test.transport", "stdio");
    }

    @Before(order = 1)
    public void startServer() throws Exception {
        String type = System.getProperty("mcp.test.transport", "stdio");
        Transport transport;
        if ("http".equals(type)) {
            var args = new java.util.ArrayList<String>();
            args.add(JAVA_BIN);
            String jacocoAgent = getJacocoAgent();
            if (jacocoAgent != null) {
                args.add(jacocoAgent);
            }
            args.addAll(List.of("-cp", System.getProperty("java.class.path"),
                    "com.amannmalik.mcp.Main", "server", "--http", "0", "-v"));
            ProcessBuilder pb = new ProcessBuilder(args);
            serverProcess = pb.start();
            var err = new BufferedReader(new InputStreamReader(
                    serverProcess.getErrorStream(), StandardCharsets.UTF_8));
            String line;
            int port = -1;
            long end = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < end && (line = err.readLine()) != null) {
                if (line.startsWith("Listening on http://127.0.0.1:")) {
                    port = Integer.parseInt(line.substring(line.lastIndexOf(':') + 1));
                    break;
                }
            }
            assertTrue(port > 0, "server failed to start");
            Thread logThread = new Thread(() -> {
                try {
                    while (err.readLine() != null) {
                    }
                } catch (IOException ignore) {
                }
            });
            logThread.setDaemon(true);
            logThread.start();
            transport = new StreamableHttpClientTransport(URI.create("http://127.0.0.1:" + port + "/"));
        } else {
            var args = new java.util.ArrayList<String>();
            args.add(JAVA_BIN);
            String jacocoAgent = getJacocoAgent();
            if (jacocoAgent != null) {
                args.add(jacocoAgent);
            }
            args.addAll(List.of("-cp", System.getProperty("java.class.path"),
                    "com.amannmalik.mcp.Main", "server", "--stdio", "-v"));
            ProcessBuilder pb = new ProcessBuilder(args);
            serverProcess = pb.start();
            long end = System.currentTimeMillis() + 500;
            boolean started = false;
            while (System.currentTimeMillis() < end) {
                if (serverProcess.isAlive()) {
                    started = true;
                    break;
                }
                Thread.sleep(50);
            }
            assertTrue(started, "server failed to start");

            transport = new StdioTransport(
                    serverProcess.getInputStream(),
                    serverProcess.getOutputStream()
            );
        }
        BlockingElicitationProvider elicitation = new BlockingElicitationProvider();
        elicitation.respond(new ElicitResult(ElicitationAction.CANCEL, null, null));
        SamplingProvider sampling = SamplingProviderFactory.createMock(new CreateMessageResponse(
                Role.ASSISTANT,
                new MessageContent.Text("ok", null, null),
                "mock-model",
                "endTurn",
                null
        ));
        InMemoryRootsProvider rootsProvider = new InMemoryRootsProvider(
                List.of(new Root("file:///tmp", "Test Root", null))
        );
        client = new McpClient(
                new ClientInfo("test-client", "Test Client", "1.0"),
                EnumSet.allOf(ClientCapability.class),
                transport,
                sampling,
                rootsProvider,
                elicitation
        );
        client.setProgressListener(progressEvents::add);
        client.setLoggingListener(logEvents::add);
        client.connect();
    }

    @After
    public void stopServer() throws Exception {
        if (client != null) {
            client.disconnect();
        }
        if (serverProcess != null && serverProcess.isAlive()) {
            serverProcess.destroyForcibly();
            serverProcess.waitFor(2, TimeUnit.SECONDS);
        }
    }

    @Given("a running MCP server and connected client")
    public void dummy() {
        // server and client started in @Before
    }

    @Then("the server capabilities should be advertised")
    public void checkCapabilities() {
        var expected = EnumSet.of(
                ServerCapability.RESOURCES,
                ServerCapability.TOOLS,
                ServerCapability.PROMPTS,
                ServerCapability.LOGGING,
                ServerCapability.COMPLETIONS
        );
        assertEquals(expected, client.serverCapabilities());
    }

    @When("the client pings the server")
    public void ping() {
        assertDoesNotThrow(() -> client.ping());
    }

    @Then("the ping succeeds")
    public void pingSucceeds() {
        // already asserted in ping()
    }

    @When("the client lists resources")
    public void listResources() throws Exception {
        JsonObject meta = Json.createObjectBuilder()
                .add("progressToken", "tok")
                .build();
        JsonObject params = Json.createObjectBuilder()
                .add("_meta", meta)
                .build();
        lastMessage = client.request("resources/list", params);
    }

    @Then("one resource uri should be {string}")
    public void verifyList(String uri) throws Exception {
        assertInstanceOf(JsonRpcResponse.class, lastMessage);
        var list = ((JsonRpcResponse) lastMessage).result().getJsonArray("resources");
        assertEquals(1, list.size());
        assertEquals(uri, list.getJsonObject(0).getString("uri"));
        Thread.sleep(100);
        assertEquals(2, progressEvents.size());
    }

    @When("the client reads {string}")
    public void readResource(String uri) throws Exception {
        lastMessage = client.request("resources/read", Json.createObjectBuilder()
                .add("uri", uri)
                .build());
    }

    @Then("the resource text should be {string}")
    public void verifyRead(String text) {
        assertInstanceOf(JsonRpcResponse.class, lastMessage);
        var block = ((JsonRpcResponse) lastMessage).result()
                .getJsonArray("contents").getJsonObject(0);
        assertEquals(text, block.getString("text"));
    }

    @When("the client lists resource templates")
    public void listResourceTemplates() throws Exception {
        lastMessage = client.request("resources/templates/list", Json.createObjectBuilder().build());
    }

    @Then("one resource template is returned")
    public void verifyTemplateCount() {
        assertInstanceOf(JsonRpcResponse.class, lastMessage);
        var templates = ((JsonRpcResponse) lastMessage).result().getJsonArray("resourceTemplates");
        assertEquals(1, templates.size());
    }

    @When("the client lists tools")
    public void listTools() throws Exception {
        lastMessage = client.request("tools/list", Json.createObjectBuilder().build());
    }

    @Then("one tool named {string} is returned")
    public void verifyToolList(String name) {
        assertInstanceOf(JsonRpcResponse.class, lastMessage);
        var tools = ((JsonRpcResponse) lastMessage).result().getJsonArray("tools");
        assertEquals(1, tools.size());
        assertEquals(name, tools.getJsonObject(0).getString("name"));
    }

    @When("the client calls {string}")
    public void callTool(String name) throws Exception {
        lastMessage = client.request("tools/call", Json.createObjectBuilder()
                .add("name", name)
                .build());
    }

    @Then("the response text should be {string}")
    public void verifyToolCall(String text) {
        assertInstanceOf(JsonRpcResponse.class, lastMessage);
        var content = ((JsonRpcResponse) lastMessage).result()
                .getJsonArray("content").getJsonObject(0);
        assertEquals(text, content.getString("text"));
    }

    @When("the client lists prompts")
    public void listPrompts() throws Exception {
        lastMessage = client.request("prompts/list", Json.createObjectBuilder().build());
    }

    @Then("one prompt is returned")
    public void verifyPromptList() {
        assertInstanceOf(JsonRpcResponse.class, lastMessage);
        var prompts = ((JsonRpcResponse) lastMessage).result().getJsonArray("prompts");
        assertEquals(1, prompts.size());
    }

    @When("the client gets prompt {string}")
    public void getPrompt(String name) throws Exception {
        lastMessage = client.request("prompts/get", Json.createObjectBuilder()
                .add("name", name)
                .add("arguments", Json.createObjectBuilder().add("test_arg", "v").build())
                .build());
    }

    @Then("the first message text should be {string}")
    public void verifyPrompt(String text) {
        assertInstanceOf(JsonRpcResponse.class, lastMessage);
        var messages = ((JsonRpcResponse) lastMessage).result().getJsonArray("messages");
        assertEquals(text, messages.getJsonObject(0).getJsonObject("content").getString("text"));
    }

    @When("the client requests a completion")
    public void requestCompletion() throws Exception {
        lastMessage = client.request("completion/complete", Json.createObjectBuilder()
                .add("ref", Json.createObjectBuilder()
                        .add("type", "ref/prompt")
                        .add("name", "test_prompt")
                        .build())
                .add("argument", Json.createObjectBuilder()
                        .add("name", "test_arg")
                        .add("value", "")
                        .build())
                .build());
    }

    @Then("the completion value should be {string}")
    public void verifyCompletion(String value) {
        assertInstanceOf(JsonRpcResponse.class, lastMessage);
        var values = ((JsonRpcResponse) lastMessage).result()
                .getJsonObject("completion")
                .getJsonArray("values");
        assertEquals(List.of(value),
                values.getValuesAs(jakarta.json.JsonString.class)
                        .stream()
                        .map(jakarta.json.JsonString::getString)
                        .toList());
    }

    @When("the client sets the log level to {string}")
    public void setLogLevel(String level) throws Exception {
        lastMessage = client.request("logging/setLevel", Json.createObjectBuilder().add("level", level).build());
    }

    @Then("the call succeeds")
    public void callSucceeds() {
        assertInstanceOf(JsonRpcResponse.class, lastMessage);
    }

    @When("the client reads an invalid uri")
    public void readInvalid() throws Exception {
        lastMessage = client.request("resources/read", Json.createObjectBuilder().add("uri", "bad://uri").build());
    }

    @Then("an error with code {int} is returned")
    public void verifyError(int code) {
        assertInstanceOf(JsonRpcError.class, lastMessage);
        assertEquals(code, ((JsonRpcError) lastMessage).error().code());
    }

    @When("the client calls an unknown tool")
    public void callUnknownTool() throws Exception {
        lastMessage = client.request("tools/call", Json.createObjectBuilder().add("name", "nope").build());
    }

    @When("the client calls an unknown method")
    public void callUnknownMethod() throws Exception {
        lastMessage = client.request("bogus/method", Json.createObjectBuilder().build());
    }

    @When("the client subscribes to {string}")
    public void subscribeResource(String uri) throws Exception {
        lastMessage = client.request("resources/subscribe", Json.createObjectBuilder().add("uri", uri).build());
    }

    @When("the client unsubscribes from {string}")
    public void unsubscribeResource(String uri) throws Exception {
        lastMessage = client.request("resources/unsubscribe", Json.createObjectBuilder().add("uri", uri).build());
    }

    @When("the client requests an invalid completion")
    public void requestInvalidCompletion() throws Exception {
        lastMessage = client.request("completion/complete", Json.createObjectBuilder()
                .add("ref", Json.createObjectBuilder()
                        .add("type", "ref/prompt")
                        .add("name", "bad")
                        .build())
                .add("argument", Json.createObjectBuilder()
                        .add("name", "test_arg")
                        .add("value", "")
                        .build())
                .build());
    }

    @When("the client sends a cancellation notification")
    public void sendCancellation() throws Exception {
        CancelledNotification note = new CancelledNotification(new RequestId.NumericId(999), "test");
        client.notify(NotificationMethod.CANCELLED.method(), CancellationCodec.toJsonObject(note));
    }

    @When("the client lists resources with an invalid progress token")
    public void listResourcesInvalidProgress() throws Exception {
        JsonObject meta = Json.createObjectBuilder()
                .add("progressToken", Json.createObjectBuilder().build())
                .build();
        JsonObject params = Json.createObjectBuilder()
                .add("_meta", meta)
                .build();
        lastMessage = client.request("resources/list", params);
    }

    @When("the client lists resources with cursor {string}")
    public void listResourcesWithCursor(String cursor) throws Exception {
        JsonObject params = Json.createObjectBuilder()
                .add("cursor", cursor)
                .build();
        lastMessage = client.request("resources/list", params);
    }

    @When("the client lists prompts with cursor {string}")
    public void listPromptsWithCursor(String cursor) throws Exception {
        JsonObject params = Json.createObjectBuilder()
                .add("cursor", cursor)
                .build();
        lastMessage = client.request("prompts/list", params);
    }

    @When("the client lists tools with cursor {string}")
    public void listToolsWithCursor(String cursor) throws Exception {
        JsonObject params = Json.createObjectBuilder()
                .add("cursor", cursor)
                .build();
        lastMessage = client.request("tools/list", params);
    }

    @When("the client gets prompt {string} without arguments")
    public void getPromptWithoutArgs(String name) throws Exception {
        lastMessage = client.request("prompts/get", Json.createObjectBuilder()
                .add("name", name)
                .build());
    }

    @Then("a log message with level {string} is received")
    public void verifyLogMessage(String level) throws Exception {
        long end = System.currentTimeMillis() + 500;
        while (System.currentTimeMillis() < end &&
                logEvents.stream().noneMatch(l -> l.level().name().equalsIgnoreCase(level))) {
            Thread.sleep(10);
        }
        assertTrue(logEvents.stream().anyMatch(l -> l.level().name().equalsIgnoreCase(level)));
    }

    @When("the client disconnects")
    public void disconnect() throws Exception {
        client.disconnect();
    }

    @Then("the server process terminates")
    public void serverTerminates() throws Exception {
        if (serverProcess.isAlive()) {
            serverProcess.waitFor(2, TimeUnit.SECONDS);
        }
        assertFalse(serverProcess.isAlive());
    }
}
