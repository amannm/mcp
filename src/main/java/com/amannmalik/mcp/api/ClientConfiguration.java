package com.amannmalik.mcp.api;

import java.time.Duration;

/// Client-side configuration for MCP connections
/// - [Architecture](specification/2025-06-18/architecture/index.mdx)  
/// - [Client Features](specification/2025-06-18/client/index.mdx)
public record ClientConfiguration(
        ClientConnectionConfig clientConnection,
        SessionConfig session
) {

    public static ClientConfiguration defaultConfiguration() {
        return new ClientConfiguration(
                ClientConnectionConfig.defaultConfig(),
                SessionConfig.defaultConfig()
        );
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
}