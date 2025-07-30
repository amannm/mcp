package com.amannmalik.mcp.util;

import com.amannmalik.mcp.NotificationMethod;
import com.amannmalik.mcp.jsonrpc.JsonRpcNotification;
import com.amannmalik.mcp.security.RateLimiter;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.Optional;

public final class ProgressUtil {
    private ProgressUtil() {
    }

    public static Optional<ProgressToken> tokenFromMeta(JsonObject params) {
        return ProgressCodec.fromMeta(params);
    }

    public static void sendProgress(
            ProgressNotification note,
            ProgressTracker tracker,
            RateLimiter limiter,
            NotificationSender sender
    ) throws IOException {
        if (!tracker.isActive(note.token())) return;
        try {
            limiter.requireAllowance(note.token().asString());
            tracker.update(note);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ignore) {
            return;
        }
        sender.send(new JsonRpcNotification(
                NotificationMethod.PROGRESS.method(),
                ProgressCodec.toJsonObject(note)
        ));
    }
}
