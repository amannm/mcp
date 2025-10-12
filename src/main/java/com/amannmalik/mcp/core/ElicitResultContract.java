package com.amannmalik.mcp.core;

import com.amannmalik.mcp.spi.ElicitationAction;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

public final class ElicitResultContract {
    private ElicitResultContract() {
    }

    public static void validate(ElicitationAction action, JsonObject content, JsonObject meta) {
        SpiPreconditions.requireNonNull(action, "action is required");
        if (action == ElicitationAction.ACCEPT && content == null) {
            throw new IllegalArgumentException("content required for ACCEPT action");
        }
        if (content != null) {
            content.forEach((key, value) -> {
                ValidationUtil.requireClean(key);
                switch (value) {
                    case JsonString js -> ValidationUtil.requireClean(js.getString());
                    case JsonNumber ignored -> {
                    }
                    case JsonValue jv when jv == JsonValue.TRUE || jv == JsonValue.FALSE -> {
                    }
                    default -> throw new IllegalArgumentException("content values must be primitive");
                }
            });
        }
        SpiPreconditions.requireMeta(meta);
    }
}
