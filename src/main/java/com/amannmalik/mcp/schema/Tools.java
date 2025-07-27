package com.amannmalik.mcp.schema;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Structured tool discovery and invocation messages.
 */
public final class Tools {
    private Tools() {}

    /** Request listing available tools. */
    public record ListToolsRequest(JsonRpcTypes.RequestId id,
                                   Optional<Cursor> cursor,
                                   Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public ListToolsRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(cursor);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "tools/list"; }
    }

    /** Schema describing structured tool input. */
    public record ToolInputSchema(Map<String, PrimitiveSchemaDefinition> properties,
                                  Optional<List<String>> required) {
        public ToolInputSchema {
            properties = Map.copyOf(properties);
            Objects.requireNonNull(required);
        }
    }

    /** Schema describing structured tool output. */
    public record ToolOutputSchema(Map<String, PrimitiveSchemaDefinition> properties,
                                   Optional<List<String>> required) {
        public ToolOutputSchema {
            properties = Map.copyOf(properties);
            Objects.requireNonNull(required);
        }
    }

    /** Tool description. */
    public record Tool(String name,
                       ToolInputSchema inputSchema,
                       Optional<ToolOutputSchema> outputSchema,
                       Optional<List<ContentBlock>> description,
                       Optional<Map<String, Object>> hints) {
        public Tool {
            Objects.requireNonNull(name);
            Objects.requireNonNull(inputSchema);
            Objects.requireNonNull(outputSchema);
            Objects.requireNonNull(description);
            Objects.requireNonNull(hints);
        }
    }

    /** Response to list available tools. */
    public record ListToolsResult(JsonRpcTypes.RequestId id,
                                  List<Tool> tools,
                                  Optional<Cursor> nextCursor,
                                  Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Result {
        public ListToolsResult {
            Objects.requireNonNull(id);
            tools = List.copyOf(tools);
            Objects.requireNonNull(nextCursor);
            Objects.requireNonNull(_meta);
        }
    }

    /** Request to invoke a tool with arguments. */
    public record CallToolRequest(JsonRpcTypes.RequestId id,
                                  String name,
                                  Map<String, Object> arguments,
                                  Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public CallToolRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(name);
            arguments = Map.copyOf(arguments);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "tools/call"; }
    }

    /** Response to tool invocation. */
    public record CallToolResult(JsonRpcTypes.RequestId id,
                                 List<ContentBlock> content,
                                 Optional<Boolean> isError,
                                 Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Result {
        public CallToolResult {
            Objects.requireNonNull(id);
            content = List.copyOf(content);
            Objects.requireNonNull(isError);
            Objects.requireNonNull(_meta);
        }
    }
}
