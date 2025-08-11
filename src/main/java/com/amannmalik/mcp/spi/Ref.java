package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public sealed interface Ref permits
        Ref.PromptRef,
        Ref.ResourceRef {

    String type();

    record PromptRef(String name, String title, JsonObject _meta) implements Ref {
        public PromptRef(String name, String title, JsonObject _meta) {
            if (name == null) throw new IllegalArgumentException("name required");
            this.name = ValidationUtil.requireClean(name);
            this.title = ValidationUtil.cleanNullable(title);
            ValidationUtil.requireMeta(_meta);
            this._meta = _meta;
        }

        @Override
        public String type() {
            return "ref/prompt";
        }
    }

    record ResourceRef(String uri) implements Ref {
        public ResourceRef(String uri) {
            if (uri == null) throw new IllegalArgumentException("uri required");
            this.uri = ValidationUtil.requireAbsoluteTemplate(uri);
        }

        @Override
        public String type() {
            return "ref/resource";
        }
    }

}
