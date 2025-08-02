package com.amannmalik.mcp.cli;

@Deprecated
public sealed interface CliConfig permits ServerConfig, ClientConfig, HostConfig {
}
