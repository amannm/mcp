package com.amannmalik.mcp.config;

import jakarta.json.*;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.*;
import java.nio.file.*;
import java.util.List;
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
            McpConfiguration cfg = McpConfigurationParser.parse(merged, DEFAULT);
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
            return McpConfigurationParser.parseDefaults(obj);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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

