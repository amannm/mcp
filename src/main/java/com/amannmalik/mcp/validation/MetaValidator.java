package com.amannmalik.mcp.validation;

import jakarta.json.JsonObject;

import java.util.regex.Pattern;


public final class MetaValidator {
    private MetaValidator() {}

    private static final Pattern LABEL =
            Pattern.compile("[A-Za-z](?:[A-Za-z0-9-]*[A-Za-z0-9])?");
    private static final Pattern NAME =
            Pattern.compile("(?:[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?)?");

    private static final Pattern RESERVED_PREFIX =
            Pattern.compile("(?:^|\\.)(?:modelcontextprotocol|mcp)\\.", Pattern.CASE_INSENSITIVE);

    public static void requireValid(String key) {
        if (key == null) throw new IllegalArgumentException("key required");
        int slash = key.indexOf('/');
        String prefix = slash >= 0 ? key.substring(0, slash) : null;
        String name = slash >= 0 ? key.substring(slash + 1) : key;

        if (prefix != null && !prefix.isEmpty()) {
            for (String label : prefix.split("\\.")) {
                if (!LABEL.matcher(label).matches()) {
                    throw new IllegalArgumentException("Invalid _meta prefix: " + key);
                }
            }
            if (RESERVED_PREFIX.matcher(prefix).find()) {
                throw new IllegalArgumentException("Reserved _meta prefix: " + prefix + "/");
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
