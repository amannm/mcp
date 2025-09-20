package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.api.ClientCapability;
import com.amannmalik.mcp.api.RequestMethod;
import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.spi.Root;
import com.amannmalik.mcp.util.EventSupport;
import com.amannmalik.mcp.util.PlatformLog;

import java.io.IOException;
import java.lang.System.Logger;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/// - [Roots](specification/2025-06-18/client/roots.mdx)
/// - [MCP roots specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:154-169)
public final class RootsManager {
    public static final JsonCodec<ListRootsRequest> CODEC =
            AbstractEntityCodec.metaOnly(ListRootsRequest::_meta, ListRootsRequest::new);
    private static final ListRootsResultAbstractEntityCodec LIST_RESULTS_CODEC = new ListRootsResultAbstractEntityCodec();
    private static final Logger LOG = PlatformLog.get(RootsManager.class);
    private final Supplier<Set<ClientCapability>> capabilities;
    private final RequestSender requester;
    private final EventSupport listChangeSupport = new EventSupport();
    private final List<Root> roots = new CopyOnWriteArrayList<>();

    public RootsManager(Supplier<Set<ClientCapability>> capabilities, RequestSender requester) {
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        this.requester = Objects.requireNonNull(requester, "requester");
    }

    public List<Root> listRoots() throws IOException {
        var fetched = fetchRoots();
        var changed = !roots.equals(fetched);
        roots.clear();
        roots.addAll(fetched);
        if (changed) {
            listChangeSupport.notifyListeners();
        }
        return List.copyOf(fetched);
    }

    public AutoCloseable onListChanged(Runnable listener) {
        return listChangeSupport.subscribe(listener);
    }

    public List<Root> roots() {
        return List.copyOf(roots);
    }

    public void refreshAsync() {
        if (!capabilities.get().contains(ClientCapability.ROOTS)) {
            return;
        }
        Thread.ofVirtual()
                .name("mcp-roots-refresh")
                .start(() -> {
                    try {
                        listRoots();
                    } catch (IOException e) {
                        LOG.log(Logger.Level.WARNING, "Failed to refresh roots", e);
                    }
                });
    }

    public void listChangedNotification() {
        refreshAsync();
    }

    private List<Root> fetchRoots() throws IOException {
        requireClientCapability(ClientCapability.ROOTS);
        var msg = requester.send(RequestMethod.ROOTS_LIST,
                CODEC.toJson(new ListRootsRequest(null)), Duration.ZERO);
        if (msg instanceof JsonRpcResponse resp) {
            return LIST_RESULTS_CODEC.fromJson(resp.result()).roots();
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    private void requireClientCapability(ClientCapability cap) {
        if (!capabilities.get().contains(cap)) {
            throw new IllegalStateException("Missing client capability: " + cap);
        }
    }
}
