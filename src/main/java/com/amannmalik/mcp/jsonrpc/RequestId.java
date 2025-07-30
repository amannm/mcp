package com.amannmalik.mcp.jsonrpc;

public sealed interface RequestId permits RequestId.StringId, RequestId.NumericId {
    record StringId(String value) implements RequestId {
    }

    record NumericId(double value) implements RequestId {
    }
}
