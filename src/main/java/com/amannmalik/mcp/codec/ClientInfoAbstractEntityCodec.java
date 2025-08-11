package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.model.ClientInfo;
import jakarta.json.*;

public final class ClientInfoAbstractEntityCodec extends AbstractEntityCodec<ClientInfo> {
    @Override
    public JsonObject toJson(ClientInfo info) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("name", info.name())
                .add("version", info.version());
        if (info.title() != null) b.add("title", info.title());
        return b.build();
    }

    @Override
    public ClientInfo fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String name = requireString(obj, "name");
        String version = requireString(obj, "version");
        String title = obj.getString("title", null);
        return new ClientInfo(name, title, version);
    }
}
