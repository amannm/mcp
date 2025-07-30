package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.validation.InputSanitizer;

import java.util.Map;

public record GetPromptRequest(String name, Map<String, String> arguments) {
    public GetPromptRequest {
        name = InputSanitizer.requireClean(name);
        arguments = arguments == null || arguments.isEmpty() ? Map.of() : Map.copyOf(arguments);
    }

    @Override
    public Map<String, String> arguments() {
        return Map.copyOf(arguments);
    }
}
