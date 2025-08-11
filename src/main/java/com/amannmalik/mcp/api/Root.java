package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.RootAbstractEntityCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record Root(String uri, String name, JsonObject _meta) {

    public Root {
        uri = ValidationUtil.requireFileUri(uri);
        if (name != null) {
            name = ValidationUtil.requireClean(name);
        }
        ValidationUtil.requireMeta(_meta);
    }

}
