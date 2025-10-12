package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListRootsResult(List<Root> roots, JsonObject _meta) implements Result {
    public ListRootsResult {
        roots = ValidationUtil.immutableList(roots);
        ValidationUtil.requireMeta(_meta);
    }

    @Override
    public List<Root> roots() {
        return ValidationUtil.copyList(roots);
    }
}
