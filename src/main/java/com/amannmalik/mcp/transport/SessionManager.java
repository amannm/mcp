package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.McpServerConfiguration;
import com.amannmalik.mcp.spi.Principal;
import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/// - [Lifecycle](specification/2025-06-18/basic/lifecycle.mdx)
/// - [Security Best Practices](specification/2025-06-18/basic/security_best_practices.mdx)
final class SessionManager {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AtomicReference<SessionState> current = new AtomicReference<>();
    private final AtomicReference<String> lastSessionId = new AtomicReference<>();
    private final String compatibilityVersion;
    private final int sessionIdByteLength;
    private final AtomicReference<String> protocolVersion;

    SessionManager(String compatibilityVersion) {
        this(compatibilityVersion, McpServerConfiguration.defaultConfiguration().sessionIdByteLength());
    }

    SessionManager(String compatibilityVersion, int sessionIdByteLength) {
        this.compatibilityVersion = Objects.requireNonNull(compatibilityVersion, "compatibilityVersion");
        if (sessionIdByteLength <= 0) {
            throw new IllegalArgumentException("sessionIdByteLength must be positive");
        }
        this.sessionIdByteLength = sessionIdByteLength;
        this.protocolVersion = new AtomicReference<>(compatibilityVersion);
    }

    String protocolVersion() {
        return protocolVersion.get();
    }

    void protocolVersion(String version) {
        this.protocolVersion.set(Objects.requireNonNull(version, "version"));
    }

    boolean validate(HttpServletRequest req,
                     HttpServletResponse resp,
                     Principal principal,
                     boolean initializing) throws IOException {
        Objects.requireNonNull(req, "req");
        Objects.requireNonNull(resp, "resp");
        Objects.requireNonNull(principal, "principal");
        var sanitized = sanitizeHeaders(
                sessionId(req),
                req.getHeader(TransportHeaders.PROTOCOL_VERSION),
                resp);
        if (sanitized.isEmpty()) {
            return false;
        }
        var headers = sanitized.get();
        var state = current.get();
        if (state == null) {
            return initializing
                    ? createSession(req, resp, principal)
                    : failForMissingSession(resp, headers.sessionId(), lastSessionId.get());
        }
        if (!validateExistingSession(req, resp, principal, state, headers.sessionId())) {
            return false;
        }
        return validateVersion(initializing, headers.version(), resp);
    }

    private String sessionId(HttpServletRequest req) {
        var header = req.getHeader(TransportHeaders.SESSION_ID);
        if (header != null) {
            return header;
        }
        var cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }
        for (var c : cookies) {
            if (TransportHeaders.SESSION_ID.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private Optional<SanitizedHeaders> sanitizeHeaders(String sessionHeader,
                                                       String versionHeader,
                                                       HttpServletResponse resp) throws IOException {
        if (hasNonVisibleAscii(sessionHeader, resp) || hasNonVisibleAscii(versionHeader, resp)) {
            return Optional.empty();
        }
        return Optional.of(new SanitizedHeaders(sessionHeader, versionHeader));
    }

    private boolean hasNonVisibleAscii(String header, HttpServletResponse resp) throws IOException {
        if (header != null && ValidationUtil.containsNonVisibleAscii(header)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return true;
        }
        return false;
    }

    private boolean createSession(HttpServletRequest req,
                                  HttpServletResponse resp,
                                  Principal principal) {
        var bytes = new byte[sessionIdByteLength];
        RANDOM.nextBytes(bytes);
        var id = Base64Util.encodeUrl(bytes);
        current.set(new SessionState(id, req.getRemoteAddr(), principal));
        lastSessionId.set(null);
        resp.setHeader(TransportHeaders.SESSION_ID, id);
        var cookie = new Cookie(TransportHeaders.SESSION_ID, id);
        cookie.setHttpOnly(true);
        cookie.setSecure(req.isSecure());
        cookie.setPath("/");
        resp.addCookie(cookie);
        return true;
    }

    private boolean failForMissingSession(HttpServletResponse resp,
                                          String header,
                                          String last) throws IOException {
        if (header != null && header.equals(last)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
        return false;
    }

    private boolean validateExistingSession(HttpServletRequest req,
                                            HttpServletResponse resp,
                                            Principal principal,
                                            SessionState state,
                                            String header) throws IOException {
        if (header == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        if (!state.id().equals(header) || !req.getRemoteAddr().equals(state.owner())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        if (!state.principal().id().equals(principal.id())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    private boolean validateVersion(boolean initializing,
                                    String version,
                                    HttpServletResponse resp) throws IOException {
        if (initializing) {
            return true;
        }
        if (version == null || !version.equals(protocolVersion.get())) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        return true;
    }

    void terminate(boolean recordId) {
        var state = current.getAndSet(null);
        if (recordId && state != null) {
            lastSessionId.set(state.id());
        } else {
            lastSessionId.set(null);
        }
        protocolVersion.set(compatibilityVersion);
    }

    private record SessionState(String id, String owner, Principal principal) {
    }
    private record SanitizedHeaders(String sessionId, String version) {
    }
}

