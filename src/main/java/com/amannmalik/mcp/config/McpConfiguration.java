package com.amannmalik.mcp.config;

import jakarta.json.*;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public record McpConfiguration(SystemConfig system,
                               PerformanceConfig performance,
                               ServerConfig server,
                               SecurityConfig security,
                               ClientConfig client,
                               HostConfig host) {

    private static final McpConfiguration DEFAULT = new McpConfiguration(
            new SystemConfig(new ProtocolConfig("2025-06-18", "2025-03-26"),
                    new TimeoutsConfig(30_000, 5_000, 2)),
            new PerformanceConfig(
                    new RateLimitsConfig(5, 10, 20, 20),
                    new PaginationConfig(100, 100, 100, 1),
                    new RuntimeConfig(1_000, 1)),
            new ServerConfig(
                    new ServerInfoConfig("mcp-java", "MCP Java Reference", "0.1.0"),
                    new TransportConfig("stdio", 0, List.of("http://localhost", "http://127.0.0.1")),
                    new MessagingConfig(
                            new ErrorCodesConfig(-32_001),
                            new LoggerNamesConfig("server", "parser", "cancellation"),
                            new ErrorMessagesConfig(
                                    "Error processing message",
                                    "Server not initialized",
                                    "Parse error",
                                    "Invalid request",
                                    "Access denied",
                                    "Request timed out"))),
            new SecurityConfig(
                    new AuthConfig("MCP_JWT_SECRET", "default"),
                    new PrivacyConfig("default")),
            new ClientConfig(
                    new ClientInfoConfig("cli", "CLI", "0"),
                    List.of("SAMPLING", "ROOTS")),
            new HostConfig("user"));

    private static final McpConfiguration CURRENT = loadFromEnv();

    public static McpConfiguration current() {
        return CURRENT;
    }

    public static McpConfiguration load(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            Load loader = new Load(LoadSettings.builder().build());
            JsonValue val = toJsonValue(loader.loadFromInputStream(in));
            if (!(val instanceof JsonObject obj)) throw new IllegalArgumentException("invalid config");
            McpConfiguration cfg = parse(obj, DEFAULT);
            validate(cfg);
            return cfg;
        }
    }

    private static McpConfiguration loadFromEnv() {
        String file = System.getenv("MCP_CONFIG");
        if (file == null || file.isBlank()) return DEFAULT;
        try {
            return load(Path.of(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static McpConfiguration parse(JsonObject obj, McpConfiguration def) {
        var system = parseSystem(obj.getJsonObject("system"), def.system());
        var perf = parsePerformance(obj.getJsonObject("performance"), def.performance());
        var server = parseServer(obj.getJsonObject("server"), def.server());
        var security = parseSecurity(obj.getJsonObject("security"), def.security());
        var client = parseClient(obj.getJsonObject("client"), def.client());
        var host = parseHost(obj.getJsonObject("host"), def.host());
        return new McpConfiguration(system, perf, server, security, client, host);
    }

    private static SystemConfig parseSystem(JsonObject obj, SystemConfig def) {
        if (obj == null) return def;
        var protocol = parseProtocol(obj.getJsonObject("protocol"), def.protocol());
        var timeouts = parseTimeouts(obj.getJsonObject("timeouts"), def.timeouts());
        return new SystemConfig(protocol, timeouts);
    }

    private static ProtocolConfig parseProtocol(JsonObject obj, ProtocolConfig def) {
        if (obj == null) return def;
        return new ProtocolConfig(
                obj.getString("version", def.version()),
                obj.getString("compatibility_version", def.compatibilityVersion()));
    }

    private static TimeoutsConfig parseTimeouts(JsonObject obj, TimeoutsConfig def) {
        if (obj == null) return def;
        long defaultMs = obj.containsKey("default_ms") ? obj.getJsonNumber("default_ms").longValue() : def.defaultMs();
        long pingMs = obj.containsKey("ping_ms") ? obj.getJsonNumber("ping_ms").longValue() : def.pingMs();
        int wait = obj.getInt("process_wait_seconds", def.processWaitSeconds());
        return new TimeoutsConfig(defaultMs, pingMs, wait);
    }

    private static PerformanceConfig parsePerformance(JsonObject obj, PerformanceConfig def) {
        if (obj == null) return def;
        var rl = parseRateLimits(obj.getJsonObject("rate_limits"), def.rateLimits());
        var pg = parsePagination(obj.getJsonObject("pagination"), def.pagination());
        var rt = parseRuntime(obj.getJsonObject("runtime"), def.runtime());
        return new PerformanceConfig(rl, pg, rt);
    }

    private static RateLimitsConfig parseRateLimits(JsonObject obj, RateLimitsConfig def) {
        if (obj == null) return def;
        return new RateLimitsConfig(
                obj.getInt("tools_per_second", def.toolsPerSecond()),
                obj.getInt("completions_per_second", def.completionsPerSecond()),
                obj.getInt("logs_per_second", def.logsPerSecond()),
                obj.getInt("progress_per_second", def.progressPerSecond()));
    }

    private static PaginationConfig parsePagination(JsonObject obj, PaginationConfig def) {
        if (obj == null) return def;
        return new PaginationConfig(
                obj.getInt("default_page_size", def.defaultPageSize()),
                obj.getInt("max_completion_values", def.maxCompletionValues()),
                obj.getInt("sse_history_limit", def.sseHistoryLimit()),
                obj.getInt("response_queue_capacity", def.responseQueueCapacity()));
    }

    private static RuntimeConfig parseRuntime(JsonObject obj, RuntimeConfig def) {
        if (obj == null) return def;
        long window = obj.containsKey("rate_limiter_window_ms") ?
                obj.getJsonNumber("rate_limiter_window_ms").longValue() : def.rateLimiterWindowMs();
        long startId = obj.containsKey("initial_request_id") ?
                obj.getJsonNumber("initial_request_id").longValue() : def.initialRequestId();
        return new RuntimeConfig(window, startId);
    }

    private static ServerConfig parseServer(JsonObject obj, ServerConfig def) {
        if (obj == null) return def;
        var info = parseServerInfo(obj.getJsonObject("info"), def.info());
        var transport = parseTransport(obj.getJsonObject("transport"), def.transport());
        var messaging = parseMessaging(obj.getJsonObject("messaging"), def.messaging());
        return new ServerConfig(info, transport, messaging);
    }

    private static ServerInfoConfig parseServerInfo(JsonObject obj, ServerInfoConfig def) {
        if (obj == null) return def;
        return new ServerInfoConfig(
                obj.getString("name", def.name()),
                obj.getString("description", def.description()),
                obj.getString("version", def.version()));
    }

    private static TransportConfig parseTransport(JsonObject obj, TransportConfig def) {
        if (obj == null) return def;
        List<String> origins = obj.containsKey("allowed_origins") ?
                obj.getJsonArray("allowed_origins").getValuesAs(JsonString.class)
                        .stream().map(JsonString::getString).toList() : def.allowedOrigins();
        return new TransportConfig(
                obj.getString("type", def.type()),
                obj.getInt("port", def.port()),
                origins);
    }

    private static MessagingConfig parseMessaging(JsonObject obj, MessagingConfig def) {
        if (obj == null) return def;
        var codes = parseErrorCodes(obj.getJsonObject("error_codes"), def.errorCodes());
        var names = parseLoggerNames(obj.getJsonObject("logger_names"), def.loggerNames());
        var messages = parseErrorMessages(obj.getJsonObject("error_messages"), def.errorMessages());
        return new MessagingConfig(codes, names, messages);
    }

    private static ErrorCodesConfig parseErrorCodes(JsonObject obj, ErrorCodesConfig def) {
        if (obj == null) return def;
        return new ErrorCodesConfig(obj.getInt("rate_limit", def.rateLimit()));
    }

    private static LoggerNamesConfig parseLoggerNames(JsonObject obj, LoggerNamesConfig def) {
        if (obj == null) return def;
        return new LoggerNamesConfig(
                obj.getString("server", def.server()),
                obj.getString("parser", def.parser()),
                obj.getString("cancellation", def.cancellation()));
    }

    private static ErrorMessagesConfig parseErrorMessages(JsonObject obj, ErrorMessagesConfig def) {
        if (obj == null) return def;
        return new ErrorMessagesConfig(
                obj.getString("processing", def.processing()),
                obj.getString("not_initialized", def.notInitialized()),
                obj.getString("parse_error", def.parseError()),
                obj.getString("invalid_request", def.invalidRequest()),
                obj.getString("access_denied", def.accessDenied()),
                obj.getString("timeout", def.timeout()));
    }

    private static SecurityConfig parseSecurity(JsonObject obj, SecurityConfig def) {
        if (obj == null) return def;
        var auth = parseAuth(obj.getJsonObject("auth"), def.auth());
        var privacy = parsePrivacy(obj.getJsonObject("privacy"), def.privacy());
        return new SecurityConfig(auth, privacy);
    }

    private static AuthConfig parseAuth(JsonObject obj, AuthConfig def) {
        if (obj == null) return def;
        return new AuthConfig(
                obj.getString("jwt_secret_env", def.jwtSecretEnv()),
                obj.getString("default_principal", def.defaultPrincipal()));
    }

    private static PrivacyConfig parsePrivacy(JsonObject obj, PrivacyConfig def) {
        if (obj == null) return def;
        return new PrivacyConfig(obj.getString("default_boundary", def.defaultBoundary()));
    }

    private static ClientConfig parseClient(JsonObject obj, ClientConfig def) {
        if (obj == null) return def;
        var info = parseClientInfo(obj.getJsonObject("info"), def.info());
        List<String> caps = obj.containsKey("capabilities") ?
                obj.getJsonArray("capabilities").getValuesAs(JsonString.class)
                        .stream().map(JsonString::getString).toList() : def.capabilities();
        return new ClientConfig(info, caps);
    }

    private static ClientInfoConfig parseClientInfo(JsonObject obj, ClientInfoConfig def) {
        if (obj == null) return def;
        return new ClientInfoConfig(
                obj.getString("name", def.name()),
                obj.getString("display_name", def.displayName()),
                obj.getString("version", def.version()));
    }

    private static HostConfig parseHost(JsonObject obj, HostConfig def) {
        if (obj == null) return def;
        return new HostConfig(obj.getString("principal", def.principal()));
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
        if (c.performance().runtime().rateLimiterWindowMs() <= 0 ||
                c.performance().runtime().initialRequestId() < 0) throw new IllegalArgumentException("runtime");
        if (c.server().transport().port() < 0 || c.server().transport().port() > 65_535)
            throw new IllegalArgumentException("port");
        if (c.server().messaging().errorCodes().rateLimit() >= 0)
            throw new IllegalArgumentException("errorCodes.rateLimit must be negative");
    }

    public record SystemConfig(ProtocolConfig protocol, TimeoutsConfig timeouts) {
    }

    public record ProtocolConfig(String version, String compatibilityVersion) {
    }

    public record TimeoutsConfig(long defaultMs, long pingMs, int processWaitSeconds) {
    }

    public record PerformanceConfig(RateLimitsConfig rateLimits, PaginationConfig pagination, RuntimeConfig runtime) {
    }

    public record RateLimitsConfig(int toolsPerSecond, int completionsPerSecond, int logsPerSecond, int progressPerSecond) {
    }

    public record PaginationConfig(int defaultPageSize, int maxCompletionValues, int sseHistoryLimit, int responseQueueCapacity) {
    }

    public record RuntimeConfig(long rateLimiterWindowMs, long initialRequestId) {
    }

    public record ServerConfig(ServerInfoConfig info, TransportConfig transport, MessagingConfig messaging) {
    }

    public record ServerInfoConfig(String name, String description, String version) {
    }

    public record TransportConfig(String type, int port, List<String> allowedOrigins) {
        public TransportConfig {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
        }
    }

    public record MessagingConfig(ErrorCodesConfig errorCodes, LoggerNamesConfig loggerNames, ErrorMessagesConfig errorMessages) {
    }

    public record ErrorCodesConfig(int rateLimit) {
    }

    public record LoggerNamesConfig(String server, String parser, String cancellation) {
    }

    public record ErrorMessagesConfig(String processing, String notInitialized, String parseError, String invalidRequest,
                                     String accessDenied, String timeout) {
    }

    public record SecurityConfig(AuthConfig auth, PrivacyConfig privacy) {
    }

    public record AuthConfig(String jwtSecretEnv, String defaultPrincipal) {
    }

    public record PrivacyConfig(String defaultBoundary) {
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

