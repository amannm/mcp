package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.config.McpConfiguration;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;

public final class ConfigCommand {
    public static CommandSpec createCommandSpec() {
        CommandSpec spec = CommandSpec.create().name("config");
        spec.usageMessage().description("Show configuration");
        return spec;
    }

    public static int execute(ParseResult parseResult) {
        Integer helpExitCode = CommandLine.executeHelpRequest(parseResult);
        if (helpExitCode != null) return helpExitCode;

        try {
            System.out.println(McpConfiguration.current());
            return 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

