package com.amannmalik.mcp.server.roots.validation;

import jakarta.json.JsonObject;

import java.util.regex.Pattern;

public final class MetaValidator {
    private MetaValidator() {
    }

    private static final Pattern LABEL =
            Pattern.compile("[A-Za-z](?:[A-Za-z0-9-]*[A-Za-z0-9])?");
    private static final Pattern NAME =
            Pattern.compile("(?:[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?)?");

    public static void requireValid(String key) {
        if (key == null) throw new IllegalArgumentException("key required");
        int slash = key.indexOf('/');
        String prefix = slash >= 0 ? key.substring(0, slash) : null;
        String name = slash >= 0 ? key.substring(slash + 1) : key;

        if (slash == 0) {
            throw new IllegalArgumentException("_meta prefix must not be empty: " + key);
        }
        if (slash >= 0 && key.indexOf('/', slash + 1) >= 0) {
            throw new IllegalArgumentException("_meta key may contain at most one '/' character: " + key);
        }

        if (prefix != null) {
            String[] labels = prefix.split("\\.");
            for (int i = 0; i < labels.length; i++) {
                String label = labels[i];
                if (!LABEL.matcher(label).matches()) {
                    throw new IllegalArgumentException("Invalid _meta prefix: " + key);
                }
                if (i < labels.length - 1 && (label.equals("modelcontextprotocol") || label.equals("mcp"))) {
                    throw new IllegalArgumentException("Reserved _meta prefix: " + key);
                }
            }
        }

        if (!NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid _meta name: " + key);
        }
    }

    public static void requireValid(JsonObject obj) {
        if (obj == null) return;
        for (String key : obj.keySet()) requireValid(key);
    }
}
