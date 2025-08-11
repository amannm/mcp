package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.ClientCapability;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record McpHostConfiguration(
        // Protocol configuration
        String version,
        String compatibilityVersion,
        long defaultTimeoutMs,
        long pingTimeoutMs,
        
        // Client identity configuration
        ClientIdentity clientIdentity,
        
        // Client capabilities configuration
        Set<ClientCapability> clientCapabilities,
        
        // Host principal configuration
        String hostPrincipal,
        
        // Transport configuration
        TransportConfig transportConfig,
        
        // Process configuration
        ProcessConfig processConfig
) {

    public static McpHostConfiguration defaultConfiguration() {
        return new McpHostConfiguration(
                "2025-06-18",
                "2025-03-26",
                30_000L,
                5_000L,
                ClientIdentity.defaultIdentity(),
                defaultClientCapabilities(),
                "user",
                TransportConfig.defaultConfig(),
                ProcessConfig.defaultConfig()
        );
    }

    private static Set<ClientCapability> defaultClientCapabilities() {
        return EnumSet.of(
                ClientCapability.SAMPLING,
                ClientCapability.ROOTS,
                ClientCapability.ELICITATION
        );
    }

    public record ClientIdentity(
            String name,
            String displayName,
            String version
    ) {
        public static ClientIdentity defaultIdentity() {
            return new ClientIdentity("cli", "CLI", "0");
        }
    }

    public record TransportConfig(
            String type,
            int port,
            List<String> allowedOrigins,
            int sseHistoryLimit,
            int responseQueueCapacity
    ) {
        public static TransportConfig defaultConfig() {
            return new TransportConfig(
                    "stdio",
                    0,
                    List.of("http://localhost", "http://127.0.0.1"),
                    100,
                    1
            );
        }

        public TransportConfig {
            allowedOrigins = List.copyOf(allowedOrigins);
        }
    }

    public record ProcessConfig(
            int waitSeconds,
            int defaultPageSize,
            int maxCompletionValues
    ) {
        public static ProcessConfig defaultConfig() {
            return new ProcessConfig(2, 100, 100);
        }
    }

    public McpHostConfiguration {
        clientCapabilities = Set.copyOf(clientCapabilities);
    }
}