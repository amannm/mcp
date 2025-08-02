package com.amannmalik.mcp.config;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

final class McpConfigurationDocs {
    private static final JsonObject SCHEMA;

    static {
        try (InputStream in = McpConfigurationDocs.class.getResourceAsStream("/mcp-configuration-schema.json")) {
            if (in == null) throw new IllegalStateException("mcp-configuration-schema.json not found");
            SCHEMA = Json.createReader(new InputStreamReader(in)).readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private McpConfigurationDocs() {
    }

    static JsonObject schema() {
        return SCHEMA;
    }

    static void writeDocumentation(Path path) throws IOException {
        Files.writeString(path, asYaml(McpConfiguration.defaults()));
    }

    private static String asYaml(McpConfiguration c) {
        StringBuilder b = new StringBuilder();
        b.append("system:\n");
        b.append("  protocol:\n");
        b.append("    version: \"").append(c.system().protocol().version()).append("\"\n");
        b.append("    compatibility_version: \"")
                .append(c.system().protocol().compatibilityVersion()).append("\"\n");
        b.append("  timeouts:\n");
        b.append("    default_ms: ").append(c.system().timeouts().defaultMs()).append("\n");
        b.append("    ping_ms: ").append(c.system().timeouts().pingMs()).append("\n");
        b.append("    process_wait_seconds: ")
                .append(c.system().timeouts().processWaitSeconds()).append("\n");
        b.append("performance:\n");
        b.append("  rate_limits:\n");
        b.append("    tools_per_second: ")
                .append(c.performance().rateLimits().toolsPerSecond()).append("\n");
        b.append("    completions_per_second: ")
                .append(c.performance().rateLimits().completionsPerSecond()).append("\n");
        b.append("    logs_per_second: ")
                .append(c.performance().rateLimits().logsPerSecond()).append("\n");
        b.append("    progress_per_second: ")
                .append(c.performance().rateLimits().progressPerSecond()).append("\n");
        b.append("  pagination:\n");
        b.append("    default_page_size: ")
                .append(c.performance().pagination().defaultPageSize()).append("\n");
        b.append("    max_completion_values: ")
                .append(c.performance().pagination().maxCompletionValues()).append("\n");
        b.append("    sse_history_limit: ")
                .append(c.performance().pagination().sseHistoryLimit()).append("\n");
        b.append("    response_queue_capacity: ")
                .append(c.performance().pagination().responseQueueCapacity()).append("\n");
        b.append("server:\n");
        b.append("  info:\n");
        b.append("    name: \"").append(c.server().info().name()).append("\"\n");
        b.append("    description: \"")
                .append(c.server().info().description()).append("\"\n");
        b.append("    version: \"").append(c.server().info().version()).append("\"\n");
        b.append("  transport:\n");
        b.append("    type: \"").append(c.server().transport().type()).append("\"\n");
        b.append("    port: ").append(c.server().transport().port()).append("\n");
        b.append("    allowed_origins:\n");
        for (var o : c.server().transport().allowedOrigins()) {
            b.append("      - \"").append(o).append("\"\n");
        }
        b.append("security:\n");
        b.append("  auth:\n");
        b.append("    jwt_secret_env: \"")
                .append(c.security().auth().jwtSecretEnv()).append("\"\n");
        b.append("    default_principal: \"")
                .append(c.security().auth().defaultPrincipal()).append("\"\n");
        b.append("client:\n");
        b.append("  info:\n");
        b.append("    name: \"").append(c.client().info().name()).append("\"\n");
        b.append("    display_name: \"")
                .append(c.client().info().displayName()).append("\"\n");
        b.append("    version: \"").append(c.client().info().version()).append("\"\n");
        b.append("  capabilities:\n");
        for (var cap : c.client().capabilities()) {
            b.append("    - \"").append(cap).append("\"\n");
        }
        b.append("host:\n");
        b.append("  principal: \"").append(c.host().principal()).append("\"\n");
        return b.toString();
    }
}
