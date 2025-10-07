package com.amannmalik.mcp.transport;

public final class AuthorizationException extends Exception {
    private final int status;

    public AuthorizationException(String message) {
        this(message, 401);
    }

    public AuthorizationException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
