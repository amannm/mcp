package com.amannmalik.mcp.transport;

/**
 * Constants for HTTP header names used by the Streamable HTTP transport.
 */
public final class TransportHeaders {
    private TransportHeaders() {
    }

    /** Header indicating the negotiated MCP protocol version. */
    public static final String PROTOCOL_VERSION = "MCP-Protocol-Version";

    /** Header used for identifying the session during Streamable HTTP calls. */
    public static final String SESSION_ID = "Mcp-Session-Id";
}

