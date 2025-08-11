package com.amannmalik.mcp.api;

import com.amannmalik.mcp.elicitation.ElicitationAction;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ElicitResult(ElicitationAction action, JsonObject content, JsonObject _meta) {

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
                switch (entry.getValue().getValueType()) {
                    case STRING -> ValidationUtil.requireClean(content.getString(entry.getKey()));
                    case NUMBER, TRUE, FALSE -> {
                    }
                    default -> throw new IllegalArgumentException(
                            "content values must be primitive");
                }
            }
        }
        ValidationUtil.requireMeta(_meta);
    }

}
