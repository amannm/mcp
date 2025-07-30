package com.amannmalik.mcp.util;

import com.amannmalik.mcp.client.roots.Root;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** Utility for verifying resource URIs are within allowed roots. */
public final class RootChecker {
    private RootChecker() {
    }

    /**
     * Returns {@code true} if the given URI is within one of the provided roots.
     * Non-file URIs are always allowed.
     */
    public static boolean withinRoots(String uri, List<Root> roots) {
        URI target;
        try {
            target = URI.create(uri);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!"file".equalsIgnoreCase(target.getScheme())) {
            return true;
        }
        if (roots == null || roots.isEmpty()) {
            return true;
        }
        Path targetPath;
        try {
            targetPath = Paths.get(target).normalize();
        } catch (Exception e) {
            return false;
        }
        for (Root r : roots) {
            try {
                URI base = URI.create(r.uri());
                if ("file".equalsIgnoreCase(base.getScheme())) {
                    Path basePath = Paths.get(base).normalize();
                    if (targetPath.startsWith(basePath)) {
                        return true;
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return false;
    }
}
