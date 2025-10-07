package com.amannmalik.mcp.util;

import java.util.*;

public final class ServiceLoaders {
    private ServiceLoaders() {
    }

    public static <T> T loadSingleton(Class<T> type) {
        Objects.requireNonNull(type, "type");
        try {
            var loader = ServiceLoader.load(type);
            var iterator = loader.iterator();
            if (!iterator.hasNext()) {
                throw new IllegalStateException("No implementation of " + type.getName()
                        + " is registered as a JPMS service. Provide a module that declares 'provides "
                        + type.getName() + " with ...'.");
            }
            T service = null;
            Class<?> implementation = null;
            while (iterator.hasNext()) {
                var candidate = iterator.next();
                if (service == null) {
                    service = candidate;
                    implementation = candidate.getClass();
                    continue;
                }
                if (!candidate.getClass().equals(implementation)) {
                    throw new IllegalStateException("Multiple implementations of " + type.getName()
                            + " detected on the module path. Configure the host to expose only one implementation.");
                }
            }
            return service;
        } catch (ServiceConfigurationError error) {
            throw new IllegalStateException("Failed to load implementation of " + type.getName(), error);
        }
    }
}
