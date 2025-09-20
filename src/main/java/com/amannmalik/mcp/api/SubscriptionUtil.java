package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.LifecycleState;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Supplier;

final class SubscriptionUtil {
    private SubscriptionUtil() {
    }

    static <S extends AutoCloseable> S subscribeListChanges(
            Supplier<LifecycleState> state,
            SubscriptionFactory<S> factory,
            IoRunnable listener) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(listener, "listener");

        return factory.onListChanged(() -> {
            if (state.get() != LifecycleState.OPERATION) {
                return;
            }
            try {
                listener.run();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @FunctionalInterface
    interface SubscriptionFactory<S extends AutoCloseable> {
        S onListChanged(Runnable listener);
    }

    @FunctionalInterface
    interface IoRunnable {
        void run() throws IOException;
    }
}
