package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.jsonrpc.JsonRpcCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.transport.StreamableHttpTransport;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResourceServerHttpIntegrationTest {
    private StreamableHttpTransport transport;
    private ResourceServer server;
    private Thread serverThread;

    @BeforeEach
    void setUp() throws Exception {
        Resource r = new Resource(
                "mem:///one.txt", "one.txt", null, null, "text/plain", null,
                new ResourceAnnotations(Set.of(Audience.USER), null, Instant.now()));
        InMemoryResourceProvider p = new InMemoryResourceProvider(
                List.of(r),
                Map.of(r.uri(), new ResourceBlock.Text(r.uri(), r.name(), null, r.mimeType(), "1", r.annotations())),
                List.of());
        transport = new StreamableHttpTransport();
        server = new ResourceServer(p, transport);
        serverThread = new Thread(() -> { try { server.serve(); } catch (Exception ignored) {} });
        serverThread.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        transport.close();
        server.close();
        serverThread.interrupt();
        serverThread.join();
    }

    @Test
    void listResourcesOverHttp() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        JsonObject initParams = Json.createObjectBuilder()
                .add("protocolVersion", "2025-06-18")
                .add("capabilities", Json.createObjectBuilder()
                        .add("client", Json.createObjectBuilder())
                        .add("server", Json.createObjectBuilder())
                        .build())
                .add("clientInfo", Json.createObjectBuilder()
                        .add("name", "c")
                        .add("title", "C")
                        .add("version", "1")
                        .build())
                .build();
        JsonRpcRequest initReq = new JsonRpcRequest(new RequestId.NumericId(1), "initialize", initParams);
        HttpRequest httpInit = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + transport.port() + "/"))
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JsonRpcCodec.toJsonObject(initReq).toString()))
                .build();
        HttpResponse<String> initResp = client.send(httpInit, HttpResponse.BodyHandlers.ofString());
        String session = initResp.headers().firstValue("Mcp-Session-Id").orElseThrow();
        JsonRpcResponse init = (JsonRpcResponse) JsonRpcCodec.fromJsonObject(Json.createReader(new java.io.StringReader(initResp.body())).readObject());
        assertEquals(new RequestId.NumericId(1), init.id());

        JsonObject listParams = Json.createObjectBuilder().build();
        JsonRpcRequest listReq = new JsonRpcRequest(new RequestId.NumericId(2), "resources/list", listParams);
        HttpRequest httpList = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + transport.port() + "/"))
                .header("Accept", "application/json")
                .header("Mcp-Session-Id", session)
                .POST(HttpRequest.BodyPublishers.ofString(JsonRpcCodec.toJsonObject(listReq).toString()))
                .build();
        HttpResponse<String> listResp = client.send(httpList, HttpResponse.BodyHandlers.ofString());
        JsonRpcResponse list = (JsonRpcResponse) JsonRpcCodec.fromJsonObject(Json.createReader(new java.io.StringReader(listResp.body())).readObject());
        JsonObject result = list.result();
        assertEquals(1, result.getJsonArray("resources").size());

        HttpRequest endReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + transport.port() + "/"))
                .header("Mcp-Session-Id", session)
                .DELETE()
                .build();
        client.send(endReq, HttpResponse.BodyHandlers.discarding());
    }
}
