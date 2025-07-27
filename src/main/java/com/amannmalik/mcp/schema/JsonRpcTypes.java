package com.amannmalik.mcp.schema;

import java.util.Objects;
import java.util.Optional;

/** JSON-RPC 2.0 base message types. */
public final class JsonRpcTypes {
    private JsonRpcTypes() {}

    public static final String JSONRPC_VERSION = "2.0";

    /** Top level marker for all JSON-RPC messages. */
    public sealed interface JsonRpcMessage
            permits JsonRpcRequest, JsonRpcNotification, JsonRpcResponse, JsonRpcError {
        String jsonrpc();
    }

    /** Identifier for requests, either a string or number. */
    public sealed interface RequestId permits StringRequestId, NumberRequestId {}

    /** String based request id. */
    public record StringRequestId(String value) implements RequestId {
        public StringRequestId {
            Objects.requireNonNull(value);
        }
        @Override public String toString() { return value; }
    }

    /** Numeric request id. */
    public record NumberRequestId(long value) implements RequestId {
        @Override public String toString() { return Long.toString(value); }
    }

    /** Client request message. */
    public record JsonRpcRequest(RequestId id, String method, Optional<Object> params)
            implements JsonRpcMessage {
        public JsonRpcRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(method);
            Objects.requireNonNull(params);
        }
        @Override public String jsonrpc() { return JSONRPC_VERSION; }
    }

    /** Notification message with no id. */
    public record JsonRpcNotification(String method, Optional<Object> params)
            implements JsonRpcMessage {
        public JsonRpcNotification {
            Objects.requireNonNull(method);
            Objects.requireNonNull(params);
        }
        @Override public String jsonrpc() { return JSONRPC_VERSION; }
    }

    /** Successful response. */
    public record JsonRpcResponse(RequestId id, Optional<Object> result)
            implements JsonRpcMessage {
        public JsonRpcResponse {
            Objects.requireNonNull(id);
            Objects.requireNonNull(result);
        }
        @Override public String jsonrpc() { return JSONRPC_VERSION; }
    }

    /** Error response. */
    public record JsonRpcError(RequestId id, int code, String message, Optional<Object> data)
            implements JsonRpcMessage {
        public JsonRpcError {
            Objects.requireNonNull(id);
            Objects.requireNonNull(message);
            Objects.requireNonNull(data);
        }
        @Override public String jsonrpc() { return JSONRPC_VERSION; }
    }
}
