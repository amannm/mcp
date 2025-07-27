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

        // Standard JSON-RPC error codes
        public static final int PARSE_ERROR = -32700;
        public static final int INVALID_REQUEST = -32600;
        public static final int METHOD_NOT_FOUND = -32601;
        public static final int INVALID_PARAMS = -32602;
        public static final int INTERNAL_ERROR = -32603;

        // Common error factory methods
        public static JsonRpcTypes.JsonRpcError createParseError(JsonRpcTypes.RequestId id) {
            return createError(id, PARSE_ERROR, "Parse error");
        }

        public static JsonRpcTypes.JsonRpcError createInvalidRequest(JsonRpcTypes.RequestId id) {
            return createError(id, INVALID_REQUEST, "Invalid Request");
        }

        public static JsonRpcTypes.JsonRpcError createMethodNotFound(JsonRpcTypes.RequestId id, String method) {
            return createError(id, METHOD_NOT_FOUND, "Method not found: " + method);
        }

        public static JsonRpcTypes.JsonRpcError createInvalidParams(JsonRpcTypes.RequestId id, String message) {
            return createError(id, INVALID_PARAMS, "Invalid params: " + message);
        }

        public static JsonRpcTypes.JsonRpcError createInternalError(JsonRpcTypes.RequestId id, String message) {
            return createError(id, INTERNAL_ERROR, "Internal error: " + message);
        }

        // Common request factories
        public static BaseOperations.PingRequest createPingRequest(JsonRpcTypes.RequestId id) {
            return new BaseOperations.PingRequest(id, Optional.empty());
        }

        public static Resources.ListResourcesRequest createListResourcesRequest(JsonRpcTypes.RequestId id) {
            return new Resources.ListResourcesRequest(id, Optional.empty(), Optional.empty());
        }

        public static Resources.ReadResourceRequest createReadResourceRequest(
                JsonRpcTypes.RequestId id, String uri) {
            return new Resources.ReadResourceRequest(id, uri, Optional.empty());
        }

        public static Tools.ListToolsRequest createListToolsRequest(JsonRpcTypes.RequestId id) {
            return new Tools.ListToolsRequest(id, Optional.empty(), Optional.empty());
        }

        public static Tools.CallToolRequest createCallToolRequest(
                JsonRpcTypes.RequestId id, String name, Map<String, Object> arguments) {
            return new Tools.CallToolRequest(id, name, arguments, Optional.empty());
        }

        public static Prompts.ListPromptsRequest createListPromptsRequest(JsonRpcTypes.RequestId id) {
            return new Prompts.ListPromptsRequest(id, Optional.empty(), Optional.empty());
        }

        public static Prompts.GetPromptRequest createGetPromptRequest(
                JsonRpcTypes.RequestId id, String name, Map<String, String> arguments) {
            return new Prompts.GetPromptRequest(id, name, arguments, Optional.empty());
        }

        // Common notification factories
        public static Initialization.InitializedNotification createInitializedNotification() {
            return new Initialization.InitializedNotification(Optional.empty());
        }

        public static BaseOperations.ProgressNotification createProgressNotification(
                BaseProtocol.ProgressToken token, double progress, Optional<Double> total) {
            return new BaseOperations.ProgressNotification(token, progress, total, Optional.empty(), Optional.empty());
        }

        public static BaseOperations.CancelledNotification createCancelledNotification(
                JsonRpcTypes.RequestId requestId, String reason) {
            return new BaseOperations.CancelledNotification(requestId, Optional.of(reason), Optional.empty());
        }

        // Request ID helpers
        public static JsonRpcTypes.StringRequestId stringId(String id) {
            return new JsonRpcTypes.StringRequestId(id);
        }

        public static JsonRpcTypes.NumberRequestId numberId(long id) {
            return new JsonRpcTypes.NumberRequestId(id);
        }

        // Progress token helpers
        public static BaseProtocol.StringProgressToken stringProgressToken(String token) {
            return new BaseProtocol.StringProgressToken(token);
        }

        public static BaseProtocol.NumberProgressToken numberProgressToken(long token) {
            return new BaseProtocol.NumberProgressToken(token);
        }
    }
}
