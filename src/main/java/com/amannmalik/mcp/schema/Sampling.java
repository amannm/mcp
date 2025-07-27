package com.amannmalik.mcp.schema;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Sampling system for LLM interaction.
 */
public final class Sampling {
    private Sampling() {}

    /** Actor role for a sampling message. */
    public enum Role { USER, ASSISTANT, SYSTEM, TOOL }

    /** Message exchanged with the model. */
    public sealed interface SamplingMessage permits ContentMessage {
        Role role();
        List<ContentBlock> content();
    }

    /** Simple message composed of content blocks. */
    public record ContentMessage(Role role, List<ContentBlock> content)
            implements SamplingMessage {
        public ContentMessage {
            Objects.requireNonNull(role);
            content = List.copyOf(content);
        }
    }

    /** Hint guiding model selection. */
    public sealed interface ModelHint permits NamedModel, ProviderModel {
        String value();
    }

    public record NamedModel(String value) implements ModelHint {
        public NamedModel {
            Objects.requireNonNull(value);
        }
    }

    public record ProviderModel(String value) implements ModelHint {
        public ProviderModel {
            Objects.requireNonNull(value);
        }
    }

    /** Preferences controlling sampling behavior. */
    public record ModelPreferences(Optional<List<ModelHint>> hints,
                                   Optional<Double> temperature,
                                   Optional<Integer> maxTokens,
                                   Optional<Double> costPriority) {
        public ModelPreferences {
            Objects.requireNonNull(hints);
            Objects.requireNonNull(temperature);
            Objects.requireNonNull(maxTokens);
            Objects.requireNonNull(costPriority);
            hints = hints.map(List::copyOf);
            temperature = temperature.filter(t -> t >= 0);
            maxTokens = maxTokens.filter(t -> t > 0);
        }
    }

    /** Request to create a new assistant message. */
    public record CreateMessageRequest(
            JsonRpcTypes.RequestId id,
            List<SamplingMessage> messages,
            Optional<ModelPreferences> modelPreferences,
            Optional<BaseProtocol.ProgressToken> progressToken,
            Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public CreateMessageRequest {
            Objects.requireNonNull(id);
            messages = List.copyOf(messages);
            Objects.requireNonNull(modelPreferences);
            Objects.requireNonNull(progressToken);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "createMessage"; }
    }
}
