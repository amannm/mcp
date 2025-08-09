package com.amannmalik.mcp;

import com.amannmalik.mcp.McpClient.McpClientListener;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.logging.LoggingMessageNotification;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.roots.InMemoryRootsProvider;
import com.amannmalik.mcp.roots.Root;
import com.amannmalik.mcp.sampling.InteractiveSamplingProvider;
import com.amannmalik.mcp.sampling.SamplingAccessController;
import com.amannmalik.mcp.sampling.SamplingProvider;
import com.amannmalik.mcp.security.*;
import com.amannmalik.mcp.tools.*;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.wire.RequestMethod;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class McpHost implements AutoCloseable {
    private final Map<String, McpClient> clients = new ConcurrentHashMap<>();
    private final SecurityPolicy policy;
    private final ConsentManager consents;
    private final Principal principal;
    private final ToolAccessController toolAccess;
    private final PrivacyBoundaryEnforcer privacyBoundary;
    private final SamplingAccessController samplingAccess;

    private static Optional<ServerCapability> serverCapabilityForMethod(String method) {
        return RequestMethod.from(method)
                .flatMap(CapabilityRequirements::forMethod)
                .or(() -> {
                    if (method.startsWith("tools/")) return Optional.of(ServerCapability.TOOLS);
                    if (method.startsWith("resources/")) return Optional.of(ServerCapability.RESOURCES);
                    if (method.startsWith("prompts/")) return Optional.of(ServerCapability.PROMPTS);
                    if (method.startsWith("completion/")) return Optional.of(ServerCapability.COMPLETIONS);
                    if (method.startsWith("logging/")) return Optional.of(ServerCapability.LOGGING);
                    return Optional.empty();
                });
    }

    private static Optional<ClientCapability> clientCapabilityForMethod(String method) {
        if (method.startsWith("roots/")) return Optional.of(ClientCapability.ROOTS);
        if (method.startsWith("sampling/")) return Optional.of(ClientCapability.SAMPLING);
        if (method.startsWith("elicitation/")) return Optional.of(ClientCapability.ELICITATION);
        return Optional.empty();
    }

    private static void requireCapability(McpClient client, ServerCapability cap) {
        if (!client.serverCapabilities().contains(cap)) {
            throw new IllegalStateException("Server capability not supported: " + cap);
        }
    }

    private static void requireCapability(McpClient client, ClientCapability cap) {
        if (!client.capabilities().contains(cap)) {
            throw new IllegalStateException("Client capability not supported: " + cap);
        }
    }

    private McpClient requireClient(String id) {
        McpClient client = clients.get(id);
        if (client == null) throw new IllegalArgumentException("Unknown client: " + id);
        return client;
    }

    private McpClient requireConnectedClient(String id) {
        McpClient client = requireClient(id);
        if (!client.connected()) throw new IllegalStateException("Client not connected: " + id);
        return client;
    }

    private McpClient requireClientForMethod(String id, String method) {
        McpClient client = requireConnectedClient(id);
        serverCapabilityForMethod(method).ifPresent(cap -> requireCapability(client, cap));
        clientCapabilityForMethod(method).ifPresent(cap -> requireCapability(client, cap));
        return client;
    }

    public McpHost(SecurityPolicy policy, Principal principal) {
        this.policy = policy;
        this.principal = principal;
        this.consents = new ConsentManager();
        this.toolAccess = new ToolAccessController();
        this.privacyBoundary = new PrivacyBoundaryEnforcer();
        this.samplingAccess = new SamplingAccessController();
    }

    public static McpHost forCli(Map<String, String> clientSpecs, boolean verbose) throws IOException {
        SecurityPolicy policy = c -> true;
        Principal principal = new Principal(McpConfiguration.current().hostPrincipal(), Set.of());
        McpHost host = new McpHost(policy, principal);
        for (var entry : clientSpecs.entrySet()) {
            host.grantConsent(entry.getKey());
            var pb = new ProcessBuilder(entry.getValue().split(" "));
            StdioTransport transport = new StdioTransport(pb, verbose ? System.err::println : s -> {});
            SamplingProvider samplingProvider = new InteractiveSamplingProvider(false);
            String currentDir = System.getProperty("user.dir");
            InMemoryRootsProvider rootsProvider = new InMemoryRootsProvider(
                    List.of(new Root("file://" + currentDir, "Current Directory", null)));
            
            McpClientListener listener = verbose ? new McpClientListener() {
                @Override
                public void onMessage(LoggingMessageNotification notification) {
                    String logger = notification.logger() == null ? "" : ":" + notification.logger();
                    System.err.println(notification.level().name().toLowerCase() + logger + " " + notification.data());
                }
            } : null;
            McpClient client = new McpClient(
                    new ClientInfo(entry.getKey(), entry.getKey(), McpConfiguration.current().clientVersion()),
                    EnumSet.noneOf(ClientCapability.class),
                    transport,
                    samplingProvider,
                    rootsProvider,
                    null,
                    listener);
            host.register(entry.getKey(), client);
        }
        return host;
    }

    public void register(String id, McpClient client) throws IOException {
        if (!policy.allow(client)) {
            throw new SecurityException("Client not authorized: " + client.info().name());
        }
        consents.requireConsent(principal, client.info().name());
        if (clients.putIfAbsent(id, client) != null) {
            throw new IllegalArgumentException("Client already registered: " + id);
        }
        try {
            client.setPrincipal(principal);
            client.setSamplingAccessPolicy(samplingAccess);
            client.configurePing(
                    McpConfiguration.current().defaultMs(),
                    McpConfiguration.current().pingMs());
            client.connect();
        } catch (IOException e) {
            clients.remove(id);
            throw e;
        }
    }

    public void unregister(String id) throws IOException {
        McpClient client = clients.remove(id);
        if (client != null) {
            client.disconnect();
        }
    }

    public void grantConsent(String scope) {
        consents.grant(principal.id(), scope);
    }

    public void revokeConsent(String scope) {
        consents.revoke(principal.id(), scope);
    }

    public void allowTool(String tool) {
        toolAccess.allow(principal.id(), tool);
    }

    public void revokeTool(String tool) {
        toolAccess.revoke(principal.id(), tool);
    }

    public void allowSampling() {
        samplingAccess.allow(principal.id());
    }

    public void revokeSampling() {
        samplingAccess.revoke(principal.id());
    }

    public void allowAudience(Role audience) {
        privacyBoundary.allow(principal.id(), audience);
    }

    public void revokeAudience(Role audience) {
        privacyBoundary.revoke(principal.id(), audience);
    }

    public Set<String> clientIds() {
        return Collections.unmodifiableSet(clients.keySet());
    }

    public String aggregateContext() {
        return clients.values().stream()
                .map(McpClient::context)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public ListToolsResult listTools(String clientId, String cursor) throws IOException {
        McpClient client = requireClient(clientId);
        requireCapability(client, ServerCapability.TOOLS);
        JsonRpcResponse resp = JsonRpc.expectResponse(client.request(
                RequestMethod.TOOLS_LIST,
                ListToolsRequest.CODEC.toJson(new ListToolsRequest(cursor, null))
        ));
        return ListToolsResult.CODEC.fromJson(resp.result());
    }

    public ToolResult callTool(String clientId, String name, JsonObject args) throws IOException {
        McpClient client = requireClient(clientId);
        requireCapability(client, ServerCapability.TOOLS);
        consents.requireConsent(principal, "tool:" + name);
        toolAccess.requireAllowed(principal, name);
        JsonRpcResponse resp = JsonRpc.expectResponse(client.request(
                RequestMethod.TOOLS_CALL,
                CallToolRequest.CODEC.toJson(new CallToolRequest(name, args, null))
        ));
        return ToolResult.CODEC.fromJson(resp.result());
    }

    public JsonObject createMessage(String clientId, JsonObject params) throws IOException {
        McpClient client = requireConnectedClient(clientId);
        requireCapability(client, ClientCapability.SAMPLING);
        consents.requireConsent(principal, "sampling");
        samplingAccess.requireAllowed(principal);
        JsonRpcResponse resp = JsonRpc.expectResponse(client.request(RequestMethod.SAMPLING_CREATE_MESSAGE, params));
        return resp.result();
    }

    public JsonRpcMessage request(String id, String method, JsonObject params) throws IOException {
        return requireClientForMethod(id, method).request(method, params);
    }

    public void notify(String id, String method, JsonObject params) throws IOException {
        requireClientForMethod(id, method).notify(method, params);
    }

    @Override
    public void close() throws IOException {
        for (String id : Set.copyOf(clients.keySet())) {
            unregister(id);
        }
    }
}
