package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.ClientInfo;
import jakarta.json.*;

public final class ClientInfoAbstractEntityCodec extends AbstractEntityCodec<ClientInfo> {
    @Override
    public JsonObject toJson(ClientInfo info) {
        var b = Json.createObjectBuilder()
                .add("name", info.name())
                .add("version", info.version());
        if (info.title() != null) b.add("title", info.title());
        return b.build();
    }

    @Override
    public ClientInfo fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        var name = requireString(obj, "name");
        var version = requireString(obj, "version");
        var title = obj.getString("title", null);
        return new ClientInfo(name, title, version);
    }
}
