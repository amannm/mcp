package com.amannmalik.mcp.client.sampling;

import com.amannmalik.mcp.client.SimpleMcpClient;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.transport.StdioTransport;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SamplingServerTest {
    private StdioTransport clientTransport;
    private StdioTransport serverTransport;
    private SamplingServer server;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws IOException {
        PipedInputStream clientIn = new PipedInputStream();
        PipedOutputStream serverOut = new PipedOutputStream(clientIn);
        PipedInputStream serverIn = new PipedInputStream();
        PipedOutputStream clientOut = new PipedOutputStream(serverIn);
        clientTransport = new StdioTransport(clientIn, clientOut);
        serverTransport = new StdioTransport(serverIn, serverOut);
        server = new SamplingServer(new EchoProvider(), serverTransport);
        serverThread = new Thread(() -> {
            try {
                server.serve();
            } catch (IOException ignored) {
            }
        });
        serverThread.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        clientTransport.close();
        server.close();
        serverThread.join();
    }

    @Test
    void createMessage() throws Exception {
        SimpleMcpClient client = new SimpleMcpClient(
                new ClientInfo("client", "Client", "1"),
                EnumSet.of(ClientCapability.EXPERIMENTAL),
                clientTransport);
        client.connect();
        CreateMessageRequest req = new CreateMessageRequest(
                List.of(new SamplingMessage(Role.USER, new MessageContent.Text("hi"))),
                null,
                null,
                null
        );
        JsonRpcMessage msg = client.request("sampling/createMessage", SamplingCodec.toJsonObject(req));
        assertTrue(msg instanceof JsonRpcResponse);
        JsonObject result = ((JsonRpcResponse) msg).result();
        CreateMessageResponse resp = SamplingCodec.toCreateMessageResponse(result);
        assertEquals("hi", ((MessageContent.Text) resp.content()).text());
        client.disconnect();
    }

    private static class EchoProvider implements SamplingProvider {
        @Override
        public CreateMessageResponse createMessage(CreateMessageRequest request) {
            SamplingMessage last = request.messages().isEmpty() ?
                    new SamplingMessage(Role.USER, new MessageContent.Text("")) :
                    request.messages().get(request.messages().size() - 1);
            return new CreateMessageResponse(
                    Role.ASSISTANT,
                    new MessageContent.Text(((MessageContent.Text) last.content()).text()),
                    null,
                    "endTurn"
            );
        }
    }
}
