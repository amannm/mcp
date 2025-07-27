package com.amannmalik.mcp.schema;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Filesystem boundary definitions.
 */
public final class Roots {
    private Roots() {}

    /** Request listing available root URIs. */
    public record ListRootsRequest(JsonRpcTypes.RequestId id,
                                   Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public ListRootsRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "roots/list"; }
    }

    /** Root filesystem entry. */
    public record Root(String uri,
                       Optional<String> name) {
        public Root {
            Objects.requireNonNull(uri);
            Objects.requireNonNull(name);
            URI.create(uri);
        }
    }

    /** Response carrying root listings. */
    public record ListRootsResult(JsonRpcTypes.RequestId id,
                                  List<Root> roots,
                                  Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Result {
        public ListRootsResult {
            Objects.requireNonNull(id);
            roots = List.copyOf(roots);
            Objects.requireNonNull(_meta);
        }
    }

    /** Notification that the roots list changed. */
    public record RootsListChangedNotification(Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Notification {
        public RootsListChangedNotification {
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "notifications/roots/changed"; }
    }
}
