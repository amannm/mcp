package com.amannmalik.mcp.jsonrpc;

public sealed interface RequestId permits RequestId.StringId, RequestId.NumericId, RequestId.NullId {

    static RequestId parse(String raw) {
        if (raw == null) throw new IllegalArgumentException("raw required");
        if (raw.equals("null")) return NullId.INSTANCE;
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
        @Override
        public String toString() {
            return value;
        }
    }

    record NumericId(long value) implements RequestId {
        @Override
        public String toString() {
            return Long.toString(value);
        }
    }

    enum NullId implements RequestId {
        INSTANCE;

        @Override
        public String toString() {
            return "null";
        }
    }
}
