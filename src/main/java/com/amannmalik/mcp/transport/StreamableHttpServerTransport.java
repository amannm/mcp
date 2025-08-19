package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.auth.AuthorizationManager;
import com.amannmalik.mcp.core.MessageDispatcher;
import com.amannmalik.mcp.core.MessageRouter;
import com.amannmalik.mcp.spi.Principal;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.io.EOFException;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public final class StreamableHttpServerTransport implements Transport {
    // Default to the previous protocol revision when the version header is
    // absent, as recommended for backwards compatibility.
    static final String COMPATIBILITY_VERSION =
            Protocol.PREVIOUS_VERSION;
    private static final Principal DEFAULT_PRINCIPAL = new Principal(
            McpServerConfiguration.defaultConfiguration().defaultPrincipal(), Set.of());
    final AuthorizationManager authManager;
    final String canonicalResource;
    final List<String> authorizationServers;
    final BlockingQueue<JsonObject> incoming = new LinkedBlockingQueue<>();
    final SseClients clients = new SseClients();
    final SessionManager sessions;
    final McpServerConfiguration config;
    private final Server server;
    private final int port;
    private final int httpsPort;
    private final Set<String> allowedOrigins;
    private final String resourceMetadataUrl;
    private final MessageDispatcher dispatcher;
    private volatile boolean closed;

    public StreamableHttpServerTransport(McpServerConfiguration config,
                                         AuthorizationManager auth) throws Exception {
        this.config = config;
        this.sessions = new SessionManager(COMPATIBILITY_VERSION, config.sessionIdByteLength());

        server = new Server();
        ServletContextHandler ctx = new ServletContextHandler();
        for (String path : config.servletPaths()) {
            if (path.equals("/")) {
                ctx.addServlet(new ServletHolder(new McpServlet(this)), "/");
            } else if (path.equals(config.resourceMetadataPath())) {
                ctx.addServlet(new ServletHolder(new MetadataServlet(this)), path);
            }
        }
        server.setHandler(ctx);

        ServerConnector http = null;
        if (config.serverPort() > 0) {
            var httpCfg = new HttpConfiguration();
            http = new ServerConnector(server, new HttpConnectionFactory(httpCfg));
            http.setHost(config.bindAddress());
            http.setPort(config.serverPort());
            server.addConnector(http);
        }

        ServerConnector https = null;
        if (config.httpsPort() > 0) {
            var httpsCfg = new HttpConfiguration();
            httpsCfg.setSecureScheme("https");
            httpsCfg.setSecurePort(config.httpsPort());
            var ssl = new SslContextFactory.Server();
            ssl.setKeyStorePath(config.keystorePath());
            ssl.setKeyStorePassword(config.keystorePassword());
            ssl.setKeyStoreType(config.keystoreType());
            ssl.setIncludeProtocols(config.tlsProtocols().toArray(String[]::new));
            ssl.setIncludeCipherSuites(config.cipherSuites().toArray(String[]::new));
            ssl.setSessionCachingEnabled(true);
            if (config.requireClientAuth()) {
                ssl.setNeedClientAuth(true);
                ssl.setTrustStorePath(config.truststorePath());
                ssl.setTrustStorePassword(config.truststorePassword());
                ssl.setTrustStoreType(config.truststoreType());
            }
            https = new ServerConnector(
                    server,
                    new SslConnectionFactory(ssl, "HTTP/1.1"),
                    new HttpConnectionFactory(httpsCfg));
            https.setHost(config.bindAddress());
            https.setPort(config.httpsPort());
            server.addConnector(https);
        }

        try {
            server.start();
        } catch (Exception e) {
            server.stop();
            server.destroy();
            throw e;
        }
        this.port = http != null ? http.getLocalPort() : https.getLocalPort();
        this.httpsPort = https != null ? https.getLocalPort() : -1;
        this.allowedOrigins = ValidationUtil.requireAllowedOrigins(Set.copyOf(config.allowedOrigins()));
        this.authManager = auth;

        String scheme = https != null ? "https" : "http";
        if (config.resourceMetadataUrl() == null || config.resourceMetadataUrl().isBlank()) {
            int metaPort = https != null ? this.httpsPort : this.port;
            this.resourceMetadataUrl = String.format(
                    config.resourceMetadataUrlTemplate(),
                    scheme,
                    config.bindAddress(),
                    metaPort);
        } else {
            this.resourceMetadataUrl = config.resourceMetadataUrl();
            if (https != null && this.resourceMetadataUrl.startsWith("http://")) {
                throw new IllegalArgumentException("HTTPS required for resource metadata URL");
            }
        }

        this.canonicalResource = scheme + "://" + config.bindAddress() + ":" + (https != null ? this.httpsPort : this.port);
        if (config.authServers().isEmpty()) {
            this.authorizationServers = List.of();
        } else {
            if (https != null && config.authServers().stream().anyMatch(u -> u.startsWith("http://"))) {
                throw new IllegalArgumentException("HTTPS required for authorization server URLs");
            }
            this.authorizationServers = List.copyOf(config.authServers());
        }
        var router = new MessageRouter(
                clients.request,
                clients.responses,
                clients.general,
                clients.lastGeneral,
                clients::removeRequest);
        this.dispatcher = new MessageDispatcher(router);
    }

    public int port() {
        return port;
    }

    public int httpsPort() {
        return httpsPort;
    }

    @Override
    public void send(JsonObject message) {
        dispatcher.dispatch(message);
    }

    void flushBacklog() {
        dispatcher.flush();
    }

    @Override
    public JsonObject receive() throws IOException {
        return receive(config.defaultTimeoutMs());
    }

    @Override
    public JsonObject receive(Duration timeoutMillis) throws IOException {
        try {
            JsonObject obj = incoming.poll(timeoutMillis.toMillis(), TimeUnit.MILLISECONDS);
            if (obj == null) {
                throw new IOException("Timeout after " + timeoutMillis + "ms waiting for message");
            }
            if (closed && obj.containsKey("_close")) {
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

    boolean verifyOrigin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!ValidationUtil.isAllowedOrigin(req.getHeader("Origin"), allowedOrigins)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    boolean validateAccept(HttpServletRequest req, HttpServletResponse resp, boolean post) throws IOException {
        String accept = req.getHeader("Accept");
        if (accept == null) {
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return false;
        }
        var types = Arrays.stream(accept.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        boolean ok = post
                ? types.size() == 2 && types.contains("application/json") && types.contains("text/event-stream")
                : types.size() == 1 && types.contains("text/event-stream");
        if (!ok) resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
        return ok;
    }

    boolean validateSession(HttpServletRequest req,
                            HttpServletResponse resp,
                            Principal principal,
                            boolean initializing) throws IOException {
        return sessions.validate(req, resp, principal, initializing);
    }

    void terminateSession(boolean recordId) {
        sessions.terminate(recordId);
        clients.clear();
        closed = true;
        if (!incoming.offer(Json.createObjectBuilder().add("_close", true).build())) {
            throw new IllegalStateException("incoming queue full");
        }
    }

}
