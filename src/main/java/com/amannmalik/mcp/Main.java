package com.amannmalik.mcp;

import com.amannmalik.mcp.cli.*;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.*;

public final class Main {
    public static void main(String[] args) {
        CommandSpec mainSpec = CommandSpec.create()
                .name("mcp");

        CommandLine commandLine = new CommandLine(mainSpec);
        commandLine.addSubcommand("server", ServerCommand.createCommandSpec());
        commandLine.addSubcommand("client", ClientCommand.createCommandSpec());
        commandLine.addSubcommand("host", HostCommand.createCommandSpec());
        commandLine.addSubcommand("config", ConfigCommand.createCommandSpec());

        try {
            ParseResult parseResult = commandLine.parseArgs(args);
            Integer helpExitCode = CommandLine.executeHelpRequest(parseResult);
            if (helpExitCode != null) System.exit(helpExitCode);
            System.exit(execute(parseResult));
        } catch (ParameterException e) {
            CommandLine cmd = e.getCommandLine();
            cmd.getErr().println(e.getMessage());
            if (!UnmatchedArgumentException.printSuggestions(e, cmd.getErr())) cmd.usage(cmd.getErr());
            System.exit(cmd.getCommandSpec().exitCodeOnInvalidInput());
        } catch (Exception e) {
            commandLine.getErr().println("ERROR: " + e.getMessage());
            System.exit(commandLine.getCommandSpec().exitCodeOnExecutionException());
        }
    }

    private static int execute(ParseResult parseResult) {
        if (parseResult.hasSubcommand()) {
            ParseResult subResult = parseResult.subcommand();
            String name = subResult.commandSpec().name();
            return switch (name) {
                case "server" -> ServerCommand.execute(subResult);
                case "client" -> ClientCommand.execute(subResult);
                case "host" -> HostCommand.execute(subResult);
                case "config" -> ConfigCommand.execute(subResult);
                default -> throw new IllegalStateException("Unknown subcommand: " + name);
            };
        }

        CommandLine.usage(parseResult.commandSpec(), System.out);
        return 0;
    }
}
