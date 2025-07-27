package com.amannmalik.mcp.schema.resources;

import com.amannmalik.mcp.schema.core.FieldValidations;

/** Reference to a resource or resource template. */
public record ResourceReference(String uri) {
    public static final String TYPE = "ref/resource";
    public ResourceReference {
        FieldValidations.requireUri(uri);
    }
    public String type() { return TYPE; }
}
