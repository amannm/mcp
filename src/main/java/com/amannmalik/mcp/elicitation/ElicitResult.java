package com.amannmalik.mcp.elicitation;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ElicitResult(ElicitationAction action,
                           JsonObject content,
                           JsonObject _meta) {
    public ElicitResult {
        if (action == null) {
            throw new IllegalArgumentException("action is required");
        }
        if (action == ElicitationAction.ACCEPT && content == null) {
            throw new IllegalArgumentException("content required for ACCEPT action");
        }
        if (content != null) {
            for (var entry : content.entrySet()) {
                InputSanitizer.requireClean(entry.getKey());
                switch (entry.getValue().getValueType()) {
                    case STRING -> InputSanitizer.requireClean(content.getString(entry.getKey()));
                    case NUMBER, TRUE, FALSE -> {
                    }
                    default -> throw new IllegalArgumentException(
                            "content values must be primitive");
                }
            }
        }
        MetaValidator.requireValid(_meta);
    }
}
