package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.ClientCapability;
import com.amannmalik.mcp.api.ClientInfo;
import com.amannmalik.mcp.core.Capabilities;
import com.amannmalik.mcp.core.ClientFeatures;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.InitializeRequest;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.*;

public final class InitializeRequestAbstractEntityCodec extends AbstractEntityCodec<InitializeRequest> {

    static final JsonCodec<ClientInfo> CLIENT_INFO_JSON_CODEC = new ClientInfoAbstractEntityCodec();

    @Override
    public JsonObject toJson(InitializeRequest req) {
        var caps = Json.createObjectBuilder();
        for (var c : req.capabilities().client()) {
            var b = Json.createObjectBuilder();
            if (c == ClientCapability.ROOTS && req.features().rootsListChanged()) {
                b.add("listChanged", true);
            }
            caps.add(c.code(), b.build());
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
        var version = requireString(obj, "protocolVersion");
        var capsObj = obj.getJsonObject("capabilities");
        Set<ClientCapability> client = EnumSet.noneOf(ClientCapability.class);
        Map<String, JsonObject> experimental = new HashMap<>();
        var rootsList = false;
        if (capsObj != null) {
            for (var entry : capsObj.entrySet()) {
                var k = entry.getKey();
                var v = entry.getValue().asJsonObject();
                var cap = ClientCapability.from(k);
                if (cap.isPresent()) {
                    var c = cap.get();
                    client.add(c);
                    if (c == ClientCapability.ROOTS) {
                        rootsList = getBoolean(v, "listChanged", false);
                    }
                } else {
                    experimental.put(k, v);
                }
            }
        }
        var caps = new Capabilities(
                Immutable.enumSet(client),
                Set.of(),
                Immutable.map(experimental),
                Map.of()
        );
        var info = CLIENT_INFO_JSON_CODEC.fromJson(getObject(obj, "clientInfo"));
        var features = new ClientFeatures(rootsList);
        return new InitializeRequest(version, caps, info, features);
    }
}
