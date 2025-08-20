package com.amannmalik.mcp.util;

import com.amannmalik.mcp.spi.Root;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class RootChecker {
    private RootChecker() {
    }

    public static boolean withinRoots(URI uri, List<Root> roots) {
        Objects.requireNonNull(roots);
        if (uri == null) return false;
        if (!"file".equalsIgnoreCase(uri.getScheme())) return true;
        if (roots.isEmpty()) return false;

        final Path targetPath;
        try {
            var p = Paths.get(uri);
            targetPath = normalize(p);
        } catch (Exception e) {
            return false;
        }

        return roots.stream()
                .map(Root::uri)
                .map(RootChecker::toPath)
                .flatMap(Optional::stream)
                .anyMatch(targetPath::startsWith);
    }

    private static Optional<Path> toPath(URI uri) {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return Optional.of(normalize(Paths.get(uri)));
        }
        return Optional.empty();
    }

    private static Path normalize(Path p) {
        try {
            return p.toRealPath();
        } catch (Exception e) {
            return p.toAbsolutePath().normalize();
        }
    }
}
