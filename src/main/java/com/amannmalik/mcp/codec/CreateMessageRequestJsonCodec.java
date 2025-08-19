package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.*;
import jakarta.json.*;

import java.util.List;
import java.util.Set;

public class CreateMessageRequestJsonCodec implements JsonCodec<CreateMessageRequest> {

    static final JsonCodec<SamplingMessage> SAMPLING_MESSAGE_JSON_CODEC = new SamplingMessageAbstractEntityCodec();
    static final JsonCodec<ModelPreferences> MODEL_PREFERENCES_JSON_CODEC = new ModelPreferencesJsonCodec();

    @Override
    public JsonObject toJson(CreateMessageRequest req) {
        var msgs = Json.createArrayBuilder();
        req.messages().forEach(m -> msgs.add(SAMPLING_MESSAGE_JSON_CODEC.toJson(m)));
        var b = Json.createObjectBuilder().add("messages", msgs.build());
        if (req.modelPreferences() != null) b.add("modelPreferences", MODEL_PREFERENCES_JSON_CODEC.toJson(req.modelPreferences()));
        if (req.systemPrompt() != null) b.add("systemPrompt", req.systemPrompt());
        if (req.includeContext() != null) b.add("includeContext", switch (req.includeContext()) {
            case NONE -> "none";
            case THIS_SERVER -> "thisServer";
            case ALL_SERVERS -> "allServers";
        });
        if (req.temperature() != null) b.add("temperature", req.temperature());
        b.add("maxTokens", req.maxTokens());
        if (!req.stopSequences().isEmpty()) {
            var arr = Json.createArrayBuilder();
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
        var messages = obj.getJsonArray("messages").stream()
                .map(v -> SAMPLING_MESSAGE_JSON_CODEC.fromJson(v.asJsonObject()))
                .toList();
        var prefs = obj.containsKey("modelPreferences")
                ? MODEL_PREFERENCES_JSON_CODEC.fromJson(obj.getJsonObject("modelPreferences"))
                : null;
        var system = obj.getString("systemPrompt", null);
        CreateMessageRequest.IncludeContext ctx = null;
        if (obj.containsKey("includeContext")) {
            ctx = switch (obj.getString("includeContext")) {
                case "none" -> CreateMessageRequest.IncludeContext.NONE;
                case "thisServer" -> CreateMessageRequest.IncludeContext.THIS_SERVER;
                case "allServers" -> CreateMessageRequest.IncludeContext.ALL_SERVERS;
                default -> throw new IllegalArgumentException("Unknown includeContext");
            };
        }
        var temp = obj.containsKey("temperature") ? obj.getJsonNumber("temperature").doubleValue() : null;
        if (!obj.containsKey("maxTokens")) throw new IllegalArgumentException("maxTokens required");
        var max = obj.getInt("maxTokens");
        List<String> stops = obj.containsKey("stopSequences")
                ? obj.getJsonArray("stopSequences").getValuesAs(JsonString.class).stream().map(JsonString::getString).toList()
                : List.of();
        var metadata = obj.getJsonObject("metadata");
        var meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        return new CreateMessageRequest(messages, prefs, system, ctx, temp, max, stops, metadata, meta);
    }
}
