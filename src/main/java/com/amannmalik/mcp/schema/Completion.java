package com.amannmalik.mcp.schema;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Argument autocompletion for prompts and resource templates.
 */
public final class Completion {
    private Completion() {}

    /** Request for completion suggestions. */
    public record CompleteRequest(
            JsonRpcTypes.RequestId id,
            CompletionReference reference,
            CompletionArgument argument,
            Optional<BaseProtocol.ProgressToken> progressToken,
            Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public CompleteRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(reference);
            Objects.requireNonNull(argument);
            Objects.requireNonNull(progressToken);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "complete"; }
    }

    /** Location that supports completion suggestions. */
    public sealed interface CompletionReference
            permits PromptReference, ResourceTemplateReference {}

    /** Reference to a prompt by name. */
    public record PromptReference(String name) implements CompletionReference {
        public PromptReference {
            Objects.requireNonNull(name);
        }
    }

    /** Reference to a resource template by name. */
    public record ResourceTemplateReference(String name) implements CompletionReference {
        public ResourceTemplateReference {
            Objects.requireNonNull(name);
        }
    }

    /** Argument to complete a value for. */
    public record CompletionArgument(String name, Optional<String> value) {
        public CompletionArgument {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);
        }
    }

    /** Suggestions returned from completion. */
    public record CompletionData(List<String> suggestions) {
        public CompletionData {
            suggestions = List.copyOf(suggestions);
        }
    }

    /** Completion response payload. */
    public record CompleteResult(
            JsonRpcTypes.RequestId id,
            CompletionData completion,
            Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Result {
        public CompleteResult {
            Objects.requireNonNull(id);
            Objects.requireNonNull(completion);
            Objects.requireNonNull(_meta);
        }
    }
}
