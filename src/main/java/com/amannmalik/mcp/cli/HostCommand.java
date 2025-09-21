package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.api.config.*;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.util.PlatformLog;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

import java.io.*;
import java.lang.System.Logger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class HostCommand {
    private static final Logger LOG = PlatformLog.get(HostCommand.class);

    public HostCommand() {
    }

    public static CommandSpec createCommandSpec() {
        var spec = CommandSpec.create()
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
        var helpExitCode = CommandLine.executeHelpRequest(parseResult);
        if (helpExitCode != null) {
            return helpExitCode;
        }

        try {
            boolean verbose = parseResult.matchedOptionValue("--verbose", false);
            List<String> clientSpecs = parseResult.matchedOptionValue("--client", Collections.emptyList());
            boolean interactive = parseResult.matchedOptionValue("--interactive", false);
            if (clientSpecs.isEmpty()) {
                throw new IllegalArgumentException("--client required");
            }

            var tls = extractTlsSettings(parseResult);
            var clientConfigs = parseClientConfigs(clientSpecs, verbose, tls);
            var config = McpHostConfiguration.withClientConfigurations(clientConfigs);

            try (var host = new McpHost(config)) {
                for (var clientConfig : clientConfigs) {
                    host.connect(clientConfig.clientId());
                    if (verbose || clientConfig.verbose()) {
                        LOG.log(Logger.Level.INFO, "Registered client: " + clientConfig.clientId());
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

    private static TlsSettings extractTlsSettings(ParseResult parseResult) {
        Path truststorePathOpt = parseResult.matchedOptionValue("--client-truststore", null);
        var truststorePath = truststorePathOpt == null ? "" : truststorePathOpt.toString();
        var truststorePassword = parseResult.matchedOptionValue("--client-truststore-password", "");
        String truststorePasswordEnv = parseResult.matchedOptionValue("--client-truststore-password-env", null);
        if (truststorePasswordEnv != null) {
            var env = System.getenv(truststorePasswordEnv);
            if (env != null) {
                truststorePassword = env;
            }
        }
        var truststoreType = parseResult.matchedOptionValue("--client-truststore-type", "PKCS12");
        Path keystorePathOpt = parseResult.matchedOptionValue("--client-keystore", null);
        var keystorePath = keystorePathOpt == null ? "" : keystorePathOpt.toString();
        var keystorePassword = parseResult.matchedOptionValue("--client-keystore-password", "");
        String keystorePasswordEnv = parseResult.matchedOptionValue("--client-keystore-password-env", null);
        if (keystorePasswordEnv != null) {
            var env = System.getenv(keystorePasswordEnv);
            if (env != null) {
                keystorePassword = env;
            }
        }
        var keystoreType = parseResult.matchedOptionValue("--client-keystore-type", "PKCS12");
        boolean verifyCertificates = parseResult.matchedOptionValue("--verify-certificates", true);
        boolean allowSelfSigned = parseResult.matchedOptionValue("--allow-self-signed", false);
        var protocols = parseResult.matchedOptionValue("--tls-protocols", List.of("TLSv1.3", "TLSv1.2"));
        List<String> pins = parseResult.matchedOptionValue("--certificate-pinning", List.of());
        var mode = !pins.isEmpty() ? CertificateValidationMode.CUSTOM : (!verifyCertificates || allowSelfSigned ? CertificateValidationMode.PERMISSIVE : CertificateValidationMode.STRICT);
        var cipherSuites = List.of("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384");
        return new TlsSettings(
                truststorePath,
                truststorePassword,
                truststoreType,
                keystorePath,
                keystorePassword,
                keystoreType,
                mode,
                protocols,
                cipherSuites,
                pins,
                verifyCertificates
        );
    }

    private static List<McpClientConfiguration> parseClientConfigs(List<String> clientSpecs,
                                                                   boolean verbose,
                                                                   TlsSettings tls) {
        List<McpClientConfiguration> configs = new ArrayList<>();
        for (var spec : clientSpecs) {
            var parts = spec.split(":", -1);
            if (parts.length < 2) {
                throw new IllegalArgumentException("id:command expected: " + spec);
            }

            var clientId = parts[0];
            var command = parts[1];
            var clientVerbose = parts.length > 2 ? Boolean.parseBoolean(parts[2]) : verbose;

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

            var tlsConfig = new TlsConfiguration(
                    tls.keystorePath(),
                    tls.keystorePassword(),
                    tls.keystoreType(),
                    tls.truststorePath(),
                    tls.truststorePassword(),
                    tls.truststoreType(),
                    tls.protocols(),
                    tls.cipherSuites()
            );
            var config = new McpClientConfiguration(
                    clientId,
                    clientId,
                    clientId,
                    "1.0.0",
                    McpHostConfiguration.defaultConfiguration().hostPrincipal(),
                    capabilities,
                    command,
                    Duration.ofSeconds(10),
                    "http://127.0.0.1",
                    Duration.ofSeconds(30),
                    true,
                    32,
                    Duration.ofSeconds(30),
                    true,
                    Duration.ofMillis(5_000L),
                    Duration.ofMillis(0L),
                    20,
                    Duration.ofMillis(1_000L),
                    clientVerbose,
                    false,
                    List.of(System.getProperty("user.dir")),
                    SamplingAccessPolicy.PERMISSIVE,
                    tlsConfig,
                    tls.validationMode(),
                    tls.pins(),
                    tls.verifyHostname()
            );
            configs.add(config);
        }
        return configs;
    }

    private static void runInteractiveMode(McpHost host) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        System.out.println("MCP Host Interactive Mode. Type 'help' for commands, 'quit' to exit.");
        Map<String, AutoCloseable> resourceSubscriptions = new ConcurrentHashMap<>();

        try {
            while (true) {
                System.out.print("mcp> ");
                var line = reader.readLine();
                if (line == null || "quit".equals(line.trim())) {
                    break;
                }

                var parts = line.trim().split("\\s+");
                if (parts.length == 0) {
                    continue;
                }

                try {
                    switch (parts[0]) {
                        case "help" -> printHelp();
                        case "clients" -> System.out.println("Active clients: " + host.clientIds());
                        case "context" -> System.out.println(host.aggregateContext());
                        case "protocol-version" -> {
                            if (parts.length != 2) {
                                System.out.println("Usage: protocol-version <client-id>");
                            } else {
                                System.out.println(host.getProtocolVersion(parts[1]));
                            }
                        }
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
                                var token = parts.length > 2 ? parts[2] : null;
                                var cursor = token == null ? Cursor.Start.INSTANCE : new Cursor.Token(token);
                                var page = host.listTools(parts[1], cursor);
                                System.out.println(page);
                            }
                        }
                        case "list-resources" -> {
                            if (parts.length < 2) {
                                System.out.println("Usage: list-resources <client-id> [cursor]");
                            } else {
                                var token = parts.length > 2 ? parts[2] : null;
                                var cursor = token == null ? Cursor.Start.INSTANCE : new Cursor.Token(token);
                                var page = host.listResources(parts[1], cursor);
                                System.out.println(page);
                            }
                        }
                        case "list-resource-templates" -> {
                            if (parts.length < 2) {
                                System.out.println("Usage: list-resource-templates <client-id> [cursor]");
                            } else {
                                var token = parts.length > 2 ? parts[2] : null;
                                var cursor = token == null ? Cursor.Start.INSTANCE : new Cursor.Token(token);
                                var page = host.listResourceTemplates(parts[1], cursor);
                                System.out.println(page);
                            }
                        }
                        case "server-capabilities" -> {
                            if (parts.length != 2) {
                                System.out.println("Usage: server-capabilities <client-id>");
                            } else {
                                System.out.println(host.serverCapabilities(parts[1]));
                            }
                        }
                        case "server-capability-names" -> {
                            if (parts.length != 2) {
                                System.out.println("Usage: server-capability-names <client-id>");
                            } else {
                                System.out.println(host.getServerCapabilityNames(parts[1]));
                            }
                        }
                        case "server-features" -> {
                            if (parts.length != 2) {
                                System.out.println("Usage: server-features <client-id>");
                            } else {
                                System.out.println(host.serverFeatures(parts[1]));
                            }
                        }
                        case "server-info" -> {
                            if (parts.length != 2) {
                                System.out.println("Usage: server-info <client-id>");
                            } else {
                                System.out.println(host.getServerInfo(parts[1]));
                            }
                        }
                        case "server-info-map" -> {
                            if (parts.length != 2) {
                                System.out.println("Usage: server-info-map <client-id>");
                            } else {
                                System.out.println(host.getServerInfoMap(parts[1]));
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
                        case "subscribe-resource" -> {
                            if (parts.length != 3) {
                                System.out.println("Usage: subscribe-resource <client-id> <resource-uri>");
                            } else {
                                var clientId = parts[1];
                                URI uri;
                                try {
                                    uri = URI.create(parts[2]);
                                } catch (IllegalArgumentException e) {
                                    System.out.println("Invalid URI: " + parts[2]);
                                    break;
                                }
                                var key = subscriptionKey(clientId, uri);
                                if (resourceSubscriptions.containsKey(key)) {
                                    System.out.println("Already subscribed to resource: " + uri);
                                    break;
                                }
                                var subscription = host.subscribeToResource(clientId, uri, update -> {
                                    var title = update.title();
                                    var suffix = (title == null || title.isBlank()) ? "" : " (" + title + ")";
                                    System.out.println("Resource updated [" + clientId + "]: " + update.uri() + suffix);
                                });
                                resourceSubscriptions.put(key, subscription);
                                System.out.println("Subscribed to resource: " + uri);
                            }
                        }
                        case "unsubscribe-resource" -> {
                            if (parts.length != 3) {
                                System.out.println("Usage: unsubscribe-resource <client-id> <resource-uri>");
                            } else {
                                var clientId = parts[1];
                                URI uri;
                                try {
                                    uri = URI.create(parts[2]);
                                } catch (IllegalArgumentException e) {
                                    System.out.println("Invalid URI: " + parts[2]);
                                    break;
                                }
                                var key = subscriptionKey(clientId, uri);
                                var subscription = resourceSubscriptions.remove(key);
                                if (subscription == null) {
                                    System.out.println("No active subscription for: " + uri);
                                } else {
                                    try {
                                        subscription.close();
                                        System.out.println("Unsubscribed from resource: " + uri);
                                    } catch (Exception e) {
                                        System.out.println("Failed to unsubscribe: " + e.getMessage());
                                    }
                                }
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
                        case "set-log-level" -> {
                            if (parts.length != 3) {
                                System.out.println("Usage: set-log-level <client-id> <level>");
                            } else {
                                try {
                                    var level = LoggingLevel.valueOf(parts[2].toUpperCase());
                                    host.setClientLogLevel(parts[1], level);
                                    System.out.println("Set log level for " + parts[1] + " to " + level);
                                } catch (IllegalArgumentException e) {
                                    System.out.println("Invalid level. Valid values: " + Arrays.toString(LoggingLevel.values()));
                                }
                            }
                        }
                        case "allow-audience" -> {
                            if (parts.length != 2) {
                                System.out.println("Usage: allow-audience <audience>");
                            } else {
                                try {
                                    var audience = Role.valueOf(parts[1].toUpperCase());
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
                                    var audience = Role.valueOf(parts[1].toUpperCase());
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
        } finally {
            resourceSubscriptions.values().forEach(subscription -> {
                try {
                    subscription.close();
                } catch (Exception e) {
                    LOG.log(Logger.Level.WARNING, "Failed to close resource subscription", e);
                }
            });
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
                  protocol-version <client-id>     - Show negotiated protocol version
                  server-capabilities <client-id>  - Show server capabilities
                  server-capability-names <client-id> - Show server capability identifiers
                  server-features <client-id>      - Show server features
                  server-info <client-id>          - Show server info summary
                  server-info-map <client-id>      - Show server info as key/value map
                  list-resources <client-id> [cursor] - List resources from client
                  list-resource-templates <client-id> [cursor] - List resource templates from client
                  subscribe-resource <client-id> <resource-uri> - Subscribe to resource updates
                  unsubscribe-resource <client-id> <resource-uri> - Cancel a resource subscription
                  allow-audience <audience>         - Allow audience access
                  revoke-audience <audience>        - Revoke audience access
                  list-tools <client-id> [cursor]  - List tools from client
                  call-tool <client-id> <tool> [args] - Call tool (args as JSON)
                  create-message <client-id> <params> - Request sampling
                  set-log-level <client-id> <level> - Set client logging verbosity
                  request <client-id> <method> [params] - Send request to client
                  notify <client-id> <method> [params]  - Send notification to client
                  quit                              - Exit interactive mode
                """);
    }

    private static String subscriptionKey(String clientId, URI uri) {
        return clientId + "|" + uri;
    }

    private record TlsSettings(
            String truststorePath,
            String truststorePassword,
            String truststoreType,
            String keystorePath,
            String keystorePassword,
            String keystoreType,
            CertificateValidationMode validationMode,
            List<String> protocols,
            List<String> cipherSuites,
            List<String> pins,
            boolean verifyHostname
    ) {
    }
}
