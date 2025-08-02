package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.lifecycle.Protocol;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    private static HttpServletRequest request(String session, String version, String remote) {
        Map<String, String> headers = new HashMap<>();
        if (session != null) headers.put(TransportHeaders.SESSION_ID, session);
        if (version != null) headers.put(TransportHeaders.PROTOCOL_VERSION, version);
        InvocationHandler h = (proxy, method, args) -> switch (method.getName()) {
            case "getHeader" -> headers.get(args[0]);
            case "getRemoteAddr" -> remote;
            default -> defaultValue(method);
        };
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                h);
    }

    private static class ResponseHandler implements InvocationHandler {
        int status;
        final Map<String, String> headers = new HashMap<>();
        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "setHeader" -> { headers.put((String) args[0], (String) args[1]); yield null; }
                case "sendError" -> { status = (int) args[0]; yield null; }
                default -> defaultValue(method);
            };
        }
    }

    private static HttpServletResponse response(ResponseHandler h) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                h);
    }

    private static Object defaultValue(Method m) {
        Class<?> rt = m.getReturnType();
        if (rt.equals(boolean.class)) return false;
        if (rt.equals(int.class)) return 0;
        if (rt.equals(long.class)) return 0L;
        return null;
    }

    @Test
    void allowsMissingProtocolHeaderWhenSessionEstablished() throws Exception {
        SessionManager mgr = new SessionManager(Protocol.PREVIOUS_VERSION);
        Principal p = new Principal("u", Set.of());

        ResponseHandler initResp = new ResponseHandler();
        assertTrue(mgr.validate(request(null, null, "127.0.0.1"), response(initResp), p, true));
        String session = initResp.headers.get(TransportHeaders.SESSION_ID);
        mgr.protocolVersion(Protocol.LATEST_VERSION);

        ResponseHandler resp = new ResponseHandler();
        assertTrue(mgr.validate(request(session, null, "127.0.0.1"), response(resp), p, false));
        assertEquals(0, resp.status);
    }

    @Test
    void rejectsMismatchedProtocolVersion() throws Exception {
        SessionManager mgr = new SessionManager(Protocol.PREVIOUS_VERSION);
        Principal p = new Principal("u", Set.of());
        ResponseHandler initResp = new ResponseHandler();
        assertTrue(mgr.validate(request(null, null, "127.0.0.1"), response(initResp), p, true));
        String session = initResp.headers.get(TransportHeaders.SESSION_ID);
        mgr.protocolVersion(Protocol.LATEST_VERSION);

        ResponseHandler resp = new ResponseHandler();
        assertFalse(mgr.validate(request(session, "2024-01-01", "127.0.0.1"), response(resp), p, false));
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, resp.status);
    }
}
