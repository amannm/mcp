package com.amannmalik.mcp.cli;

public sealed interface CliConfig permits ServerConfig, ClientConfig, HostConfig {
}
