package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public sealed interface Ref permits
        Ref.PromptRef,
        Ref.ResourceRef {
    String type();

    record PromptRef(String name, String title, JsonObject _meta) implements Ref {
        public PromptRef {
            ValidationUtil.requireNonNull(name, "name required");
            name = ValidationUtil.requireClean(name);
            title = ValidationUtil.cleanNullable(title);
            ValidationUtil.requireMeta(_meta);
        }

        @Override
        public String type() {
            return "ref/prompt";
        }
    }

    record ResourceRef(String uri) implements Ref {
        public ResourceRef {
            ValidationUtil.requireNonNull(uri, "uri required");
            uri = ValidationUtil.requireAbsoluteTemplate(uri);
        }

        @Override
        public String type() {
            return "ref/resource";
        }
    }
}
