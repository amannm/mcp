package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.client.DefaultMcpClient;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.security.*;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.transport.StdioTransport;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "host", description = "Run MCP host", mixinStandardHelpOptions = true)
public final class HostCommand implements Callable<Integer> {
    @CommandLine.Option(names = {"-c", "--config"}, description = "Config file")
    private Path config;

    private HostConfig resolved;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose logging")
    private boolean verbose;

    @CommandLine.Option(names = "--client", description = "Client as id:command", split = ",")
    private List<String> clientSpecs;

    public HostCommand() {}

    public HostCommand(HostConfig config, boolean verbose) {
        this.resolved = config;
        this.verbose = verbose;
    }

    @Override
    public Integer call() throws Exception {
        HostConfig cfg;
        if (resolved != null) {
            cfg = resolved;
        } else if (config != null) {
            CliConfig loaded = ConfigLoader.load(config);
            if (!(loaded instanceof HostConfig hc)) throw new IllegalArgumentException("host config expected");
            cfg = hc;
        } else {
            if (clientSpecs == null || clientSpecs.isEmpty()) {
                throw new IllegalArgumentException("--client required");
            }
            Map<String, String> map = new LinkedHashMap<>();
            for (String spec : clientSpecs) {
                int idx = spec.indexOf(':');
                if (idx <= 0 || idx == spec.length() - 1) throw new IllegalArgumentException("id:command expected: " + spec);
                map.put(spec.substring(0, idx), spec.substring(idx + 1));
            }
            cfg = new HostConfig(map);
        }

        ConsentManager consents = new ConsentManager();
        ToolAccessController tools = new ToolAccessController();
        SecurityPolicy policy = c -> true;
        Principal principal = new Principal("user", Set.of());

        try (HostProcess host = new HostProcess(policy, consents, tools, principal)) {
            for (var entry : cfg.clients().entrySet()) {
                host.grantConsent(entry.getKey());
                var pb = new ProcessBuilder(entry.getValue().split(" "));
                StdioTransport t = new StdioTransport(pb, verbose ? System.err::println : s -> {});
                DefaultMcpClient client = new DefaultMcpClient(
                        new ClientInfo(entry.getKey(), entry.getKey(), "0"),
                        EnumSet.noneOf(ClientCapability.class),
                        t);
                host.register(entry.getKey(), client);
                if (verbose) System.err.println("Registered client: " + entry.getKey());
            }
            System.out.println(host.aggregateContext());
        }
        return 0;
    }
}
