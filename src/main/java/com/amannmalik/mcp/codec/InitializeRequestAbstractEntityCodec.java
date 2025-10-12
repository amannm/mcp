package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.ClientCapability;
import com.amannmalik.mcp.api.ClientInfo;
import com.amannmalik.mcp.core.Capabilities;
import com.amannmalik.mcp.spi.ClientFeatures;
import com.amannmalik.mcp.spi.InitializeRequest;
import com.amannmalik.mcp.util.Immutable;
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
        var builder = Json.createObjectBuilder()
                .add("protocolVersion", req.protocolVersion())
                .add("capabilities", caps.build())
                .add("clientInfo", CLIENT_INFO_JSON_CODEC.toJson(req.clientInfo()));
        if (req.features().rootsListChanged()) {
            var roots = Json.createObjectBuilder()
                    .add("listChanged", true)
                    .build();
            builder.add("features", Json.createObjectBuilder().add("roots", roots).build());
        }
        return builder.build();
    }

    @Override
    public InitializeRequest fromJson(JsonObject obj) {
        var version = requireString(obj, "protocolVersion");
        var capsObj = obj.getJsonObject("capabilities");
        var client = EnumSet.noneOf(ClientCapability.class);
        var experimental = new HashMap<String, JsonObject>();
        var rootsList = false;
        if (capsObj != null) {
            for (var entry : capsObj.entrySet()) {
                var k = entry.getKey();
                var v = entry.getValue().asJsonObject();
                var cap = ClientCapability.from(k);
                if (cap.isPresent()) {
                    var c = cap.get();
                    client.add(c);
                    if (c == ClientCapability.ROOTS && getBoolean(v, "listChanged", false)) {
                        rootsList = true;
                    }
                } else {
                    experimental.put(k, v);
                }
            }
        }
        var featuresObj = obj.getJsonObject("features");
        if (featuresObj != null) {
            var roots = featuresObj.getJsonObject("roots");
            if (roots != null && getBoolean(roots, "listChanged", false)) {
                rootsList = true;
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
