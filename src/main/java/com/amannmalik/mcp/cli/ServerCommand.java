package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.Main;
import jakarta.json.JsonObject;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "server", description = "Run MCP server")
public final class ServerCommand implements Callable<Integer> {
    @ParentCommand
    private Main parent;

    @Override
    public Integer call() throws Exception {
        Path cfgPath = parent.config();
        JsonObject cfg = cfgPath == null ? null : ConfigLoader.load(cfgPath);
        if (parent.verbose()) System.out.println("Config: " + cfg);
        System.out.println("Server mode not yet implemented");
        return 0;
    }
}
