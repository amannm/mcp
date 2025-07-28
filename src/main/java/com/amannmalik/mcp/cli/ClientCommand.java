package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.client.SimpleMcpClient;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.transport.StdioTransport;
import picocli.CommandLine;

import jakarta.json.Json;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "client", description = "Run MCP client", mixinStandardHelpOptions = true)
public final class ClientCommand implements Callable<Integer> {
    @CommandLine.Option(names = {"-c", "--config"}, description = "Config file")
    private Path config;

    @CommandLine.Option(names = "--command", description = "Server command for stdio")
    private String command;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose logging")
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        ClientConfig cfg;
        if (config != null) {
            CliConfig loaded = ConfigLoader.load(config);
            if (!(loaded instanceof ClientConfig cc)) throw new IllegalArgumentException("client config expected");
            cfg = cc;
        } else {
            if (command == null) throw new IllegalArgumentException("command required");
            cfg = new ClientConfig(TransportType.STDIO, command);
        }

        StdioTransport transport = new StdioTransport(new ProcessBuilder(cfg.command().split(" ")),
                verbose ? System.err::println : s -> {});
        SimpleMcpClient client = new SimpleMcpClient(
                new ClientInfo("cli", "CLI", "0"),
                EnumSet.noneOf(ClientCapability.class),
                transport);
        client.connect();
        JsonRpcMessage msg = client.request("ping", Json.createObjectBuilder().build());
        if (verbose && msg instanceof JsonRpcResponse) {
            System.err.println("Ping OK");
        }
        client.disconnect();
        return 0;
    }
}
