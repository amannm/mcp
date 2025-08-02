package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.auth.*;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.security.OriginValidator;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.*;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "server", description = "Run MCP server", mixinStandardHelpOptions = true)
public final class ServerCommand implements Callable<Integer> {
    @CommandLine.Option(names = "--http", description = "HTTP port")
    private Integer httpPort;

    @CommandLine.Option(names = "--stdio", description = "Use stdio transport")
    private boolean stdio;

    @CommandLine.Option(names = "--instructions", description = "Instructions file")
    private Path instructionsFile;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose logging")
    private boolean verbose;

    @CommandLine.Option(names = {"--audience"}, description = "Expected JWT audience for authorization")
    private String expectedAudience;

    @CommandLine.Option(names = {"--resource-metadata"}, description = "Resource metadata URL")
    private String resourceMetadataUrl;

    @CommandLine.Option(names = {"--auth-server"}, description = "Authorization server URL", split = ",")
    private List<String> authServers;

    @CommandLine.Option(names = "--test-mode", description = "Disable auth for testing")
    private boolean testMode;

    @Override
    public Integer call() throws Exception {
        TransportType defType = parseTransport(McpConfiguration.current().server().transport().type());
        TransportType type = httpPort == null ? defType : TransportType.HTTP;
        int port = httpPort == null ? McpConfiguration.current().server().transport().port() : httpPort;
        if (stdio) type = TransportType.STDIO;
        List<String> auth = authServers;
        if (!testMode) {
            if (auth == null || auth.isEmpty()) throw new IllegalArgumentException("--auth-server is required");
        } else {
            auth = List.of();
        }

        Transport t;
        switch (type) {
            case STDIO -> t = new StdioTransport(System.in, System.out);
            case HTTP -> {
                OriginValidator originValidator = new OriginValidator(
                        Set.copyOf(McpConfiguration.current().server().transport().allowedOrigins()));
                AuthorizationManager authManager = null;
                if (expectedAudience != null && !expectedAudience.isBlank()) {
                    String secretEnv = System.getenv("MCP_JWT_SECRET");
                    JwtTokenValidator tokenValidator = secretEnv == null || secretEnv.isBlank()
                            ? new JwtTokenValidator(expectedAudience)
                            : new JwtTokenValidator(expectedAudience, secretEnv.getBytes(StandardCharsets.UTF_8));
                    authManager = new AuthorizationManager(List.of(new BearerTokenAuthorizationStrategy(tokenValidator)));
                }
                StreamableHttpTransport ht = new StreamableHttpTransport(
                        port, originValidator, authManager,
                        resourceMetadataUrl, auth);
                if (verbose) System.err.println("Listening on http://127.0.0.1:" + ht.port());
                t = ht;
            }
            default -> throw new IllegalStateException();
        }

        String instructions = instructionsFile == null ? null : Files.readString(instructionsFile);

        try (McpServer server = new McpServer(t, instructions)) {
            server.serve();
        }
        return 0;
    }

    private static TransportType parseTransport(String name) {
        return switch (name) {
            case "stdio" -> TransportType.STDIO;
            case "http" -> TransportType.HTTP;
            default -> throw new IllegalArgumentException("unknown transport: " + name);
        };
    }
}
