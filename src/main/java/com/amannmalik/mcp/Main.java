package com.amannmalik.mcp;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "mcp",
        version = "0.1",
        mixinStandardHelpOptions = true)
public class Main implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
