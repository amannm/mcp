package com.amannmalik.mcp.server;

import com.amannmalik.mcp.client.roots.ListRootsRequest;
import com.amannmalik.mcp.client.roots.Root;
import com.amannmalik.mcp.client.roots.RootsCodec;
import com.amannmalik.mcp.client.roots.RootsListener;
import com.amannmalik.mcp.client.roots.RootsSubscription;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ProtocolLifecycle;
import com.amannmalik.mcp.wire.RequestMethod;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class RootsManager {
    private final ProtocolLifecycle lifecycle;
    private final RequestSender requester;
    private final List<RootsListener> listeners = new CopyOnWriteArrayList<>();
    private final List<Root> roots = new CopyOnWriteArrayList<>();

    RootsManager(ProtocolLifecycle lifecycle, RequestSender requester) {
        this.lifecycle = lifecycle;
        this.requester = requester;
    }

    List<Root> listRoots() throws IOException {
        List<Root> fetched = fetchRoots();
        boolean changed = !roots.equals(fetched);
        roots.clear();
        roots.addAll(fetched);
        if (changed) listeners.forEach(RootsListener::listChanged);
        return List.copyOf(fetched);
    }

    RootsSubscription subscribe(RootsListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    List<Root> roots() {
        return List.copyOf(roots);
    }

    void refreshAsync() {
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

    void listChangedNotification() {
        refreshAsync();
    }

    private List<Root> fetchRoots() throws IOException {
        requireClientCapability(ClientCapability.ROOTS);
        JsonRpcMessage msg = requester.send(RequestMethod.ROOTS_LIST,
                RootsCodec.toJsonObject(new ListRootsRequest(null)));
        if (msg instanceof JsonRpcResponse resp) {
            return RootsCodec.toRoots(resp.result());
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    private void requireClientCapability(ClientCapability cap) {
        if (!lifecycle.negotiatedClientCapabilities().contains(cap)) {
            throw new IllegalStateException("Missing client capability: " + cap);
        }
    }
}

