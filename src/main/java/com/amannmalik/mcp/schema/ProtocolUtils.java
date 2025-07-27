package com.amannmalik.mcp.schema;

import java.util.Map;
import java.util.Optional;

/**
 * Protocol constants and helper factory methods.
 */
public final class ProtocolUtils {
    private ProtocolUtils() {}

    public static final class McpProtocol {
        private McpProtocol() {}

        public static final String PROTOCOL_VERSION = "2025-06-18";
        public static final String JSONRPC_VERSION = JsonRpcTypes.JSONRPC_VERSION;

        public static JsonRpcTypes.JsonRpcError createError(
                JsonRpcTypes.RequestId id,
                int code,
                String message) {
            return new JsonRpcTypes.BasicError(id, code, message);
        }

        public static Initialization.InitializeRequest createInitializeRequest(
                JsonRpcTypes.RequestId id,
                Capabilities.ClientCapabilities capabilities,
                Optional<Map<String, Object>> clientInfo,
                Optional<Map<String, Object>> _meta) {
            return new Initialization.InitializeRequest(
                    id,
                    PROTOCOL_VERSION,
                    capabilities,
                    clientInfo,
                    _meta);
        }
    }
}
