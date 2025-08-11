package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.CreateMessageRequest;
import com.amannmalik.mcp.api.SamplingMessage;
import com.amannmalik.mcp.sampling.ModelPreferences;
import jakarta.json.*;

import java.util.List;
import java.util.Set;

public class CreateMessageRequestJsonCodec implements JsonCodec<CreateMessageRequest> {

    static final JsonCodec<SamplingMessage> SAMPLING_MESSAGE_JSON_CODEC = new SamplingMessageAbstractEntityCodec();

    @Override
    public JsonObject toJson(CreateMessageRequest req) {
        JsonArrayBuilder msgs = Json.createArrayBuilder();
        req.messages().forEach(m -> msgs.add(SAMPLING_MESSAGE_JSON_CODEC.toJson(m)));
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
                .map(v -> SAMPLING_MESSAGE_JSON_CODEC.fromJson(v.asJsonObject()))
                .toList();
        ModelPreferences prefs = obj.containsKey("modelPreferences")
                ? ModelPreferences.CODEC.fromJson(obj.getJsonObject("modelPreferences"))
                : null;
        String system = obj.getString("systemPrompt", null);
        CreateMessageRequest.IncludeContext ctx = null;
        if (obj.containsKey("includeContext")) {
            ctx = switch (obj.getString("includeContext")) {
                case "none" -> CreateMessageRequest.IncludeContext.NONE;
                case "thisServer" -> CreateMessageRequest.IncludeContext.THIS_SERVER;
                case "allServers" -> CreateMessageRequest.IncludeContext.ALL_SERVERS;
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
}
