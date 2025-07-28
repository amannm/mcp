package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.server.SimpleServer;
import com.amannmalik.mcp.transport.StreamableHttpTransport;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.transport.Transport;
import picocli.CommandLine;

import java.nio.file.Path;
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

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose logging")
    private boolean verbose;

    public ServerCommand() {}

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
            cfg = new ServerConfig(type, port);
        }

        Transport t;
        switch (cfg.transport()) {
            case STDIO -> t = new StdioTransport(System.in, System.out);
            case HTTP -> {
                StreamableHttpTransport ht = new StreamableHttpTransport(cfg.port());
                if (verbose) System.err.println("Listening on http://127.0.0.1:" + ht.port());
                t = ht;
            }
            default -> throw new IllegalStateException();
        }

        try (McpServer server = new SimpleServer(t)) {
            server.serve();
        }
        return 0;
    }
}
