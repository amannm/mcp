package com.amannmalik.mcp.transport;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/// Transport-level configuration for MCP connections
/// - [Transports](specification/2025-06-18/basic/transports.mdx)
/// - [Security Best Practices](specification/2025-06-18/basic/security_best_practices.mdx)
public record TransportConfiguration(
        // Server binding configuration
        ServerBindConfig serverBind,

        // Client connection configuration  
        ClientConnectionConfig clientConnection,

        // Session management configuration
        SessionConfig session,

        // Server-sent events configuration
        SseConfig sse,

        // HTTP servlet configuration
        ServletConfig servlet
) {

    public static TransportConfiguration defaultConfiguration() {
        return new TransportConfiguration(
                ServerBindConfig.defaultConfig(),
                ClientConnectionConfig.defaultConfig(),
                SessionConfig.defaultConfig(),
                SseConfig.defaultConfig(),
                ServletConfig.defaultConfig()
        );
    }

    public record ServerBindConfig(
            String bindAddress,
            Set<String> servletPaths,
            String resourceMetadataPath,
            String resourceMetadataUrlTemplate
    ) {
        
        public ServerBindConfig {
            servletPaths = Set.copyOf(servletPaths);
            if (bindAddress == null || bindAddress.isBlank()) {
                throw new IllegalArgumentException("Bind address required");
            }
        }

        public static ServerBindConfig defaultConfig() {
            return new ServerBindConfig(
                    "127.0.0.1",
                    Set.of("/", "/.well-known/oauth-protected-resource"),
                    "/.well-known/oauth-protected-resource",
                    "http://%s:%d/.well-known/oauth-protected-resource"
            );
        }
    }

    public record ClientConnectionConfig(
            Duration defaultReceiveTimeout,
            String defaultOriginHeader,
            Duration httpRequestTimeout,
            boolean enableKeepAlive
    ) {

        public static ClientConnectionConfig defaultConfig() {
            return new ClientConnectionConfig(
                    Duration.ofSeconds(10),
                    "http://127.0.0.1",
                    Duration.ofSeconds(30),
                    true
            );
        }
    }

    public record SessionConfig(
            int sessionIdByteLength,
            Duration initializeRequestTimeout,
            boolean strictVersionValidation
    ) {

        public SessionConfig {
            if (sessionIdByteLength <= 0) {
                throw new IllegalArgumentException("Session ID byte length must be positive");
            }
            if (initializeRequestTimeout.isNegative() || initializeRequestTimeout.isZero()) {
                throw new IllegalArgumentException("Initialize request timeout must be positive");
            }
        }

        public static SessionConfig defaultConfig() {
            return new SessionConfig(
                    32,
                    Duration.ofSeconds(30),
                    true
            );
        }
    }

    public record SseConfig(
            int clientPrefixByteLength,
            boolean enableHistoryReplay,
            Duration clientTimeout
    ) {

        public SseConfig {
            if (clientPrefixByteLength <= 0) {
                throw new IllegalArgumentException("Client prefix byte length must be positive");
            }
        }

        public static SseConfig defaultConfig() {
            return new SseConfig(
                    8,
                    true,
                    Duration.ofMinutes(5)
            );
        }
    }

    public record ServletConfig(
            List<String> acceptedContentTypes,
            List<String> producedContentTypes,
            boolean enableAsyncProcessing
    ) {

        public ServletConfig {
            acceptedContentTypes = List.copyOf(acceptedContentTypes);
            producedContentTypes = List.copyOf(producedContentTypes);
        }

        public static ServletConfig defaultConfig() {
            return new ServletConfig(
                    List.of("application/json", "text/event-stream"),
                    List.of("application/json", "text/event-stream"),
                    true
            );
        }
    }
}