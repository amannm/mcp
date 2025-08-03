package com.amannmalik.mcp.lifecycle;

import com.amannmalik.mcp.core.JsonCodec;
import jakarta.json.*;

import java.util.*;

public record InitializeRequest(
        String protocolVersion,
        Capabilities capabilities,
        ClientInfo clientInfo,
        ClientFeatures features
) {
    public static final JsonCodec<InitializeRequest> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(InitializeRequest req) {
            JsonObjectBuilder caps = Json.createObjectBuilder();
            for (ClientCapability c : req.capabilities().client()) {
                JsonObjectBuilder b = Json.createObjectBuilder();
                if (c == ClientCapability.ROOTS && req.features().rootsListChanged()) {
                    b.add("listChanged", true);
                }
                caps.add(c.name().toLowerCase(), b.build());
            }
            req.capabilities().clientExperimental().forEach(caps::add);
            JsonObjectBuilder info = Json.createObjectBuilder()
                    .add("name", req.clientInfo().name())
                    .add("version", req.clientInfo().version());
            if (req.clientInfo().title() != null) info.add("title", req.clientInfo().title());
            return Json.createObjectBuilder()
                    .add("protocolVersion", req.protocolVersion())
                    .add("capabilities", caps.build())
                    .add("clientInfo", info.build())
                    .build();
        }

        @Override
        public InitializeRequest fromJson(JsonObject obj) {
            String version = obj.getString("protocolVersion", null);
            if (version == null) throw new IllegalArgumentException("protocolVersion required");
            JsonObject capsObj = obj.getJsonObject("capabilities");
            Set<ClientCapability> client = EnumSet.noneOf(ClientCapability.class);
            Map<String, JsonObject> experimental = new HashMap<>();
            boolean rootsList = false;
            if (capsObj != null) {
                for (var entry : capsObj.entrySet()) {
                    String k = entry.getKey();
                    JsonObject v = entry.getValue().asJsonObject();
                    switch (k) {
                        case "roots" -> {
                            client.add(ClientCapability.ROOTS);
                            rootsList = v.getBoolean("listChanged", false);
                        }
                        case "sampling" -> client.add(ClientCapability.SAMPLING);
                        case "elicitation" -> client.add(ClientCapability.ELICITATION);
                        default -> experimental.put(k, v);
                    }
                }
            }
            Capabilities caps = new Capabilities(
                    client.isEmpty() ? Set.of() : EnumSet.copyOf(client),
                    Set.of(),
                    experimental.isEmpty() ? Map.of() : Map.copyOf(experimental),
                    Map.of()
            );
            JsonObject ci = obj.getJsonObject("clientInfo");
            if (ci == null) throw new IllegalArgumentException("clientInfo required");
            ClientInfo info = new ClientInfo(
                    ci.getString("name"),
                    ci.containsKey("title") ? ci.getString("title") : null,
                    ci.getString("version")
            );
            ClientFeatures features = new ClientFeatures(rootsList);
            return new InitializeRequest(version, caps, info, features);
        }
    };

    public InitializeRequest {
        if (protocolVersion == null) {
            throw new IllegalArgumentException("protocolVersion must not be null");
        }
        if (features == null) features = ClientFeatures.EMPTY;
    }
}
