package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.client.McpClient;
import com.amannmalik.mcp.client.roots.InMemoryRootsProvider;
import com.amannmalik.mcp.client.roots.Root;
import com.amannmalik.mcp.client.sampling.InteractiveSamplingProvider;
import com.amannmalik.mcp.client.sampling.SamplingProvider;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.server.logging.LoggingLevel;
import com.amannmalik.mcp.transport.StdioTransport;
import picocli.CommandLine;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "client", description = "Run MCP client", mixinStandardHelpOptions = true)
public final class ClientCommand implements Callable<Integer> {
    @CommandLine.Option(names = "--command", description = "Server command for stdio")
    private String command;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose logging")
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        if (command == null) throw new IllegalArgumentException("command required");

        StdioTransport transport = new StdioTransport(new ProcessBuilder(command.split(" ")), 
                verbose ? System.err::println : s -> {
                });
        SamplingProvider samplingProvider = new InteractiveSamplingProvider(false);

        String currentDir = System.getProperty("user.dir");
        InMemoryRootsProvider rootsProvider = new InMemoryRootsProvider(
                List.of(new Root("file://" + currentDir, "Current Directory", null)));

        McpConfiguration cc = McpConfiguration.current();
        ClientInfo info = new ClientInfo(
                cc.client().info().name(),
                cc.client().info().displayName(),
                cc.client().info().version());
        EnumSet<ClientCapability> caps = cc.client().capabilities().isEmpty()
                ? EnumSet.noneOf(ClientCapability.class)
                : cc.client().capabilities().stream()
                .map(ClientCapability::valueOf)
                .collect(() -> EnumSet.noneOf(ClientCapability.class), EnumSet::add, EnumSet::addAll);

        McpClient client = new McpClient(
                info,
                caps,
                transport,
                samplingProvider,
                rootsProvider,
                null);
        client.connect();
        if (verbose) {
            client.setLoggingListener(n -> {
                String logger = n.logger() == null ? "" : ":" + n.logger();
                System.err.println(n.level().name().toLowerCase() + logger + " " + n.data());
            });
            try {
                client.setLogLevel(LoggingLevel.DEBUG);
            } catch (IOException e) {
                System.err.println("Failed to set log level: " + e.getMessage());
            }
        }
        client.ping();
        if (verbose) System.err.println("Ping OK");
        client.disconnect();
        return 0;
    }
}

