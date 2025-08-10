package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.*;
import com.amannmalik.mcp.jsonrpc.JsonCodec;
import jakarta.json.*;

import java.util.*;

record InitializeRequest(
        String protocolVersion,
        Capabilities capabilities,
        ClientInfo clientInfo,
        ClientFeatures features
) {
    static final JsonCodec<InitializeRequest> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(InitializeRequest req) {
            JsonObjectBuilder caps = Json.createObjectBuilder();
            for (var c : req.capabilities().client()) {
                JsonObjectBuilder b = Json.createObjectBuilder();
                if (c == ClientCapability.ROOTS && req.features().rootsListChanged()) {
                    b.add("listChanged", true);
                }
                caps.add(c.name().toLowerCase(), b.build());
            }
            req.capabilities().clientExperimental().forEach(caps::add);
            return Json.createObjectBuilder()
                    .add("protocolVersion", req.protocolVersion())
                    .add("capabilities", caps.build())
                    .add("clientInfo", ClientInfo.CODEC.toJson(req.clientInfo()))
                    .build();
        }

        @Override
        public InitializeRequest fromJson(JsonObject obj) {
            String version = requireString(obj, "protocolVersion");
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
            ClientInfo info = ClientInfo.CODEC.fromJson(getObject(obj, "clientInfo"));
            ClientFeatures features = new ClientFeatures(rootsList);
            return new InitializeRequest(version, caps, info, features);
        }
    };

    InitializeRequest {
        if (protocolVersion == null) {
            throw new IllegalArgumentException("protocolVersion must not be null");
        }
        if (features == null) features = ClientFeatures.EMPTY;
    }
}
