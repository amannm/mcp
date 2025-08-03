package com.amannmalik.mcp.server.roots.validation;

import java.net.URI;

public final class UriTemplateValidator {
    private UriTemplateValidator() {
    }

    public static String requireAbsoluteTemplate(String template) {
        if (template == null) throw new IllegalArgumentException("uriTemplate is required");
        checkBraces(template);
        String replaced = template.replaceAll("\\{[^}]*}", "x");
        URI parsed;
        try {
            parsed = URI.create(replaced).normalize();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URI template: " + template, e);
        }
        if (!parsed.isAbsolute()) {
            throw new IllegalArgumentException("URI template must be absolute: " + template);
        }
        if (parsed.getFragment() != null) {
            throw new IllegalArgumentException("URI template must not contain fragment: " + template);
        }
        return template;
    }

    private static void checkBraces(String template) {
        int depth = 0;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            if (depth < 0) throw new IllegalArgumentException("Unmatched braces in URI template: " + template);
        }
        if (depth != 0) throw new IllegalArgumentException("Unmatched braces in URI template: " + template);
    }
}
