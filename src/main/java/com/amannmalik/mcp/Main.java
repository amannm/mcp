package com.amannmalik.mcp;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "mcp",
        mixinStandardHelpOptions = true,
        version = "0.1",
        header = "mcp - reference implementation")
public class Main implements Callable<Integer> {

    @Override
    public Integer call() {
            return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
