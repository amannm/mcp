package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.validation.InputSanitizer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicReference;

final class SessionManager {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static record SessionState(String id, String owner, Principal principal) {}
    private final AtomicReference<SessionState> current = new AtomicReference<>();
    private final AtomicReference<String> lastSessionId = new AtomicReference<>();
    private volatile String protocolVersion;
    private final String compatibilityVersion;

    SessionManager(String compatibilityVersion) {
        this.compatibilityVersion = compatibilityVersion;
        this.protocolVersion = compatibilityVersion;
    }

    String protocolVersion() {
        return protocolVersion;
    }

    void protocolVersion(String version) {
        this.protocolVersion = version;
    }

    boolean validate(HttpServletRequest req,
                     HttpServletResponse resp,
                     Principal principal,
                     boolean initializing) throws IOException {
        if (principal == null) throw new IllegalArgumentException("principal required");
        SessionState state = current.get();
        String last = lastSessionId.get();
        String header = req.getHeader(TransportHeaders.SESSION_ID);
        String version = req.getHeader(TransportHeaders.PROTOCOL_VERSION);
        if (!sanitizeHeaders(header, version, resp)) {
            return false;
        }
        return checkSession(req, resp, principal, initializing, state, last, header, version);
    }

    private boolean sanitizeHeaders(String sessionHeader,
                                    String versionHeader,
                                    HttpServletResponse resp) throws IOException {
        if (sessionHeader != null && !InputSanitizer.isVisibleAscii(sessionHeader)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        if (versionHeader != null && !InputSanitizer.isVisibleAscii(versionHeader)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        return true;
    }

    private boolean checkSession(HttpServletRequest req,
                                 HttpServletResponse resp,
                                 Principal principal,
                                 boolean initializing,
                                 SessionState state,
                                 String last,
                                 String header,
                                 String version) throws IOException {
        if (state == null && initializing) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String id = Base64Util.encodeUrl(bytes);
            current.set(new SessionState(id, req.getRemoteAddr(), principal));
            lastSessionId.set(null);
            resp.setHeader(TransportHeaders.SESSION_ID, id);
            return true;
        }
        if (state == null) {
            if (header != null && header.equals(last)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
            return false;
        }
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
        if (!initializing) {
            // Per spec 2025-06-18, the client should send MCP-Protocol-Version on
            // subsequent requests. For backwards compatibility, tolerate a
            // missing header when the negotiated version is already known.
            if (version != null && !version.equals(protocolVersion)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return false;
            }
        }
        return true;
    }

    void terminate(boolean recordId) {
        SessionState state = current.getAndSet(null);
        if (recordId && state != null) {
            lastSessionId.set(state.id());
        } else {
            lastSessionId.set(null);
        }
        protocolVersion = compatibilityVersion;
    }
}

