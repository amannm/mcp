package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.ClientCapability;
import com.amannmalik.mcp.api.JsonRpcMethod;
import com.amannmalik.mcp.api.NotificationMethod;
import com.amannmalik.mcp.api.RequestMethod;
import com.amannmalik.mcp.api.ServerCapability;

import java.util.Map;
import java.util.Optional;

public final class CapabilityRequirements {
    private static final Map<RequestMethod, ServerCapability> SERVER_MAP = Map.ofEntries(
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

    private static final Map<JsonRpcMethod, ClientCapability> CLIENT_MAP = Map.ofEntries(
            Map.entry(RequestMethod.ROOTS_LIST, ClientCapability.ROOTS),
            Map.entry(NotificationMethod.ROOTS_LIST_CHANGED, ClientCapability.ROOTS),
            Map.entry(RequestMethod.SAMPLING_CREATE_MESSAGE, ClientCapability.SAMPLING),
            Map.entry(RequestMethod.ELICITATION_CREATE, ClientCapability.ELICITATION)
    );

    private CapabilityRequirements() {
    }

    public static Optional<ServerCapability> forMethod(RequestMethod method) {
        return Optional.ofNullable(SERVER_MAP.get(method));
    }

    public static Optional<ClientCapability> forClient(JsonRpcMethod method) {
        return Optional.ofNullable(CLIENT_MAP.get(method));
    }
}
