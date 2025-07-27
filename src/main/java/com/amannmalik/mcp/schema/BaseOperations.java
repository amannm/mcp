package com.amannmalik.mcp.schema;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Basic protocol operations: ping, progress tracking, and cancellation.
 */
public final class BaseOperations {
    private BaseOperations() {}

    /** Ping request used to verify connection health. */
    public record PingRequest(JsonRpcTypes.RequestId id,
                              Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public PingRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "ping"; }
    }

    /** Notification providing progress updates for long-running operations. */
    public record ProgressNotification(BaseProtocol.ProgressToken progressToken,
                                       double progress,
                                       Optional<Double> total,
                                       Optional<String> message,
                                       Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Notification {
        public ProgressNotification {
            Objects.requireNonNull(progressToken);
            Objects.requireNonNull(total);
            Objects.requireNonNull(message);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "notifications/progress"; }
    }

    /** Notification that a request has been cancelled. */
    public record CancelledNotification(JsonRpcTypes.RequestId requestId,
                                        Optional<String> reason,
                                        Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Notification {
        public CancelledNotification {
            Objects.requireNonNull(requestId);
            Objects.requireNonNull(reason);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "notifications/cancelled"; }
    }
}

/** Typedef for opaque pagination cursor. */
record Cursor(String value) {
    public Cursor {
        Objects.requireNonNull(value);
    }
}
