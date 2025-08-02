package com.amannmalik.mcp.config;

import jakarta.json.*;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public record McpConfiguration(SystemConfig system,
                               PerformanceConfig performance,
                               ServerConfig server,
                               SecurityConfig security,
                               ClientConfig client,
                               HostConfig host) {
    private static final AtomicReference<McpConfiguration> REF = new AtomicReference<>(loadFromEnv());
    private static final AtomicBoolean WATCHING = new AtomicBoolean();

    public static McpConfiguration current() {
        return REF.get();
    }

    public static void reload() {
        REF.set(loadFromEnv());
    }

    static void reload(Path path, String env) {
        try {
            REF.set(load(path, env));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static McpConfiguration loadFromEnv() {
        String file = System.getenv("MCP_CONFIG");
        String env = System.getenv("MCP_ENV");
        if (file != null && !file.isBlank()) {
            Path p = Path.of(file);
            watch(p);
            try {
                return load(p, env);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return DEFAULT;
    }

    public static McpConfiguration load(Path path) throws IOException {
        return load(path, System.getenv("MCP_ENV"));
    }

    public static McpConfiguration load(Path path, String env) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            Load loader = new Load(LoadSettings.builder().build());
            JsonValue val = toJsonValue(loader.loadFromInputStream(in));
            if (!(val instanceof JsonObject obj)) throw new IllegalArgumentException("invalid config");
            JsonObject merged = applyEnvironment(obj, env);
            McpConfiguration cfg = parse(merged, DEFAULT);
            validate(cfg);
            return cfg;
        }
    }

    private static JsonObject applyEnvironment(JsonObject obj, String env) {
        JsonObject base = withoutEnvironments(obj);
        if (env == null || env.isBlank()) return base;
        JsonObject envs = obj.getJsonObject("environments");
        if (envs == null) return base;
        JsonObject ovr = envs.getJsonObject(env);
        return ovr == null ? base : merge(base, ovr);
    }

    private static JsonObject withoutEnvironments(JsonObject obj) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        for (var e : obj.entrySet()) if (!e.getKey().equals("environments")) b.add(e.getKey(), e.getValue());
        return b.build();
    }

    private static JsonObject merge(JsonObject base, JsonObject override) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        for (var e : base.entrySet()) {
            String k = e.getKey();
            JsonValue bv = e.getValue();
            JsonValue ov = override.get(k);
            if (ov != null && bv instanceof JsonObject bObj && ov instanceof JsonObject oObj) {
                b.add(k, merge(bObj, oObj));
            } else if (ov != null) {
                b.add(k, ov);
            } else {
                b.add(k, bv);
            }
        }
        for (var e : override.entrySet()) {
            if (!base.containsKey(e.getKey())) b.add(e.getKey(), e.getValue());
        }
        return b.build();
    }

    private static void watch(Path path) {
        if (!WATCHING.compareAndSet(false, true)) return;
        Thread.startVirtualThread(() -> {
            try (WatchService svc = FileSystems.getDefault().newWatchService()) {
                Path dir = path.getParent();
                if (dir == null) return;
                dir.register(svc, StandardWatchEventKinds.ENTRY_MODIFY);
                while (true) {
                    WatchKey key = svc.take();
                    for (var ev : key.pollEvents()) {
                        var ctx = ev.context();
                        if (ctx instanceof Path p && p.equals(path.getFileName())) reload();
                    }
                    key.reset();
                }
            } catch (InterruptedException | IOException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private static void validate(McpConfiguration c) {
        if (c.system().timeouts().defaultMs() <= 0 ||
                c.system().timeouts().pingMs() <= 0 ||
                c.system().timeouts().processWaitSeconds() <= 0) throw new IllegalArgumentException("timeouts");
        if (c.performance().rateLimits().toolsPerSecond() < 0 ||
                c.performance().rateLimits().completionsPerSecond() < 0 ||
                c.performance().rateLimits().logsPerSecond() < 0 ||
                c.performance().rateLimits().progressPerSecond() < 0) throw new IllegalArgumentException("rateLimits");
        if (c.performance().pagination().defaultPageSize() <= 0 ||
                c.performance().pagination().maxCompletionValues() <= 0 ||
                c.performance().pagination().responseQueueCapacity() <= 0) throw new IllegalArgumentException("pagination");
        if (c.server().transport().port() < 0 || c.server().transport().port() > 65_535) throw new IllegalArgumentException("port");
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
            case Number n -> {
                if (n instanceof Float || n instanceof Double) yield Json.createValue(n.doubleValue());
                yield Json.createValue(n.longValue());
            }
            case Boolean b -> b ? JsonValue.TRUE : JsonValue.FALSE;
            case null -> JsonValue.NULL;
            default -> Json.createValue(value.toString());
        };
    }

    public static JsonObject schema() {
        return SCHEMA;
    }

    public static void writeDocumentation(Path path) throws IOException {
        Files.writeString(path, asYaml(DEFAULT));
    }

    private static String asYaml(McpConfiguration c) {
        StringBuilder b = new StringBuilder();
        b.append("system:\n");
        b.append("  protocol:\n");
        b.append("    version: \"").append(c.system().protocol().version()).append("\"\n");
        b.append("    compatibility_version: \"").append(c.system().protocol().compatibilityVersion()).append("\"\n");
        b.append("  timeouts:\n");
        b.append("    default_ms: ").append(c.system().timeouts().defaultMs()).append('\n');
        b.append("    ping_ms: ").append(c.system().timeouts().pingMs()).append('\n');
        b.append("    process_wait_seconds: ").append(c.system().timeouts().processWaitSeconds()).append('\n');
        b.append("performance:\n");
        b.append("  rate_limits:\n");
        b.append("    tools_per_second: ").append(c.performance().rateLimits().toolsPerSecond()).append('\n');
        b.append("    completions_per_second: ").append(c.performance().rateLimits().completionsPerSecond()).append('\n');
        b.append("    logs_per_second: ").append(c.performance().rateLimits().logsPerSecond()).append('\n');
        b.append("    progress_per_second: ").append(c.performance().rateLimits().progressPerSecond()).append('\n');
        b.append("  pagination:\n");
        b.append("    default_page_size: ").append(c.performance().pagination().defaultPageSize()).append('\n');
        b.append("    max_completion_values: ").append(c.performance().pagination().maxCompletionValues()).append('\n');
        b.append("    sse_history_limit: ").append(c.performance().pagination().sseHistoryLimit()).append('\n');
        b.append("    response_queue_capacity: ").append(c.performance().pagination().responseQueueCapacity()).append('\n');
        b.append("server:\n");
        b.append("  info:\n");
        b.append("    name: \"").append(c.server().info().name()).append("\"\n");
        b.append("    description: \"").append(c.server().info().description()).append("\"\n");
        b.append("    version: \"").append(c.server().info().version()).append("\"\n");
        b.append("  transport:\n");
        b.append("    type: \"").append(c.server().transport().type()).append("\"\n");
        b.append("    port: ").append(c.server().transport().port()).append('\n');
        b.append("    allowed_origins:\n");
        for (var o : c.server().transport().allowedOrigins()) b.append("      - \"").append(o).append("\"\n");
        b.append("security:\n");
        b.append("  auth:\n");
        b.append("    jwt_secret_env: \"").append(c.security().auth().jwtSecretEnv()).append("\"\n");
        b.append("    default_principal: \"").append(c.security().auth().defaultPrincipal()).append("\"\n");
        b.append("client:\n");
        b.append("  info:\n");
        b.append("    name: \"").append(c.client().info().name()).append("\"\n");
        b.append("    display_name: \"").append(c.client().info().displayName()).append("\"\n");
        b.append("    version: \"").append(c.client().info().version()).append("\"\n");
        b.append("  capabilities:\n");
        for (var cap : c.client().capabilities()) b.append("    - \"").append(cap).append("\"\n");
        b.append("host:\n");
        b.append("  principal: \"").append(c.host().principal()).append("\"\n");
        return b.toString();
    }

    private static final JsonObject SCHEMA = Json.createReader(new StringReader("""
            {
              "type": "object",
              "properties": {
                "system": {
                  "type": "object",
                  "properties": {
                    "protocol": {
                      "type": "object",
                      "properties": {
                        "version": {"type": "string"},
                        "compatibility_version": {"type": "string"}
                      }
                    },
                    "timeouts": {
                      "type": "object",
                      "properties": {
                        "default_ms": {"type": "integer"},
                        "ping_ms": {"type": "integer"},
                        "process_wait_seconds": {"type": "integer"}
                      }
                    }
                  }
                },
                "performance": {
                  "type": "object",
                  "properties": {
                    "rate_limits": {
                      "type": "object",
                      "properties": {
                        "tools_per_second": {"type": "integer"},
                        "completions_per_second": {"type": "integer"},
                        "logs_per_second": {"type": "integer"},
                        "progress_per_second": {"type": "integer"}
                      }
                    },
                    "pagination": {
                      "type": "object",
                      "properties": {
                        "default_page_size": {"type": "integer"},
                        "max_completion_values": {"type": "integer"},
                        "sse_history_limit": {"type": "integer"},
                        "response_queue_capacity": {"type": "integer"}
                      }
                    }
                  }
                },
                "server": {
                  "type": "object",
                  "properties": {
                    "info": {
                      "type": "object",
                      "properties": {
                        "name": {"type": "string"},
                        "description": {"type": "string"},
                        "version": {"type": "string"}
                      }
                    },
                    "transport": {
                      "type": "object",
                      "properties": {
                        "type": {"type": "string"},
                        "port": {"type": "integer"},
                        "allowed_origins": {"type": "array", "items": {"type": "string"}}
                      }
                    }
                  }
                },
                "security": {
                  "type": "object",
                  "properties": {
                    "auth": {
                      "type": "object",
                      "properties": {
                        "jwt_secret_env": {"type": "string"},
                        "default_principal": {"type": "string"}
                      }
                    }
                  }
                },
                "client": {
                  "type": "object",
                  "properties": {
                    "info": {
                      "type": "object",
                      "properties": {
                        "name": {"type": "string"},
                        "display_name": {"type": "string"},
                        "version": {"type": "string"}
                      }
                    },
                    "capabilities": {"type": "array", "items": {"type": "string"}}
                  }
                },
                "host": {
                  "type": "object",
                  "properties": {
                    "principal": {"type": "string"}
                  }
                }
              }
            }
            """)).readObject();

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

