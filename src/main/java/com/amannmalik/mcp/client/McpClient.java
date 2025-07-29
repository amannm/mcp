package com.amannmalik.mcp.client;

import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import jakarta.json.JsonObject;

import java.io.IOException;


public interface McpClient extends AutoCloseable {
    ClientInfo info();

    void connect() throws IOException;

    void disconnect() throws IOException;

    boolean connected();


    String context();

    JsonRpcMessage request(String method, JsonObject params) throws IOException;

    void notify(String method, JsonObject params) throws IOException;

    @Override
    default void close() throws IOException {
        disconnect();
    }
}
