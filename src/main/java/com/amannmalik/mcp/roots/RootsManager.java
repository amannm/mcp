package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ProtocolLifecycle;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.wire.RequestMethod;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/// - [Roots](specification/2025-06-18/client/roots.mdx)
/// - [MCP roots specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:154-169)
public final class RootsManager {
    private final ProtocolLifecycle lifecycle;
    private final RequestSender requester;
    private final ChangeSupport<Root> listChangeSupport = new ChangeSupport<>();
    private final List<Root> roots = new CopyOnWriteArrayList<>();

    public RootsManager(ProtocolLifecycle lifecycle, RequestSender requester) {
        this.lifecycle = lifecycle;
        this.requester = requester;
    }

    public List<Root> listRoots() throws IOException {
        List<Root> fetched = fetchRoots();
        boolean changed = !roots.equals(fetched);
        roots.clear();
        roots.addAll(fetched);
        if (changed) listChangeSupport.notifyListeners();
        return List.copyOf(fetched);
    }

    public ChangeSubscription subscribe(ChangeListener<Root> listener) {
        return listChangeSupport.subscribe(listener);
    }

    public List<Root> roots() {
        return List.copyOf(roots);
    }

    public void refreshAsync() {
        if (!lifecycle.negotiatedClientCapabilities().contains(ClientCapability.ROOTS)) return;
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
                ListRootsRequest.CODEC.toJson(new ListRootsRequest(null)));
        if (msg instanceof JsonRpcResponse resp) {
            return ListRootsResult.CODEC.fromJson(resp.result()).roots();
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    private void requireClientCapability(ClientCapability cap) {
        if (!lifecycle.negotiatedClientCapabilities().contains(cap)) {
            throw new IllegalStateException("Missing client capability: " + cap);
        }
    }
}

