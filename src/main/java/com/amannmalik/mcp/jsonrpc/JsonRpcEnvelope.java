package com.amannmalik.mcp.jsonrpc;

import com.amannmalik.mcp.api.RequestId;
import jakarta.json.JsonObject;

import java.util.Objects;
import java.util.Optional;

/**
 * Lightweight classifier for JSON-RPC messages that extracts the request id and method, if present.
 */
public final class JsonRpcEnvelope {
    private final JsonObject message;
    // Avoid Optional as fields; store nullable and expose via Optional accessors
    private final RequestId id; // nullable
    private final String method; // nullable
    private final Type type;

    private JsonRpcEnvelope(JsonObject message) {
        this.message = Objects.requireNonNull(message, "message");
        this.id = RequestId.fromNullable(message.get("id")).orElse(null);
        this.method = message.getString("method", null);
        this.type = classify(message, method != null, id != null);
    }

    public static JsonRpcEnvelope of(JsonObject message) {
        return new JsonRpcEnvelope(message);
    }

    private static Type classify(JsonObject message, boolean hasMethod, boolean hasId) {
        if (hasMethod && hasId) {
            return Type.REQUEST;
        }
        if (hasMethod) {
            return Type.NOTIFICATION;
        }
        if (message.containsKey("result") || message.containsKey("error")) {
            return Type.RESPONSE;
        }
        return Type.INVALID;
    }

    public JsonObject message() {
        return message;
    }

    public Optional<RequestId> id() {
        return Optional.ofNullable(id);
    }

    public Optional<String> method() {
        return Optional.ofNullable(method);
    }

    public Type type() {
        return type;
    }

    public boolean isRequest() {
        return type == Type.REQUEST;
    }

    public boolean isNotification() {
        return type == Type.NOTIFICATION;
    }

    public boolean isResponse() {
        return type == Type.RESPONSE;
    }

    public RequestId requireId() {
        if (id == null) {
            throw new IllegalArgumentException("request id missing");
        }
        return id;
    }

    public enum Type {
        REQUEST,
        NOTIFICATION,
        RESPONSE,
        INVALID
    }
}
