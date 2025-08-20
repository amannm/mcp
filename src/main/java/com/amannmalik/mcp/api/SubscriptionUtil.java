package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.LifecycleState;

import java.io.IOException;
import java.util.function.Supplier;

final class SubscriptionUtil {
    private SubscriptionUtil() {
    }

    static <S extends AutoCloseable> S subscribeListChanges(
            Supplier<LifecycleState> state,
            SubscriptionFactory<S> factory,
            IoRunnable listener) {
        return factory.onListChanged(() -> {
            if (state.get() != LifecycleState.OPERATION) {
                return;
            }
            try {
                listener.run();
            } catch (IOException e) {
                throw new RuntimeException(e);
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
