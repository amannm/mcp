package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.core.MessageDispatcher;
import com.amannmalik.mcp.core.MessageRouter;
import com.amannmalik.mcp.spi.Principal;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StreamableHttpServerTransport implements Transport {
    static final String COMPATIBILITY_VERSION = Protocol.PREVIOUS_VERSION;
    private static final JsonObject CLOSE_SIGNAL = Json.createObjectBuilder().add("_close", true).build();
    private final AuthorizationManager authManager;
    private final String canonicalResource;
    private final List<String> authorizationServers;
    private final BlockingQueue<JsonObject> incoming = new LinkedBlockingQueue<>();
    private final SseClients clients = new SseClients();
    private final SessionManager sessions;
    private final McpServerConfiguration config;
    private final Server server;
    private final int port;
    private final int httpsPort;
    private final Set<String> allowedOrigins;
    private final String resourceMetadataUrl;
    private final Principal defaultPrincipal;
    private final MessageDispatcher dispatcher;
    private final AtomicBoolean closed = new AtomicBoolean();

    public StreamableHttpServerTransport(McpServerConfiguration config,
                                         AuthorizationManager auth) throws Exception {
        this.config = Objects.requireNonNull(config, "config");
        this.sessions = new SessionManager(COMPATIBILITY_VERSION, config.sessionIdByteLength());
        this.defaultPrincipal = createDefaultPrincipal(config);
        var bindings = startServer(config);
        this.server = bindings.server();
        this.port = bindings.httpPort();
        this.httpsPort = bindings.httpsPort();
        this.allowedOrigins = ValidationUtil.requireAllowedOrigins(Set.copyOf(config.allowedOrigins()));
        this.authManager = auth;
        var scheme = bindings.scheme();
        var listenerPort = bindings.primaryPort();
        this.resourceMetadataUrl = metadataUrl(config, scheme, listenerPort, bindings.httpsEnabled());
        this.canonicalResource = scheme + "://" + config.bindAddress() + ":" + listenerPort;
        this.authorizationServers = authorizationServers(config, bindings.httpsEnabled());
        var router = new MessageRouter(clients.routes());
        this.dispatcher = new MessageDispatcher(router);
    }

    private static Principal createDefaultPrincipal(McpServerConfiguration config) {
        var principalId = Objects.requireNonNull(config.defaultPrincipal(), "defaultPrincipal");
        if (principalId.isBlank()) {
            throw new IllegalArgumentException("defaultPrincipal must not be blank");
        }
        return new Principal(principalId, Set.of());
    }

    private static void validateCertificateKeySize(String path, String password, String type) {
        try (var in = Files.newInputStream(Path.of(path))) {
            var ks = KeyStore.getInstance(type);
            ks.load(in, password.toCharArray());
            var aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                var cert = ks.getCertificate(aliases.nextElement());
                if (cert instanceof X509Certificate x509) {
                    var size = switch (x509.getPublicKey()) {
                        case RSAKey k -> k.getModulus().bitLength();
                        case ECKey k -> k.getParams().getCurve().getField().getFieldSize();
                        default -> 0;
                    };
                    if (size < 2048) {
                        throw new IllegalArgumentException("Certificate key size too small: " + size);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to validate certificate key size", e);
        }
    }

    private static void startJetty(Server server) throws Exception {
        try {
            server.start();
        } catch (Exception e) {
            server.stop();
            server.destroy();
            throw e;
        }
    }

    private static List<String> authorizationServers(McpServerConfiguration config, boolean https) {
        if (config.authServers().isEmpty()) {
            return List.of();
        }
        if (https && config.authServers().stream().anyMatch(u -> u.startsWith("http://"))) {
            throw new IllegalArgumentException("HTTPS required for authorization server URLs");
        }
        return List.copyOf(config.authServers());
    }

    private ServletContextHandler servletContext(McpServerConfiguration config) {
        var ctx = new ServletContextHandler();
        for (var path : config.servletPaths()) {
            if (path.equals("/")) {
                ctx.addServlet(new ServletHolder(new McpServlet(this, config.httpResponseQueueCapacity())), "/");
            } else if (path.equals(config.resourceMetadataPath())) {
                ctx.addServlet(new ServletHolder(new MetadataServlet(this)), path);
            }
        }
        return ctx;
    }

    private ServerConnector httpConnector(Server server, McpServerConfiguration config) {
        if (config.serverPort() <= 0) {
            return null;
        }
        var cfg = new HttpConfiguration();
        var connector = new ServerConnector(server, new HttpConnectionFactory(cfg));
        connector.setHost(config.bindAddress());
        connector.setPort(config.serverPort());
        server.addConnector(connector);
        return connector;
    }

    private ServerConnector httpsConnector(Server server, McpServerConfiguration config) {
        if (config.httpsPort() <= 0) {
            return null;
        }
        var cfg = new HttpConfiguration();
        cfg.setSecureScheme("https");
        cfg.setSecurePort(config.httpsPort());
        var ssl = new SslContextFactory.Server();
        ssl.setKeyStorePath(config.keystorePath());
        ssl.setKeyStorePassword(config.keystorePassword());
        ssl.setKeyStoreType(config.keystoreType());
        validateCertificateKeySize(config.keystorePath(), config.keystorePassword(), config.keystoreType());
        ssl.setIncludeProtocols(config.tlsProtocols().toArray(String[]::new));
        ssl.setIncludeCipherSuites(config.cipherSuites().toArray(String[]::new));
        ssl.setUseCipherSuitesOrder(true);
        ssl.setRenegotiationAllowed(false);
        ssl.setEnableOCSP(true);
        ssl.setSessionCachingEnabled(true);
        ssl.setSslSessionTimeout((int) Duration.ofMinutes(5).toSeconds());
        if (config.requireClientAuth()) {
            ssl.setNeedClientAuth(true);
            ssl.setTrustStorePath(config.truststorePath());
            ssl.setTrustStorePassword(config.truststorePassword());
            ssl.setTrustStoreType(config.truststoreType());
        }
        var connector = new ServerConnector(
                server,
                new SslConnectionFactory(ssl, "HTTP/1.1"),
                new HttpConnectionFactory(cfg));
        connector.setHost(config.bindAddress());
        connector.setPort(config.httpsPort());
        server.addConnector(connector);
        return connector;
    }

    private ServerBindings startServer(McpServerConfiguration config) throws Exception {
        var server = new Server();
        server.setHandler(servletContext(config));
        var http = httpConnector(server, config);
        var https = httpsConnector(server, config);
        startJetty(server);
        return new ServerBindings(server, http, https);
    }

    private String metadataUrl(McpServerConfiguration config,
                               String scheme,
                               int port,
                               boolean https) {
        if (config.resourceMetadataUrl() == null || config.resourceMetadataUrl().isBlank()) {
            return String.format(
                    config.resourceMetadataUrlTemplate(),
                    scheme,
                    config.bindAddress(),
                    port);
        }
        if (https && config.resourceMetadataUrl().startsWith("http://")) {
            throw new IllegalArgumentException("HTTPS required for resource metadata URL");
        }
        return config.resourceMetadataUrl();
    }

    public String canonicalResource() {
        return canonicalResource;
    }

    public List<String> authorizationServers() {
        return List.copyOf(authorizationServers);
    }

    public int port() {
        return port;
    }

    public int httpsPort() {
        return httpsPort;
    }

    public String protocolVersion() {
        return sessions.protocolVersion();
    }

    void updateProtocolVersion(String version) {
        sessions.protocolVersion(version);
    }

    Duration initializeRequestTimeout() {
        return config.initializeRequestTimeout();
    }

    @Override
    public void send(JsonObject message) {
        dispatcher.dispatch(message);
    }

    void flushBacklog() {
        dispatcher.flush();
    }

    void submitIncoming(JsonObject message) throws InterruptedException {
        incoming.put(message);
    }

    BlockingQueue<JsonObject> registerResponseQueue(RequestId id, int capacity) {
        return clients.registerResponseQueue(id, capacity);
    }

    void removeResponseQueue(RequestId id) {
        clients.removeResponseQueue(id);
    }

    Optional<String> sessionId() {
        return sessions.currentSessionId();
    }

    SseClient registerGeneralClient(AsyncContext context, String lastEventId) throws IOException {
        return clients.registerGeneral(context, lastEventId, this::createClient);
    }

    SseClient registerRequestClient(RequestId id, AsyncContext context) throws IOException {
        return clients.registerRequest(id, context, this::createClient);
    }

    void unregisterRequestClient(RequestId id, SseClient client) {
        clients.removeRequest(id, client);
    }

    @Override
    public JsonObject receive() throws IOException {
        return receive(config.defaultTimeoutMs());
    }

    @Override
    public JsonObject receive(Duration timeout) throws IOException {
        var duration = ValidationUtil.requirePositive(timeout, "timeout");
        var waitMillis = duration.toMillis();
        try {
            var obj = incoming.poll(waitMillis, TimeUnit.MILLISECONDS);
            if (obj == null) {
                throw new IOException("Timeout after " + waitMillis + "ms waiting for message");
            }
            if (closed.get() && obj.containsKey("_close")) {
                throw new EOFException();
            }
            return obj;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for message", e);
        }
    }

    @Override
    public void close() throws IOException {
        terminateSession(false);
        clients.failPending();
        try {
            server.stop();
            server.join();
            server.destroy();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void listen() {
        // do nothing
    }

    @Override
    public void setProtocolVersion(String version) {
        // do nothing
    }

    Optional<Principal> authorize(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return AuthorizationUtil.authorize(
                authManager,
                req,
                resp,
                resourceMetadataUrl,
                defaultPrincipal);
    }

    boolean enforceHttps(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.isSecure()) {
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            return true;
        }
        return switch (config.httpsMode()) {
            case MIXED -> true;
            case REDIRECT -> {
                var url = "https://" + req.getServerName() + ":" + httpsPort + req.getRequestURI();
                var q = req.getQueryString();
                if (q != null && !q.isEmpty()) {
                    url += "?" + q;
                }
                resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
                resp.setHeader("Location", url);
                yield false;
            }
            case STRICT -> {
                resp.setStatus(HttpServletResponse.SC_UPGRADE_REQUIRED);
                resp.setHeader("Upgrade", "TLS/1.3");
                yield false;
            }
        };
    }

    boolean verifyOrigin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var origin = req.getHeader("Origin");
        if (!ValidationUtil.isAllowedOrigin(origin, allowedOrigins)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        resp.setHeader("Access-Control-Allow-Origin", origin);
        return true;
    }

    boolean validateAccept(HttpServletRequest req, HttpServletResponse resp, boolean post) throws IOException {
        return post
                ? requireAcceptHeader(req, resp, true, AcceptHeader.APPLICATION_JSON, AcceptHeader.TEXT_EVENT_STREAM)
                : requireAcceptHeader(req, resp, false, AcceptHeader.TEXT_EVENT_STREAM);
    }

    boolean validateSession(HttpServletRequest req,
                            HttpServletResponse resp,
                            Principal principal,
                            boolean initializing) throws IOException {
        return sessions.validate(req, resp, principal, initializing);
    }

    private boolean requireAcceptHeader(HttpServletRequest req,
                                        HttpServletResponse resp,
                                        boolean allowAdditional,
                                        String... expectedTypes) throws IOException {
        var header = req.getHeader("Accept");
        if (header == null) {
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return false;
        }
        AcceptHeader parsed;
        try {
            parsed = AcceptHeader.parse(header);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return false;
        }
        var valid = allowAdditional
                ? parsed.containsAll(expectedTypes)
                : parsed.matchesExactly(expectedTypes);
        if (!valid) {
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return false;
        }
        return true;
    }

    private SseClient createClient(AsyncContext context) throws IOException {
        return new SseClient(context, config.sseClientPrefixByteLength(), config.sseHistoryLimit());
    }

    void terminateSession(boolean recordId) {
        sessions.terminate(recordId);
        clients.clear();
        if (closed.compareAndSet(false, true)) {
            signalClosure();
        }
    }

    private void signalClosure() {
        if (!incoming.offer(CLOSE_SIGNAL)) {
            throw new IllegalStateException("incoming queue full");
        }
    }

    private record ServerBindings(Server server, ServerConnector http, ServerConnector https) {
        private int httpPort() {
            if (http != null) {
                return http.getLocalPort();
            }
            if (https != null) {
                return https.getLocalPort();
            }
            throw new IllegalStateException("Server must expose at least one connector");
        }

        private int httpsPort() {
            return https != null ? https.getLocalPort() : -1;
        }

        private boolean httpsEnabled() {
            return https != null;
        }

        private String scheme() {
            return httpsEnabled() ? "https" : "http";
        }

        private int primaryPort() {
            return httpsEnabled() ? httpsPort() : httpPort();
        }
    }
}
