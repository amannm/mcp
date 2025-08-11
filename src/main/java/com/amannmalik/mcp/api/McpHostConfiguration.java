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

        // Transport server configuration (when host serves)
        String transportType,
        int serverPort,
        List<String> allowedOrigins,
        int sseHistoryLimit,
        int responseQueueCapacity,

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
        allowedOrigins = List.copyOf(allowedOrigins);
        clientConfigurations = List.copyOf(clientConfigurations);
        if (processWaitSeconds <= 0)
            throw new IllegalArgumentException("Invalid process wait seconds");
        if (serverPort < 0 || serverPort > 65_535)
            throw new IllegalArgumentException("Invalid port number");
        if (defaultPageSize <= 0 || maxCompletionValues <= 0 || responseQueueCapacity <= 0)
            throw new IllegalArgumentException("Invalid pagination configuration");
        if (sseHistoryLimit < 0)
            throw new IllegalArgumentException("Invalid SSE history limit");
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
                "stdio",
                0,
                List.of("http://localhost", "http://127.0.0.1"),
                100,
                1,
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
                "stdio",
                0,
                List.of("http://localhost", "http://127.0.0.1"),
                100,
                1,
                2,
                100,
                100,
                false,
                clientConfigurations
        );
    }
}