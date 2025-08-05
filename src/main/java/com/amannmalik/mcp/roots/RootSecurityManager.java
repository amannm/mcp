package com.amannmalik.mcp.roots;

import java.util.*;

public final class RootSecurityManager {
    private final Map<String, RootConfig> roots = new LinkedHashMap<>();
    private boolean violationLogged;
    private String lastNotification;

    public void configure(List<RootConfig> configs) {
        configs.forEach(c -> roots.put(c.uri(), c));
    }

    public List<RootConfig> list() {
        return List.copyOf(roots.values());
    }

    public boolean checkAccess(String path) {
        boolean allowed = roots.keySet().stream().anyMatch(path::startsWith);
        if (!allowed) violationLogged = true;
        return allowed;
    }

    public boolean violationLogged() {
        return violationLogged;
    }

    public void addRoot(RootConfig cfg) {
        roots.put(cfg.uri(), cfg);
        lastNotification = "notifications/roots/list_changed";
    }

    public String lastNotification() {
        return lastNotification;
    }

    public record RootConfig(String uri, String name, String permissions) {
        public RootConfig {
            Objects.requireNonNull(uri);
            Objects.requireNonNull(name);
            Objects.requireNonNull(permissions);
        }
    }
}
