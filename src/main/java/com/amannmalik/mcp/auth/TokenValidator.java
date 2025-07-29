package com.amannmalik.mcp.auth;

@FunctionalInterface
public interface TokenValidator {
    Principal validate(String token);
}
