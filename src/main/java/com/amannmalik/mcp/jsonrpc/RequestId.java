package com.amannmalik.mcp.jsonrpc;

public sealed interface RequestId permits RequestId.StringId, RequestId.NumericId {
    String asString();

    record StringId(String value) implements RequestId {
        @Override
        public String asString() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    record NumericId(long value) implements RequestId {
        @Override
        public String asString() {
            return Long.toString(value);
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }
    }
}
