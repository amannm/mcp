package com.amannmalik.mcp.config;

import jakarta.json.*;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public record McpConfiguration(SystemConfig system,
                               PerformanceConfig performance,
                               ServerConfig server,
                               SecurityConfig security,
                               ClientConfig client,
                               HostConfig host) {

    private static final McpConfiguration DEFAULT = loadDefaults();

    private static volatile McpConfiguration CURRENT = loadFromEnv();
    private static final AtomicBoolean WATCHING = new AtomicBoolean();
    private static final CopyOnWriteArrayList<Consumer<McpConfiguration>> LISTENERS = new CopyOnWriteArrayList<>();

    static {
        addChangeListener(c -> System.err.println("Configuration reloaded"));
    }


    public static McpConfiguration current() {
        return CURRENT;
    }

    static McpConfiguration defaults() {
        return DEFAULT;
    }

    public static void reload() {
        CURRENT = loadFromEnv();
        notifyListeners();
    }

    static void reload(Path path, String env) {
        try {
            CURRENT = load(path, env);
            notifyListeners();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void addChangeListener(Consumer<McpConfiguration> l) {
        LISTENERS.add(l);
    }

    private static void notifyListeners() {
        for (var l : LISTENERS) l.accept(CURRENT);
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

    private static McpConfiguration loadDefaults() {
        try (InputStream in = McpConfiguration.class.getResourceAsStream("/mcp-defaults.yaml")) {
            if (in == null) throw new IllegalStateException("mcp-defaults.yaml not found in resources");
            Load loader = new Load(LoadSettings.builder().build());
            JsonValue val = toJsonValue(loader.loadFromInputStream(in));
            if (!(val instanceof JsonObject obj)) throw new IllegalArgumentException("invalid defaults config");
            return parseDefaults(obj);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static McpConfiguration parseDefaults(JsonObject obj) {
        SystemConfig system = parseSystemDefaults(required(obj.getJsonObject("system"), "system"));
        PerformanceConfig perf = parsePerformanceDefaults(required(obj.getJsonObject("performance"), "performance"));
        ServerConfig server = parseServerDefaults(required(obj.getJsonObject("server"), "server"));
        SecurityConfig security = parseSecurityDefaults(required(obj.getJsonObject("security"), "security"));
        ClientConfig client = parseClientDefaults(required(obj.getJsonObject("client"), "client"));
        HostConfig host = parseHostDefaults(required(obj.getJsonObject("host"), "host"));
        return new McpConfiguration(system, perf, server, security, client, host);
    }

    private static SystemConfig parseSystemDefaults(JsonObject obj) {
        ProtocolConfig protocol = parseProtocolDefaults(required(obj.getJsonObject("protocol"), "protocol"));
        TimeoutsConfig timeouts = parseTimeoutsDefaults(required(obj.getJsonObject("timeouts"), "timeouts"));
        return new SystemConfig(protocol, timeouts);
    }

    private static ProtocolConfig parseProtocolDefaults(JsonObject obj) {
        return new ProtocolConfig(
                obj.getString("version"),
                obj.getString("compatibility_version"));
    }

    private static TimeoutsConfig parseTimeoutsDefaults(JsonObject obj) {
        return new TimeoutsConfig(
                obj.getJsonNumber("default_ms").longValue(),
                obj.getJsonNumber("ping_ms").longValue(),
                obj.getInt("process_wait_seconds"));
    }

    private static PerformanceConfig parsePerformanceDefaults(JsonObject obj) {
        RateLimitsConfig rl = parseRateLimitsDefaults(required(obj.getJsonObject("rate_limits"), "rate_limits"));
        PaginationConfig pg = parsePaginationDefaults(required(obj.getJsonObject("pagination"), "pagination"));
        return new PerformanceConfig(rl, pg);
    }

    private static RateLimitsConfig parseRateLimitsDefaults(JsonObject obj) {
        return new RateLimitsConfig(
                obj.getInt("tools_per_second"),
                obj.getInt("completions_per_second"),
                obj.getInt("logs_per_second"),
                obj.getInt("progress_per_second"));
    }

    private static PaginationConfig parsePaginationDefaults(JsonObject obj) {
        return new PaginationConfig(
                obj.getInt("default_page_size"),
                obj.getInt("max_completion_values"),
                obj.getInt("sse_history_limit"),
                obj.getInt("response_queue_capacity"));
    }

    private static ServerConfig parseServerDefaults(JsonObject obj) {
        ServerInfoConfig info = parseServerInfoDefaults(required(obj.getJsonObject("info"), "server info"));
        TransportConfig transport = parseTransportDefaults(required(obj.getJsonObject("transport"), "transport"));
        return new ServerConfig(info, transport);
    }

    private static ServerInfoConfig parseServerInfoDefaults(JsonObject obj) {
        return new ServerInfoConfig(
                obj.getString("name"),
                obj.getString("description"),
                obj.getString("version"));
    }

    private static TransportConfig parseTransportDefaults(JsonObject obj) {
        List<String> origins = obj.getJsonArray("allowed_origins").getValuesAs(JsonString.class)
                .stream().map(JsonString::getString).toList();
        return new TransportConfig(
                obj.getString("type"),
                obj.getInt("port"),
                origins);
    }

    private static SecurityConfig parseSecurityDefaults(JsonObject obj) {
        AuthConfig auth = parseAuthDefaults(required(obj.getJsonObject("auth"), "auth"));
        return new SecurityConfig(auth);
    }

    private static AuthConfig parseAuthDefaults(JsonObject obj) {
        return new AuthConfig(
                obj.getString("jwt_secret_env"),
                obj.getString("default_principal"));
    }

    private static ClientConfig parseClientDefaults(JsonObject obj) {
        ClientInfoConfig info = parseClientInfoDefaults(required(obj.getJsonObject("info"), "client info"));
        List<String> caps = Objects.requireNonNull(obj.getJsonArray("capabilities"), "capabilities config required in defaults")
                .getValuesAs(JsonString.class)
                .stream().map(JsonString::getString).toList();
        return new ClientConfig(info, caps);
    }

    private static ClientInfoConfig parseClientInfoDefaults(JsonObject obj) {
        return new ClientInfoConfig(
                obj.getString("name"),
                obj.getString("display_name"),
                obj.getString("version"));
    }

    private static HostConfig parseHostDefaults(JsonObject obj) {
        return new HostConfig(obj.getString("principal"));
    }

    private static JsonObject required(JsonObject obj, String name) {
        return Objects.requireNonNull(obj, name + " config required in defaults");
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
        if (!WATCHING.compareAndExchange(false, true)) return;
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
        return McpConfigurationDocs.schema();
    }

    public static void writeDocumentation(Path path) throws IOException {
        McpConfigurationDocs.writeDocumentation(path);
    }

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

