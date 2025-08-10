package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.ResourceUpdatedNotificationAbstractEntityCodec;
import com.amannmalik.mcp.util.ValidationUtil;

public record ResourceUpdatedNotification(String uri, String title) {
    public static final JsonCodec<ResourceUpdatedNotification> CODEC = new ResourceUpdatedNotificationAbstractEntityCodec();

    public ResourceUpdatedNotification {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        title = ValidationUtil.cleanNullable(title);
    }

}
