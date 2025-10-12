package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.SpiPreconditions;
import jakarta.json.JsonObject;

import java.util.List;

public record ListRootsResult(List<Root> roots, JsonObject _meta) implements Result {
    public ListRootsResult {
        roots = SpiPreconditions.immutableList(roots);
        SpiPreconditions.requireMeta(_meta);
    }

    @Override
    public List<Root> roots() {
        return SpiPreconditions.copyList(roots);
    }
}
