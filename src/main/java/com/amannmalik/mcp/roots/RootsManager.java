package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.spi.ChangeSubscription;
import com.amannmalik.mcp.api.JsonRpcMessage;
import com.amannmalik.mcp.api.model.*;
import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.spi.Root;
import com.amannmalik.mcp.util.ChangeSupport;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/// - [Roots](specification/2025-06-18/client/roots.mdx)
/// - [MCP roots specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:154-169)
public final class RootsManager {
    public static final JsonCodec<ListRootsRequest> CODEC =
            AbstractEntityCodec.metaOnly(ListRootsRequest::_meta, ListRootsRequest::new);
    private static final ListRootsResultAbstractEntityCodec LIST_RESULTS_CODEC = new ListRootsResultAbstractEntityCodec();
    private final Supplier<Set<ClientCapability>> capabilities;
    private final RequestSender requester;
    private final ChangeSupport<Change> listChangeSupport = new ChangeSupport<>();
    private final List<Root> roots = new CopyOnWriteArrayList<>();

    public RootsManager(Supplier<Set<ClientCapability>> capabilities, RequestSender requester) {
        this.capabilities = capabilities;
        this.requester = requester;
    }

    public List<Root> listRoots() throws IOException {
        List<Root> fetched = fetchRoots();
        boolean changed = !roots.equals(fetched);
        roots.clear();
        roots.addAll(fetched);
        if (changed) listChangeSupport.notifyListeners(Change.INSTANCE);
        return List.copyOf(fetched);
    }

    public ChangeSubscription subscribe(Consumer<Change> listener) {
        return listChangeSupport.subscribe(listener);
    }

    public List<Root> roots() {
        return List.copyOf(roots);
    }

    public void refreshAsync() {
        if (!capabilities.get().contains(ClientCapability.ROOTS)) return;
        Thread t = new Thread(() -> {
            try {
                listRoots();
            } catch (IOException ignore) {
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void listChangedNotification() {
        refreshAsync();
    }

    private List<Root> fetchRoots() throws IOException {
        requireClientCapability(ClientCapability.ROOTS);
        JsonRpcMessage msg = requester.send(RequestMethod.ROOTS_LIST,
                CODEC.toJson(new ListRootsRequest(null)), 0L);
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

