package com.amannmalik.mcp.api;

import java.util.*;

public record McpHostConfiguration(
        // Protocol configuration
        String protocolVersion,
        String compatibilityVersion,

        // Host identity (when host acts as a client to other MCP systems)
        String hostClientName,
        String hostClientDisplayName,
        String hostClientVersion,
        Set<ClientCapability> hostClientCapabilities,

        // Security configuration
        String hostPrincipal,

        // Global process configuration
        int processWaitSeconds,
        int defaultPageSize,
        int maxCompletionValues,

        // Global behavior
        boolean globalVerbose,

        // Managed client configurations
        List<McpClientConfiguration> clientConfigurations
) {

    public McpHostConfiguration {
        hostClientCapabilities = Set.copyOf(hostClientCapabilities);
        clientConfigurations = List.copyOf(clientConfigurations);
        if (processWaitSeconds <= 0)
            throw new IllegalArgumentException("Invalid process wait seconds");
        if (defaultPageSize <= 0 || maxCompletionValues <= 0)
            throw new IllegalArgumentException("Invalid pagination configuration");
    }

    public static McpHostConfiguration defaultConfiguration() {
        return new McpHostConfiguration(
                "2025-06-18",
                "2025-03-26",
                "mcp-host",
                "MCP Host",
                "1.0.0",
                defaultHostClientCapabilities(),
                "user",
                2,
                100,
                100,
                false,
                List.of()
        );
    }

    private static Set<ClientCapability> defaultHostClientCapabilities() {
        return EnumSet.of(
                ClientCapability.SAMPLING,
                ClientCapability.ROOTS,
                ClientCapability.ELICITATION
        );
    }

    public static McpHostConfiguration withClientConfigurations(List<McpClientConfiguration> clientConfigurations) {
        return new McpHostConfiguration(
                "2025-06-18",
                "2025-03-26",
                "mcp-host",
                "MCP Host",
                "1.0.0",
                defaultHostClientCapabilities(),
                "user",
                0,
                100,
                100,
                false,
                clientConfigurations
        );
    }
}