package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.Main;
import jakarta.json.JsonObject;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "client", description = "Run MCP client")
public final class ClientCommand implements Callable<Integer> {
    @ParentCommand
    private Main parent;

    @Override
    public Integer call() throws Exception {
        Path cfgPath = parent.config();
        JsonObject cfg = cfgPath == null ? null : ConfigLoader.load(cfgPath);
        if (parent.verbose()) System.out.println("Config: " + cfg);
        System.out.println("Client mode not yet implemented");
        return 0;
    }
}
