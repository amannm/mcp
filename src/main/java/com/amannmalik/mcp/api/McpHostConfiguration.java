package com.amannmalik.mcp.api;

import java.time.Duration;
import java.util.*;

public record McpHostConfiguration(
        String protocolVersion,
        String compatibilityVersion,
        String hostClientName,
        String hostClientDisplayName,
        String hostClientVersion,
        Set<ClientCapability> hostClientCapabilities,
        String hostPrincipal,
        Duration processWaitSeconds,
        int defaultPageSize,
        int maxCompletionValues,
        boolean globalVerbose,
        List<McpClientConfiguration> clientConfigurations
) {
    public McpHostConfiguration {
        hostClientCapabilities = Set.copyOf(hostClientCapabilities);
        clientConfigurations = List.copyOf(clientConfigurations);
        if (processWaitSeconds.isNegative()) {
            throw new IllegalArgumentException("Invalid process wait seconds");
        }
        if (defaultPageSize <= 0 || maxCompletionValues <= 0) {
            throw new IllegalArgumentException("Invalid pagination configuration");
        }
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
                Duration.ofSeconds(2),
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
                Duration.ofSeconds(2),
                100,
                100,
                false,
                clientConfigurations
        );
    }
}