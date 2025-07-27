package com.amannmalik.mcp.schema;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Connection initialization messages. */
public final class Initialization {
    private Initialization() {}

    public record InitializeRequest(JsonRpcTypes.RequestId id,
                                    String protocolVersion,
                                    Capabilities.ClientCapabilities capabilities,
                                    Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public static final String METHOD = "initialize";
        public InitializeRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(protocolVersion);
            Objects.requireNonNull(capabilities);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return METHOD; }
    }

    public record InitializeResult(JsonRpcTypes.RequestId id,
                                   String protocolVersion,
                                   Capabilities.ServerCapabilities capabilities,
                                   Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Result {
        public InitializeResult {
            Objects.requireNonNull(id);
            Objects.requireNonNull(protocolVersion);
            Objects.requireNonNull(capabilities);
            Objects.requireNonNull(_meta);
        }
    }

    public record InitializedNotification(Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Notification {
        public static final String METHOD = "initialized";
        public InitializedNotification {
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return METHOD; }
    }
}
