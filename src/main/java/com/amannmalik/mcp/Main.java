package com.amannmalik.mcp;

import com.amannmalik.mcp.cli.ClientCommand;
import com.amannmalik.mcp.cli.ServerCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "mcp", mixinStandardHelpOptions = true,
        subcommands = {ServerCommand.class, ClientCommand.class})
public final class Main implements Callable<Integer> {
    @Option(names = {"-c", "--config"}, description = "Configuration file",
            scope = CommandLine.ScopeType.INHERIT)
    private Path config;

    @Option(names = {"-v", "--verbose"}, description = "Verbose output",
            scope = CommandLine.ScopeType.INHERIT)
    private boolean verbose;

    public Path config() { return config; }
    public boolean verbose() { return verbose; }

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
