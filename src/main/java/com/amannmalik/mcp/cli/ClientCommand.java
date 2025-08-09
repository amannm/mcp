package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.McpClient;
import com.amannmalik.mcp.logging.LoggingLevel;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

import java.io.IOException;

/// - [Overview](specification/2025-06-18/index.mdx)
/// - [Client Features](specification/2025-06-18/client/index.mdx)
public final class ClientCommand {
    public static CommandSpec createCommandSpec() {
        CommandSpec spec = CommandSpec.create()
                .name("client")
                .addOption(OptionSpec.builder("--command")
                        .type(String.class)
                        .description("Server command for stdio")
                        .build())
                .addOption(OptionSpec.builder("-v", "--verbose")
                        .type(boolean.class)
                        .description("Verbose logging")
                        .build());
        spec.usageMessage().description("Run MCP client");
        return spec;
    }

    public static int execute(ParseResult parseResult) {
        Integer helpExitCode = CommandLine.executeHelpRequest(parseResult);
        if (helpExitCode != null) return helpExitCode;

        try {
            String command = parseResult.matchedOptionValue("--command", null);
            boolean verbose = parseResult.matchedOptionValue("--verbose", false);

            if (command == null) throw new IllegalArgumentException("command required");

            McpClient client = McpClient.forCli(command, verbose);
            client.connect();
            if (verbose) {
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

