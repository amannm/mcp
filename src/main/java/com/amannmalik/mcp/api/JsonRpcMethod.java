package com.amannmalik.mcp.api;

import java.util.Optional;

public sealed interface JsonRpcMethod permits
        RequestMethod,
        NotificationMethod {
    String method();

    default Optional<ClientCapability> clientCapability() {
        return Optional.empty();
    }
}
