package com.amannmalik.mcp.config;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;

import java.util.List;
import java.util.Objects;

final class McpConfigurationParser {
    private McpConfigurationParser() {
    }

    static McpConfiguration parseDefaults(JsonObject obj) {
        var system = parseSystemDefaults(required(obj.getJsonObject("system"), "system"));
        var perf = parsePerformanceDefaults(required(obj.getJsonObject("performance"), "performance"));
        var server = parseServerDefaults(required(obj.getJsonObject("server"), "server"));
        var security = parseSecurityDefaults(required(obj.getJsonObject("security"), "security"));
        var client = parseClientDefaults(required(obj.getJsonObject("client"), "client"));
        var host = parseHostDefaults(required(obj.getJsonObject("host"), "host"));
        return new McpConfiguration(system, perf, server, security, client, host);
    }

    static McpConfiguration parse(JsonObject obj, McpConfiguration def) {
        var system = parseSystem(obj.getJsonObject("system"), def.system());
        var perf = parsePerformance(obj.getJsonObject("performance"), def.performance());
        var server = parseServer(obj.getJsonObject("server"), def.server());
        var security = parseSecurity(obj.getJsonObject("security"), def.security());
        var client = parseClient(obj.getJsonObject("client"), def.client());
        var host = parseHost(obj.getJsonObject("host"), def.host());
        return new McpConfiguration(system, perf, server, security, client, host);
    }

    private static McpConfiguration.SystemConfig parseSystemDefaults(JsonObject obj) {
        var protocol = parseProtocolDefaults(required(obj.getJsonObject("protocol"), "protocol"));
        var timeouts = parseTimeoutsDefaults(required(obj.getJsonObject("timeouts"), "timeouts"));
        return new McpConfiguration.SystemConfig(protocol, timeouts);
    }

    private static McpConfiguration.ProtocolConfig parseProtocolDefaults(JsonObject obj) {
        return new McpConfiguration.ProtocolConfig(
                obj.getString("version"),
                obj.getString("compatibility_version"));
    }

    private static McpConfiguration.TimeoutsConfig parseTimeoutsDefaults(JsonObject obj) {
        return new McpConfiguration.TimeoutsConfig(
                obj.getJsonNumber("default_ms").longValue(),
                obj.getJsonNumber("ping_ms").longValue(),
                obj.getInt("process_wait_seconds"));
    }

    private static McpConfiguration.PerformanceConfig parsePerformanceDefaults(JsonObject obj) {
        var rl = parseRateLimitsDefaults(required(obj.getJsonObject("rate_limits"), "rate_limits"));
        var pg = parsePaginationDefaults(required(obj.getJsonObject("pagination"), "pagination"));
        return new McpConfiguration.PerformanceConfig(rl, pg);
    }

    private static McpConfiguration.RateLimitsConfig parseRateLimitsDefaults(JsonObject obj) {
        return new McpConfiguration.RateLimitsConfig(
                obj.getInt("tools_per_second"),
                obj.getInt("completions_per_second"),
                obj.getInt("logs_per_second"),
                obj.getInt("progress_per_second"));
    }

    private static McpConfiguration.PaginationConfig parsePaginationDefaults(JsonObject obj) {
        return new McpConfiguration.PaginationConfig(
                obj.getInt("default_page_size"),
                obj.getInt("max_completion_values"),
                obj.getInt("sse_history_limit"),
                obj.getInt("response_queue_capacity"));
    }

    private static McpConfiguration.ServerConfig parseServerDefaults(JsonObject obj) {
        var info = parseServerInfoDefaults(required(obj.getJsonObject("info"), "server info"));
        var transport = parseTransportDefaults(required(obj.getJsonObject("transport"), "transport"));
        return new McpConfiguration.ServerConfig(info, transport);
    }

    private static McpConfiguration.ServerInfoConfig parseServerInfoDefaults(JsonObject obj) {
        return new McpConfiguration.ServerInfoConfig(
                obj.getString("name"),
                obj.getString("description"),
                obj.getString("version"));
    }

    private static McpConfiguration.TransportConfig parseTransportDefaults(JsonObject obj) {
        List<String> origins = obj.getJsonArray("allowed_origins").getValuesAs(JsonString.class)
                .stream().map(JsonString::getString).toList();
        return new McpConfiguration.TransportConfig(
                obj.getString("type"),
                obj.getInt("port"),
                origins);
    }

    private static McpConfiguration.SecurityConfig parseSecurityDefaults(JsonObject obj) {
        var auth = parseAuthDefaults(required(obj.getJsonObject("auth"), "auth"));
        return new McpConfiguration.SecurityConfig(auth);
    }

    private static McpConfiguration.AuthConfig parseAuthDefaults(JsonObject obj) {
        return new McpConfiguration.AuthConfig(
                obj.getString("jwt_secret_env"),
                obj.getString("default_principal"));
    }

    private static McpConfiguration.ClientConfig parseClientDefaults(JsonObject obj) {
        var info = parseClientInfoDefaults(required(obj.getJsonObject("info"), "client info"));
        List<String> caps = Objects.requireNonNull(obj.getJsonArray("capabilities"), "capabilities config required in defaults")
                .getValuesAs(JsonString.class)
                .stream().map(JsonString::getString).toList();
        return new McpConfiguration.ClientConfig(info, caps);
    }

    private static McpConfiguration.ClientInfoConfig parseClientInfoDefaults(JsonObject obj) {
        return new McpConfiguration.ClientInfoConfig(
                obj.getString("name"),
                obj.getString("display_name"),
                obj.getString("version"));
    }

    private static McpConfiguration.HostConfig parseHostDefaults(JsonObject obj) {
        return new McpConfiguration.HostConfig(obj.getString("principal"));
    }

    private static McpConfiguration.SystemConfig parseSystem(JsonObject obj, McpConfiguration.SystemConfig def) {
        if (obj == null) return def;
        var protocol = parseProtocol(obj.getJsonObject("protocol"), def.protocol());
        var timeouts = parseTimeouts(obj.getJsonObject("timeouts"), def.timeouts());
        return new McpConfiguration.SystemConfig(protocol, timeouts);
    }

    private static McpConfiguration.ProtocolConfig parseProtocol(JsonObject obj, McpConfiguration.ProtocolConfig def) {
        if (obj == null) return def;
        return new McpConfiguration.ProtocolConfig(
                obj.getString("version", def.version()),
                obj.getString("compatibility_version", def.compatibilityVersion()));
    }

    private static McpConfiguration.TimeoutsConfig parseTimeouts(JsonObject obj, McpConfiguration.TimeoutsConfig def) {
        if (obj == null) return def;
        long defaultMs = obj.containsKey("default_ms") ? obj.getJsonNumber("default_ms").longValue() : def.defaultMs();
        long pingMs = obj.containsKey("ping_ms") ? obj.getJsonNumber("ping_ms").longValue() : def.pingMs();
        int wait = obj.getInt("process_wait_seconds", def.processWaitSeconds());
        return new McpConfiguration.TimeoutsConfig(defaultMs, pingMs, wait);
    }

    private static McpConfiguration.PerformanceConfig parsePerformance(JsonObject obj, McpConfiguration.PerformanceConfig def) {
        if (obj == null) return def;
        var rl = parseRateLimits(obj.getJsonObject("rate_limits"), def.rateLimits());
        var pg = parsePagination(obj.getJsonObject("pagination"), def.pagination());
        return new McpConfiguration.PerformanceConfig(rl, pg);
    }

    private static McpConfiguration.RateLimitsConfig parseRateLimits(JsonObject obj, McpConfiguration.RateLimitsConfig def) {
        if (obj == null) return def;
        return new McpConfiguration.RateLimitsConfig(
                obj.getInt("tools_per_second", def.toolsPerSecond()),
                obj.getInt("completions_per_second", def.completionsPerSecond()),
                obj.getInt("logs_per_second", def.logsPerSecond()),
                obj.getInt("progress_per_second", def.progressPerSecond()));
    }

    private static McpConfiguration.PaginationConfig parsePagination(JsonObject obj, McpConfiguration.PaginationConfig def) {
        if (obj == null) return def;
        return new McpConfiguration.PaginationConfig(
                obj.getInt("default_page_size", def.defaultPageSize()),
                obj.getInt("max_completion_values", def.maxCompletionValues()),
                obj.getInt("sse_history_limit", def.sseHistoryLimit()),
                obj.getInt("response_queue_capacity", def.responseQueueCapacity()));
    }

    private static McpConfiguration.ServerConfig parseServer(JsonObject obj, McpConfiguration.ServerConfig def) {
        if (obj == null) return def;
        var info = parseServerInfo(obj.getJsonObject("info"), def.info());
        var transport = parseTransport(obj.getJsonObject("transport"), def.transport());
        return new McpConfiguration.ServerConfig(info, transport);
    }

    private static McpConfiguration.ServerInfoConfig parseServerInfo(JsonObject obj, McpConfiguration.ServerInfoConfig def) {
        if (obj == null) return def;
        return new McpConfiguration.ServerInfoConfig(
                obj.getString("name", def.name()),
                obj.getString("description", def.description()),
                obj.getString("version", def.version()));
    }

    private static McpConfiguration.TransportConfig parseTransport(JsonObject obj, McpConfiguration.TransportConfig def) {
        if (obj == null) return def;
        List<String> origins = obj.containsKey("allowed_origins")
                ? obj.getJsonArray("allowed_origins").getValuesAs(JsonString.class).stream().map(JsonString::getString).toList()
                : def.allowedOrigins();
        return new McpConfiguration.TransportConfig(
                obj.getString("type", def.type()),
                obj.getInt("port", def.port()),
                origins);
    }

    private static McpConfiguration.SecurityConfig parseSecurity(JsonObject obj, McpConfiguration.SecurityConfig def) {
        if (obj == null) return def;
        var auth = parseAuth(obj.getJsonObject("auth"), def.auth());
        return new McpConfiguration.SecurityConfig(auth);
    }

    private static McpConfiguration.AuthConfig parseAuth(JsonObject obj, McpConfiguration.AuthConfig def) {
        if (obj == null) return def;
        return new McpConfiguration.AuthConfig(
                obj.getString("jwt_secret_env", def.jwtSecretEnv()),
                obj.getString("default_principal", def.defaultPrincipal()));
    }

    private static McpConfiguration.ClientConfig parseClient(JsonObject obj, McpConfiguration.ClientConfig def) {
        if (obj == null) return def;
        var info = parseClientInfo(obj.getJsonObject("info"), def.info());
        List<String> caps = obj.containsKey("capabilities")
                ? obj.getJsonArray("capabilities").getValuesAs(JsonString.class).stream().map(JsonString::getString).toList()
                : def.capabilities();
        return new McpConfiguration.ClientConfig(info, caps);
    }

    private static McpConfiguration.ClientInfoConfig parseClientInfo(JsonObject obj, McpConfiguration.ClientInfoConfig def) {
        if (obj == null) return def;
        return new McpConfiguration.ClientInfoConfig(
                obj.getString("name", def.name()),
                obj.getString("display_name", def.displayName()),
                obj.getString("version", def.version()));
    }

    private static McpConfiguration.HostConfig parseHost(JsonObject obj, McpConfiguration.HostConfig def) {
        if (obj == null) return def;
        return new McpConfiguration.HostConfig(obj.getString("principal", def.principal()));
    }

    private static JsonObject required(JsonObject obj, String name) {
        return Objects.requireNonNull(obj, name + " config required in defaults");
    }
}

