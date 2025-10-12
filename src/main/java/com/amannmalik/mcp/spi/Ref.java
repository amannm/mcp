package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.SpiPreconditions;
import jakarta.json.JsonObject;

public sealed interface Ref permits
        Ref.PromptRef,
        Ref.ResourceRef {

    String type();

    record PromptRef(String name, String title, JsonObject _meta) implements Ref {
        public PromptRef {
            SpiPreconditions.requireNonNull(name, "name required");
            name = SpiPreconditions.requireClean(name);
            title = SpiPreconditions.cleanNullable(title);
            SpiPreconditions.requireMeta(_meta);
        }

        @Override
        public String type() {
            return "ref/prompt";
        }
    }

    record ResourceRef(String uri) implements Ref {
        public ResourceRef {
            SpiPreconditions.requireNonNull(uri, "uri required");
            uri = SpiPreconditions.requireAbsoluteTemplate(uri);
        }

        @Override
        public String type() {
            return "ref/resource";
        }
    }

}
