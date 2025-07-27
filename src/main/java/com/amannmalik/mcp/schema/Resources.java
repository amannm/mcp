package com.amannmalik.mcp.schema;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Resources {
    private Resources() {}

    public record ListResourcesRequest(JsonRpcTypes.RequestId id,
                                       Optional<Cursor> cursor,
                                       Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public ListResourcesRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(cursor);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "resources/list"; }
    }

    public record ListResourcesResult(JsonRpcTypes.RequestId id,
                                      List<Resource> resources,
                                      Optional<Cursor> nextCursor,
                                      Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Result {
        public ListResourcesResult {
            Objects.requireNonNull(id);
            resources = List.copyOf(resources);
            Objects.requireNonNull(nextCursor);
            Objects.requireNonNull(_meta);
        }
    }

    public record Resource(String name,
                           String uri,
                           Optional<String> description,
                           Optional<String> mediaType) {
        public Resource {
            Objects.requireNonNull(name);
            Objects.requireNonNull(uri);
            Objects.requireNonNull(description);
            Objects.requireNonNull(mediaType);
        }
    }

    public record ReadResourceRequest(JsonRpcTypes.RequestId id,
                                      String uri,
                                      Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public ReadResourceRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(uri);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "resources/read"; }
    }

    public record ReadResourceResult(JsonRpcTypes.RequestId id,
                                     ResourceContents contents,
                                     Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Result {
        public ReadResourceResult {
            Objects.requireNonNull(id);
            Objects.requireNonNull(contents);
            Objects.requireNonNull(_meta);
        }
    }

    public record SubscribeResourcesRequest(JsonRpcTypes.RequestId id,
                                            Optional<String> filter,
                                            Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public SubscribeResourcesRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(filter);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "resources/subscribe"; }
    }

    public record ResourcesChangedNotification(Resource resource,
                                               Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Notification {
        public ResourcesChangedNotification {
            Objects.requireNonNull(resource);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "notifications/resources/changed"; }
    }

    public sealed interface ResourceContents
            permits ResourceContents.TextResourceContents,
                    ResourceContents.BlobResourceContents {
        String mediaType();

        record TextResourceContents(String text, String mediaType)
                implements ResourceContents {
            public TextResourceContents {
                Objects.requireNonNull(text);
                Objects.requireNonNull(mediaType);
            }
        }

        record BlobResourceContents(byte[] data, String mediaType)
                implements ResourceContents {
            public BlobResourceContents {
                Objects.requireNonNull(data);
                Objects.requireNonNull(mediaType);
            }
        }
    }

    public record ResourceTemplate(String name,
                                   String uriTemplate,
                                   Optional<String> description,
                                   Optional<String> mediaType) {
        public ResourceTemplate {
            Objects.requireNonNull(name);
            Objects.requireNonNull(uriTemplate);
            Objects.requireNonNull(description);
            Objects.requireNonNull(mediaType);
        }
    }

    public record ListResourceTemplatesRequest(JsonRpcTypes.RequestId id,
                                               Optional<Cursor> cursor,
                                               Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Request {
        public ListResourceTemplatesRequest {
            Objects.requireNonNull(id);
            Objects.requireNonNull(cursor);
            Objects.requireNonNull(_meta);
        }
        @Override public String method() { return "resources/templates/list"; }
    }

    public record ListResourceTemplatesResult(JsonRpcTypes.RequestId id,
                                              List<ResourceTemplate> templates,
                                              Optional<Cursor> nextCursor,
                                              Optional<Map<String, Object>> _meta)
            implements BaseProtocol.Result {
        public ListResourceTemplatesResult {
            Objects.requireNonNull(id);
            templates = List.copyOf(templates);
            Objects.requireNonNull(nextCursor);
            Objects.requireNonNull(_meta);
        }
    }
}
