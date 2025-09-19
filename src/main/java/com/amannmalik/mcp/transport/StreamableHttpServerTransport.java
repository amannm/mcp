package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.auth.AuthorizationManager;
import com.amannmalik.mcp.core.MessageDispatcher;
import com.amannmalik.mcp.core.MessageRouter;
import com.amannmalik.mcp.spi.Principal;
import com.amannmalik.mcp.util.PlatformLog;
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
import java.lang.System.Logger;
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
import java.util.Objects;

public final class StreamableHttpServerTransport implements Transport {
    // Default to the previous protocol revision when the version header is
    // absent, as recommended for backwards compatibility.
    static final String COMPATIBILITY_VERSION =
            Protocol.PREVIOUS_VERSION;
    private static final Logger LOG = PlatformLog.get(StreamableHttpServerTransport.class);
    private static final JsonObject CLOSE_SIGNAL = Json.createObjectBuilder().add("_close", true).build();
    private static final Principal DEFAULT_PRINCIPAL = new Principal(
            McpServerConfiguration.defaultConfiguration().defaultPrincipal(), Set.of());
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
    private final MessageDispatcher dispatcher;
    private final AtomicBoolean closed = new AtomicBoolean();

    public StreamableHttpServerTransport(McpServerConfiguration config,
                                         AuthorizationManager auth) throws Exception {
        this.config = Objects.requireNonNull(config, "config");
        this.sessions = new SessionManager(COMPATIBILITY_VERSION, config.sessionIdByteLength());
        this.server = new Server();
        server.setHandler(servletContext(config));
        var http = httpConnector(server, config);
        var https = httpsConnector(server, config);
        startServer();
        this.port = http != null ? http.getLocalPort() : https.getLocalPort();
        this.httpsPort = https != null ? https.getLocalPort() : -1;
        this.allowedOrigins = ValidationUtil.requireAllowedOrigins(Set.copyOf(config.allowedOrigins()));
        this.authManager = auth;
        var scheme = https != null ? "https" : "http";
        this.resourceMetadataUrl = metadataUrl(config, scheme, https != null ? this.httpsPort : this.port, https != null);
        this.canonicalResource = scheme + "://" + config.bindAddress() + ":" + (https != null ? this.httpsPort : this.port);
        this.authorizationServers = authorizationServers(config, https != null);
        var router = new MessageRouter(
                clients::requestClient,
                clients::removeResponse,
                clients::generalSnapshot,
                clients::lastGeneralClient,
                clients::removeRequest);
        this.dispatcher = new MessageDispatcher(router);
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

    private void startServer() throws Exception {
        try {
            server.start();
        } catch (Exception e) {
            server.stop();
            server.destroy();
            throw e;
        }
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

    private List<String> authorizationServers(McpServerConfiguration config, boolean https) {
        if (config.authServers().isEmpty()) {
            return List.of();
        }
        if (https && config.authServers().stream().anyMatch(u -> u.startsWith("http://"))) {
            throw new IllegalArgumentException("HTTPS required for authorization server URLs");
        }
        return List.copyOf(config.authServers());
    }

    public String canonicalResource() {
        return canonicalResource;
    }

    public List<String> authorizationServers() {
        if (authorizationServers.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(authorizationServers));
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
    public JsonObject receive(Duration timeoutMillis) throws IOException {
        try {
            var obj = incoming.poll(timeoutMillis.toMillis(), TimeUnit.MILLISECONDS);
            if (obj == null) {
                throw new IOException("Timeout after " + timeoutMillis + "ms waiting for message");
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

    Optional<Principal> authorize(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return AuthorizationUtil.authorize(
                authManager,
                req,
                resp,
                resourceMetadataUrl,
                DEFAULT_PRINCIPAL);
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
        var header = req.getHeader("Accept");
        if (header == null) {
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return false;
        }
        AcceptHeader accept;
        try {
            accept = AcceptHeader.parse(header);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return false;
        }
        var ok = post
                ? accept.matchesExactly(AcceptHeader.APPLICATION_JSON, AcceptHeader.TEXT_EVENT_STREAM)
                : accept.matchesExactly(AcceptHeader.TEXT_EVENT_STREAM);
        if (!ok) {
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }
        return ok;
    }

    boolean validateSession(HttpServletRequest req,
                            HttpServletResponse resp,
                            Principal principal,
                            boolean initializing) throws IOException {
        return sessions.validate(req, resp, principal, initializing);
    }

    private SseClient createClient(AsyncContext context) throws IOException {
        return new SseClient(context, config.sseClientPrefixByteLength(), config.sseHistoryLimit());
    }

    void terminateSession(boolean recordId) {
        sessions.terminate(recordId);
        clients.clear();
        closed.set(true);
        signalClosure();
    }

    private void signalClosure() {
        if (!incoming.offer(CLOSE_SIGNAL)) {
            throw new IllegalStateException("incoming queue full");
        }
    }
}
