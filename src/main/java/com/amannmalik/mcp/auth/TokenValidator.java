package com.amannmalik.mcp.auth;

import com.amannmalik.mcp.api.model.Principal;

@FunctionalInterface
public interface TokenValidator {
    Principal validate(String token) throws AuthorizationException;
}
