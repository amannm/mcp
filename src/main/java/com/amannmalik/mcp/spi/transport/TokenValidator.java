package com.amannmalik.mcp.spi.transport;

import com.amannmalik.mcp.spi.Principal;

@FunctionalInterface
public interface TokenValidator {
    Principal validate(String token) throws AuthorizationException;
}
