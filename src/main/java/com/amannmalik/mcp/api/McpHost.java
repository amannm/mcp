package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.McpClient.McpClientListener;
import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.elicitation.InteractiveElicitationProvider;
import com.amannmalik.mcp.jsonrpc.JsonRpc;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.roots.InMemoryRootsProvider;
import com.amannmalik.mcp.sampling.InteractiveSamplingProvider;
import com.amannmalik.mcp.security.*;
import com.amannmalik.mcp.spi.*;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final JsonCodec<ListResourcesResult> LIST_RESOURCES_RESULT_JSON_CODEC =
            AbstractEntityCodec.paginatedResult(
                    "resources",
                    "resource",
                    r -> new Pagination.Page<>(r.resources(), r.nextCursor()),
                    ListResourcesResult::_meta,
                    new ResourceAbstractEntityCodec(),
                    (page, meta) -> new ListResourcesResult(page.items(), page.nextCursor(), meta));

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private final Map<String, McpClient> clients = new ConcurrentHashMap<>();
    private final ConsentController consents;
    private final Principal principal;
    private final ToolAccessController toolAccess;
    private final ResourceAccessController privacyBoundary;
    private final SamplingAccessController samplingAccess;

    public McpHost(McpHostConfiguration config) throws IOException {
        this.principal = new Principal(config.hostPrincipal(), Set.of());
        this.consents = new ConsentController();
        this.toolAccess = new ToolAccessController();
        this.privacyBoundary = new ResourceAccessController();
        this.samplingAccess = new SamplingAccessController();
        for (McpClientConfiguration clientConfig : config.clientConfigurations()) {
            grantConsent(clientConfig.clientId());
            SamplingProvider samplingProvider = new InteractiveSamplingProvider(clientConfig.interactiveSampling());
            List<Root> roots = clientConfig.rootDirectories().stream()
                    .map(dir -> new Root("file://" + dir, dir, null))
                    .toList();
            InMemoryRootsProvider rootsProvider = new InMemoryRootsProvider(roots);

            McpClientListener listener = (clientConfig.verbose() || config.globalVerbose()) ? new McpClientListener() {
                @Override
                public void onMessage(LoggingMessageNotification notification) {
                    String logger = notification.logger() == null ? "" : ":" + notification.logger();
                    System.err.println("[" + clientConfig.clientId() + "] " +
                            notification.level().name().toLowerCase() + logger + " " + notification.data());
                }
            } : null;

            ElicitationProvider elicitationProvider = new InteractiveElicitationProvider();

            McpClient client = new McpClient(
                    clientConfig,
                    config.globalVerbose(),
                    samplingProvider,
                    rootsProvider,
                    elicitationProvider,
                    listener);

            register(clientConfig.clientId(), client, clientConfig);
        }
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

    private void register(String id, McpClient client, McpClientConfiguration clientConfig) {
        consents.requireConsent(principal, client.info().name());
        if (clients.putIfAbsent(id, client) != null) {
            throw new IllegalArgumentException("Client already registered: " + id);
        }
        client.setPrincipal(principal);
        client.setSamplingAccessPolicy(samplingAccess);
        client.configurePing(
                clientConfig.pingInterval(),
                clientConfig.pingTimeout());
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

    public ListResourcesResult listResources(String clientId, Cursor cursor) throws IOException {
        McpClient client = requireClient(clientId);
        requireCapability(client, ServerCapability.RESOURCES);
        String token = cursor instanceof Cursor.Token(var value) ? value : null;
        JsonRpcResponse resp = JsonRpc.expectResponse(client.request(
                RequestMethod.RESOURCES_LIST,
                AbstractEntityCodec.paginatedRequest(
                        ListResourcesRequest::cursor,
                        ListResourcesRequest::_meta,
                        ListResourcesRequest::new).toJson(new ListResourcesRequest(token, null)),
                TIMEOUT
        ));
        return LIST_RESOURCES_RESULT_JSON_CODEC.fromJson(resp.result());
    }

    public ListToolsResult listTools(String clientId, Cursor cursor) throws IOException {
        McpClient client = requireClient(clientId);
        requireCapability(client, ServerCapability.TOOLS);
        String token = cursor instanceof Cursor.Token(var value) ? value : null;
        JsonRpcResponse resp = JsonRpc.expectResponse(client.request(
                RequestMethod.TOOLS_LIST,
                AbstractEntityCodec.paginatedRequest(
                        ListToolsRequest::cursor,
                        ListToolsRequest::_meta,
                        ListToolsRequest::new).toJson(new ListToolsRequest(token, null)),
                TIMEOUT
        ));
        return LIST_TOOLS_RESULT_JSON_CODEC.fromJson(resp.result());
    }

    public ToolResult callTool(String clientId, String name, JsonObject args) throws IOException {
        McpClient client = requireClient(clientId);
        requireCapability(client, ServerCapability.TOOLS);
        consents.requireConsent(principal, "tool:" + name);
        Tool tool = findTool(clientId, name)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + name));
        toolAccess.requireAllowed(principal, tool);
        JsonRpcResponse resp = JsonRpc.expectResponse(client.request(
                RequestMethod.TOOLS_CALL,
                CALL_TOOL_REQUEST_CODEC.toJson(new CallToolRequest(name, args, null)),
                TIMEOUT
        ));
        return TOOL_RESULT_ABSTRACT_ENTITY_CODEC.fromJson(resp.result());
    }

    private Optional<Tool> findTool(String clientId, String name) throws IOException {
        Cursor cursor = Cursor.Start.INSTANCE;
        do {
            ListToolsResult page = listTools(clientId, cursor);
            for (Tool t : page.tools()) {
                if (t.name().equals(name)) return Optional.of(t);
            }
            cursor = page.nextCursor();
        } while (!(cursor instanceof Cursor.End));
        return Optional.empty();
    }

    public JsonObject createMessage(String clientId, JsonObject params) throws IOException {
        McpClient client = requireConnectedClient(clientId);
        requireCapability(client, ClientCapability.SAMPLING);
        consents.requireConsent(principal, "sampling");
        samplingAccess.requireAllowed(principal);
        JsonRpcResponse resp = JsonRpc.expectResponse(client.request(RequestMethod.SAMPLING_CREATE_MESSAGE, params, TIMEOUT));
        return resp.result();
    }

    public JsonRpcMessage request(String id, RequestMethod method, JsonObject params) throws IOException {
        return requireClientForMethod(id, method).request(method, params, TIMEOUT);
    }

    public JsonRpcMessage request(String id, RequestId reqId, RequestMethod method, JsonObject params) throws IOException {
        return requireClientForMethod(id, method).request(reqId, method, params, TIMEOUT);
    }

    public void notify(String id, NotificationMethod method, JsonObject params) throws IOException {
        requireClientForMethod(id, method).sendNotification(method, params);
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

    public Set<ServerCapability> serverCapabilities(String id) {
        return EnumSet.copyOf(requireClient(id).serverCapabilities());
    }

    public Set<ServerFeature> serverFeatures(String id) {
        return EnumSet.copyOf(requireClient(id).serverFeatures());
    }

    private McpClient requireConnectedClient(String id) {
        McpClient client = requireClient(id);
        if (!client.connected()) throw new IllegalStateException("Client not connected: " + id);
        return client;
    }

    private McpClient requireClientForMethod(String id, JsonRpcMethod method) {
        McpClient client = requireConnectedClient(id);
        if (method instanceof RequestMethod rm) {
            for (ServerCapability cap : rm.serverCapabilities()) {
                requireCapability(client, cap);
            }
        }
        for (ClientCapability cap : method.clientCapabilities()) {
            requireCapability(client, cap);
        }
        return client;
    }

}
