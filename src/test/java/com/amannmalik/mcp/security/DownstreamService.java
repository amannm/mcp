package com.amannmalik.mcp.security;

import com.amannmalik.mcp.auth.Principal;

public final class DownstreamService {
    private boolean tokenReceived;

    public void process(Principal principal) {
        tokenReceived = false;
    }

    public void processToken(String token) {
        tokenReceived = true;
    }

    public boolean tokenReceived() {
        return tokenReceived;
    }
}

