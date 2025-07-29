package com.amannmalik.mcp.jsonrpc;

/** Identifier for a JSON-RPC request. */
public sealed interface RequestId permits RequestId.StringId, RequestId.NumericId {
    record StringId(String value) implements RequestId {}
    record NumericId(long value) implements RequestId {}
}
