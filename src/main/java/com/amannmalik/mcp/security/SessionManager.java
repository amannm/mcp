package com.amannmalik.mcp.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, String> sessions;

    public SessionManager() {
        this(new ConcurrentHashMap<>());
    }

    public SessionManager(Map<String, String> sessions) {
        this.sessions = sessions;
    }

    public String create(String user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(id, user);
        return id;
    }

    public boolean validate(String id, String user) {
        return user.equals(sessions.get(id));
    }

    public String owner(String id) {
        return sessions.get(id);
    }
}
