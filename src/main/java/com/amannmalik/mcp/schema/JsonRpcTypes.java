package com.amannmalik.mcp.schema;

import java.util.Objects;

/**
 * JSON-RPC 2.0 base message types.
 */
public final class JsonRpcTypes {
    private JsonRpcTypes() {}

    public static final String JSONRPC_VERSION = "2.0";

    /** All JSON-RPC messages carry the version field. */
    public sealed interface JsonRpcMessage
            permits JsonRpcRequest, JsonRpcNotification, JsonRpcResponse {
        String jsonrpc();
    }

    /** Request identifier can be string or number. */
    public sealed interface RequestId permits StringRequestId, NumberRequestId {
        Object raw();
    }

    public record StringRequestId(String value) implements RequestId {
        public StringRequestId {
            Objects.requireNonNull(value);
        }
        @Override public Object raw() { return value; }
    }

    public record NumberRequestId(long value) implements RequestId {
        @Override public Object raw() { return value; }
    }

    /** Base JSON-RPC request. */
    public sealed interface JsonRpcRequest extends JsonRpcMessage permits com.amannmalik.mcp.schema.BaseProtocol.Request {
        RequestId id();
        String method();
        @Override default String jsonrpc() { return JSONRPC_VERSION; }
    }

    /** Base JSON-RPC notification. */
    public sealed interface JsonRpcNotification extends JsonRpcMessage permits com.amannmalik.mcp.schema.BaseProtocol.Notification {
        String method();
        @Override default String jsonrpc() { return JSONRPC_VERSION; }
    }

    /** Base JSON-RPC response. */
    public sealed interface JsonRpcResponse extends JsonRpcMessage permits com.amannmalik.mcp.schema.BaseProtocol.Result, JsonRpcError {
        RequestId id();
        @Override default String jsonrpc() { return JSONRPC_VERSION; }
    }

    /** JSON-RPC error response. */
    public sealed interface JsonRpcError extends JsonRpcResponse permits BasicError {
        int code();
        String message();
    }

    public record BasicError(RequestId id, int code, String message) implements JsonRpcError {
        public BasicError {
            Objects.requireNonNull(id);
            Objects.requireNonNull(message);
        }
    }
}
