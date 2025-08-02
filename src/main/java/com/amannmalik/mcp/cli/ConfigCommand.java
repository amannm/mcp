package com.amannmalik.mcp.cli;

import com.amannmalik.mcp.config.McpConfiguration;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(name = "config", description = "Manage configuration", mixinStandardHelpOptions = true)
public final class ConfigCommand implements Callable<Integer> {
    @CommandLine.Option(names = "--reload", description = "Reload configuration")
    private boolean reload;

    @CommandLine.Option(names = "--watch", description = "Watch for configuration changes")
    private boolean watch;

    @Override
    public Integer call() throws Exception {
        if (reload) McpConfiguration.reload();
        System.out.println(McpConfiguration.current());
        if (watch) {
            McpConfiguration.addChangeListener(c -> System.out.println(c));
            Thread.sleep(Long.MAX_VALUE);
        }
        return 0;
    }
}

