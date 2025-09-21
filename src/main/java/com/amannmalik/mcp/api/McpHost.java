package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.config.McpClientConfiguration;
import com.amannmalik.mcp.api.config.McpHostConfiguration;
import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.elicitation.InteractiveElicitationProvider;
import com.amannmalik.mcp.jsonrpc.JsonRpc;
import com.amannmalik.mcp.roots.InMemoryRootsProvider;
import com.amannmalik.mcp.sampling.InteractiveSamplingProvider;
import com.amannmalik.mcp.security.*;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.util.PlatformLog;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.lang.System.Logger;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Logger LOG = PlatformLog.get(McpHost.class);
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
        for (var clientConfig : config.clientConfigurations()) {
            grantConsent(clientConfig.serverName());
            SamplingProvider samplingProvider = new InteractiveSamplingProvider(clientConfig.interactiveSampling());
            var roots = clientConfig.rootDirectories().stream()
                    .map(dir -> new Root(URI.create("file://" + dir), dir, null))
                    .toList();
            var rootsProvider = new InMemoryRootsProvider(roots);

            var listener = (clientConfig.verbose() || config.globalVerbose()) ? new McpClientListener() {
                @Override
                public void onMessage(LoggingMessageNotification notification) {
                    var logger = notification.logger() == null ? "" : ":" + notification.logger();
                    var level = PlatformLog.toPlatformLevel(notification.level());
                    LOG.log(level, () -> "[" + clientConfig.clientId() + "]" + logger + " " + notification.data());
                }
            } : null;

            ElicitationProvider elicitationProvider = new InteractiveElicitationProvider();

            var client = new McpClient(
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
        for (var id : Set.copyOf(clients.keySet())) {
            unregister(id);
        }
    }

    public void connect(String id) throws IOException {
        var client = requireClient(id);
        client.connect();
    }

    public void unregister(String id) throws IOException {
        var client = clients.remove(id);
        if (client != null) {
            client.close();
        }
    }

    public String aggregateContext() {
        return clients.values().stream()
                .map(McpClient::context)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public ListResourcesResult listResources(String clientId, Cursor cursor) throws IOException {
        var client = requireClient(clientId);
        requireCapability(client, ServerCapability.RESOURCES);
        var result = client.listResources(cursor);
        var filtered = result.resources().stream()
                .filter(r -> allowed(r.annotations()))
                .toList();
        if (filtered.size() == result.resources().size()) {
            return result;
        }
        return new ListResourcesResult(filtered, result.nextCursor(), result._meta());
    }

    public ListResourceTemplatesResult listResourceTemplates(String clientId, Cursor cursor) throws IOException {
        var client = requireClient(clientId);
        requireCapability(client, ServerCapability.RESOURCES);
        var result = client.listResourceTemplates(cursor);
        var filtered = result.resourceTemplates().stream()
                .filter(t -> allowed(t.annotations()))
                .toList();
        if (filtered.size() == result.resourceTemplates().size()) {
            return result;
        }
        return new ListResourceTemplatesResult(filtered, result.nextCursor(), result._meta());
    }

    public AutoCloseable subscribeToResource(String clientId, URI uri, Consumer<ResourceUpdate> listener) throws IOException {
        var client = requireClient(clientId);
        requireCapability(client, ServerCapability.RESOURCES);
        var resource = findResource(client, uri)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found: " + uri));
        privacyBoundary.requireAllowed(principal, resource.annotations());
        return client.subscribeResource(uri, listener);
    }

    public ListToolsResult listTools(String clientId, Cursor cursor) throws IOException {
        var client = requireClient(clientId);
        requireCapability(client, ServerCapability.TOOLS);
        var token = cursor instanceof Cursor.Token(var value) ? value : null;
        var resp = JsonRpc.expectResponse(client.request(
                RequestMethod.TOOLS_LIST,
                PaginatedRequest.CODEC.toJson(new PaginatedRequest(token, null)),
                TIMEOUT
        ));
        return LIST_TOOLS_RESULT_JSON_CODEC.fromJson(resp.result());
    }

    public ToolResult callTool(String clientId, String name, JsonObject args) throws IOException {
        var client = requireClient(clientId);
        requireCapability(client, ServerCapability.TOOLS);
        consents.requireConsent(principal, "tool:" + name);
        var tool = findTool(clientId, name)
                .orElseThrow(() -> new IllegalArgumentException("Tool not found: " + name));
        toolAccess.requireAllowed(principal, tool);
        var resp = JsonRpc.expectResponse(client.request(
                RequestMethod.TOOLS_CALL,
                CALL_TOOL_REQUEST_CODEC.toJson(new CallToolRequest(name, args, null)),
                TIMEOUT
        ));
        return TOOL_RESULT_ABSTRACT_ENTITY_CODEC.fromJson(resp.result());
    }

    public JsonObject createMessage(String clientId, JsonObject params) throws IOException {
        var client = requireClient(clientId);
        if (!client.connected()) {
            throw new IllegalStateException("Client not connected: " + clientId);
        }
        requireCapability(client, ClientCapability.SAMPLING);
        consents.requireConsent(principal, "sampling");
        samplingAccess.requireAllowed(principal);
        var resp = JsonRpc.expectResponse(client.request(RequestMethod.SAMPLING_CREATE_MESSAGE, params, TIMEOUT));
        return resp.result();
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

    public McpClient client(String id) {
        return requireClient(id);
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

    private Optional<Tool> findTool(String clientId, String name) throws IOException {
        Cursor cursor = Cursor.Start.INSTANCE;
        do {
            var page = listTools(clientId, cursor);
            for (var t : page.tools()) {
                if (t.name().equals(name)) {
                    return Optional.of(t);
                }
            }
            cursor = page.nextCursor();
        } while (!(cursor instanceof Cursor.End));
        return Optional.empty();
    }

    private Optional<Resource> findResource(McpClient client, URI uri) throws IOException {
        Cursor cursor = Cursor.Start.INSTANCE;
        do {
            var page = client.listResources(cursor);
            for (var resource : page.resources()) {
                if (resource.uri().equals(uri)) {
                    return Optional.of(resource);
                }
            }
            cursor = page.nextCursor();
        } while (!(cursor instanceof Cursor.End));
        return Optional.empty();
    }

    private McpClient requireClient(String id) {
        var client = clients.get(id);
        if (client == null) {
            throw new IllegalArgumentException("Unknown client: " + id);
        }
        return client;
    }

    private boolean allowed(Annotations annotations) {
        try {
            privacyBoundary.requireAllowed(principal, annotations);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }
}
