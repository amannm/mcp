package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.config.LoggingLevel;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonValue;

import java.net.URI;

public sealed interface Notification permits
        Notification.CancelledNotification,
        Notification.LoggingMessageNotification,
        Notification.ProgressNotification,
        Notification.ResourceUpdatedNotification,
        Notification.RootsListChangedNotification,
        Notification.ToolListChangedNotification {

    record CancelledNotification(RequestId requestId, String reason) implements Notification {
        public CancelledNotification {
            if (requestId == null) {
                throw new IllegalArgumentException("requestId is required");
            }
            reason = ValidationUtil.cleanNullable(reason);
        }
    }

    record LoggingMessageNotification(LoggingLevel level, String logger, JsonValue data) implements Notification {
        public LoggingMessageNotification {
            if (level == null || data == null) {
                throw new IllegalArgumentException("level and data are required");
            }
            logger = ValidationUtil.cleanNullable(logger);
        }
    }

    record ProgressNotification(
            ProgressToken token,
            double progress,
            Double total,
            String message
    ) implements Notification {

        public ProgressNotification {
            if (token == null) {
                throw new IllegalArgumentException("token is required");
            }
            ValidationUtil.requireNonNegative(progress, "progress");
            if (total != null) {
                total = ValidationUtil.requirePositive(total, "total");
                if (progress > total) {
                    throw new IllegalArgumentException("progress must not exceed total");
                }
            }
            message = ValidationUtil.cleanNullable(message);
        }
    }

    record ResourceUpdatedNotification(URI uri, String title) implements Notification {
        public ResourceUpdatedNotification {
            uri = ValidationUtil.requireAbsoluteUri(uri);
            title = ValidationUtil.cleanNullable(title);
        }
    }

    record RootsListChangedNotification() implements Notification {
    }

    record ToolListChangedNotification() implements Notification {
    }
}
