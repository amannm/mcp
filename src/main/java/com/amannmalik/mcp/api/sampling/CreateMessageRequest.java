package com.amannmalik.mcp.api.sampling;

import com.amannmalik.mcp.api.JsonCodec;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.sampling.ModelPreferences;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.List;
import java.util.Set;

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
    public static final JsonCodec<CreateMessageRequest> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(CreateMessageRequest req) {
            JsonArrayBuilder msgs = Json.createArrayBuilder();
            req.messages().forEach(m -> msgs.add(SamplingMessage.CODEC.toJson(m)));
            JsonObjectBuilder b = Json.createObjectBuilder().add("messages", msgs.build());
            if (req.modelPreferences() != null) b.add("modelPreferences", ModelPreferences.CODEC.toJson(req.modelPreferences()));
            if (req.systemPrompt() != null) b.add("systemPrompt", req.systemPrompt());
            if (req.includeContext() != null) b.add("includeContext", switch (req.includeContext()) {
                case NONE -> "none";
                case THIS_SERVER -> "thisServer";
                case ALL_SERVERS -> "allServers";
            });
            if (req.temperature() != null) b.add("temperature", req.temperature());
            b.add("maxTokens", req.maxTokens());
            if (!req.stopSequences().isEmpty()) {
                JsonArrayBuilder arr = Json.createArrayBuilder();
                req.stopSequences().forEach(arr::add);
                b.add("stopSequences", arr.build());
            }
            if (req.metadata() != null) b.add("metadata", req.metadata());
            if (req._meta() != null) b.add("_meta", req._meta());
            return b.build();
        }

        @Override
        public CreateMessageRequest fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            AbstractEntityCodec.requireOnlyKeys(obj, Set.of("messages", "modelPreferences", "systemPrompt", "includeContext", "temperature", "maxTokens", "stopSequences", "metadata", "_meta"));
            List<SamplingMessage> messages = obj.getJsonArray("messages").stream()
                    .map(v -> SamplingMessage.CODEC.fromJson(v.asJsonObject()))
                    .toList();
            ModelPreferences prefs = obj.containsKey("modelPreferences")
                    ? ModelPreferences.CODEC.fromJson(obj.getJsonObject("modelPreferences"))
                    : null;
            String system = obj.getString("systemPrompt", null);
            IncludeContext ctx = null;
            if (obj.containsKey("includeContext")) {
                ctx = switch (obj.getString("includeContext")) {
                    case "none" -> IncludeContext.NONE;
                    case "thisServer" -> IncludeContext.THIS_SERVER;
                    case "allServers" -> IncludeContext.ALL_SERVERS;
                    default -> throw new IllegalArgumentException("Unknown includeContext");
                };
            }
            Double temp = obj.containsKey("temperature") ? obj.getJsonNumber("temperature").doubleValue() : null;
            if (!obj.containsKey("maxTokens")) throw new IllegalArgumentException("maxTokens required");
            int max = obj.getInt("maxTokens");
            List<String> stops = obj.containsKey("stopSequences")
                    ? obj.getJsonArray("stopSequences").getValuesAs(JsonString.class).stream().map(JsonString::getString).toList()
                    : List.of();
            JsonObject metadata = obj.getJsonObject("metadata");
            JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
            return new CreateMessageRequest(messages, prefs, system, ctx, temp, max, stops, metadata, meta);
        }
    };

    public enum IncludeContext {NONE, THIS_SERVER, ALL_SERVERS}

    public CreateMessageRequest {
        messages = messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
        systemPrompt = ValidationUtil.cleanNullable(systemPrompt);
        if (stopSequences == null || stopSequences.isEmpty()) {
            stopSequences = List.of();
        } else {
            stopSequences = stopSequences.stream().map(ValidationUtil::requireClean).toList();
        }
        maxTokens = ValidationUtil.requirePositive(maxTokens, "maxTokens");
        ValidationUtil.requireMeta(_meta);
    }
}
