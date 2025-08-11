package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.ClientCapability;
import com.amannmalik.mcp.api.ClientInfo;
import com.amannmalik.mcp.core.Capabilities;
import com.amannmalik.mcp.core.ClientFeatures;
import com.amannmalik.mcp.util.InitializeRequest;
import jakarta.json.*;

import java.util.*;

public final class InitializeRequestAbstractEntityCodec extends AbstractEntityCodec<InitializeRequest> {

    static final JsonCodec<ClientInfo> CLIENT_INFO_JSON_CODEC = new ClientInfoAbstractEntityCodec();

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
                .add("clientInfo", CLIENT_INFO_JSON_CODEC.toJson(req.clientInfo()))
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
        ClientInfo info = CLIENT_INFO_JSON_CODEC.fromJson(getObject(obj, "clientInfo"));
        ClientFeatures features = new ClientFeatures(rootsList);
        return new InitializeRequest(version, caps, info, features);
    }
}
