package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicReference;

final class SessionVerifier {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private final AtomicReference<String> lastSessionId = new AtomicReference<>();
    private final AtomicReference<String> sessionOwner = new AtomicReference<>();
    private final AtomicReference<Principal> sessionPrincipal = new AtomicReference<>();

    boolean validate(HttpServletRequest req,
                     HttpServletResponse resp,
                     Principal principal,
                     boolean initializing,
                     String protocolVersion) throws IOException {
        String session = sessionId.get();
        String last = lastSessionId.get();
        String header = req.getHeader(TransportHeaders.SESSION_ID);
        String version = req.getHeader(StreamableHttpTransport.PROTOCOL_HEADER);
        if (!sanitize(header, version, resp)) return false;
        if (session == null && initializing) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            session = Base64Util.encodeUrl(bytes);
            sessionId.set(session);
            sessionOwner.set(req.getRemoteAddr());
            sessionPrincipal.set(principal);
            lastSessionId.set(null);
            resp.setHeader(TransportHeaders.SESSION_ID, session);
            return true;
        }
        if (session == null) {
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
        if (!session.equals(header) || !req.getRemoteAddr().equals(sessionOwner.get())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        if (sessionPrincipal.get() != null && !sessionPrincipal.get().id().equals(principal.id())) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        if (!initializing && (version == null || !version.equals(protocolVersion))) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        return true;
    }

    void terminate(boolean recordId) {
        if (recordId) {
            lastSessionId.set(sessionId.get());
        } else {
            lastSessionId.set(null);
        }
        sessionId.set(null);
        sessionOwner.set(null);
        sessionPrincipal.set(null);
    }

    private static boolean sanitize(String sessionHeader,
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
}
