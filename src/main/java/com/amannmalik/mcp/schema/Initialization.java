package com.amannmalik.mcp.schema;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Connection initialization messages.
 */
public final class Initialization {
    private Initialization() {}

    /** Client initialization request. */
    public record InitializeRequest(JsonRpcTypes.RequestId id,
                                    String protocolVersion,
                                    Capabilities.ClientCapabilities capabilities,
                                    Optional<Map<String, Object>> clientInfo,
                                    Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public InitializeRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(protocolVersion);
            Objects.requireNonNull(capabilities);
            Objects.requireNonNull(clientInfo);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "initialize"; }
    }

    /** Server initialization response. */
    public record InitializeResult(JsonRpcTypes.RequestId id,
                                   String protocolVersion,
                                   Capabilities.ServerCapabilities capabilities,
                                   Optional<Map<String, Object>> serverInfo,
                                   Optional<String> instructions,
                                   Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Result {
        public InitializeResult {
            Objects.requireNonNull(id);
            Objects.requireNonNull(protocolVersion);
            Objects.requireNonNull(capabilities);
            Objects.requireNonNull(serverInfo);
            Objects.requireNonNull(instructions);
            Objects.requireNonNull(_meta);
        }
    }

    /** Sent once initialization completed. */
    public record InitializedNotification(Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Notification {
        public InitializedNotification {
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "notifications/initialized"; }
    }
}
