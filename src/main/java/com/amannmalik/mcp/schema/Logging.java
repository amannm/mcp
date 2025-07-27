package com.amannmalik.mcp.schema;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Simple structured logging messages with configurable severity level.
 */
public final class Logging {
    private Logging() {}

    /** RFC-5424 severity levels. */
    public enum LoggingLevel {
        DEBUG, INFO, NOTICE, WARNING, ERROR, CRITICAL, ALERT, EMERGENCY
    }

    /** Request to set minimum logging level. */
    public record SetLevelRequest(JsonRpcTypes.RequestId id,
                                  LoggingLevel level,
                                  Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public SetLevelRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(level);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "logging/setLevel"; }
    }

    /** Notification carrying a log message payload. */
    public record LoggingMessageNotification(LoggingLevel level,
                                             Map<String, Object> data,
                                             Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Notification {
        public LoggingMessageNotification {
            Objects.requireNonNull(level);
            data = Map.copyOf(data);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "notifications/logging/message"; }
    }
}
