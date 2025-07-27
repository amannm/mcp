package com.amannmalik.mcp.schema;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-initiated user input requests.
 */
public final class Elicitation {
    private Elicitation() {}

    public enum ElicitAction { SUBMIT, CANCEL }

    public record ElicitRequest(JsonRpcTypes.RequestId id,
                                String message,
                                ElicitationSchema requestedSchema,
                                Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public ElicitRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(message);
            Objects.requireNonNull(requestedSchema);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "elicitation/elicit"; }
    }

    public record ElicitationSchema(Map<String, PrimitiveSchemaDefinition> properties,
                                    Optional<List<String>> required) {
        public ElicitationSchema {
            properties = Map.copyOf(properties);
            Objects.requireNonNull(required);
        }
    }

    public record ElicitResult(JsonRpcTypes.RequestId id,
                               ElicitAction action,
                               Optional<Map<String, Object>> content,
                               Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Result {
        public ElicitResult {
            Objects.requireNonNull(id);
            Objects.requireNonNull(action);
            Objects.requireNonNull(content);
            Objects.requireNonNull(_meta);
        }
    }
}
