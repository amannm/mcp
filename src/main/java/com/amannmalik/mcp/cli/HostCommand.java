package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.client.McpClient;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.security.ConsentManager;
import com.amannmalik.mcp.security.HostProcess;
import com.amannmalik.mcp.security.PrivacyBoundaryEnforcer;
import com.amannmalik.mcp.security.SecurityPolicy;
import com.amannmalik.mcp.security.ToolAccessController;
import com.amannmalik.mcp.security.SamplingAccessController;
import com.amannmalik.mcp.transport.StdioTransport;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @CommandLine.Option(names = "--interactive", description = "Interactive mode for client management")
    private boolean interactive;

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
        PrivacyBoundaryEnforcer privacyBoundary = new PrivacyBoundaryEnforcer();
        SamplingAccessController sampling = new SamplingAccessController();
        SecurityPolicy policy = c -> true;
        Principal principal = new Principal("user", Set.of());

        try (HostProcess host = new HostProcess(policy, consents, tools, privacyBoundary, sampling, principal)) {
            for (var entry : cfg.clients().entrySet()) {
                host.grantConsent(entry.getKey());
                var pb = new ProcessBuilder(entry.getValue().split(" "));
                StdioTransport t = new StdioTransport(pb, verbose ? System.err::println : s -> {
                });
                McpClient client = new McpClient(
                        new ClientInfo(entry.getKey(), entry.getKey(), "0"),
                        EnumSet.noneOf(ClientCapability.class),
                        t);
                host.register(entry.getKey(), client);
                if (verbose) System.err.println("Registered client: " + entry.getKey());
            }

            if (interactive) {
                runInteractiveMode(host);
            } else {
                System.out.println(host.aggregateContext());
            }
        }
        return 0;
    }

    private void runInteractiveMode(HostProcess host) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("MCP Host Interactive Mode. Type 'help' for commands, 'quit' to exit.");

        while (true) {
            System.out.print("mcp> ");
            String line = reader.readLine();
            if (line == null || "quit".equals(line.trim())) {
                break;
            }

            String[] parts = line.trim().split("\\s+");
            if (parts.length == 0) continue;

            try {
                switch (parts[0]) {
                    case "help" -> printHelp();
                    case "clients" -> System.out.println("Active clients: " + host.clientIds());
                    case "context" -> System.out.println(host.aggregateContext());
                    case "grant-consent" -> {
                        if (parts.length != 2) {
                            System.out.println("Usage: grant-consent <scope>");
                        } else {
                            host.grantConsent(parts[1]);
                            System.out.println("Granted consent for: " + parts[1]);
                        }
                    }
                    case "revoke-consent" -> {
                        if (parts.length != 2) {
                            System.out.println("Usage: revoke-consent <scope>");
                        } else {
                            host.revokeConsent(parts[1]);
                            System.out.println("Revoked consent for: " + parts[1]);
                        }
                    }
                    case "allow-tool" -> {
                        if (parts.length != 2) {
                            System.out.println("Usage: allow-tool <tool>");
                        } else {
                            host.allowTool(parts[1]);
                            System.out.println("Allowed tool: " + parts[1]);
                        }
                    }
                    case "revoke-tool" -> {
                        if (parts.length != 2) {
                            System.out.println("Usage: revoke-tool <tool>");
                        } else {
                            host.revokeTool(parts[1]);
                            System.out.println("Revoked tool: " + parts[1]);
                        }
                    }
                    case "allow-sampling" -> {
                        host.allowSampling();
                        System.out.println("Sampling allowed");
                    }
                    case "revoke-sampling" -> {
                        host.revokeSampling();
                        System.out.println("Sampling revoked");
                    }
                    case "list-tools" -> {
                        if (parts.length < 2) {
                            System.out.println("Usage: list-tools <client-id> [cursor]");
                        } else {
                            String cursor = parts.length > 2 ? parts[2] : null;
                            System.out.println(host.listTools(parts[1], cursor));
                        }
                    }
                    case "call-tool" -> {
                        if (parts.length < 3) {
                            System.out.println("Usage: call-tool <client-id> <tool-name> [json-args]");
                        } else {
                            var args = parts.length > 3 ?
                                    jakarta.json.Json.createReader(new java.io.StringReader(parts[3])).readObject() :
                                    null;
                            System.out.println(host.callTool(parts[1], parts[2], args));
                        }
                    }
                    case "allow-audience" -> {
                        if (parts.length != 2) {
                            System.out.println("Usage: allow-audience <audience>");
                        } else {
                            try {
                                Role audience = Role.valueOf(parts[1].toUpperCase());
                                host.allowAudience(audience);
                                System.out.println("Allowed audience: " + audience);
                            } catch (IllegalArgumentException e) {
                                System.out.println("Invalid audience. Valid values: " + java.util.Arrays.toString(Role.values()));
                            }
                        }
                    }
                    case "revoke-audience" -> {
                        if (parts.length != 2) {
                            System.out.println("Usage: revoke-audience <audience>");
                        } else {
                            try {
                                Role audience = Role.valueOf(parts[1].toUpperCase());
                                host.revokeAudience(audience);
                                System.out.println("Revoked audience: " + audience);
                            } catch (IllegalArgumentException e) {
                                System.out.println("Invalid audience. Valid values: " + java.util.Arrays.toString(Role.values()));
                            }
                        }
                    }
                    case "request" -> {
                        if (parts.length < 3) {
                            System.out.println("Usage: request <client-id> <method> [json-params]");
                        } else {
                            var params = parts.length > 3 ?
                                    jakarta.json.Json.createReader(new java.io.StringReader(parts[3])).readObject() :
                                    jakarta.json.Json.createObjectBuilder().build();
                            System.out.println(host.request(parts[1], parts[2], params));
                        }
                    }
                    case "notify" -> {
                        if (parts.length < 3) {
                            System.out.println("Usage: notify <client-id> <method> [json-params]");
                        } else {
                            var params = parts.length > 3 ?
                                    jakarta.json.Json.createReader(new java.io.StringReader(parts[3])).readObject() :
                                    jakarta.json.Json.createObjectBuilder().build();
                            host.notify(parts[1], parts[2], params);
                            System.out.println("Notification sent");
                        }
                    }
                    default -> System.out.println("Unknown command: " + parts[0] + ". Type 'help' for available commands.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void printHelp() {
        System.out.println("""
                Available commands:
                  help                              - Show this help
                  clients                           - List active client IDs
                  context                           - Show aggregated context
                  grant-consent <scope>             - Grant consent for scope
                  revoke-consent <scope>            - Revoke consent for scope
                  allow-tool <tool>                 - Allow tool access
                  revoke-tool <tool>                - Revoke tool access
                  allow-sampling                   - Allow sampling requests
                  revoke-sampling                  - Revoke sampling requests
                  allow-audience <audience>         - Allow audience access
                  revoke-audience <audience>        - Revoke audience access
                  list-tools <client-id> [cursor]  - List tools from client
                  call-tool <client-id> <tool> [args] - Call tool (args as JSON)
                  request <client-id> <method> [params] - Send request to client
                  notify <client-id> <method> [params]  - Send notification to client
                  quit                              - Exit interactive mode
                """);
    }
}
