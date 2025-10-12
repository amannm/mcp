package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.PlatformLog;

import java.lang.System.Logger;

public sealed interface RequestId permits
        RequestId.StringId,
        RequestId.NumericId,
        RequestId.NullId {
    Logger LOG = PlatformLog.get(RequestId.class);

    static RequestId parse(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("raw required");
        }
        if (raw.equals("null")) {
            return NullId.INSTANCE;
        }
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() > 1) {
            return new StringId(raw.substring(1, raw.length() - 1));
        }
        try {
            return new NumericId(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            LOG.log(Logger.Level.DEBUG, "Non-numeric request id: " + raw, e);
            return new StringId(raw);
        }
    }

    enum NullId implements RequestId {
        INSTANCE;

        @Override
        public String toString() {
            return "null";
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
}
