package com.amannmalik.mcp.util;

import com.amannmalik.mcp.spi.Root;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public final class RootChecker {
    private RootChecker() {
    }

    public static boolean withinRoots(URI uri, Collection<? extends Root> roots) {
        Objects.requireNonNull(roots, "roots");
        if (uri == null) {
            return false;
        }
        if (!"file".equalsIgnoreCase(uri.getScheme())) {
            return true;
        }
        if (roots.isEmpty()) {
            return false;
        }

        var targetPath = filePath(uri).orElse(null);
        if (targetPath == null) {
            return false;
        }

        return roots.stream()
                .map(Root::uri)
                .flatMap(RootChecker::filePathStream)
                .anyMatch(targetPath::startsWith);
    }

    private static Optional<Path> filePath(URI uri) {
        if (!"file".equalsIgnoreCase(uri.getScheme())) {
            return Optional.empty();
        }
        try {
            return Optional.of(normalize(Paths.get(uri)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Stream<Path> filePathStream(URI uri) {
        return filePath(uri).stream();
    }

    private static Path normalize(Path p) {
        try {
            return p.toRealPath();
        } catch (Exception e) {
            return p.toAbsolutePath().normalize();
        }
    }
}
