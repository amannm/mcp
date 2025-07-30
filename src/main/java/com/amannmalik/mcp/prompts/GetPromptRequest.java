package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

import java.util.Map;

public record GetPromptRequest(String name,
                               Map<String, String> arguments,
                               JsonObject _meta) {
    public GetPromptRequest {
        if (name == null) throw new IllegalArgumentException("name required");
        name = InputSanitizer.requireClean(name);
        arguments = InputSanitizer.requireCleanMap(arguments);
        MetaValidator.requireValid(_meta);
    }
}
