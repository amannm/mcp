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
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.util.ProgressNotification;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public final class McpConformanceSteps {
    private static final String JAVA_BIN = System.getProperty("java.home") +
            File.separator + "bin" + File.separator + "java";

    private Process serverProcess;
    private McpClient client;
    private JsonRpcMessage lastMessage;
    private final List<ProgressNotification> progressEvents = new CopyOnWriteArrayList<>();

    @Before
    public void startServer() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                JAVA_BIN, "-cp", System.getProperty("java.class.path"),
                "com.amannmalik.mcp.Main", "server", "--stdio", "-v"
        );
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

        StdioTransport transport = new StdioTransport(
                serverProcess.getInputStream(),
                serverProcess.getOutputStream()
        );
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
