package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.HostRuntime;
import com.amannmalik.mcp.spi.*;
import jakarta.json.JsonObject;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface McpHost extends Closeable {
    static McpHost create(McpHostConfiguration config) throws IOException {
        return new HostRuntime(config);
    }

    @Override
    void close() throws IOException;

    void connect(String id) throws IOException;

    void unregister(String id) throws IOException;

    String aggregateContext();

    ListResourcesResult listResources(String clientId, Cursor cursor) throws IOException;

    ListResourceTemplatesResult listResourceTemplates(String clientId, Cursor cursor) throws IOException;

    Closeable subscribeToResource(String clientId, URI uri, Consumer<ResourceUpdate> listener) throws IOException;

    ListToolsResult listTools(String clientId, Cursor cursor) throws IOException;

    ToolResult callTool(String clientId, String name, JsonObject args) throws IOException;

    JsonObject createMessage(String clientId, JsonObject params) throws IOException;

    void grantConsent(String scope);

    void revokeConsent(String scope);

    void allowTool(String tool);

    void revokeTool(String tool);

    void allowSampling();

    void revokeSampling();

    void allowAudience(Role audience);

    void revokeAudience(Role audience);

    Set<String> clientIds();

    McpClient client(String id);

    Events events(String id);

    interface Events {
        List<Notification.LoggingMessageNotification> messages();

        boolean resourceListChanged();

        boolean toolListChanged();

        boolean promptsListChanged();

        void clearMessages();

        void resetResourceListChanged();

        void resetToolListChanged();

        void resetPromptsListChanged();
    }
}
