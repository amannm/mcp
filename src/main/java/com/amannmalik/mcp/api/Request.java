package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.AbstractEntityCodec;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.net.URI;
import java.util.Map;

public sealed interface Request permits
        Request.CallToolRequest,
        Request.GetPromptRequest,
        Request.PaginatedRequest,
        Request.ReadResourceRequest,
        Request.SetLevelRequest,
        Request.SubscribeRequest,
        Request.UnsubscribeRequest {
    record CallToolRequest(String name, JsonObject arguments, JsonObject _meta) implements Request {
        public CallToolRequest {
            if (name == null) {
                throw new IllegalArgumentException("name required");
            }
            name = ValidationUtil.requireClean(name);
            arguments = arguments == null ? JsonValue.EMPTY_JSON_OBJECT : arguments;
            ValidationUtil.requireMeta(_meta);
        }
    }

    record GetPromptRequest(String name, Map<String, String> arguments, JsonObject _meta) implements Request {
        public GetPromptRequest {
            if (name == null) {
                throw new IllegalArgumentException("name required");
            }
            name = ValidationUtil.requireClean(name);
            arguments = ValidationUtil.requireCleanMap(arguments);
            ValidationUtil.requireMeta(_meta);
        }

        @Override
        public Map<String, String> arguments() {
            return Map.copyOf(arguments);
        }
    }

    record PaginatedRequest(String cursor, JsonObject _meta) implements Request {
        static final JsonCodec<PaginatedRequest> CODEC =
                AbstractEntityCodec.paginatedRequest(
                        PaginatedRequest::cursor,
                        PaginatedRequest::_meta,
                        PaginatedRequest::new);

        public PaginatedRequest {
            ValidationUtil.requireMeta(_meta);
        }
    }

    record ReadResourceRequest(URI uri, JsonObject _meta) implements Request {
        public ReadResourceRequest {
            uri = ValidationUtil.requireAbsoluteUri(uri);
            ValidationUtil.requireMeta(_meta);
        }
    }

    record SetLevelRequest(LoggingLevel level, JsonObject _meta) implements Request {
        public SetLevelRequest {
            if (level == null) {
                throw new IllegalArgumentException("level is required");
            }
            ValidationUtil.requireMeta(_meta);
        }
    }

    record SubscribeRequest(URI uri, JsonObject _meta) implements Request {
        public SubscribeRequest {
            uri = ValidationUtil.requireAbsoluteUri(uri);
            ValidationUtil.requireMeta(_meta);
        }
    }

    record UnsubscribeRequest(URI uri, JsonObject _meta) implements Request {
        public UnsubscribeRequest {
            uri = ValidationUtil.requireAbsoluteUri(uri);
            ValidationUtil.requireMeta(_meta);
        }
    }
}
