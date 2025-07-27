package com.amannmalik.mcp.schema;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Prompt system operations and data structures.
 */
public final class Prompts {
    private Prompts() {}

    /** Request to list available prompts. */
    public record ListPromptsRequest(JsonRpcTypes.RequestId id,
                                     Optional<Cursor> cursor,
                                     Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public ListPromptsRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(cursor);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "prompts/list"; }
    }

    /** Prompt argument definition. */
    public record PromptArgument(String name,
                                 Optional<String> description) {
        public PromptArgument {
            Objects.requireNonNull(name);
            Objects.requireNonNull(description);
        }
    }

    /** Prompt metadata. */
    public record Prompt(String name,
                         Optional<String> description,
                         List<PromptArgument> arguments) {
        public Prompt {
            Objects.requireNonNull(name);
            Objects.requireNonNull(description);
            arguments = List.copyOf(arguments);
        }
    }

    /** Response to list available prompts. */
    public record ListPromptsResult(JsonRpcTypes.RequestId id,
                                    List<Prompt> prompts,
                                    Optional<Cursor> nextCursor,
                                    Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Result {
        public ListPromptsResult {
            Objects.requireNonNull(id);
            prompts = List.copyOf(prompts);
            Objects.requireNonNull(nextCursor);
            Objects.requireNonNull(_meta);
        }
    }

    /** Request to retrieve a prompt with arguments applied. */
    public record GetPromptRequest(JsonRpcTypes.RequestId id,
                                   String name,
                                   Map<String, String> arguments,
                                   Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public GetPromptRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(name);
            arguments = Map.copyOf(arguments);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "prompts/get"; }
    }

    /** Response to prompt retrieval with arguments applied. */
    public record GetPromptResult(JsonRpcTypes.RequestId id,
                                  List<ContentBlock> content,
                                  Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Result {
        public GetPromptResult {
            Objects.requireNonNull(id);
            content = List.copyOf(content);
            Objects.requireNonNull(_meta);
        }
    }
}
