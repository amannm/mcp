package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.InitializeRequestAbstractEntityCodec;
import com.amannmalik.mcp.codec.InitializeResponseAbstractEntityCodec;
import com.amannmalik.mcp.core.*;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.InitializeRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.lang.System.Logger;

final class ClientHandshake {
    private static final InitializeRequestAbstractEntityCodec REQUEST_CODEC = new InitializeRequestAbstractEntityCodec();
    private static final Logger LOG = System.getLogger(ClientHandshake.class.getName());

    private ClientHandshake() {
    }

    static Result perform(RequestId id,
                          Transport transport,
                          ClientInfo info,
                          Set<ClientCapability> capabilities,
                          boolean rootsListChangedSupported,
                          Duration timeout) throws IOException {
        var init = new InitializeRequest(
                Protocol.LATEST_VERSION,
                new Capabilities(capabilities, Set.of(), Map.of(), Map.of()),
                info,
                new ClientFeatures(rootsListChangedSupported));
        var request = new JsonRpcRequest(id, RequestMethod.INITIALIZE.method(), REQUEST_CODEC.toJson(init));
        transport.send(JsonRpcEndpoint.CODEC.toJson(request));
        JsonRpcMessage msg;
        try {
            msg = JsonRpcEndpoint.CODEC.fromJson(transport.receive(timeout));
        } catch (IOException e) {
            try {
                transport.close();
            } catch (IOException e2) {
                LOG.log(Logger.Level.WARNING, "Failed to close transport after initialization failure", e2);
            }
            throw new IOException("Initialization failed: " + e.getMessage(), e);
        }
        JsonRpcResponse resp;
        try {
            resp = JsonRpc.expectResponse(msg);
        } catch (IOException e) {
            throw new IOException("Initialization failed: " + e.getMessage(), e);
        }
        var ir = new InitializeResponseAbstractEntityCodec().fromJson(resp.result());
        var serverVersion = ir.protocolVersion();
        if (!Protocol.LATEST_VERSION.equals(serverVersion) && !Protocol.PREVIOUS_VERSION.equals(serverVersion)) {
            try {
                transport.close();
            } catch (IOException e2) {
                LOG.log(Logger.Level.WARNING, "Failed to close transport after unsupported protocol", e2);
            }
            throw new UnsupportedProtocolVersionException(serverVersion, Protocol.LATEST_VERSION + " or " + Protocol.PREVIOUS_VERSION);
        }
        transport.setProtocolVersion(serverVersion);
        var features = ir.features();
        var caps = ir.capabilities().server();
        return new Result(
                serverVersion,
                ir.serverInfo(),
                caps,
                Immutable.enumSet(features),
                ir.instructions());
    }

    record Result(String protocolVersion,
                  ServerInfo serverInfo,
                  Set<ServerCapability> capabilities,
                  Set<ServerFeature> features,
                  String instructions) {
    }
}
