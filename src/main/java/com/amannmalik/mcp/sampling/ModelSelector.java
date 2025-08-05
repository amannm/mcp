package com.amannmalik.mcp.sampling;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ModelSelector {
    public String select(List<Hint> hints) {
        return hints.stream()
                .sorted(Comparator.comparingInt(Hint::order))
                .map(Hint::hint)
                .findFirst()
                .orElseThrow();
    }

    public record Hint(String hint, int order) {
        public Hint {
            Objects.requireNonNull(hint);
        }
    }
}
