package com.amannmalik.mcp;

import com.amannmalik.mcp.cli.*;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "mcp",
        subcommands = {ServerCommand.class, ClientCommand.class, HostCommand.class},
        mixinStandardHelpOptions = true)
public final class Main implements Callable<Integer> {
    @CommandLine.Option(names = {"-c", "--config"}, description = "Config file")
    private Path config;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Verbose logging")
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        if (config != null) {
            CliConfig cfg = ConfigLoader.load(config);
            return switch (cfg) {
                case ServerConfig sc -> new ServerCommand(sc, verbose).call();
                case ClientConfig cc -> new ClientCommand(cc, verbose).call();
                case HostConfig hc -> new HostCommand(hc, verbose).call();
            };
        }
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
