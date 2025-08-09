package com.amannmalik.mcp.core;

public class UnsupportedProtocolVersionException extends RuntimeException {
    private final String requested;
    private final String supported;

    public UnsupportedProtocolVersionException(String requested, String supported) {
        super("Unsupported protocol version: " + requested + " (supported: " + supported + ")");
        this.requested = requested;
        this.supported = supported;
    }

    public String requested() {
        return requested;
    }

    public String supported() {
        return supported;
    }
}
