package com.amannmalik.mcp.client.sampling;

import com.amannmalik.mcp.prompts.Role;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.Base64;
import java.util.List;


public final class SamplingCodec {
    private SamplingCodec() {
    }

    public static JsonObject toJsonObject(CreateMessageRequest req) {
        JsonArrayBuilder msgs = Json.createArrayBuilder();
        for (SamplingMessage m : req.messages()) msgs.add(toJsonObject(m));
        JsonObjectBuilder obj = Json.createObjectBuilder().add("messages", msgs.build());
        if (req.modelPreferences() != null) obj.add("modelPreferences", toJsonObject(req.modelPreferences()));
        if (req.systemPrompt() != null) obj.add("systemPrompt", req.systemPrompt());
        if (req.includeContext() != null) obj.add("includeContext", switch (req.includeContext()) {
            case NONE -> "none";
            case THIS_SERVER -> "thisServer";
            case ALL_SERVERS -> "allServers";
        });
        if (req.temperature() != null) obj.add("temperature", req.temperature());
        obj.add("maxTokens", req.maxTokens());
        if (!req.stopSequences().isEmpty()) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            req.stopSequences().forEach(arr::add);
            obj.add("stopSequences", arr.build());
        }
        if (req.metadata() != null) obj.add("metadata", req.metadata());
        return obj.build();
    }

    public static CreateMessageRequest toCreateMessageRequest(JsonObject obj) {
        List<SamplingMessage> messages = obj.getJsonArray("messages").stream()
                .map(v -> toSamplingMessage(v.asJsonObject()))
                .toList();
        ModelPreferences prefs = obj.containsKey("modelPreferences")
                ? toModelPreferences(obj.getJsonObject("modelPreferences"))
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
        int max = obj.getInt("maxTokens");
        List<String> stops = obj.containsKey("stopSequences")
                ? obj.getJsonArray("stopSequences").getValuesAs(jakarta.json.JsonString.class)
                .stream().map(jakarta.json.JsonString::getString).toList()
                : List.of();
        JsonObject metadata = obj.getJsonObject("metadata");
        return new CreateMessageRequest(messages, prefs, system, ctx, temp, max, stops, metadata);
    }

    public static JsonObject toJsonObject(CreateMessageResponse resp) {
        JsonObjectBuilder obj = Json.createObjectBuilder()
                .add("role", resp.role().name().toLowerCase())
                .add("content", toJsonObject(resp.content()));
        if (resp.model() != null) obj.add("model", resp.model());
        if (resp.stopReason() != null) obj.add("stopReason", resp.stopReason());
        return obj.build();
    }

    public static CreateMessageResponse toCreateMessageResponse(JsonObject obj) {
        Role role = Role.valueOf(obj.getString("role").toUpperCase());
        MessageContent content = toContent(obj.getJsonObject("content"));
        String model = obj.getString("model", null);
        String stop = obj.getString("stopReason", null);
        return new CreateMessageResponse(role, content, model, stop);
    }

    static JsonObject toJsonObject(SamplingMessage msg) {
        return Json.createObjectBuilder()
                .add("role", msg.role().name().toLowerCase())
                .add("content", toJsonObject(msg.content()))
                .build();
    }

    static SamplingMessage toSamplingMessage(JsonObject obj) {
        Role role = Role.valueOf(obj.getString("role").toUpperCase());
        MessageContent content = toContent(obj.getJsonObject("content"));
        return new SamplingMessage(role, content);
    }

    static JsonObject toJsonObject(MessageContent content) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("type", content.type());
        switch (content) {
            case MessageContent.Text t -> b.add("text", t.text());
            case MessageContent.Image i -> b.add("data", Base64.getEncoder().encodeToString(i.data()))
                    .add("mimeType", i.mimeType());
            case MessageContent.Audio a -> b.add("data", Base64.getEncoder().encodeToString(a.data()))
                    .add("mimeType", a.mimeType());
        }
        return b.build();
    }

    static MessageContent toContent(JsonObject obj) {
        return switch (obj.getString("type")) {
            case "text" -> new MessageContent.Text(obj.getString("text"));
            case "image" -> new MessageContent.Image(Base64.getDecoder().decode(obj.getString("data")), obj.getString("mimeType"));
            case "audio" -> new MessageContent.Audio(Base64.getDecoder().decode(obj.getString("data")), obj.getString("mimeType"));
            default -> throw new IllegalArgumentException("Unknown content type");
        };
    }

    static JsonObject toJsonObject(ModelPreferences prefs) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (!prefs.hints().isEmpty()) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            for (ModelHint h : prefs.hints()) arr.add(Json.createObjectBuilder().add("name", h.name()).build());
            b.add("hints", arr.build());
        }
        if (prefs.costPriority() != null) b.add("costPriority", prefs.costPriority());
        if (prefs.speedPriority() != null) b.add("speedPriority", prefs.speedPriority());
        if (prefs.intelligencePriority() != null) b.add("intelligencePriority", prefs.intelligencePriority());
        return b.build();
    }

    static ModelPreferences toModelPreferences(JsonObject obj) {
        List<ModelHint> hints = obj.containsKey("hints")
                ? obj.getJsonArray("hints").stream()
                .map(v -> new ModelHint(v.asJsonObject().getString("name")))
                .toList()
                : List.of();
        Double cost = obj.containsKey("costPriority") ? obj.getJsonNumber("costPriority").doubleValue() : null;
        Double speed = obj.containsKey("speedPriority") ? obj.getJsonNumber("speedPriority").doubleValue() : null;
        Double intel = obj.containsKey("intelligencePriority") ? obj.getJsonNumber("intelligencePriority").doubleValue() : null;
        return new ModelPreferences(hints, cost, speed, intel);
    }
}
