package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.ClientRuntime;
import com.amannmalik.mcp.spi.*;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public interface McpClient extends AutoCloseable {

    static McpClient create(McpClientConfiguration config,
                            boolean globalVerbose,
                            SamplingProvider sampling,
                            RootsProvider roots,
                            ElicitationProvider elicitation,
                            McpClientListener listener) throws IOException {
        return new ClientRuntime(
                config,
                globalVerbose,
                sampling,
                roots,
                elicitation,
                listener);
    }

    ClientInfo info();

    void connect() throws IOException;

    void disconnect() throws IOException;

    boolean connected();

    Set<ClientCapability> capabilities();

    Set<ServerCapability> serverCapabilities();

    Set<ServerFeature> serverFeatures();

    String context();

    void ping(Duration timeoutMillis) throws IOException;

    void setLogLevel(LoggingLevel level) throws IOException;

    JsonRpcMessage request(RequestMethod method, JsonObject params, Duration timeoutMillis) throws IOException;

    JsonRpcMessage request(RequestId id, RequestMethod method, JsonObject params, Duration timeoutMillis) throws IOException;

    void sendNotification(NotificationMethod method, JsonObject params) throws IOException;

    @Override
    void close() throws IOException;

    String protocolVersion();

    ServerInfo serverInfo();

    Set<String> serverCapabilityNames();

    Map<String, String> serverInfoMap();

    ListResourcesResult listResources(Cursor cursor) throws IOException;

    ListResourceTemplatesResult listResourceTemplates(Cursor cursor) throws IOException;

    void configurePing(Duration intervalMillis, Duration timeoutMillis);

    void setSamplingAccessPolicy(SamplingAccessPolicy policy);

    void setPrincipal(Principal principal);

    AutoCloseable subscribeResource(URI uri, Consumer<ResourceUpdate> listener) throws IOException;

    interface McpClientListener {
        default void onProgress(Notification.ProgressNotification notification) {
        }

        default void onMessage(Notification.LoggingMessageNotification notification) {
        }

        default void onResourceListChanged() {
        }

        default void onToolListChanged() {
        }

        default void onPromptsListChanged() {
        }
    }
}
