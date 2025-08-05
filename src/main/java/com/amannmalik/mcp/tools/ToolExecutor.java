package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.elicitation.*;
import jakarta.json.*;

import java.util.ArrayList;
import java.util.List;

public final class ToolExecutor {
    private final ToolProvider provider;
    private final ElicitationProvider elicitation;

    public ToolExecutor(ToolProvider provider, ElicitationProvider elicitation) {
        if (provider == null) throw new IllegalArgumentException("provider required");
        this.provider = provider;
        this.elicitation = elicitation;
    }

    public ToolExecutor(ToolProvider provider) {
        this(provider, null);
    }

    public ToolResult execute(String name, JsonObject arguments) {
        if (name == null || arguments == null) {
            throw new IllegalArgumentException("name and arguments required");
        }
        Tool tool = provider.find(name).orElseThrow();
        JsonObject args = arguments;
        JsonObject schema = tool.inputSchema();
        if (schema != null) {
            JsonArray req = schema.getJsonArray("required");
            JsonObject props = schema.getJsonObject("properties");
            if (req != null && props != null && !req.isEmpty()) {
                List<String> missing = new ArrayList<>();
                for (JsonValue v : req) {
                    String f = ((JsonString) v).getString();
                    if (!args.containsKey(f)) missing.add(f);
                }
                if (!missing.isEmpty()) {
                    if (elicitation == null) {
                        throw new IllegalArgumentException("missing argument: " + missing.getFirst());
                    }
                    JsonObjectBuilder propBuilder = Json.createObjectBuilder();
                    JsonArrayBuilder reqBuilder = Json.createArrayBuilder();
                    for (String m : missing) {
                        propBuilder.add(m, props.get(m));
                        reqBuilder.add(m);
                    }
                    JsonObject reqSchema = Json.createObjectBuilder()
                            .add("type", "object")
                            .add("properties", propBuilder.build())
                            .add("required", reqBuilder.build())
                            .build();
                    ElicitResult result;
                    try {
                        result = elicitation.elicit(new ElicitRequest("missing required arguments", reqSchema, null), 0);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return error("elicitation interrupted");
                    }
                    if (result.action() != ElicitationAction.ACCEPT || result.content() == null) {
                        return error("elicitation rejected");
                    }
                    JsonObjectBuilder merged = Json.createObjectBuilder();
                    args.forEach(merged::add);
                    result.content().forEach(merged::add);
                    args = merged.build();
                }
            }
        }
        return provider.call(name, args);
    }

    private static ToolResult error(String message) {
        JsonArray content = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("type", "text").add("text", message))
                .build();
        return new ToolResult(content, null, true, null);
    }
}
