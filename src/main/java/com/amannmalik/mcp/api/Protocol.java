package com.amannmalik.mcp.api;

import java.util.List;

public final class Protocol {
    private Protocol() {}

    public static final String LATEST_VERSION = "2025-06-18";
    public static final String PREVIOUS_VERSION = "2025-03-26";
    public static final List<String> SUPPORTED_VERSIONS = List.of(LATEST_VERSION, PREVIOUS_VERSION);
}

