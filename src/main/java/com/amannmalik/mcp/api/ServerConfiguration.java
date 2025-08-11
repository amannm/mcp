package com.amannmalik.mcp.api;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/// Server-side configuration for MCP connections
/// - [Architecture](specification/2025-06-18/architecture/index.mdx)
/// - [Server Features](specification/2025-06-18/server/index.mdx)
public record ServerConfiguration(
        ServerBindConfig serverBind,
        SessionConfig session,
        SseConfig sse,
        ServletConfig servlet
) {

    public static ServerConfiguration defaultConfiguration() {
        return new ServerConfiguration(
                ServerBindConfig.defaultConfig(),
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