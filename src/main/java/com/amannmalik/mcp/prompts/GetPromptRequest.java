package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.validation.InputSanitizer;

import java.util.Map;

public record GetPromptRequest(String name, Map<String, String> arguments) {
    public GetPromptRequest {
        if (name == null) throw new IllegalArgumentException("name required");
        name = InputSanitizer.requireClean(name);
        arguments = InputSanitizer.requireCleanMap(arguments);
    }
}
