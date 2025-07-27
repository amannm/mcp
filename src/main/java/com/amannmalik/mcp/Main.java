package com.amannmalik.mcp;

import picocli.CommandLine;

import java.util.concurrent.Callable;

public class Main implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }
}
