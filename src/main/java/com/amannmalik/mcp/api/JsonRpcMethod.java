package com.amannmalik.mcp.api;

import java.util.EnumSet;

public sealed interface JsonRpcMethod permits
        RequestMethod,
        NotificationMethod {
    String method();

    EnumSet<ClientCapability> clientCapabilities();
}
