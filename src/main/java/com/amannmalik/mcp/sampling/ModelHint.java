package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.ModelHintJsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;

public record ModelHint(String name) {
    public static final JsonCodec<ModelHint> CODEC = new ModelHintJsonCodec();

    public ModelHint {
        if (name != null) name = ValidationUtil.requireClean(name);
    }

}
