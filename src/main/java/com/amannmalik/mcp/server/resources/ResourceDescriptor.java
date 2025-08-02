package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.annotations.Annotations;
import jakarta.json.JsonObject;

public sealed interface ResourceDescriptor permits Resource, ResourceTemplate {
    String name();
    String title();
    String description();
    String mimeType();
    Annotations annotations();
    JsonObject _meta();
}

