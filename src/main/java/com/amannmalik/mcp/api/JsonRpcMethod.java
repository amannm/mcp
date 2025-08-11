package com.amannmalik.mcp.api;

import java.util.EnumSet;

public sealed interface JsonRpcMethod permits
        RequestMethod,
        NotificationMethod {
    String method();

    default EnumSet<ClientCapability> clientCapabilities() {
        return EnumSet.noneOf(ClientCapability.class);
    }
}
