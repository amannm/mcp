package com.amannmalik.mcp.cli;

import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.*;

/// - [Overview](specification/2025-06-18/index.mdx)
/// - [Architecture](specification/2025-06-18/architecture/index.mdx)
public final class Entrypoint {
    public static void main(String[] args) {
        CommandSpec mainSpec = CommandSpec.create()
                .name("mcp");

        CommandLine commandLine = new CommandLine(mainSpec);
        commandLine.addSubcommand("server", ServerCommand.createCommandSpec());
        commandLine.addSubcommand("host", HostCommand.createCommandSpec());

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

    public Entrypoint() {
    }

    private static int execute(ParseResult parseResult) {
        if (parseResult.hasSubcommand()) {
            ParseResult subResult = parseResult.subcommand();
            String name = subResult.commandSpec().name();
            return switch (name) {
                case "server" -> ServerCommand.execute(subResult);
                case "host" -> HostCommand.execute(subResult);
                default -> throw new IllegalStateException("Unknown subcommand: " + name);
            };
        }

        CommandLine.usage(parseResult.commandSpec(), System.out);
        return 0;
    }
}
