package com.amannmalik.mcp.schema;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * MCP base protocol interfaces building on JSON-RPC types.
 */
public final class BaseProtocol {
    private BaseProtocol() {}

    /** Common metadata holder. */
    public sealed interface WithMeta {
        Optional<Map<String, Object>> _meta();
    }

    /** Base request type. */
    public sealed interface Request extends JsonRpcTypes.JsonRpcRequest, WithMeta
            permits StubRequest, BaseOperations.PingRequest,
                    Initialization.InitializeRequest,
                    Resources.ListResourcesRequest,
                    Resources.ReadResourceRequest,
                    Resources.SubscribeResourcesRequest,
                    Resources.ListResourceTemplatesRequest {
    }

    /** Base result type. */
    public sealed interface Result extends JsonRpcTypes.JsonRpcResponse, WithMeta
            permits EmptyResult, Initialization.InitializeResult,
                    Resources.ListResourcesResult,
                    Resources.ReadResourceResult,
                    Resources.ListResourceTemplatesResult {
    }

    /** Base notification type. */
    public sealed interface Notification extends JsonRpcTypes.JsonRpcNotification, WithMeta
            permits StubNotification, BaseOperations.ProgressNotification,
                    BaseOperations.CancelledNotification,
                    Initialization.InitializedNotification,
                    Resources.ResourcesChangedNotification {
    }

    /** Progress token can be string or number. */
    public sealed interface ProgressToken permits StringProgressToken, NumberProgressToken {
        Object raw();
    }

    public record StringProgressToken(String value) implements ProgressToken {
        public StringProgressToken {
            Objects.requireNonNull(value);
        }
        @Override public Object raw() { return value; }
    }

    public record NumberProgressToken(long value) implements ProgressToken {
        @Override public Object raw() { return value; }
    }

    /** Result with no additional fields. */
    public record EmptyResult(JsonRpcTypes.RequestId id,
                              Optional<Map<String, Object>> _meta) implements Result {
        public EmptyResult {
            Objects.requireNonNull(id);
            Objects.requireNonNull(_meta);
        }
    }

    // placeholders required to seal the hierarchies until concrete types arrive
    private record StubRequest(JsonRpcTypes.RequestId id, String method,
                               Optional<Map<String, Object>> _meta) implements Request {}

    private record StubNotification(String method,
                                    Optional<Map<String, Object>> _meta) implements Notification {}
}
