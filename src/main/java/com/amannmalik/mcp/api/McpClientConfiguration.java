package com.amannmalik.mcp.api;

import com.amannmalik.mcp.spi.SamplingAccessPolicy;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/// Complete MCP client configuration merging connection, session, and protocol settings
/// - [Architecture](specification/2025-06-18/architecture/index.mdx)
/// - [Client Features](specification/2025-06-18/client/index.mdx)
public record McpClientConfiguration(
        String clientId,
        String serverName,
        String serverDisplayName,
        String serverVersion,
        String principal,
        Set<ClientCapability> clientCapabilities,
        String commandSpec,
        Duration defaultReceiveTimeout,
        String defaultOriginHeader,
        Duration httpRequestTimeout,
        boolean enableKeepAlive,
        int sessionIdByteLength,
        Duration initializeRequestTimeout,
        boolean strictVersionValidation,
        Duration pingTimeout,
        Duration pingInterval,
        int progressPerSecond,
        Duration rateLimiterWindow,
        boolean verbose,
        boolean interactiveSampling,
        List<String> rootDirectories,
        SamplingAccessPolicy samplingAccessPolicy,
        McpClientTlsConfiguration tlsConfiguration
) {

    public McpClientConfiguration {
        if (clientId == null || clientId.isBlank())
            throw new IllegalArgumentException("Client ID is required");
        if (serverName == null || serverName.isBlank())
            throw new IllegalArgumentException("Server name is required");
        if (principal == null || principal.isBlank())
            throw new IllegalArgumentException("Principal is required");
        clientCapabilities = Set.copyOf(clientCapabilities);
        if (defaultReceiveTimeout == null || defaultReceiveTimeout.isNegative() || defaultReceiveTimeout.isZero())
            throw new IllegalArgumentException("Default receive timeout must be positive");
        if (defaultOriginHeader == null || defaultOriginHeader.isBlank())
            throw new IllegalArgumentException("Default origin header is required");
        if (httpRequestTimeout == null || httpRequestTimeout.isNegative() || httpRequestTimeout.isZero())
            throw new IllegalArgumentException("HTTP request timeout must be positive");
        if (sessionIdByteLength <= 0)
            throw new IllegalArgumentException("Session ID byte length must be positive");
        if (initializeRequestTimeout == null || initializeRequestTimeout.isNegative() || initializeRequestTimeout.isZero())
            throw new IllegalArgumentException("Initialize request timeout must be positive");
        if (pingTimeout == null || pingTimeout.isNegative() || pingTimeout.isZero())
            throw new IllegalArgumentException("Ping timeout must be positive");
        if (pingInterval == null || pingInterval.isNegative() || pingInterval.isZero())
            throw new IllegalArgumentException("Ping interval must be positive");
        if (progressPerSecond < 0)
            throw new IllegalArgumentException("Progress per second must be non-negative");
        if (rateLimiterWindow == null || rateLimiterWindow.isNegative() || rateLimiterWindow.isZero())
            throw new IllegalArgumentException("Rate limiter window must be positive");
        rootDirectories = List.copyOf(rootDirectories);
        if (samplingAccessPolicy == null)
            throw new IllegalArgumentException("Sampling access policy is required");
        if (tlsConfiguration == null)
            throw new IllegalArgumentException("TLS configuration is required");
    }

    public static McpClientConfiguration defaultConfiguration(String clientId, String serverName, String principal) {
        return new McpClientConfiguration(
                clientId,
                serverName,
                serverName,
                "1.0.0",
                principal,
                Set.of(ClientCapability.SAMPLING, ClientCapability.ROOTS),
                "",
                Duration.ofSeconds(10),
                "http://127.0.0.1",
                Duration.ofSeconds(30),
                true,
                32,
                Duration.ofSeconds(30),
                true,
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                10,
                Duration.ofMinutes(1),
                false,
                false,
                List.of(),
                SamplingAccessPolicy.PERMISSIVE,
                McpClientTlsConfiguration.defaultConfiguration()
        );
    }
}

