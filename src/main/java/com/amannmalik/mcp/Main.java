package com.amannmalik.mcp;

import com.amannmalik.mcp.cli.*;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "mcp",
        subcommands = {ServerCommand.class, ClientCommand.class, HostCommand.class, ConfigCommand.class},
        mixinStandardHelpOptions = true)
public final class Main implements Callable<Integer> {
    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
