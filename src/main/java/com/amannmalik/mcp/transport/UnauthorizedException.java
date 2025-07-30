package com.amannmalik.mcp.transport;

import java.io.IOException;

/**
 * Thrown when the server responds with HTTP 401 Unauthorized.
 * <p>
 * The {@code wwwAuthenticate} header may be used by callers to
 * initiate an authorization flow.
 */
public final class UnauthorizedException extends IOException {
    private final String wwwAuthenticate;

    public UnauthorizedException(String wwwAuthenticate) {
        super("HTTP 401 Unauthorized");
        this.wwwAuthenticate = wwwAuthenticate;
    }

    public String wwwAuthenticate() {
        return wwwAuthenticate;
    }
}

