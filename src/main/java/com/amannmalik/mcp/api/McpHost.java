package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.McpClient.McpClientListener;
import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.core.CapabilityRequirements;
import com.amannmalik.mcp.elicitation.InteractiveElicitationProvider;
import com.amannmalik.mcp.jsonrpc.JsonRpc;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.roots.InMemoryRootsProvider;
import com.amannmalik.mcp.sampling.InteractiveSamplingProvider;
import com.amannmalik.mcp.security.*;
import com.amannmalik.mcp.spi.*;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class McpHost implements AutoCloseable {
    private static final CallToolRequestAbstractEntityCodec CALL_TOOL_REQUEST_CODEC = new CallToolRequestAbstractEntityCodec();
    private static final JsonCodec<ToolResult> TOOL_RESULT_ABSTRACT_ENTITY_CODEC = new ToolResultAbstractEntityCodec();
    private static final JsonCodec<ListToolsResult> LIST_TOOLS_RESULT_JSON_CODEC =
            AbstractEntityCodec.paginatedResult(
                    "tools",
                    "tool",
                    r -> new Pagination.Page<>(r.tools(), r.nextCursor()),
                    ListToolsResult::_meta,
                    new ToolAbstractEntityCodec(),
                    (page, meta) -> new ListToolsResult(page.items(), page.nextCursor(), meta));

    private final McpHostConfiguration config;
    private final Map<String, McpClient> clients = new ConcurrentHashMap<>();
    private final Predicate<McpClient> policy;
    private final ConsentController consents;
    private final Principal principal;
    private final ToolAccessController toolAccess;
    private final ResourceAccessController privacyBoundary;
    private final SamplingAccessController samplingAccess;

    public McpHost(McpHostConfiguration config, Map<String, String> clientSpecs, boolean verbose) throws IOException {
        this.config = config;
        Predicate<McpClient> policy = c -> true;
        Principal principal = new Principal(config.hostPrincipal(), Set.of());
        this.policy = policy;
        this.principal = principal;
        this.consents = new ConsentController();
        this.toolAccess = new ToolAccessController();
        this.privacyBoundary = new ResourceAccessController();
        this.samplingAccess = new SamplingAccessController();
        for (var entry : clientSpecs.entrySet()) {
            grantConsent(entry.getKey());
            Transport transport = TransportFactory.createStdioTransport(entry.getValue().split(" "), verbose);
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
            EnumSet<ClientCapability> caps = config.clientCapabilities().isEmpty()
                    ? EnumSet.noneOf(ClientCapability.class)
                    : EnumSet.copyOf(config.clientCapabilities());
            ElicitationProvider elicitationProvider = new InteractiveElicitationProvider();
            McpClient client = new McpClient(
                    new ClientInfo(entry.getKey(), entry.getKey(), config.clientIdentity().version()),
                    caps,
                    transport,
                    samplingProvider,
                    rootsProvider,
                    elicitationProvider,
                    listener);
            register(entry.getKey(), client);
        }
    }

    private static Optional<ServerCapability> serverCapabilityForMethod(RequestMethod requestMethod) {
        return CapabilityRequirements.forMethod(requestMethod)
                .or(() -> {
                    if (requestMethod.method().startsWith("tools/")) return Optional.of(ServerCapability.TOOLS);
                    if (requestMethod.method().startsWith("resources/")) return Optional.of(ServerCapability.RESOURCES);
                    if (requestMethod.method().startsWith("prompts/")) return Optional.of(ServerCapability.PROMPTS);
                    if (requestMethod.method().startsWith("completion/")) return Optional.of(ServerCapability.COMPLETIONS);
                    if (requestMethod.method().startsWith("logging/")) return Optional.of(ServerCapability.LOGGING);
                    return Optional.empty();
                });
    }

    private static Optional<ClientCapability> clientCapabilityForMethod(JsonRpcMethod method) {
        if (method.method().startsWith("roots/")) return Optional.of(ClientCapability.ROOTS);
        if (method.method().startsWith("sampling/")) return Optional.of(ClientCapability.SAMPLING);
        if (method.method().startsWith("elicitation/")) return Optional.of(ClientCapability.ELICITATION);
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

    @Override
    public void close() throws IOException {
        for (String id : Set.copyOf(clients.keySet())) {
            unregister(id);
        }
    }

    private void register(String id, McpClient client) {
        if (!policy.test(client)) {
            throw new SecurityException("Client not authorized: " + client.info().name());
        }
        consents.requireConsent(principal, client.info().name());
        if (clients.putIfAbsent(id, client) != null) {
            throw new IllegalArgumentException("Client already registered: " + id);
        }
        client.setPrincipal(principal);
        client.setSamplingAccessPolicy(samplingAccess);
        client.configurePing(
                config.defaultTimeoutMs(),
                config.pingTimeoutMs());
    }

    public void connect(String id) throws IOException {
        McpClient client = requireClient(id);
        client.connect();
    }

    public void unregister(String id) throws IOException {
        McpClient client = clients.remove(id);
        if (client != null) {
            client.disconnect();
        }
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
                AbstractEntityCodec.paginatedRequest(
                        ListToolsRequest::cursor,
                        ListToolsRequest::_meta,
                        ListToolsRequest::new).toJson(new ListToolsRequest(cursor, null)),
                0L
        ));
        return LIST_TOOLS_RESULT_JSON_CODEC.fromJson(resp.result());
    }

    public ToolResult callTool(String clientId, String name, JsonObject args) throws IOException {
        McpClient client = requireClient(clientId);
        requireCapability(client, ServerCapability.TOOLS);
        consents.requireConsent(principal, "tool:" + name);
        toolAccess.requireAllowed(principal, name);
        JsonRpcResponse resp = JsonRpc.expectResponse(client.request(
                RequestMethod.TOOLS_CALL,
                CALL_TOOL_REQUEST_CODEC.toJson(new CallToolRequest(name, args, null)),
                0L
        ));
        return TOOL_RESULT_ABSTRACT_ENTITY_CODEC.fromJson(resp.result());
    }

    public JsonObject createMessage(String clientId, JsonObject params) throws IOException {
        McpClient client = requireConnectedClient(clientId);
        requireCapability(client, ClientCapability.SAMPLING);
        consents.requireConsent(principal, "sampling");
        samplingAccess.requireAllowed(principal);
        JsonRpcResponse resp = JsonRpc.expectResponse(client.request(RequestMethod.SAMPLING_CREATE_MESSAGE, params, 0L));
        return resp.result();
    }

    public JsonRpcMessage request(String id, RequestMethod method, JsonObject params) throws IOException {
        return requireClientForMethod(id, method).request(method, params, 0L);
    }

    public void notify(String id, NotificationMethod method, JsonObject params) throws IOException {
        requireClientForMethod(id, method).notify(method, params);
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

    private McpClient requireClientForMethod(String id, JsonRpcMethod method) {
        McpClient client = requireConnectedClient(id);
        if (method instanceof RequestMethod rm) {
            serverCapabilityForMethod(rm).ifPresent(cap -> requireCapability(client, cap));
        }
        clientCapabilityForMethod(method).ifPresent(cap -> requireCapability(client, cap));
        return client;
    }

}
