package com.amannmalik.mcp.jsonrpc;

import com.amannmalik.mcp.api.JsonRpcMessage;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.JsonRpcMessageJsonCodec;

public final class JsonRpcCodec {
    private JsonRpcCodec() {
    }

    public static final JsonCodec<JsonRpcMessage> CODEC = new JsonRpcMessageJsonCodec();

}
