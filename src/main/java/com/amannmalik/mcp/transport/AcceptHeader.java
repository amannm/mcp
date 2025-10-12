package com.amannmalik.mcp.transport;

import java.util.*;

final class AcceptHeader {
    static final String APPLICATION_JSON = "application/json";
    static final String TEXT_EVENT_STREAM = "text/event-stream";
    private final Set<String> mediaTypes;

    private AcceptHeader(Set<String> mediaTypes) {
        this.mediaTypes = mediaTypes;
    }

    static AcceptHeader parse(String header) {
        Objects.requireNonNull(header, "header");
        Set<String> types = new LinkedHashSet<>();
        for (var token : header.split(",")) {
            normalize(token).ifPresent(types::add);
        }
        if (types.isEmpty()) {
            throw new IllegalArgumentException("No media types present");
        }
        return new AcceptHeader(Set.copyOf(types));
    }

    private static Optional<String> normalize(String token) {
        var trimmed = token.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        var semicolon = trimmed.indexOf(';');
        var type = semicolon >= 0 ? trimmed.substring(0, semicolon) : trimmed;
        var cleaned = type.trim();
        if (cleaned.isEmpty()) {
            return Optional.empty();
        }
        if (cleaned.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Invalid media type: " + token);
        }
        var slash = cleaned.indexOf('/');
        if (slash <= 0 || slash == cleaned.length() - 1) {
            throw new IllegalArgumentException("Invalid media type: " + token);
        }
        return Optional.of(cleaned.toLowerCase(Locale.ROOT));
    }

    private static String normalizeType(String type) {
        Objects.requireNonNull(type, "type");
        return type.toLowerCase(Locale.ROOT);
    }

    boolean containsAll(String... requiredTypes) {
        for (var type : requiredTypes) {
            var normalized = normalizeType(type);
            if (!mediaTypes.contains(normalized)) {
                return false;
            }
        }
        return true;
    }

    boolean matchesExactly(String... requiredTypes) {
        Set<String> expected = new LinkedHashSet<>(requiredTypes.length);
        for (var type : requiredTypes) {
            expected.add(normalizeType(type));
        }
        return mediaTypes.equals(Set.copyOf(expected));
    }
}
