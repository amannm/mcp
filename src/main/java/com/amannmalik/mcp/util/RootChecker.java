package com.amannmalik.mcp.util;

import com.amannmalik.mcp.client.roots.Root;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public final class RootChecker {
    private RootChecker() {
    }

    public static boolean withinRoots(String uri, List<Root> roots) {
        Objects.requireNonNull(roots);
        final URI target;
        try {
            target = URI.create(uri);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!"file".equalsIgnoreCase(target.getScheme()) || roots.isEmpty()) return true;

        final Path targetPath;
        try {
            targetPath = Paths.get(target).toRealPath();
        } catch (Exception e) {
            return false;
        }

        return roots.stream()
                .map(Root::uri)
                .map(RootChecker::toRealPath)
                .flatMap(Optional::stream)
                .anyMatch(base -> targetPath.startsWith(base));
    }

    private static Optional<Path> toRealPath(String uri) {
        try {
            URI base = URI.create(uri);
            if ("file".equalsIgnoreCase(base.getScheme())) {
                return Optional.of(Paths.get(base).toRealPath());
            }
        } catch (Exception ignore) {
            // ignore invalid root entries
        }
        return Optional.empty();
    }
}
