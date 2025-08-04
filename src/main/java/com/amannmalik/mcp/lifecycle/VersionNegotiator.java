package com.amannmalik.mcp.lifecycle;

import java.util.List;

public final class VersionNegotiator {
    private final List<String> supported;
    private boolean compatibility;

    public VersionNegotiator(List<String> supported) {
        this.supported = List.copyOf(supported);
    }

    public String negotiate(String requested) {
        compatibility = false;
        if (supported.contains(requested)) {
            compatibility = !supported.get(0).equals(requested);
            return requested;
        }
        compatibility = true;
        return supported.get(0);
    }

    public boolean compatibility() {
        return compatibility;
    }

    public List<String> supported() {
        return supported;
    }
}

