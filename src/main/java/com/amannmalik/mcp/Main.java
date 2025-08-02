package com.amannmalik.mcp;

import com.amannmalik.mcp.cli.*;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;

public final class Main {
    public static void main(String[] args) {
        CommandSpec mainSpec = CommandSpec.create()
                .name("mcp")
                .mixinStandardHelpOptions(true);
        
        CommandSpec serverSpec = ServerCommand.createCommandSpec();
        CommandSpec clientSpec = ClientCommand.createCommandSpec();
        CommandSpec hostSpec = HostCommand.createCommandSpec();
        CommandSpec configSpec = ConfigCommand.createCommandSpec();
        
        mainSpec.addSubcommand("server", serverSpec);
        mainSpec.addSubcommand("client", clientSpec);
        mainSpec.addSubcommand("host", hostSpec);
        mainSpec.addSubcommand("config", configSpec);
        
        CommandLine commandLine = new CommandLine(mainSpec);
        commandLine.setExecutionStrategy(Main::execute);
        
        System.exit(commandLine.execute(args));
    }
    
    private static int execute(ParseResult parseResult) {
        Integer helpExitCode = CommandLine.executeHelpRequest(parseResult);
        if (helpExitCode != null) return helpExitCode;
        
        if (parseResult.hasSubcommand()) {
            ParseResult subResult = parseResult.subcommand();
            String subcommandName = subResult.commandSpec().name();
            
            return switch (subcommandName) {
                case "server" -> ServerCommand.execute(subResult);
                case "client" -> ClientCommand.execute(subResult);
                case "host" -> HostCommand.execute(subResult);
                case "config" -> ConfigCommand.execute(subResult);
                default -> throw new IllegalStateException("Unknown subcommand: " + subcommandName);
            };
        }
        
        CommandLine.usage(parseResult.commandSpec(), System.out);
        return 0;
    }
}
