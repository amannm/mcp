package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;

public final class ConsentController {
    private final PrincipalPermissions<String> consents = new PrincipalPermissions<>();

    public void grant(String principalId, String scope) {
        consents.grant(principalId, scope);
    }

    public void revoke(String principalId, String scope) {
        consents.revoke(principalId, scope);
    }

    public void requireConsent(Principal principal, String scope) {
        if (principal == null) throw new IllegalArgumentException("principal required");
        if (scope == null || scope.isBlank()) throw new IllegalArgumentException("scope required");
        consents.requirePermission(principal.id(), scope, "User consent required: " + scope);
    }
}
