package com.amannmalik.mcp.client.sampling;

import com.amannmalik.mcp.prompts.Role;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
                ? obj.getJsonArray("stopSequences").getValuesAs(JsonString.class)
                .stream().map(JsonString::getString).toList()
                : List.of();
        JsonObject metadata = obj.getJsonObject("metadata");
        return new CreateMessageRequest(messages, prefs, system, ctx, temp, max, stops, metadata);
    }

    public static JsonObject toJsonObject(CreateMessageResponse resp) {
        JsonObjectBuilder obj = Json.createObjectBuilder()
                .add("role", resp.role().name().toLowerCase())
                .add("content", toJsonObject(resp.content()))
                .add("model", resp.model());
        if (resp.stopReason() != null) obj.add("stopReason", resp.stopReason());
        if (resp._meta() != null) obj.add("_meta", resp._meta());
        return obj.build();
    }

    public static CreateMessageResponse toCreateMessageResponse(JsonObject obj) {
        Role role = Role.valueOf(obj.getString("role").toUpperCase());
        MessageContent content = toContent(obj.getJsonObject("content"));
        if (!obj.containsKey("model")) throw new IllegalArgumentException("model required");
        String model = obj.getString("model");
        String stop = obj.getString("stopReason", null);
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        return new CreateMessageResponse(role, content, model, stop, meta);
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
        if (content.annotations() != null) b.add("annotations", toJsonObject(content.annotations()));
        if (content._meta() != null) b.add("_meta", content._meta());
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
        Annotations ann = obj.containsKey("annotations") ? toAnnotations(obj.getJsonObject("annotations")) : null;
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        return switch (obj.getString("type")) {
            case "text" -> new MessageContent.Text(obj.getString("text"), ann, meta);
            case "image" -> new MessageContent.Image(Base64.getDecoder().decode(obj.getString("data")), obj.getString("mimeType"), ann, meta);
            case "audio" -> new MessageContent.Audio(Base64.getDecoder().decode(obj.getString("data")), obj.getString("mimeType"), ann, meta);
            default -> throw new IllegalArgumentException("Unknown content type");
        };
    }

    static JsonObject toJsonObject(ModelPreferences prefs) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (!prefs.hints().isEmpty()) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            for (ModelHint h : prefs.hints()) {
                JsonObjectBuilder hb = Json.createObjectBuilder();
                if (h.name() != null) hb.add("name", h.name());
                arr.add(hb.build());
            }
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
                .map(v -> {
                    JsonObject h = v.asJsonObject();
                    String name = h.getString("name", null);
                    return new ModelHint(name);
                })
                .toList()
                : List.of();
        Double cost = obj.containsKey("costPriority") ? obj.getJsonNumber("costPriority").doubleValue() : null;
        Double speed = obj.containsKey("speedPriority") ? obj.getJsonNumber("speedPriority").doubleValue() : null;
        Double intel = obj.containsKey("intelligencePriority") ? obj.getJsonNumber("intelligencePriority").doubleValue() : null;
        return new ModelPreferences(hints, cost, speed, intel);
    }

    static JsonObject toJsonObject(Annotations ann) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (!ann.audience().isEmpty()) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            ann.audience().forEach(r -> arr.add(r.name().toLowerCase()));
            b.add("audience", arr);
        }
        if (ann.priority() != null) b.add("priority", ann.priority());
        if (ann.lastModified() != null) b.add("lastModified", ann.lastModified().toString());
        return b.build();
    }

    static Annotations toAnnotations(JsonObject obj) {
        Set<Role> audience = EnumSet.noneOf(Role.class);
        var arr = obj.getJsonArray("audience");
        if (arr != null) {
            arr.getValuesAs(JsonString.class).forEach(js -> audience.add(Role.valueOf(js.getString().toUpperCase())));
        }
        Double priority = obj.containsKey("priority") ? obj.getJsonNumber("priority").doubleValue() : null;
        Instant lastModified = null;
        if (obj.containsKey("lastModified")) {
            try {
                lastModified = Instant.parse(obj.getString("lastModified"));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid lastModified", e);
            }
        }
        return new Annotations(audience.isEmpty() ? Set.of() : EnumSet.copyOf(audience), priority, lastModified);
    }
}
