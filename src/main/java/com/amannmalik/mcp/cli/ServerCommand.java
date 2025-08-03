package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.McpServer;
import com.amannmalik.mcp.auth.*;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.transport.*;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ServerCommand {
    public static CommandSpec createCommandSpec() {
        CommandSpec spec = CommandSpec.create()
                .name("server")
                .addOption(OptionSpec.builder("--http")
                        .type(Integer.class)
                        .description("HTTP port")
                        .build())
                .addOption(OptionSpec.builder("--stdio")
                        .type(boolean.class)
                        .description("Use stdio transport")
                        .build())
                .addOption(OptionSpec.builder("--instructions")
                        .type(Path.class)
                        .description("Instructions file")
                        .build())
                .addOption(OptionSpec.builder("-v", "--verbose")
                        .type(boolean.class)
                        .description("Verbose logging")
                        .build())
                .addOption(OptionSpec.builder("--audience")
                        .type(String.class)
                        .description("Expected JWT audience for authorization")
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
                        .description("Disable auth for testing")
                        .build());
        spec.usageMessage().description("Run MCP server");
        return spec;
    }

    public static int execute(ParseResult parseResult) {
        Integer helpExitCode = CommandLine.executeHelpRequest(parseResult);
        if (helpExitCode != null) return helpExitCode;

        try {
            Integer httpPort = parseResult.matchedOptionValue("--http", null);
            boolean stdio = parseResult.matchedOptionValue("--stdio", false);
            Path instructionsFile = parseResult.matchedOptionValue("--instructions", null);
            boolean verbose = parseResult.matchedOptionValue("--verbose", false);
            String expectedAudience = parseResult.matchedOptionValue("--audience", null);
            String resourceMetadataUrl = parseResult.matchedOptionValue("--resource-metadata", null);
            List<String> authServers = parseResult.matchedOptionValue("--auth-server", Collections.emptyList());
            boolean testMode = parseResult.matchedOptionValue("--test-mode", false);

            TransportType defType = parseTransport(McpConfiguration.current().server().transport().type());
            TransportType type = httpPort == null ? defType : TransportType.HTTP;
            int port = httpPort == null ? McpConfiguration.current().server().transport().port() : httpPort;
            if (stdio) type = TransportType.STDIO;
            List<String> auth = authServers;
            if (!testMode) {
                if (auth == null || auth.isEmpty()) throw new IllegalArgumentException("--auth-server is required");
            } else {
                auth = List.of();
            }

            Transport t;
            switch (type) {
                case STDIO -> t = new StdioTransport(System.in, System.out);
                case HTTP -> {
                    OriginValidator originValidator = new OriginValidator(
                            Set.copyOf(McpConfiguration.current().server().transport().allowedOrigins()));
                    AuthorizationManager authManager = null;
                    if (expectedAudience != null && !expectedAudience.isBlank()) {
                        String secretEnv = System.getenv("MCP_JWT_SECRET");
                        JwtTokenValidator tokenValidator = secretEnv == null || secretEnv.isBlank()
                                ? new JwtTokenValidator(expectedAudience)
                                : new JwtTokenValidator(expectedAudience, secretEnv.getBytes(StandardCharsets.UTF_8));
                        authManager = new AuthorizationManager(List.of(new BearerTokenAuthorizationStrategy(tokenValidator)));
                    }
                    StreamableHttpTransport ht = new StreamableHttpTransport(
                            port, originValidator, authManager,
                            resourceMetadataUrl, auth);
                    if (verbose) System.err.println("Listening on http://127.0.0.1:" + ht.port());
                    t = ht;
                }
                default -> throw new IllegalStateException();
            }

            String instructions = instructionsFile == null ? null : Files.readString(instructionsFile);

            try (McpServer server = new McpServer(t, instructions)) {
                server.serve();
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static TransportType parseTransport(String name) {
        return switch (name) {
            case "stdio" -> TransportType.STDIO;
            case "http" -> TransportType.HTTP;
            default -> throw new IllegalArgumentException("unknown transport: " + name);
        };
    }
}
