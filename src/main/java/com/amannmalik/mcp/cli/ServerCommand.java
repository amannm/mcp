package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.util.ServiceLoaders;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/// - [Server](specification/2025-06-18/server/index.mdx)
/// - [Conformance Suite](src/test/resources/com/amannmalik/mcp/mcp.feature)
public final class ServerCommand {
    public ServerCommand() {
    }

    public static CommandSpec createCommandSpec() {
        var spec = CommandSpec.create()
                .name("server")
                .addOption(OptionSpec.builder("--http")
                        .type(Integer.class)
                        .description("HTTP port")
                        .build())
                .addOption(OptionSpec.builder("--stdio")
                        .type(boolean.class)
                        .arity("0")
                        .defaultValue("false")
                        .description("Use stdio transport")
                        .build())
                .addOption(OptionSpec.builder("--instructions")
                        .type(Path.class)
                        .description("Instructions file")
                        .build())
                .addOption(OptionSpec.builder("-v", "--verbose")
                        .type(boolean.class)
                        .arity("0")
                        .defaultValue("false")
                        .description("Verbose logging")
                        .build())
                .addOption(OptionSpec.builder("--audience")
                        .type(String.class)
                        .description("Expected JWT audience for authorization")
                        .build())
                .addOption(OptionSpec.builder("--jwt-secret")
                        .type(String.class)
                        .description("JWT HMAC secret (leave blank to use public key validation)")
                        .build())
                .addOption(OptionSpec.builder("--resource-metadata")
                        .type(String.class)
                        .description("Resource metadata URL")
                        .build())
                .addOption(OptionSpec.builder("--auth-server")
                        .type(List.class)
                        .auxiliaryTypes(String.class)
                        .splitRegex(",")
                        .description("Authorization server URL")
                        .build())
                .addOption(OptionSpec.builder("--test-mode")
                        .type(boolean.class)
                        .arity("0")
                        .defaultValue("false")
                        .description("Disable auth for testing")
                        .build())
                .addOption(OptionSpec.builder("--https-port")
                        .type(Integer.class)
                        .description("HTTPS port")
                        .build())
                .addOption(OptionSpec.builder("--keystore")
                        .type(Path.class)
                        .description("Keystore path")
                        .build())
                .addOption(OptionSpec.builder("--keystore-password")
                        .type(String.class)
                        .description("Keystore password")
                        .build())
                .addOption(OptionSpec.builder("--keystore-password-env")
                        .type(String.class)
                        .description("Env var with keystore password")
                        .build())
                .addOption(OptionSpec.builder("--keystore-type")
                        .type(String.class)
                        .description("Keystore type")
                        .build())
                .addOption(OptionSpec.builder("--truststore")
                        .type(Path.class)
                        .description("Truststore path")
                        .build())
                .addOption(OptionSpec.builder("--truststore-password")
                        .type(String.class)
                        .description("Truststore password")
                        .build())
                .addOption(OptionSpec.builder("--truststore-password-env")
                        .type(String.class)
                        .description("Env var with truststore password")
                        .build())
                .addOption(OptionSpec.builder("--truststore-type")
                        .type(String.class)
                        .description("Truststore type")
                        .build())
                .addOption(OptionSpec.builder("--tls-protocols")
                        .type(List.class)
                        .auxiliaryTypes(String.class)
                        .splitRegex(",")
                        .description("Allowed TLS protocols")
                        .build())
                .addOption(OptionSpec.builder("--cipher-suites")
                        .type(List.class)
                        .auxiliaryTypes(String.class)
                        .splitRegex(",")
                        .description("Allowed cipher suites")
                        .build())
                .addOption(OptionSpec.builder("--require-client-auth")
                        .type(boolean.class)
                        .arity("0")
                        .defaultValue("false")
                        .description("Require client certificates")
                        .build())
                .addOption(OptionSpec.builder("--https-only")
                        .type(boolean.class)
                        .arity("0")
                        .defaultValue("false")
                        .description("Disable HTTP and enforce HTTPS")
                        .build());
        spec.usageMessage().description(
                "Run MCP server",
                "TLS example:",
                "  --https-port 3443 --keystore server.p12 --keystore-password-env KS_PASS --https-only");
        return spec;
    }

    public static int execute(ParseResult parseResult) {
        var helpExitCode = CommandLine.executeHelpRequest(parseResult);
        if (helpExitCode != null) {
            return helpExitCode;
        }

        try {
            boolean stdio = parseResult.matchedOptionValue("--stdio", false);
            boolean verbose = parseResult.matchedOptionValue("--verbose", false);
            var httpPort = parseResult.matchedOptionValue("--http", 3000);
            boolean httpsOnly = parseResult.matchedOptionValue("--https-only", false);
            if (httpsOnly) {
                httpPort = 0;
            }
            String expectedAudience = parseResult.matchedOptionValue("--audience", null);
            String jwtSecret = parseResult.matchedOptionValue("--jwt-secret", "");
            String resourceMetadataUrl = parseResult.matchedOptionValue("--resource-metadata", null);
            List<String> authServers = parseResult.matchedOptionValue("--auth-server", Collections.emptyList());
            boolean testMode = parseResult.matchedOptionValue("--test-mode", false);
            var base = McpServerConfiguration.defaultConfiguration();
            int httpsPort = parseResult.matchedOptionValue("--https-port", base.httpsPort());
            var keystorePathOpt = parseResult.matchedOptionValue("--keystore", Path.of(base.keystorePath()));
            var keystorePath = keystorePathOpt.toString();
            var keystorePassword = parseResult.matchedOptionValue("--keystore-password", base.keystorePassword());
            String keystorePasswordEnv = parseResult.matchedOptionValue("--keystore-password-env", null);
            if (keystorePasswordEnv != null) {
                var env = System.getenv(keystorePasswordEnv);
                if (env != null) {
                    keystorePassword = env;
                }
            }
            var keystoreType = parseResult.matchedOptionValue("--keystore-type", base.keystoreType());
            Path truststorePathOpt = parseResult.matchedOptionValue("--truststore", null);
            var truststorePath = truststorePathOpt == null ? base.truststorePath() : truststorePathOpt.toString();
            var truststorePassword = parseResult.matchedOptionValue("--truststore-password", base.truststorePassword());
            String truststorePasswordEnv = parseResult.matchedOptionValue("--truststore-password-env", null);
            if (truststorePasswordEnv != null) {
                var env = System.getenv(truststorePasswordEnv);
                if (env != null) {
                    truststorePassword = env;
                }
            }
            var truststoreType = parseResult.matchedOptionValue("--truststore-type", base.truststoreType());
            var tlsProtocols = parseResult.matchedOptionValue("--tls-protocols", base.tlsProtocols());
            var cipherSuites = parseResult.matchedOptionValue("--cipher-suites", base.cipherSuites());
            boolean requireClientAuth = parseResult.matchedOptionValue("--require-client-auth", base.requireClientAuth());
            var config = stdio
                    ? base.withTransport("stdio", base.serverPort(), base.allowedOrigins(), "", "", null, List.of(), true, verbose)
                    : base.withTransport("http", httpPort, base.allowedOrigins(),
                    expectedAudience == null ? "" : expectedAudience,
                    jwtSecret,
                    resourceMetadataUrl,
                    authServers,
                    testMode,
                    verbose);
            var tlsConfig = new TlsConfiguration(keystorePath, keystorePassword, keystoreType, truststorePath, truststorePassword, truststoreType, tlsProtocols, cipherSuites);
            config = config.withTls(httpsPort, tlsConfig, requireClientAuth);
            Path instructionsFile = parseResult.matchedOptionValue("--instructions", null);
            var instructions = instructionsFile == null ? null : Files.readString(instructionsFile);
            var resources = ServiceLoaders.loadSingleton(ResourceProvider.class);
            var tools = ServiceLoaders.loadSingleton(ToolProvider.class);
            var prompts = ServiceLoaders.loadSingleton(PromptProvider.class);
            var completions = ServiceLoaders.loadSingleton(CompletionProvider.class);
            var sampling = ServiceLoaders.loadSingleton(SamplingProvider.class);
            var toolAccess = ServiceLoaders.loadSingleton(ToolAccessPolicy.class);
            var samplingAccessPolicy = ServiceLoaders.loadSingleton(SamplingAccessPolicy.class);
            try (var server = new McpServer(config, resources,
                    tools,
                    prompts,
                    completions,
                    sampling,
                    privacyBoundary(config.defaultBoundary()),
                    toolAccess,
                    samplingAccessPolicy,
                    defaultPrincipal(),
                    instructions)) {
                server.serve();
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ResourceAccessPolicy privacyBoundary(String principalId) {
        var controller = ServiceLoaders.loadSingleton(ResourceAccessPolicy.class);
        for (var role : Role.values()) {
            controller.allow(principalId, role);
        }
        return controller;
    }

    private static Principal defaultPrincipal() {
        return new Principal(McpServerConfiguration.defaultConfiguration().defaultPrincipal(), Set.of());
    }

}
