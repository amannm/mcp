package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.config.McpConfiguration;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

public final class ConfigCommand {
    public static CommandSpec createCommandSpec() {
        CommandSpec spec = CommandSpec.create()
                .name("config")
                .addOption(OptionSpec.builder("--reload")
                        .type(boolean.class)
                        .description("Reload configuration")
                        .build())
                .addOption(OptionSpec.builder("--watch")
                        .type(boolean.class)
                        .description("Watch for configuration changes")
                        .build());
        spec.usageMessage().description("Manage configuration");
        return spec;
    }
    
    public static int execute(ParseResult parseResult) {
        Integer helpExitCode = CommandLine.executeHelpRequest(parseResult);
        if (helpExitCode != null) return helpExitCode;
        
        try {
            boolean reload = parseResult.matchedOptionValue("--reload", false);
            boolean watch = parseResult.matchedOptionValue("--watch", false);
            
            if (reload) McpConfiguration.reload();
            System.out.println(McpConfiguration.current());
            if (watch) {
                McpConfiguration.addChangeListener(c -> System.out.println(c));
                Thread.sleep(Long.MAX_VALUE);
            }
            return 0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

