package com.amannmalik.mcp.cli;

import java.util.List;

public record ServerConfig(
        TransportType transport,
        int port,
        String instructions,
        String expectedAudience,
        String resourceMetadataUrl,
        List<String> authorizationServers) implements CliConfig {
    public ServerConfig {
        if (transport == null) throw new IllegalArgumentException("transport");
        if (transport == TransportType.HTTP && port < 0) {
            throw new IllegalArgumentException("port must be non-negative for HTTP");
        }

        if (resourceMetadataUrl != null && !resourceMetadataUrl.isBlank()) {
            try {
                var uri = java.net.URI.create(resourceMetadataUrl);
                if (!uri.isAbsolute() || uri.getFragment() != null) {
                    throw new IllegalArgumentException("invalid resourceMetadataUrl");
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("invalid resourceMetadataUrl", e);
            }
        }

        if (authorizationServers == null || authorizationServers.isEmpty()) {
            authorizationServers = List.of();
        } else {
            var validated = new java.util.ArrayList<String>(authorizationServers.size());
            for (String as : authorizationServers) {
                try {
                    var uri = java.net.URI.create(as);
                    if (!uri.isAbsolute() || uri.getFragment() != null) {
                        throw new IllegalArgumentException();
                    }
                    String scheme = uri.getScheme();
                    if ("https".equalsIgnoreCase(scheme)) {
                        validated.add(as);
                        continue;
                    }
                    if ("http".equalsIgnoreCase(scheme)) {
                        String host = uri.getHost();
                        if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
                            validated.add(as);
                            continue;
                        }
                    }
                    throw new IllegalArgumentException();
                } catch (Exception e) {
                    throw new IllegalArgumentException("invalid authorizationServer: " + as, e);
                }
            }
            authorizationServers = List.copyOf(validated);
        }
    }
}
