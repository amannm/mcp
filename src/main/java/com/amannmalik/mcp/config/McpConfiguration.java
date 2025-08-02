package com.amannmalik.mcp.config;

import jakarta.json.*;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record McpConfiguration(SystemConfig system,
                               PerformanceConfig performance,
                               ServerConfig server,
                               SecurityConfig security,
                               ClientConfig client,
                               HostConfig host) {
    public static McpConfiguration current() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        static final McpConfiguration INSTANCE = loadFromEnv();
    }

    private static McpConfiguration loadFromEnv() {
        String env = System.getenv("MCP_CONFIG");
        if (env != null && !env.isBlank()) {
            try {
                return load(Path.of(env));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return DEFAULT;
    }

    public static McpConfiguration load(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            Load loader = new Load(LoadSettings.builder().build());
            JsonValue val = toJsonValue(loader.loadFromInputStream(in));
            if (!(val instanceof JsonObject obj)) throw new IllegalArgumentException("invalid config");
            return parse(obj, DEFAULT);
        }
    }

    private static McpConfiguration parse(JsonObject obj, McpConfiguration def) {
        SystemConfig system = parseSystem(obj.getJsonObject("system"), def.system);
        PerformanceConfig perf = parsePerformance(obj.getJsonObject("performance"), def.performance);
        ServerConfig server = parseServer(obj.getJsonObject("server"), def.server);
        SecurityConfig security = parseSecurity(obj.getJsonObject("security"), def.security);
        ClientConfig client = parseClient(obj.getJsonObject("client"), def.client);
        HostConfig host = parseHost(obj.getJsonObject("host"), def.host);
        return new McpConfiguration(system, perf, server, security, client, host);
    }

    private static SystemConfig parseSystem(JsonObject obj, SystemConfig def) {
        if (obj == null) return def;
        ProtocolConfig protocol = parseProtocol(obj.getJsonObject("protocol"), def.protocol);
        TimeoutsConfig timeouts = parseTimeouts(obj.getJsonObject("timeouts"), def.timeouts);
        return new SystemConfig(protocol, timeouts);
    }

    private static ProtocolConfig parseProtocol(JsonObject obj, ProtocolConfig def) {
        if (obj == null) return def;
        return new ProtocolConfig(
                obj.getString("version", def.version),
                obj.getString("compatibility_version", def.compatibilityVersion));
    }

    private static TimeoutsConfig parseTimeouts(JsonObject obj, TimeoutsConfig def) {
        if (obj == null) return def;
        long defaultMs = obj.containsKey("default_ms") ? obj.getJsonNumber("default_ms").longValue() : def.defaultMs;
        long pingMs = obj.containsKey("ping_ms") ? obj.getJsonNumber("ping_ms").longValue() : def.pingMs;
        int wait = obj.getInt("process_wait_seconds", def.processWaitSeconds);
        return new TimeoutsConfig(defaultMs, pingMs, wait);
    }

    private static PerformanceConfig parsePerformance(JsonObject obj, PerformanceConfig def) {
        if (obj == null) return def;
        RateLimitsConfig rl = parseRateLimits(obj.getJsonObject("rate_limits"), def.rateLimits);
        PaginationConfig pg = parsePagination(obj.getJsonObject("pagination"), def.pagination);
        return new PerformanceConfig(rl, pg);
    }

    private static RateLimitsConfig parseRateLimits(JsonObject obj, RateLimitsConfig def) {
        if (obj == null) return def;
        return new RateLimitsConfig(
                obj.getInt("tools_per_second", def.toolsPerSecond),
                obj.getInt("completions_per_second", def.completionsPerSecond),
                obj.getInt("logs_per_second", def.logsPerSecond),
                obj.getInt("progress_per_second", def.progressPerSecond));
    }

    private static PaginationConfig parsePagination(JsonObject obj, PaginationConfig def) {
        if (obj == null) return def;
        return new PaginationConfig(
                obj.getInt("default_page_size", def.defaultPageSize),
                obj.getInt("max_completion_values", def.maxCompletionValues),
                obj.getInt("sse_history_limit", def.sseHistoryLimit),
                obj.getInt("response_queue_capacity", def.responseQueueCapacity));
    }

    private static ServerConfig parseServer(JsonObject obj, ServerConfig def) {
        if (obj == null) return def;
        ServerInfoConfig info = parseServerInfo(obj.getJsonObject("info"), def.info);
        TransportConfig transport = parseTransport(obj.getJsonObject("transport"), def.transport);
        return new ServerConfig(info, transport);
    }

    private static ServerInfoConfig parseServerInfo(JsonObject obj, ServerInfoConfig def) {
        if (obj == null) return def;
        return new ServerInfoConfig(
                obj.getString("name", def.name),
                obj.getString("description", def.description),
                obj.getString("version", def.version));
    }

    private static TransportConfig parseTransport(JsonObject obj, TransportConfig def) {
        if (obj == null) return def;
        List<String> origins = obj.containsKey("allowed_origins")
                ? obj.getJsonArray("allowed_origins").getValuesAs(JsonString.class).stream().map(JsonString::getString).toList()
                : def.allowedOrigins;
        return new TransportConfig(
                obj.getString("type", def.type),
                obj.getInt("port", def.port),
                origins);
    }

    private static SecurityConfig parseSecurity(JsonObject obj, SecurityConfig def) {
        if (obj == null) return def;
        AuthConfig auth = parseAuth(obj.getJsonObject("auth"), def.auth);
        return new SecurityConfig(auth);
    }

    private static AuthConfig parseAuth(JsonObject obj, AuthConfig def) {
        if (obj == null) return def;
        return new AuthConfig(
                obj.getString("jwt_secret_env", def.jwtSecretEnv),
                obj.getString("default_principal", def.defaultPrincipal));
    }

    private static ClientConfig parseClient(JsonObject obj, ClientConfig def) {
        if (obj == null) return def;
        ClientInfoConfig info = parseClientInfo(obj.getJsonObject("info"), def.info);
        List<String> caps = obj.containsKey("capabilities")
                ? obj.getJsonArray("capabilities").getValuesAs(JsonString.class).stream().map(JsonString::getString).toList()
                : def.capabilities;
        return new ClientConfig(info, caps);
    }

    private static ClientInfoConfig parseClientInfo(JsonObject obj, ClientInfoConfig def) {
        if (obj == null) return def;
        return new ClientInfoConfig(
                obj.getString("name", def.name),
                obj.getString("display_name", def.displayName),
                obj.getString("version", def.version));
    }

    private static HostConfig parseHost(JsonObject obj, HostConfig def) {
        if (obj == null) return def;
        return new HostConfig(obj.getString("principal", def.principal));
    }

    private static JsonValue toJsonValue(Object value) {
        return switch (value) {
            case java.util.Map<?, ?> m -> {
                var b = Json.createObjectBuilder();
                for (var e : m.entrySet()) {
                    b.add(e.getKey().toString(), toJsonValue(e.getValue()));
                }
                yield b.build();
            }
            case java.util.List<?> l -> {
                JsonArrayBuilder b = Json.createArrayBuilder();
                for (var item : l) b.add(toJsonValue(item));
                yield b.build();
            }
            case String s -> Json.createValue(s);
            case Number n -> Json.createValue(n.toString());
            case Boolean b -> b ? JsonValue.TRUE : JsonValue.FALSE;
            case null -> JsonValue.NULL;
            default -> Json.createValue(value.toString());
        };
    }

    private static final McpConfiguration DEFAULT = new McpConfiguration(
            new SystemConfig(new ProtocolConfig("2025-06-18", "2025-03-26"), new TimeoutsConfig(30_000L, 5_000L, 2)),
            new PerformanceConfig(new RateLimitsConfig(5, 10, 20, 20), new PaginationConfig(100, 100, 100, 1)),
            new ServerConfig(new ServerInfoConfig("mcp-java", "MCP Java Reference", "0.1.0"),
                    new TransportConfig("stdio", 0, List.of("http://localhost", "http://127.0.0.1"))),
            new SecurityConfig(new AuthConfig("MCP_JWT_SECRET", "default")),
            new ClientConfig(new ClientInfoConfig("cli", "CLI", "0"), List.of("SAMPLING", "ROOTS")),
            new HostConfig("user"));

    public record SystemConfig(ProtocolConfig protocol, TimeoutsConfig timeouts) {
    }

    public record ProtocolConfig(String version, String compatibilityVersion) {
    }

    public record TimeoutsConfig(long defaultMs, long pingMs, int processWaitSeconds) {
    }

    public record PerformanceConfig(RateLimitsConfig rateLimits, PaginationConfig pagination) {
    }

    public record RateLimitsConfig(int toolsPerSecond, int completionsPerSecond, int logsPerSecond, int progressPerSecond) {
    }

    public record PaginationConfig(int defaultPageSize, int maxCompletionValues, int sseHistoryLimit, int responseQueueCapacity) {
    }

    public record ServerConfig(ServerInfoConfig info, TransportConfig transport) {
    }

    public record ServerInfoConfig(String name, String description, String version) {
    }

    public record TransportConfig(String type, int port, List<String> allowedOrigins) {
        public TransportConfig {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }

    public record SecurityConfig(AuthConfig auth) {
    }

    public record AuthConfig(String jwtSecretEnv, String defaultPrincipal) {
    }

    public record ClientConfig(ClientInfoConfig info, List<String> capabilities) {
        public ClientConfig {
            capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        }
    }

    public record ClientInfoConfig(String name, String displayName, String version) {
    }

    public record HostConfig(String principal) {
    }
}

