package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.Notification.LoggingMessageNotification;
import com.amannmalik.mcp.api.Notification.ProgressNotification;
import com.amannmalik.mcp.api.Request.CallToolRequest;
import com.amannmalik.mcp.api.Request.PaginatedRequest;
import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.jsonrpc.JsonRpc;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.util.PlatformLog;
import com.amannmalik.mcp.util.ServiceLoaders;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.lang.System.Logger;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class McpHost implements AutoCloseable {
    private static final CallToolRequestAbstractEntityCodec CALL_TOOL_REQUEST_CODEC = new CallToolRequestAbstractEntityCodec();
    private static final JsonCodec<ToolResult> TOOL_RESULT_ABSTRACT_ENTITY_CODEC = new ToolResultAbstractEntityCodec();
    private static final JsonCodec<ListToolsResult> LIST_TOOLS_RESULT_JSON_CODEC = AbstractEntityCodec.paginatedResult(
            "tools",
            "tool",
            r -> new Pagination.Page<>(r.tools(), r.nextCursor()),
            ListToolsResult::_meta,
            new ToolAbstractEntityCodec(),
            (page, meta) -> new ListToolsResult(page.items(), page.nextCursor(), meta));
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Logger LOG = PlatformLog.get(McpHost.class);
    private final Map<String, McpClient> clients = new ConcurrentHashMap<>();
    private final Principal principal;
    private final Map<String, Set<String>> consents = new ConcurrentHashMap<>();
    private final ToolAccessPolicy toolAccess;
    private final ResourceAccessPolicy privacyBoundary;
    private final SamplingAccessPolicy samplingAccess;
    private final Set<String> allowedTools = ConcurrentHashMap.newKeySet();
    private final Set<Role> allowedAudiences = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean samplingAllowed = new AtomicBoolean();
    private final Map<String, Events> events = new ConcurrentHashMap<>();

    public McpHost(McpHostConfiguration config) throws IOException {
        this.principal = new Principal(config.hostPrincipal(), Set.of());
        this.toolAccess = ServiceLoaders.loadSingleton(ToolAccessPolicy.class);
        this.privacyBoundary = ServiceLoaders.loadSingleton(ResourceAccessPolicy.class);
        this.samplingAccess = ServiceLoaders.loadSingleton(SamplingAccessPolicy.class);
        for (var clientConfig : config.clientConfigurations()) {
            grantConsent(clientConfig.serverName());
            SamplingProvider samplingProvider = null;
            if (clientConfig.clientCapabilities().contains(ClientCapability.SAMPLING)) {
                samplingProvider = ServiceLoaders.loadSingleton(SamplingProvider.class);
            }
            RootsProvider rootsProvider = null;
            if (clientConfig.clientCapabilities().contains(ClientCapability.ROOTS)) {
                var roots = clientConfig.rootDirectories().stream()
                        .map(dir -> new Root(URI.create("file://" + dir), dir, null))
                        .toList();
                rootsProvider = new ClientRootsProvider(roots);
            }
            var listener = new McpClient.McpClientListener() {
                @Override
                public void onProgress(ProgressNotification notification) {
                    events.computeIfAbsent(clientConfig.clientId(), k -> new Events()).messages.add(
                            new LoggingMessageNotification(LoggingLevel.INFO, "progress",
                                    Json.createValue(notification.message() == null ? "" : notification.message())));
                }

                @Override
                public void onMessage(LoggingMessageNotification notification) {
                    var ev = events.computeIfAbsent(clientConfig.clientId(), k -> new Events());
                    ev.messages.add(notification);
                    if (clientConfig.verbose() || config.globalVerbose()) {
                        var logger = notification.logger() == null ? "" : ":" + notification.logger();
                        var level = PlatformLog.toPlatformLevel(notification.level());
                        LOG.log(level, () -> "[" + clientConfig.clientId() + "]" + logger + " " + notification.data());
                    }
                }

                @Override
                public void onResourceListChanged() {
                    events.computeIfAbsent(clientConfig.clientId(), k -> new Events()).resourceListChanged.set(true);
                }

                @Override
                public void onToolListChanged() {
                    events.computeIfAbsent(clientConfig.clientId(), k -> new Events()).toolListChanged.set(true);
                }

                @Override
                public void onPromptsListChanged() {
                    events.computeIfAbsent(clientConfig.clientId(), k -> new Events()).promptsListChanged.set(true);
                }
            };
            ElicitationProvider elicitationProvider = null;
            if (clientConfig.clientCapabilities().contains(ClientCapability.ELICITATION)) {
                elicitationProvider = ServiceLoaders.loadSingleton(ElicitationProvider.class);
            }
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
        Optional<Resource> result = Optional.empty();
        boolean finished = false;
        Cursor cursor = Cursor.Start.INSTANCE;
        do {
            var page = client.listResources(cursor);
            for (var resource : page.resources()) {
                if (resource.uri().equals(uri)) {
                    result = Optional.of(resource);
                    finished = true;
                    break;
                }
            }
            if (finished) break;
            cursor = page.nextCursor();
        } while (!(cursor instanceof Cursor.End));
        if (!finished) {
            result = Optional.empty();
        }
        var resource = result.orElseThrow(() -> new IllegalArgumentException("Resource not found: " + uri));
        ensureAudienceAllowed(resource.annotations());
        privacyBoundary.requireAllowed(principal, resource.annotations());
        return client.subscribeResource(uri, listener);
    }

    public ListToolsResult listTools(String clientId, Cursor cursor) throws IOException {
        var client = requireClient(clientId);
        requireCapability(client, ServerCapability.TOOLS);
        var token = cursor instanceof Cursor.Token(var value) ? value : null;
        var resp = JsonRpc.expectResponse(client.request(RequestMethod.TOOLS_LIST, PaginatedRequest.CODEC.toJson(new PaginatedRequest(token, null)), TIMEOUT));
        return LIST_TOOLS_RESULT_JSON_CODEC.fromJson(resp.result());
    }

    public ToolResult callTool(String clientId, String name, JsonObject args) throws IOException {
        var client = requireClient(clientId);
        requireCapability(client, ServerCapability.TOOLS);
        requireConsent(principal, "tool:" + name);
        Optional<Tool> result = Optional.empty();
        boolean finished = false;
        Cursor cursor = Cursor.Start.INSTANCE;
        do {
            var page = listTools(clientId, cursor);
            for (var t : page.tools()) {
                if (t.name().equals(name)) {
                    result = Optional.of(t);
                    finished = true;
                    break;
                }
            }
            if (finished) break;
            cursor = page.nextCursor();
        } while (!(cursor instanceof Cursor.End));
        if (!finished) {
            result = Optional.empty();
        }
        var tool = result.orElseThrow(() -> new IllegalArgumentException("Tool not found: " + name));
        ensureToolAllowed(tool.name());
        toolAccess.requireAllowed(principal, tool);
        var resp = JsonRpc.expectResponse(client.request(RequestMethod.TOOLS_CALL, CALL_TOOL_REQUEST_CODEC.toJson(new CallToolRequest(name, args, null)), TIMEOUT));
        return TOOL_RESULT_ABSTRACT_ENTITY_CODEC.fromJson(resp.result());
    }

    public JsonObject createMessage(String clientId, JsonObject params) throws IOException {
        var client = requireClient(clientId);
        if (!client.connected()) {
            throw new IllegalStateException("Client not connected: " + clientId);
        }
        requireCapability(client, ClientCapability.SAMPLING);
        requireConsent(principal, "sampling");
        ensureSamplingAllowed();
        samplingAccess.requireAllowed(principal);
        var resp = JsonRpc.expectResponse(client.request(RequestMethod.SAMPLING_CREATE_MESSAGE, params, TIMEOUT));
        return resp.result();
    }

    public void grantConsent(String scope) {
        grant(principal.id(), scope);
    }

    public void revokeConsent(String scope) {
        revoke(principal.id(), scope);
    }

    public void allowTool(String tool) {
        allowedTools.add(tool);
        toolAccess.allow(principal.id(), tool);
    }

    public void revokeTool(String tool) {
        allowedTools.remove(tool);
        toolAccess.revoke(principal.id(), tool);
    }

    public void allowSampling() {
        samplingAllowed.set(true);
        samplingAccess.allow(principal.id());
    }

    public void revokeSampling() {
        samplingAllowed.set(false);
        samplingAccess.revoke(principal.id());
    }

    public void allowAudience(Role audience) {
        allowedAudiences.add(audience);
        privacyBoundary.allow(principal.id(), audience);
    }

    public void revokeAudience(Role audience) {
        allowedAudiences.remove(audience);
        privacyBoundary.revoke(principal.id(), audience);
    }

    public Set<String> clientIds() {
        return Collections.unmodifiableSet(clients.keySet());
    }

    public McpClient client(String id) {
        return requireClient(id);
    }

    public Events events(String id) {
        return events.computeIfAbsent(id, k -> new Events());
    }

    private void register(String id, McpClient client, McpClientConfiguration clientConfig) {
        requireConsent(principal, client.info().name());
        if (clients.putIfAbsent(id, client) != null) {
            throw new IllegalArgumentException("Client already registered: " + id);
        }
        client.setPrincipal(principal);
        client.setSamplingAccessPolicy(samplingAccess);
        client.configurePing(
                clientConfig.pingInterval(),
                clientConfig.pingTimeout());
    }

    private McpClient requireClient(String id) {
        var client = clients.get(id);
        if (client == null) {
            throw new IllegalArgumentException("Unknown client: " + id);
        }
        return client;
    }

    private void grant(String principalId, String scope) {
        if (principalId == null || principalId.isBlank()) {
            throw new IllegalArgumentException("principalId required");
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope required");
        }
        consents.computeIfAbsent(principalId, k -> ConcurrentHashMap.newKeySet()).add(scope);
    }

    private void revoke(String principalId, String scope) {
        if (principalId == null || principalId.isBlank()) {
            throw new IllegalArgumentException("principalId required");
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope required");
        }
        var set = consents.get(principalId);
        if (set != null) {
            set.remove(scope);
        }
    }

    private void requireConsent(Principal principal, String scope) {
        if (principal == null) {
            throw new IllegalArgumentException("principal required");
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("scope required");
        }
        var set = consents.get(principal.id());
        if (set == null || !set.contains(scope)) {
            throw new SecurityException("User consent required: " + scope);
        }
    }

    private boolean allowed(Annotations annotations) {
        try {
            ensureAudienceAllowed(annotations);
            privacyBoundary.requireAllowed(principal, annotations);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    private void ensureToolAllowed(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("tool required");
        }
        if (!allowedTools.contains(name)) {
            throw new SecurityException("Tool not authorized: " + name);
        }
    }

    private void ensureSamplingAllowed() {
        if (!samplingAllowed.get()) {
            throw new SecurityException("Sampling not authorized");
        }
    }

    private void ensureAudienceAllowed(Annotations annotations) {
        if (annotations == null || annotations.audience().isEmpty()) {
            return;
        }
        for (var role : annotations.audience()) {
            if (!allowedAudiences.contains(role)) {
                throw new SecurityException("Audience not permitted: " + annotations.audience());
            }
        }
    }

    private static final class ClientRootsProvider implements RootsProvider {
        private final CopyOnWriteArrayList<Root> roots;
        private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

        private ClientRootsProvider(List<Root> initial) {
            this.roots = new CopyOnWriteArrayList<>(initial);
        }

        @Override
        public Pagination.Page<Root> list(Cursor cursor) {
            return Pagination.page(roots, cursor == null ? Cursor.Start.INSTANCE : cursor, Pagination.DEFAULT_PAGE_SIZE);
        }

        @Override
        public AutoCloseable onListChanged(Runnable listener) {
            Objects.requireNonNull(listener, "listener");
            listeners.add(listener);
            return () -> listeners.remove(listener);
        }

        @Override
        public boolean supportsListChanged() {
            return true;
        }

        @Override
        public void close() {
            listeners.clear();
        }
    }

    public static final class Events {
        final List<LoggingMessageNotification> messages = new CopyOnWriteArrayList<>();
        final AtomicBoolean resourceListChanged = new AtomicBoolean();
        final AtomicBoolean toolListChanged = new AtomicBoolean();
        final AtomicBoolean promptsListChanged = new AtomicBoolean();

        public Events() {
        }

        public List<LoggingMessageNotification> messages() {
            return List.copyOf(messages);
        }

        public boolean resourceListChanged() {
            return resourceListChanged.get();
        }

        public boolean toolListChanged() {
            return toolListChanged.get();
        }

        public boolean promptsListChanged() {
            return promptsListChanged.get();
        }

        public void clearMessages() {
            messages.clear();
        }

        public void resetResourceListChanged() {
            resourceListChanged.set(false);
        }

        public void resetToolListChanged() {
            toolListChanged.set(false);
        }

        public void resetPromptsListChanged() {
            promptsListChanged.set(false);
        }
    }
}
