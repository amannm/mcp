package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.List;

public record CreateMessageRequest(
        List<SamplingMessage> messages,
        ModelPreferences modelPreferences,
        String systemPrompt,
        IncludeContext includeContext,
        Double temperature,
        int maxTokens,
        List<String> stopSequences,
        JsonObject metadata,
        JsonObject _meta
) {
    public static final JsonCodec<CreateMessageRequest> JSON = new Codec();

    public enum IncludeContext {NONE, THIS_SERVER, ALL_SERVERS}

    public CreateMessageRequest {
        messages = messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
        systemPrompt = InputSanitizer.cleanNullable(systemPrompt);
        if (stopSequences == null || stopSequences.isEmpty()) {
            stopSequences = List.of();
        } else {
            stopSequences = stopSequences.stream()
                    .map(InputSanitizer::requireClean)
                    .toList();
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be > 0");
        }
        MetaValidator.requireValid(_meta);
    }

    private static final class Codec extends AbstractEntityCodec<CreateMessageRequest> {
        @Override
        public JsonObject toJson(CreateMessageRequest req) {
            JsonArrayBuilder msgs = array();
            req.messages().forEach(m -> msgs.add(SamplingMessage.JSON.toJson(m)));
            JsonObjectBuilder obj = object().add("messages", msgs.build());
            if (req.modelPreferences() != null) obj.add("modelPreferences", ModelPreferences.JSON.toJson(req.modelPreferences()));
            if (req.systemPrompt() != null) obj.add("systemPrompt", req.systemPrompt());
            if (req.includeContext() != null) obj.add("includeContext", switch (req.includeContext()) {
                case NONE -> "none";
                case THIS_SERVER -> "thisServer";
                case ALL_SERVERS -> "allServers";
            });
            if (req.temperature() != null) obj.add("temperature", req.temperature());
            obj.add("maxTokens", req.maxTokens());
            if (!req.stopSequences().isEmpty()) {
                JsonArrayBuilder arr = array();
                req.stopSequences().forEach(arr::add);
                obj.add("stopSequences", arr.build());
            }
            if (req.metadata() != null) obj.add("metadata", req.metadata());
            if (req._meta() != null) obj.add("_meta", req._meta());
            return obj.build();
        }

        @Override
        public CreateMessageRequest fromJson(JsonObject obj) {
            List<SamplingMessage> messages = requireArray(obj, "messages").stream()
                    .map(v -> SamplingMessage.JSON.fromJson(v.asJsonObject()))
                    .toList();
            ModelPreferences prefs = obj.containsKey("modelPreferences")
                    ? ModelPreferences.JSON.fromJson(obj.getJsonObject("modelPreferences"))
                    : null;
            String system = getString(obj, "systemPrompt");
            IncludeContext ctx = null;
            if (obj.containsKey("includeContext")) {
                ctx = switch (obj.getString("includeContext")) {
                    case "none" -> IncludeContext.NONE;
                    case "thisServer" -> IncludeContext.THIS_SERVER;
                    case "allServers" -> IncludeContext.ALL_SERVERS;
                    default -> throw error("Unknown includeContext");
                };
            }
            Double temp = getDouble(obj, "temperature");
            int max = requireInt(obj, "maxTokens");
            List<String> stops = obj.containsKey("stopSequences")
                    ? obj.getJsonArray("stopSequences").getValuesAs(JsonString.class).stream().map(JsonString::getString).toList()
                    : List.of();
            JsonObject metadata = obj.getJsonObject("metadata");
            JsonObject meta = getObject(obj, "_meta");
            return new CreateMessageRequest(messages, prefs, system, ctx, temp, max, stops, metadata, meta);
        }
    }
}
