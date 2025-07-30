package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.auth.AuthorizationManager;
import com.amannmalik.mcp.auth.BearerTokenAuthorizationStrategy;
import com.amannmalik.mcp.auth.JwtTokenValidator;
import com.amannmalik.mcp.security.OriginValidator;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.transport.StreamableHttpTransport;
import com.amannmalik.mcp.transport.Transport;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "server", description = "Run MCP server", mixinStandardHelpOptions = true)
public final class ServerCommand implements Callable<Integer> {
    @CommandLine.Option(names = {"-c", "--config"}, description = "Config file")
    private Path config;

    private ServerConfig resolved;

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
        } else if (config != null) {
            CliConfig loaded = ConfigLoader.load(config);
            if (!(loaded instanceof ServerConfig sc)) throw new IllegalArgumentException("server config expected");
            cfg = sc;
        } else {
            TransportType type = httpPort == null ? TransportType.STDIO : TransportType.HTTP;
            int port = httpPort == null ? 0 : httpPort;
            if (stdio) type = TransportType.STDIO;
            cfg = new ServerConfig(type, port, null, expectedAudience);
        }

        Transport t;
        switch (cfg.transport()) {
            case STDIO -> t = new StdioTransport(System.in, System.out);
            case HTTP -> {
                OriginValidator originValidator = new OriginValidator(Set.of("http://localhost", "http://127.0.0.1"));
                AuthorizationManager authManager = null;
                if (cfg.expectedAudience() != null && !cfg.expectedAudience().isBlank()) {
                    JwtTokenValidator tokenValidator = new JwtTokenValidator(cfg.expectedAudience());
                    BearerTokenAuthorizationStrategy authStrategy = new BearerTokenAuthorizationStrategy(tokenValidator);
                    authManager = new AuthorizationManager(List.of(authStrategy));
                }
                StreamableHttpTransport ht = new StreamableHttpTransport(cfg.port(), originValidator, authManager);
                if (verbose) System.err.println("Listening on http://127.0.0.1:" + ht.port());
                t = ht;
            }
            default -> throw new IllegalStateException();
        }

        String instructions = cfg.instructions();
        if (instructionsFile != null) {
            instructions = Files.readString(instructionsFile);
        }

        try (McpServer server = new McpServer(t, instructions)) {
            server.serve();
        }
        return 0;
    }
}
