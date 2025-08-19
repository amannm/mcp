package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.spi.*;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class HostCommand {
    public HostCommand() {
    }

    public static CommandSpec createCommandSpec() {
        CommandSpec spec = CommandSpec.create()
                .name("host")
                .addOption(OptionSpec.builder("-v", "--verbose")
                        .type(boolean.class)
                        .description("Verbose logging")
                        .build())
                .addOption(OptionSpec.builder("--client")
                        .type(List.class)
                        .auxiliaryTypes(String.class)
                        .splitRegex(",")
                        .description("Client as id:command or id:command:verbose:capabilities")
                        .build())
                .addOption(OptionSpec.builder("--interactive")
                        .type(boolean.class)
                        .description("Interactive mode for client management")
                        .build())
                .addOption(OptionSpec.builder("--client-truststore")
                        .type(Path.class)
                        .description("Client truststore path")
                        .build())
                .addOption(OptionSpec.builder("--client-truststore-password")
                        .type(String.class)
                        .description("Client truststore password")
                        .build())
                .addOption(OptionSpec.builder("--client-truststore-password-env")
                        .type(String.class)
                        .description("Env var with client truststore password")
                        .build())
                .addOption(OptionSpec.builder("--client-truststore-type")
                        .type(String.class)
                        .description("Client truststore type")
                        .build())
                .addOption(OptionSpec.builder("--client-keystore")
                        .type(Path.class)
                        .description("Client keystore path")
                        .build())
                .addOption(OptionSpec.builder("--client-keystore-password")
                        .type(String.class)
                        .description("Client keystore password")
                        .build())
                .addOption(OptionSpec.builder("--client-keystore-password-env")
                        .type(String.class)
                        .description("Env var with client keystore password")
                        .build())
                .addOption(OptionSpec.builder("--client-keystore-type")
                        .type(String.class)
                        .description("Client keystore type")
                        .build())
                .addOption(OptionSpec.builder("--verify-certificates")
                        .type(boolean.class)
                        .arity("0..1")
                        .defaultValue("true")
                        .description("Verify server certificates")
                        .build())
                .addOption(OptionSpec.builder("--allow-self-signed")
                        .type(boolean.class)
                        .arity("0")
                        .defaultValue("false")
                        .description("Allow self-signed certificates")
                        .build())
                .addOption(OptionSpec.builder("--tls-protocols")
                        .type(List.class)
                        .auxiliaryTypes(String.class)
                        .splitRegex(",")
                        .description("Allowed TLS protocols")
                        .build())
                .addOption(OptionSpec.builder("--certificate-pinning")
                        .type(List.class)
                        .auxiliaryTypes(String.class)
                        .splitRegex(",")
                        .description("Certificate pins")
                        .build());
        spec.usageMessage().description("Run MCP host");
        return spec;
    }

    public static int execute(ParseResult parseResult) {
        Integer helpExitCode = CommandLine.executeHelpRequest(parseResult);
        if (helpExitCode != null) return helpExitCode;

        try {
            boolean verbose = parseResult.matchedOptionValue("--verbose", false);
            List<String> clientSpecs = parseResult.matchedOptionValue("--client", Collections.emptyList());
            boolean interactive = parseResult.matchedOptionValue("--interactive", false);

            McpClientTlsConfiguration baseTls = McpClientTlsConfiguration.defaultConfiguration();
            Path truststorePathOpt = parseResult.matchedOptionValue("--client-truststore", null);
            String truststorePath = truststorePathOpt == null ? baseTls.truststorePath() : truststorePathOpt.toString();
            String truststorePassword = parseResult.matchedOptionValue("--client-truststore-password", baseTls.truststorePassword());
            String truststorePasswordEnv = parseResult.matchedOptionValue("--client-truststore-password-env", null);
            if (truststorePasswordEnv != null) {
                String env = System.getenv(truststorePasswordEnv);
                if (env != null) truststorePassword = env;
            }
            String truststoreType = parseResult.matchedOptionValue("--client-truststore-type", baseTls.truststoreType());
            Path keystorePathOpt = parseResult.matchedOptionValue("--client-keystore", null);
            String keystorePath = keystorePathOpt == null ? baseTls.keystorePath() : keystorePathOpt.toString();
            String keystorePassword = parseResult.matchedOptionValue("--client-keystore-password", baseTls.keystorePassword());
            String keystorePasswordEnv = parseResult.matchedOptionValue("--client-keystore-password-env", null);
            if (keystorePasswordEnv != null) {
                String env = System.getenv(keystorePasswordEnv);
                if (env != null) keystorePassword = env;
            }
            String keystoreType = parseResult.matchedOptionValue("--client-keystore-type", baseTls.keystoreType());
            boolean verifyCertificates = parseResult.matchedOptionValue("--verify-certificates", true);
            boolean allowSelfSigned = parseResult.matchedOptionValue("--allow-self-signed", false);
            List<String> tlsProtocols = parseResult.matchedOptionValue("--tls-protocols", baseTls.tlsProtocols());
            List<String> certificatePins = parseResult.matchedOptionValue("--certificate-pinning", baseTls.certificatePins());
            CertificateValidationMode validationMode = !certificatePins.isEmpty() ? CertificateValidationMode.CUSTOM : (!verifyCertificates || allowSelfSigned ? CertificateValidationMode.PERMISSIVE : CertificateValidationMode.STRICT);
            boolean verifyHostname = verifyCertificates;
            McpClientTlsConfiguration tlsConfig = new McpClientTlsConfiguration(
                    truststorePath,
                    truststorePassword,
                    truststoreType,
                    keystorePath,
                    keystorePassword,
                    keystoreType,
                    validationMode,
                    tlsProtocols,
                    baseTls.cipherSuites(),
                    certificatePins,
                    verifyHostname
            );

            if (clientSpecs.isEmpty()) throw new IllegalArgumentException("--client required");

            List<McpClientConfiguration> clientConfigs = new ArrayList<>();
            for (String spec : clientSpecs) {
                String[] parts = spec.split(":", -1);
                if (parts.length < 2) {
                    throw new IllegalArgumentException("id:command expected: " + spec);
                }

                String clientId = parts[0];
                String command = parts[1];
                boolean clientVerbose = parts.length > 2 ? Boolean.parseBoolean(parts[2]) : verbose;

                Set<ClientCapability> capabilities = EnumSet.of(
                        ClientCapability.SAMPLING,
                        ClientCapability.ROOTS,
                        ClientCapability.ELICITATION);

                if (parts.length > 3 && !parts[3].isEmpty()) {
                    capabilities = Arrays.stream(parts[3].split(","))
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .map(ClientCapability::valueOf)
                            .collect(Collectors.toCollection(() -> EnumSet.noneOf(ClientCapability.class)));
                }

                McpClientConfiguration clientConfig = new McpClientConfiguration(
                        clientId,
                        clientId,
                        clientId,
                        "1.0.0",
                        McpHostConfiguration.defaultConfiguration().hostPrincipal(),
                        capabilities,
                        command,
                        java.time.Duration.ofSeconds(10),
                        "http://127.0.0.1",
                        java.time.Duration.ofSeconds(30),
                        true,
                        32,
                        java.time.Duration.ofSeconds(30),
                        true,
                        java.time.Duration.ofMillis(5_000L),
                        java.time.Duration.ofMillis(0L),
                        20,
                        java.time.Duration.ofMillis(1_000L),
                        clientVerbose,
                        false,
                        List.of(System.getProperty("user.dir")),
                        SamplingAccessPolicy.PERMISSIVE,
                        tlsConfig
                );
                clientConfigs.add(clientConfig);
            }

            McpHostConfiguration config = McpHostConfiguration.withClientConfigurations(clientConfigs);

            try (McpHost host = new McpHost(config)) {
                for (McpClientConfiguration clientConfig : clientConfigs) {
                    host.connect(clientConfig.clientId());
                    if (verbose || clientConfig.verbose()) {
                        System.err.println("Registered client: " + clientConfig.clientId());
                    }
                }
                if (interactive) {
                    runInteractiveMode(host);
                } else {
                    System.out.println(host.aggregateContext());
                }
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void runInteractiveMode(McpHost host) throws IOException {
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
                            String token = parts.length > 2 ? parts[2] : null;
                            Cursor cursor = token == null ? Cursor.Start.INSTANCE : new Cursor.Token(token);
                            var page = host.listTools(parts[1], cursor);
                            System.out.println(page);
                        }
                    }
                    case "call-tool" -> {
                        if (parts.length < 3) {
                            System.out.println("Usage: call-tool <client-id> <tool-name> [json-args]");
                        } else {
                            var args = parts.length > 3 ?
                                    Json.createReader(new StringReader(parts[3])).readObject() :
                                    null;
                            var result = host.callTool(parts[1], parts[2], args);
                            System.out.println(result);
                        }
                    }
                    case "create-message" -> {
                        if (parts.length < 3) {
                            System.out.println("Usage: create-message <client-id> <json-params>");
                        } else {
                            var params = Json.createReader(new StringReader(parts[2])).readObject();
                            System.out.println(host.createMessage(parts[1], params));
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
                                System.out.println("Invalid audience. Valid values: " + Arrays.toString(Role.values()));
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
                                System.out.println("Invalid audience. Valid values: " + Arrays.toString(Role.values()));
                            }
                        }
                    }
                    case "request" -> {
                        if (parts.length < 3) {
                            System.out.println("Usage: request <client-id> <method> [json-params]");
                        } else {
                            var params = parts.length > 3 ?
                                    Json.createReader(new StringReader(parts[3])).readObject() :
                                    JsonValue.EMPTY_JSON_OBJECT;
                            System.out.println(host.request(parts[1], RequestMethod.from(parts[2]).orElseThrow(), params));
                        }
                    }
                    case "notify" -> {
                        if (parts.length < 3) {
                            System.out.println("Usage: notify <client-id> <method> [json-params]");
                        } else {
                            var params = parts.length > 3 ?
                                    Json.createReader(new StringReader(parts[3])).readObject() :
                                    JsonValue.EMPTY_JSON_OBJECT;
                            host.notify(parts[1], NotificationMethod.from(parts[2]).orElseThrow(), params);
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

    private static void printHelp() {
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
                  create-message <client-id> <params> - Request sampling
                  request <client-id> <method> [params] - Send request to client
                  notify <client-id> <method> [params]  - Send notification to client
                  quit                              - Exit interactive mode
                """);
    }
}
