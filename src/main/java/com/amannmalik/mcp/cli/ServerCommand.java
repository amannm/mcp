package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.auth.*;
import com.amannmalik.mcp.security.OriginValidator;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.*;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "server", description = "Run MCP server", mixinStandardHelpOptions = true)
public final class ServerCommand implements Callable<Integer> {
    @CommandLine.Option(names = {"-c", "--config"}, description = "Config file")
    private Optional<Path> config = Optional.empty();

    private ServerConfig resolved;

    @CommandLine.Option(names = "--http", description = "HTTP port")
    private int httpPort = -1;

    @CommandLine.Option(names = "--stdio", description = "Use stdio transport")
    private boolean stdio = false;

    @CommandLine.Option(names = "--instructions", description = "Instructions file")
    private Optional<Path> instructionsFile = Optional.empty();

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose logging")
    private boolean verbose = false;

    @CommandLine.Option(names = {"--audience"}, description = "Expected JWT audience for authorization")
    private Optional<String> expectedAudience = Optional.empty();

    @CommandLine.Option(names = {"--resource-metadata"}, description = "Resource metadata URL")
    private Optional<String> resourceMetadataUrl = Optional.empty();

    @CommandLine.Option(names = {"--auth-server"}, description = "Authorization server URL", split = ",")
    private List<String> authServers = new ArrayList<>();

    @CommandLine.Option(names = "--test-mode", description = "Disable auth for testing")
    private boolean testMode = false;

    public ServerCommand() {
    }

    public ServerCommand(ServerConfig config, boolean verbose) {
        this.resolved = config;
        this.verbose = verbose;
    }

    @Override
    public Integer call() throws Exception {
        ServerConfig cfg;
        if (resolved != null) {
            cfg = resolved;
        } else if (config.isPresent()) {
            CliConfig loaded = ConfigLoader.load(config.get());
            if (!(loaded instanceof ServerConfig sc)) throw new IllegalArgumentException("server config expected");
            cfg = sc;
        } else {
            TransportType type = httpPort < 0 ? TransportType.STDIO : TransportType.HTTP;
            int port = httpPort < 0 ? 0 : httpPort;
            if (stdio) type = TransportType.STDIO;
            List<String> auth = authServers;
            if (!testMode) {
                if (auth.isEmpty()) {
                    throw new IllegalArgumentException("--auth-server is required");
                }
            } else {
                auth = List.of();
            }
            cfg = new ServerConfig(type, port, null,
                    expectedAudience.orElse(null), resourceMetadataUrl.orElse(null), auth);
        }

        Transport t;
        switch (cfg.transport()) {
            case STDIO -> t = new StdioTransport(System.in, System.out);
            case HTTP -> {
                OriginValidator originValidator = new OriginValidator(Set.of("http://localhost", "http://127.0.0.1"));
                AuthorizationManager authManager = null;
                if (cfg.expectedAudience() != null && !cfg.expectedAudience().isBlank()) {
                    String secretEnv = System.getenv("MCP_JWT_SECRET");
                    JwtTokenValidator tokenValidator = secretEnv == null || secretEnv.isBlank()
                            ? new JwtTokenValidator(cfg.expectedAudience())
                            : new JwtTokenValidator(cfg.expectedAudience(), secretEnv.getBytes(StandardCharsets.UTF_8));
                    BearerTokenAuthorizationStrategy authStrategy = new BearerTokenAuthorizationStrategy(tokenValidator);
                    authManager = new AuthorizationManager(List.of(authStrategy));
                }
                StreamableHttpTransport ht = new StreamableHttpTransport(
                        cfg.port(), originValidator, authManager,
                        cfg.resourceMetadataUrl(), cfg.authorizationServers());
                if (verbose) System.err.println("Listening on http://127.0.0.1:" + ht.port());
                t = ht;
            }
            default -> throw new IllegalStateException();
        }

        String instructions = cfg.instructions();
        if (instructionsFile.isPresent()) {
            instructions = Files.readString(instructionsFile.get());
        }

        try (McpServer server = new McpServer(t, instructions)) {
            server.serve();
        }
        return 0;
    }
}
