package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.util.ServerDefaults;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/// - [Server](specification/2025-06-18/server/index.mdx)
/// - [Conformance Suite](src/test/resources/com/amannmalik/mcp/mcp.feature)
public final class ServerCommand {
    public ServerCommand() {
    }

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
            boolean stdio = parseResult.matchedOptionValue("--stdio", false);
            boolean verbose = parseResult.matchedOptionValue("--verbose", false);
            Transport transport;
            if (stdio) {
                transport = TransportFactory.createStdioTransport(new String[0], verbose);
            } else {
                Integer httpPort = parseResult.matchedOptionValue("--http", 3000);
                String expectedAudience = parseResult.matchedOptionValue("--audience", null);
                String resourceMetadataUrl = parseResult.matchedOptionValue("--resource-metadata", null);
                List<String> authServers = parseResult.matchedOptionValue("--auth-server", Collections.emptyList());
                boolean testMode = parseResult.matchedOptionValue("--test-mode", false);
                transport = TransportFactory.createHttpTransport(httpPort, expectedAudience, resourceMetadataUrl, authServers, testMode, verbose);
            }
            Path instructionsFile = parseResult.matchedOptionValue("--instructions", null);
            String instructions = instructionsFile == null ? null : Files.readString(instructionsFile);
            try (McpServer server = new McpServer(McpServerConfiguration.defaultConfiguration(), ServerDefaults.resources(),
                    ServerDefaults.tools(),
                    ServerDefaults.prompts(),
                    ServerDefaults.completions(),
                    ServerDefaults.sampling(),
                    ServerDefaults.privacyBoundary(McpServerConfiguration.defaultConfiguration().defaultBoundary()),
                    ServerDefaults.toolAccess(),
                    ServerDefaults.samplingAccess(),
                    ServerDefaults.principal(),
                    instructions,
                    transport)) {
                server.serve();
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
