package com.amannmalik.mcp.api;

import com.amannmalik.mcp.auth.*;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.transport.StreamableHttpServerTransport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public final class TransportFactory {
    public static Transport createHttpTransport(Integer httpPort,
                                                String expectedAudience,
                                                String resourceMetadataUrl,
                                                List<String> authServers,
                                                boolean insecure,
                                                boolean verbose) throws Exception {

        int port = httpPort == null ? McpConfiguration.current().port() : httpPort;
        List<String> auth = authServers;
        if (!insecure) {
            if (auth == null || auth.isEmpty()) throw new IllegalArgumentException("auth server must be specified");
        } else {
            auth = List.of();
        }
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
        return ht;
    }

    public static Transport createStdioTransport(String[] commands, boolean verbose) throws IOException {
        return commands.length == 0
                ? new StdioTransport(System.in, System.out)
                : new StdioTransport(commands, verbose ? System.err::println : _ -> {
        });
    }
}
