package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

public record ElicitResult(ElicitationAction action, JsonObject content, JsonObject _meta) implements Result {

    public ElicitResult {
        if (action == null) {
            throw new IllegalArgumentException("action is required");
        }
        if (action == ElicitationAction.ACCEPT && content == null) {
            throw new IllegalArgumentException("content required for ACCEPT action");
        }
        if (content != null) {
            for (var entry : content.entrySet()) {
                ValidationUtil.requireClean(entry.getKey());
                var value = entry.getValue();
                switch (value) {
                    case JsonString js -> ValidationUtil.requireClean(js.getString());
                    case JsonNumber ignored -> {
                    }
                    case JsonValue jv when jv == JsonValue.TRUE || jv == JsonValue.FALSE -> {
                    }
                    default -> throw new IllegalArgumentException("content values must be primitive");
                }
            }
        }
        ValidationUtil.requireMeta(_meta);
    }
}
