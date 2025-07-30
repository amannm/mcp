package com.amannmalik.mcp.jsonrpc;

public sealed interface RequestId permits RequestId.StringId, RequestId.NumericId {
    static RequestId parse(String raw) {
        if (raw == null) throw new IllegalArgumentException("raw required");
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() > 1) {
            return new StringId(raw.substring(1, raw.length() - 1));
        }
        try {
            return new NumericId(Long.parseLong(raw));
        } catch (NumberFormatException ignore) {
            return new StringId(raw);
        }
    }

    record StringId(String value) implements RequestId {
    }

    record NumericId(long value) implements RequestId {
    }
}
