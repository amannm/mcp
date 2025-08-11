package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.model.RequestMethod;
import com.amannmalik.mcp.api.model.ServerCapability;

import java.util.Map;
import java.util.Optional;

public final class CapabilityRequirements {
    private static final Map<RequestMethod, ServerCapability> MAP = Map.ofEntries(
            Map.entry(RequestMethod.RESOURCES_LIST, ServerCapability.RESOURCES),
            Map.entry(RequestMethod.RESOURCES_TEMPLATES_LIST, ServerCapability.RESOURCES),
            Map.entry(RequestMethod.RESOURCES_READ, ServerCapability.RESOURCES),
            Map.entry(RequestMethod.RESOURCES_SUBSCRIBE, ServerCapability.RESOURCES),
            Map.entry(RequestMethod.RESOURCES_UNSUBSCRIBE, ServerCapability.RESOURCES),
            Map.entry(RequestMethod.TOOLS_LIST, ServerCapability.TOOLS),
            Map.entry(RequestMethod.TOOLS_CALL, ServerCapability.TOOLS),
            Map.entry(RequestMethod.PROMPTS_LIST, ServerCapability.PROMPTS),
            Map.entry(RequestMethod.PROMPTS_GET, ServerCapability.PROMPTS),
            Map.entry(RequestMethod.LOGGING_SET_LEVEL, ServerCapability.LOGGING),
            Map.entry(RequestMethod.COMPLETION_COMPLETE, ServerCapability.COMPLETIONS)
    );

    private CapabilityRequirements() {
    }

    public static Optional<ServerCapability> forMethod(RequestMethod method) {
        return Optional.ofNullable(MAP.get(method));
    }
}
