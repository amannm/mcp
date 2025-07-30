package com.amannmalik.mcp.lifecycle;

/**
 * Constants for protocol version negotiation.
 */
public final class Protocol {
    private Protocol() {
    }

    /** Latest protocol revision supported by this implementation. */
    public static final String LATEST_VERSION = "2025-06-18";

    /** Previous revision used for backwards compatibility. */
    public static final String PREVIOUS_VERSION = "2025-03-26";
}

