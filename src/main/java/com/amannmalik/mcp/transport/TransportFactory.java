package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.auth.*;
import com.amannmalik.mcp.config.McpConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class TransportFactory {
    public static Transport createTransport(Integer httpPort, boolean stdio, String expectedAudience,
                                            String resourceMetadataUrl, List<String> authServers, boolean testMode,
                                            boolean verbose) throws Exception {
        TransportType defType = parseTransport(McpConfiguration.current().transportType());
        TransportType type = httpPort == null ? defType : TransportType.HTTP;
        int port = httpPort == null ? McpConfiguration.current().port() : httpPort;
        if (stdio) type = TransportType.STDIO;

        List<String> auth = authServers;
        if (!testMode) {
            if (auth == null || auth.isEmpty()) throw new IllegalArgumentException("--auth-server is required");
        } else {
            auth = List.of();
        }

        return switch (type) {
            case STDIO -> new StdioTransport(System.in, System.out);
            case HTTP -> {
                AuthorizationManager authManager = null;
                if (expectedAudience != null && !expectedAudience.isBlank()) {
                    String secretEnv = System.getenv("MCP_JWT_SECRET");
                    JwtTokenValidator tokenValidator = secretEnv == null || secretEnv.isBlank()
                            ? new JwtTokenValidator(expectedAudience)
                            : new JwtTokenValidator(expectedAudience, secretEnv.getBytes(StandardCharsets.UTF_8));
                    authManager = new AuthorizationManager(List.of(new BearerTokenAuthorizationStrategy(tokenValidator)));
                }
                StreamableHttpServerTransport ht = new StreamableHttpServerTransport(
                        port, Set.copyOf(McpConfiguration.current().allowedOrigins()), authManager,
                        resourceMetadataUrl, auth);
                if (verbose) System.err.println("Listening on http://127.0.0.1:" + ht.port());
                yield ht;
            }
        };
    }

    static TransportType parseTransport(String name) {
        return switch (name) {
            case "stdio" -> TransportType.STDIO;
            case "http" -> TransportType.HTTP;
            default -> throw new IllegalArgumentException("unknown transport: " + name);
        };
    }

    public static Transport createTransport(String[] s, boolean verbose) throws IOException {
        return new StdioTransport(s, verbose ? System.err::println : _ -> {
        });
    }
}
