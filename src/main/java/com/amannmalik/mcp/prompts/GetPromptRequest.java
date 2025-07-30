package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.validation.InputSanitizer;

import java.util.HashMap;
import java.util.Map;

public record GetPromptRequest(String name, Map<String, String> arguments) {
    public GetPromptRequest {
        if (name == null) throw new IllegalArgumentException("name required");
        name = InputSanitizer.requireClean(name);
        if (arguments == null || arguments.isEmpty()) {
            arguments = Map.of();
        } else {
            Map<String, String> copy = new HashMap<>();
            arguments.forEach((k, v) -> copy.put(InputSanitizer.requireClean(k), InputSanitizer.requireClean(v)));
            arguments = Map.copyOf(copy);
        }
    }
}
