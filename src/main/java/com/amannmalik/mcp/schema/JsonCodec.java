package com.amannmalik.mcp.schema;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Jakarta JSON-P based serialization for MCP messages.
 */
public final class JsonCodec {
    private JsonCodec() {}

    public static final class McpJsonCodec {
        private McpJsonCodec() {}

        public static JsonObject toJson(JsonRpcTypes.JsonRpcMessage message) {
            if (message instanceof BaseProtocol.Request r) return requestToJson(r);
            if (message instanceof BaseProtocol.Result r) return resultToJson(r);
            if (message instanceof BaseProtocol.Notification n) return notificationToJson(n);
            if (message instanceof JsonRpcTypes.JsonRpcError e) return errorToJson(e);
            throw new IllegalArgumentException("Unsupported message type: " + message.getClass());
        }

        @SuppressWarnings("unchecked")
        public static <T extends JsonRpcTypes.JsonRpcMessage> T fromJson(JsonObject json, Class<T> type) {
            if (BaseProtocol.Request.class.isAssignableFrom(type)) {
                return (T) requestFromJson(json, (Class<? extends BaseProtocol.Request>) type);
            }
            if (BaseProtocol.Result.class.isAssignableFrom(type)) {
                return (T) resultFromJson(json, (Class<? extends BaseProtocol.Result>) type);
            }
            if (BaseProtocol.Notification.class.isAssignableFrom(type)) {
                return (T) notificationFromJson(json, (Class<? extends BaseProtocol.Notification>) type);
            }
            if (JsonRpcTypes.JsonRpcError.class.isAssignableFrom(type)) {
                return (T) errorFromJson(json);
            }
            throw new IllegalArgumentException("Unknown message type: " + type);
        }

        public static JsonObject requestToJson(BaseProtocol.Request request) {
            JsonObjectBuilder params = Json.createObjectBuilder();
            switch (request) {
                case BaseOperations.PingRequest r -> addMeta(params, r._meta());
                case Initialization.InitializeRequest r -> {
                    params.add("protocolVersion", r.protocolVersion());
                    params.add("capabilities", clientCapabilitiesToJson(r.capabilities()));
                    r.clientInfo().ifPresent(m -> params.add("clientInfo", mapToJson(m)));
                    addMeta(params, r._meta());
                }
                case Resources.ListResourcesRequest r -> {
                    r.cursor().ifPresent(c -> params.add("cursor", c.value()));
                    addMeta(params, r._meta());
                }
                case Resources.ReadResourceRequest r -> {
                    params.add("uri", r.uri());
                    addMeta(params, r._meta());
                }
                case Resources.SubscribeResourcesRequest r -> {
                    r.filter().ifPresent(f -> params.add("filter", f));
                    addMeta(params, r._meta());
                }
                case Resources.ListResourceTemplatesRequest r -> {
                    r.cursor().ifPresent(c -> params.add("cursor", c.value()));
                    addMeta(params, r._meta());
                }
                case Prompts.ListPromptsRequest r -> {
                    r.cursor().ifPresent(c -> params.add("cursor", c.value()));
                    addMeta(params, r._meta());
                }
                case Prompts.GetPromptRequest r -> {
                    params.add("name", r.name());
                    params.add("arguments", mapToJsonStrings(r.arguments()));
                    addMeta(params, r._meta());
                }
                case Tools.ListToolsRequest r -> {
                    r.cursor().ifPresent(c -> params.add("cursor", c.value()));
                    addMeta(params, r._meta());
                }
                case Tools.CallToolRequest r -> {
                    params.add("name", r.name());
                    params.add("arguments", mapToJson(r.arguments()));
                    addMeta(params, r._meta());
                }
                case Sampling.CreateMessageRequest r -> {
                    params.add("messages", messagesToJson(r.messages()));
                    r.modelPreferences().ifPresent(p -> params.add("modelPreferences", modelPrefsToJson(p)));
                    r.progressToken().ifPresent(t -> params.add("progressToken", progressTokenToJson(t)));
                    addMeta(params, r._meta());
                }
                case Logging.SetLevelRequest r -> {
                    params.add("level", r.level().name());
                    addMeta(params, r._meta());
                }
                case Completion.CompleteRequest r -> {
                    params.add("reference", completionRefToJson(r.reference()));
                    params.add("argument", completionArgToJson(r.argument()));
                    r.progressToken().ifPresent(t -> params.add("progressToken", progressTokenToJson(t)));
                    addMeta(params, r._meta());
                }
                case Roots.ListRootsRequest r -> addMeta(params, r._meta());
                case Elicitation.ElicitRequest r -> {
                    params.add("message", r.message());
                    params.add("requestedSchema", elicitationSchemaToJson(r.requestedSchema()));
                    addMeta(params, r._meta());
                }
                case BaseProtocol.Request rr -> throw new IllegalArgumentException("Unsupported request type: " + rr.getClass());
            }
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("jsonrpc", JsonRpcTypes.JSONRPC_VERSION)
                    .add("id", idToJson(request.id()))
                    .add("method", request.method());
            JsonObject p = params.build();
            if (!p.isEmpty()) b.add("params", p);
            return b.build();
        }

        public static JsonObject resultToJson(BaseProtocol.Result result) {
            JsonObjectBuilder res = Json.createObjectBuilder();
            switch (result) {
                case BaseProtocol.EmptyResult r -> {}
                case Initialization.InitializeResult r -> {
                    res.add("protocolVersion", r.protocolVersion());
                    res.add("capabilities", serverCapabilitiesToJson(r.capabilities()));
                    r.serverInfo().ifPresent(m -> res.add("serverInfo", mapToJson(m)));
                    r.instructions().ifPresent(i -> res.add("instructions", i));
                }
                case Resources.ListResourcesResult r -> {
                    res.add("resources", resourcesToJson(r.resources()));
                    r.nextCursor().ifPresent(c -> res.add("nextCursor", c.value()));
                }
                case Resources.ReadResourceResult r -> {
                    res.add("contents", resourceContentsToJson(r.contents()));
                }
                case Resources.ListResourceTemplatesResult r -> {
                    res.add("templates", resourceTemplatesToJson(r.templates()));
                    r.nextCursor().ifPresent(c -> res.add("nextCursor", c.value()));
                }
                case Tools.ListToolsResult r -> {
                    res.add("tools", toolsToJson(r.tools()));
                    r.nextCursor().ifPresent(c -> res.add("nextCursor", c.value()));
                }
                case Tools.CallToolResult r -> {
                    res.add("content", contentListToJson(r.content()));
                    r.isError().ifPresent(bv -> res.add("isError", bv));
                }
                case Completion.CompleteResult r -> {
                    res.add("completion", completionDataToJson(r.completion()));
                }
                case Roots.ListRootsResult r -> {
                    res.add("roots", rootsToJson(r.roots()));
                }
                case Prompts.ListPromptsResult r -> {
                    res.add("prompts", promptsToJson(r.prompts()));
                    r.nextCursor().ifPresent(c -> res.add("nextCursor", c.value()));
                }
                case Prompts.GetPromptResult r -> {
                    res.add("content", contentListToJson(r.content()));
                }
                case Elicitation.ElicitResult r -> {
                    res.add("action", r.action().name());
                    r.content().ifPresent(m -> res.add("content", mapToJson(m)));
                }
                case BaseProtocol.Result rr -> throw new IllegalArgumentException("Unsupported result type: " + rr.getClass());
            }
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("jsonrpc", JsonRpcTypes.JSONRPC_VERSION)
                    .add("id", idToJson(result.id()))
                    .add("result", res.build());
            addMeta(b, result._meta());
            return b.build();
        }

        public static JsonObject notificationToJson(BaseProtocol.Notification notification) {
            JsonObjectBuilder params = Json.createObjectBuilder();
            switch (notification) {
                case BaseOperations.ProgressNotification n -> {
                    params.add("progressToken", progressTokenToJson(n.progressToken()));
                    params.add("progress", n.progress());
                    n.total().ifPresent(t -> params.add("total", t));
                    n.message().ifPresent(m -> params.add("message", m));
                    addMeta(params, n._meta());
                }
                case BaseOperations.CancelledNotification n -> {
                    params.add("requestId", idToJson(n.requestId()));
                    n.reason().ifPresent(r -> params.add("reason", r));
                    addMeta(params, n._meta());
                }
                case Initialization.InitializedNotification n -> addMeta(params, n._meta());
                case Resources.ResourcesChangedNotification n -> {
                    params.add("resource", resourceToJson(n.resource()));
                    addMeta(params, n._meta());
                }
                case Logging.LoggingMessageNotification n -> {
                    params.add("level", n.level().name());
                    params.add("data", mapToJson(n.data()));
                    addMeta(params, n._meta());
                }
                case Roots.RootsListChangedNotification n -> addMeta(params, n._meta());
                case BaseProtocol.Notification nn -> throw new IllegalArgumentException("Unsupported notification type: " + nn.getClass());
            }
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("jsonrpc", JsonRpcTypes.JSONRPC_VERSION)
                    .add("method", notification.method());
            JsonObject p = params.build();
            if (!p.isEmpty()) b.add("params", p);
            return b.build();
        }

        private static JsonObject errorToJson(JsonRpcTypes.JsonRpcError error) {
            JsonObject err = Json.createObjectBuilder()
                    .add("code", error.code())
                    .add("message", error.message())
                    .build();
            return Json.createObjectBuilder()
                    .add("jsonrpc", JsonRpcTypes.JSONRPC_VERSION)
                    .add("id", idToJson(error.id()))
                    .add("error", err)
                    .build();
        }

        private static BaseProtocol.Request requestFromJson(JsonObject json, Class<? extends BaseProtocol.Request> type) {
            JsonObject params = json.getJsonObject("params");
            JsonRpcTypes.RequestId id = idFromJson(json.get("id"));
            if (type == BaseOperations.PingRequest.class) {
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new BaseOperations.PingRequest(id, meta);
            }
            if (type == Initialization.InitializeRequest.class) {
                String protocolVersion = params.getString("protocolVersion");
                Capabilities.ClientCapabilities caps = clientCapabilitiesFromJson(params.getJsonObject("capabilities"));
                Optional<Map<String, Object>> clientInfo = optionalMap(params, "clientInfo");
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Initialization.InitializeRequest(id, protocolVersion, caps, clientInfo, meta);
            }
            if (type == Resources.ListResourcesRequest.class) {
                Optional<Cursor> cursor = optionalString(params, "cursor").map(Cursor::new);
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Resources.ListResourcesRequest(id, cursor, meta);
            }
            if (type == Resources.ReadResourceRequest.class) {
                String uri = params.getString("uri");
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Resources.ReadResourceRequest(id, uri, meta);
            }
            if (type == Resources.SubscribeResourcesRequest.class) {
                Optional<String> filter = optionalString(params, "filter");
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Resources.SubscribeResourcesRequest(id, filter, meta);
            }
            if (type == Resources.ListResourceTemplatesRequest.class) {
                Optional<Cursor> cursor = optionalString(params, "cursor").map(Cursor::new);
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Resources.ListResourceTemplatesRequest(id, cursor, meta);
            }
            if (type == Prompts.ListPromptsRequest.class) {
                Optional<Cursor> cursor = optionalString(params, "cursor").map(Cursor::new);
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Prompts.ListPromptsRequest(id, cursor, meta);
            }
            if (type == Prompts.GetPromptRequest.class) {
                String name = params.getString("name");
                Map<String, String> arguments = stringMapFromJson(params.getJsonObject("arguments"));
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Prompts.GetPromptRequest(id, name, arguments, meta);
            }
            if (type == Tools.ListToolsRequest.class) {
                Optional<Cursor> cursor = optionalString(params, "cursor").map(Cursor::new);
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Tools.ListToolsRequest(id, cursor, meta);
            }
            if (type == Tools.CallToolRequest.class) {
                String name = params.getString("name");
                Map<String, Object> arguments = jsonToMap(params.getJsonObject("arguments"));
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Tools.CallToolRequest(id, name, arguments, meta);
            }
            if (type == Sampling.CreateMessageRequest.class) {
                List<Sampling.SamplingMessage> messages = messagesFromJson(params.getJsonArray("messages"));
                Optional<Sampling.ModelPreferences> prefs = optional(params, "modelPreferences").map(v -> modelPrefsFromJson(v.asJsonObject()));
                Optional<BaseProtocol.ProgressToken> token = optional(params, "progressToken").map(McpJsonCodec::progressTokenFromJson);
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Sampling.CreateMessageRequest(id, messages, prefs, token, meta);
            }
            if (type == Logging.SetLevelRequest.class) {
                Logging.LoggingLevel level = Logging.LoggingLevel.valueOf(params.getString("level"));
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Logging.SetLevelRequest(id, level, meta);
            }
            if (type == Completion.CompleteRequest.class) {
                Completion.CompletionReference ref = completionRefFromJson(params.getJsonObject("reference"));
                Completion.CompletionArgument arg = completionArgFromJson(params.getJsonObject("argument"));
                Optional<BaseProtocol.ProgressToken> token = optional(params, "progressToken").map(McpJsonCodec::progressTokenFromJson);
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Completion.CompleteRequest(id, ref, arg, token, meta);
            }
            if (type == Roots.ListRootsRequest.class) {
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Roots.ListRootsRequest(id, meta);
            }
            if (type == Elicitation.ElicitRequest.class) {
                String message = params.getString("message");
                Elicitation.ElicitationSchema schema = elicitationSchemaFromJson(params.getJsonObject("requestedSchema"));
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Elicitation.ElicitRequest(id, message, schema, meta);
            }
            throw new IllegalArgumentException("Unsupported request class: " + type);
        }

        private static BaseProtocol.Result resultFromJson(JsonObject json, Class<? extends BaseProtocol.Result> type) {
            JsonObject res = json.getJsonObject("result");
            JsonRpcTypes.RequestId id = idFromJson(json.get("id"));
            if (type == BaseProtocol.EmptyResult.class) {
                Optional<Map<String, Object>> meta = metaFromJson(json);
                return new BaseProtocol.EmptyResult(id, meta);
            }
            if (type == Initialization.InitializeResult.class) {
                String protocolVersion = res.getString("protocolVersion");
                Capabilities.ServerCapabilities caps = serverCapabilitiesFromJson(res.getJsonObject("capabilities"));
                Optional<Map<String, Object>> serverInfo = optionalMap(res, "serverInfo");
                Optional<String> instructions = optionalString(res, "instructions");
                Optional<Map<String, Object>> meta = metaFromJson(json);
                return new Initialization.InitializeResult(id, protocolVersion, caps, serverInfo, instructions, meta);
            }
            if (type == Resources.ListResourcesResult.class) {
                List<Resources.Resource> resources = resourcesFromJson(res.getJsonArray("resources"));
                Optional<Cursor> next = optionalString(res, "nextCursor").map(Cursor::new);
                Optional<Map<String, Object>> meta = metaFromJson(json);
                return new Resources.ListResourcesResult(id, resources, next, meta);
            }
            if (type == Resources.ReadResourceResult.class) {
                Resources.ResourceContents contents = resourceContentsFromJson(res.getJsonObject("contents"));
                Optional<Map<String, Object>> meta = metaFromJson(json);
                return new Resources.ReadResourceResult(id, contents, meta);
            }
            if (type == Resources.ListResourceTemplatesResult.class) {
                List<Resources.ResourceTemplate> templates = resourceTemplatesFromJson(res.getJsonArray("templates"));
                Optional<Cursor> next = optionalString(res, "nextCursor").map(Cursor::new);
                Optional<Map<String, Object>> meta = metaFromJson(json);
                return new Resources.ListResourceTemplatesResult(id, templates, next, meta);
            }
            if (type == Tools.ListToolsResult.class) {
                List<Tools.Tool> tools = toolsFromJson(res.getJsonArray("tools"));
                Optional<Cursor> next = optionalString(res, "nextCursor").map(Cursor::new);
                Optional<Map<String, Object>> meta = metaFromJson(json);
                return new Tools.ListToolsResult(id, tools, next, meta);
            }
            if (type == Tools.CallToolResult.class) {
                List<ContentBlock> content = contentListFromJson(res.getJsonArray("content"));
                Optional<Boolean> isError = optionalBoolean(res, "isError");
                Optional<Map<String, Object>> meta = metaFromJson(json);
                return new Tools.CallToolResult(id, content, isError, meta);
            }
            if (type == Completion.CompleteResult.class) {
                Completion.CompletionData data = completionDataFromJson(res.getJsonObject("completion"));
                Optional<Map<String, Object>> meta = metaFromJson(json);
                return new Completion.CompleteResult(id, data, meta);
            }
            if (type == Roots.ListRootsResult.class) {
                List<Roots.Root> roots = rootsFromJson(res.getJsonArray("roots"));
                Optional<Map<String, Object>> meta = metaFromJson(json);
                return new Roots.ListRootsResult(id, roots, meta);
            }
            if (type == Prompts.ListPromptsResult.class) {
                List<Prompts.Prompt> prompts = promptsFromJson(res.getJsonArray("prompts"));
                Optional<Cursor> next = optionalString(res, "nextCursor").map(Cursor::new);
                Optional<Map<String, Object>> meta = metaFromJson(json);
                return new Prompts.ListPromptsResult(id, prompts, next, meta);
            }
            if (type == Prompts.GetPromptResult.class) {
                List<ContentBlock> content = contentListFromJson(res.getJsonArray("content"));
                Optional<Map<String, Object>> meta = metaFromJson(json);
                return new Prompts.GetPromptResult(id, content, meta);
            }
            if (type == Elicitation.ElicitResult.class) {
                Elicitation.ElicitAction action = Elicitation.ElicitAction.valueOf(res.getString("action"));
                Optional<Map<String, Object>> content = optionalMap(res, "content");
                Optional<Map<String, Object>> meta = metaFromJson(json);
                return new Elicitation.ElicitResult(id, action, content, meta);
            }
            throw new IllegalArgumentException("Unsupported result class: " + type);
        }

        private static BaseProtocol.Notification notificationFromJson(JsonObject json, Class<? extends BaseProtocol.Notification> type) {
            JsonObject params = json.getJsonObject("params");
            if (type == BaseOperations.ProgressNotification.class) {
                BaseProtocol.ProgressToken token = progressTokenFromJson(params.get("progressToken"));
                double progress = params.getJsonNumber("progress").doubleValue();
                Optional<Double> total = optionalNumber(params, "total");
                Optional<String> message = optionalString(params, "message");
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new BaseOperations.ProgressNotification(token, progress, total, message, meta);
            }
            if (type == BaseOperations.CancelledNotification.class) {
                JsonRpcTypes.RequestId requestId = idFromJson(params.get("requestId"));
                Optional<String> reason = optionalString(params, "reason");
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new BaseOperations.CancelledNotification(requestId, reason, meta);
            }
            if (type == Initialization.InitializedNotification.class) {
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Initialization.InitializedNotification(meta);
            }
            if (type == Resources.ResourcesChangedNotification.class) {
                Resources.Resource resource = resourceFromJson(params.getJsonObject("resource"));
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Resources.ResourcesChangedNotification(resource, meta);
            }
            if (type == Logging.LoggingMessageNotification.class) {
                Logging.LoggingLevel level = Logging.LoggingLevel.valueOf(params.getString("level"));
                Map<String, Object> data = jsonToMap(params.getJsonObject("data"));
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Logging.LoggingMessageNotification(level, data, meta);
            }
            if (type == Roots.RootsListChangedNotification.class) {
                Optional<Map<String, Object>> meta = metaFromJson(params);
                return new Roots.RootsListChangedNotification(meta);
            }
            throw new IllegalArgumentException("Unsupported notification class: " + type);
        }

        private static JsonRpcTypes.JsonRpcError errorFromJson(JsonObject json) {
            JsonRpcTypes.RequestId id = idFromJson(json.get("id"));
            JsonObject err = json.getJsonObject("error");
            int code = err.getInt("code");
            String message = err.getString("message");
            return new JsonRpcTypes.BasicError(id, code, message);
        }

        private static JsonValue idToJson(JsonRpcTypes.RequestId id) {
            return switch (id) {
                case JsonRpcTypes.StringRequestId s -> Json.createValue(s.value());
                case JsonRpcTypes.NumberRequestId n -> Json.createValue(n.value());
            };
        }

        private static JsonRpcTypes.RequestId idFromJson(JsonValue v) {
            return switch (v.getValueType()) {
                case STRING -> new JsonRpcTypes.StringRequestId(((JsonString) v).getString());
                case NUMBER -> new JsonRpcTypes.NumberRequestId(((JsonNumber) v).longValue());
                default -> throw new IllegalArgumentException("id must be string or number");
            };
        }

        private static JsonValue progressTokenToJson(BaseProtocol.ProgressToken t) {
            return switch (t) {
                case BaseProtocol.StringProgressToken s -> Json.createValue(s.value());
                case BaseProtocol.NumberProgressToken n -> Json.createValue(n.value());
            };
        }

        private static BaseProtocol.ProgressToken progressTokenFromJson(JsonValue v) {
            return switch (v.getValueType()) {
                case STRING -> new BaseProtocol.StringProgressToken(((JsonString) v).getString());
                case NUMBER -> new BaseProtocol.NumberProgressToken(((JsonNumber) v).longValue());
                default -> throw new IllegalArgumentException("progressToken must be string or number");
            };
        }

        private static void addMeta(JsonObjectBuilder b, Optional<Map<String, Object>> meta) {
            meta.ifPresent(m -> b.add("_meta", mapToJson(m)));
        }

        private static Optional<Map<String, Object>> metaFromJson(JsonObject obj) {
            return optionalMap(obj, "_meta");
        }

        private static Optional<JsonValue> optional(JsonObject obj, String name) {
            return obj.containsKey(name) ? Optional.of(obj.get(name)) : Optional.empty();
        }

        private static Optional<String> optionalString(JsonObject obj, String name) {
            return obj.containsKey(name) ? Optional.of(obj.getString(name)) : Optional.empty();
        }

        private static Optional<Boolean> optionalBoolean(JsonObject obj, String name) {
            return obj.containsKey(name) ? Optional.of(obj.getBoolean(name)) : Optional.empty();
        }

        private static Optional<Double> optionalNumber(JsonObject obj, String name) {
            return obj.containsKey(name) ? Optional.of(obj.getJsonNumber(name).doubleValue()) : Optional.empty();
        }

        private static Optional<Map<String, Object>> optionalMap(JsonObject obj, String name) {
            return obj.containsKey(name) ? Optional.of(jsonToMap(obj.getJsonObject(name))) : Optional.empty();
        }

        private static Map<String, String> stringMapFromJson(JsonObject obj) {
            Map<String, String> map = new LinkedHashMap<>();
            obj.forEach((k, v) -> map.put(k, ((JsonString) v).getString()));
            return map;
        }

        private static JsonObject clientCapabilitiesToJson(Capabilities.ClientCapabilities c) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            c.roots().ifPresent(v -> b.add("roots", JsonValue.TRUE));
            c.sampling().ifPresent(v -> b.add("sampling", JsonValue.TRUE));
            c.experimental().ifPresent(m -> b.add("experimental", mapToJson(m)));
            return b.build();
        }

        private static Capabilities.ClientCapabilities clientCapabilitiesFromJson(JsonObject obj) {
            Optional<Capabilities.RootsCapability> roots = obj.containsKey("roots") ? Optional.of(new Capabilities.RootsCapability()) : Optional.empty();
            Optional<Capabilities.SamplingCapability> sampling = obj.containsKey("sampling") ? Optional.of(new Capabilities.SamplingCapability()) : Optional.empty();
            Optional<Map<String, Object>> experimental = optionalMap(obj, "experimental");
            return new Capabilities.ClientCapabilities(roots, sampling, experimental);
        }

        private static JsonObject serverCapabilitiesToJson(Capabilities.ServerCapabilities s) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            s.prompts().ifPresent(v -> b.add("prompts", JsonValue.TRUE));
            s.resources().ifPresent(v -> b.add("resources", JsonValue.TRUE));
            s.tools().ifPresent(v -> b.add("tools", JsonValue.TRUE));
            s.logging().ifPresent(v -> b.add("logging", JsonValue.TRUE));
            s.experimental().ifPresent(m -> b.add("experimental", mapToJson(m)));
            return b.build();
        }

        private static Capabilities.ServerCapabilities serverCapabilitiesFromJson(JsonObject obj) {
            Optional<Capabilities.PromptsCapability> prompts = obj.containsKey("prompts") ? Optional.of(new Capabilities.PromptsCapability()) : Optional.empty();
            Optional<Capabilities.ResourcesCapability> resources = obj.containsKey("resources") ? Optional.of(new Capabilities.ResourcesCapability()) : Optional.empty();
            Optional<Capabilities.ToolsCapability> tools = obj.containsKey("tools") ? Optional.of(new Capabilities.ToolsCapability()) : Optional.empty();
            Optional<Capabilities.LoggingCapability> logging = obj.containsKey("logging") ? Optional.of(new Capabilities.LoggingCapability()) : Optional.empty();
            Optional<Map<String, Object>> experimental = optionalMap(obj, "experimental");
            return new Capabilities.ServerCapabilities(prompts, resources, tools, logging, experimental);
        }

        private static JsonObject mapToJson(Map<String, Object> map) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            map.forEach((k, v) -> b.add(k, toJsonValue(v)));
            return b.build();
        }

        private static JsonObject mapToJsonStrings(Map<String, String> map) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            map.forEach(b::add);
            return b.build();
        }

        private static Map<String, Object> jsonToMap(JsonObject obj) {
            Map<String, Object> map = new LinkedHashMap<>();
            obj.forEach((k, v) -> map.put(k, fromJsonValue(v)));
            return map;
        }

        private static JsonArrayBuilder listToJson(List<?> list) {
            JsonArrayBuilder ab = Json.createArrayBuilder();
            list.forEach(v -> ab.add(toJsonValue(v)));
            return ab;
        }

        private static List<Object> jsonToList(jakarta.json.JsonArray arr) {
            List<Object> list = new ArrayList<>();
            arr.forEach(v -> list.add(fromJsonValue(v)));
            return list;
        }

        private static JsonValue toJsonValue(Object v) {
            if (v == null) return JsonValue.NULL;
            return switch (v) {
                case String s -> Json.createValue(s);
                case Integer i -> Json.createValue(i);
                case Long l -> Json.createValue(l);
                case Double d -> Json.createValue(d);
                case Float f -> Json.createValue(f.doubleValue());
                case Boolean b -> b ? JsonValue.TRUE : JsonValue.FALSE;
                case JsonValue j -> j;
                case Map<?, ?> m -> mapToJson((Map<String, Object>) m);
                case List<?> l -> listToJson(l).build();
                case Enum<?> e -> Json.createValue(e.name());
                default -> Json.createValue(v.toString());
            };
        }

        private static Object fromJsonValue(JsonValue v) {
            return switch (v.getValueType()) {
                case STRING -> ((JsonString) v).getString();
                case NUMBER -> ((JsonNumber) v).numberValue();
                case TRUE -> true;
                case FALSE -> false;
                case NULL -> null;
                case OBJECT -> jsonToMap(v.asJsonObject());
                case ARRAY -> jsonToList(v.asJsonArray());
            };
        }

        private static JsonObject contentBlockToJson(ContentBlock block) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            block.annotation().ifPresent(a -> b.add("annotation", a));
            switch (block) {
                case ContentBlock.TextContent tc -> {
                    b.add("type", "text");
                    b.add("text", tc.text());
                }
                case ContentBlock.ImageContent ic -> {
                    b.add("type", "image");
                    b.add("uri", ic.uri());
                }
                case ContentBlock.AudioContent ac -> {
                    b.add("type", "audio");
                    b.add("uri", ac.uri());
                }
                case ContentBlock.ResourceLink rl -> {
                    b.add("type", "link");
                    b.add("uri", rl.uri());
                }
                case ContentBlock.EmbeddedResource er -> {
                    b.add("type", "embed");
                    b.add("name", er.name());
                    b.add("data", Base64.getEncoder().encodeToString(er.data()));
                    b.add("mediaType", er.mediaType());
                }
            }
            return b.build();
        }

        private static ContentBlock contentBlockFromJson(JsonObject obj) {
            String type = obj.getString("type");
            Optional<String> annotation = optionalString(obj, "annotation");
            return switch (type) {
                case "text" -> new ContentBlock.TextContent(obj.getString("text"), annotation);
                case "image" -> new ContentBlock.ImageContent(obj.getString("uri"), annotation);
                case "audio" -> new ContentBlock.AudioContent(obj.getString("uri"), annotation);
                case "link" -> new ContentBlock.ResourceLink(obj.getString("uri"), annotation);
                case "embed" -> new ContentBlock.EmbeddedResource(obj.getString("name"), Base64.getDecoder().decode(obj.getString("data")), obj.getString("mediaType"), annotation);
                default -> throw new IllegalArgumentException("unknown content block type: " + type);
            };
        }

        private static JsonValue contentListToJson(List<ContentBlock> list) {
            JsonArrayBuilder ab = Json.createArrayBuilder();
            list.forEach(c -> ab.add(contentBlockToJson(c)));
            return ab.build();
        }

        private static List<ContentBlock> contentListFromJson(jakarta.json.JsonArray arr) {
            List<ContentBlock> list = new ArrayList<>();
            arr.forEach(v -> list.add(contentBlockFromJson(v.asJsonObject())));
            return list;
        }

        private static JsonObject resourceToJson(Resources.Resource r) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("name", r.name());
            b.add("uri", r.uri());
            r.description().ifPresent(d -> b.add("description", d));
            r.mediaType().ifPresent(m -> b.add("mediaType", m));
            return b.build();
        }

        private static Resources.Resource resourceFromJson(JsonObject obj) {
            String name = obj.getString("name");
            String uri = obj.getString("uri");
            Optional<String> desc = optionalString(obj, "description");
            Optional<String> media = optionalString(obj, "mediaType");
            return new Resources.Resource(name, uri, desc, media);
        }

        private static JsonValue resourcesToJson(List<Resources.Resource> list) {
            JsonArrayBuilder ab = Json.createArrayBuilder();
            list.forEach(r -> ab.add(resourceToJson(r)));
            return ab.build();
        }

        private static List<Resources.Resource> resourcesFromJson(jakarta.json.JsonArray arr) {
            List<Resources.Resource> list = new ArrayList<>();
            arr.forEach(v -> list.add(resourceFromJson(v.asJsonObject())));
            return list;
        }

        private static JsonObject resourceTemplateToJson(Resources.ResourceTemplate t) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("name", t.name());
            b.add("uriTemplate", t.uriTemplate());
            t.description().ifPresent(d -> b.add("description", d));
            t.mediaType().ifPresent(m -> b.add("mediaType", m));
            return b.build();
        }

        private static Resources.ResourceTemplate resourceTemplateFromJson(JsonObject obj) {
            String name = obj.getString("name");
            String uriTemplate = obj.getString("uriTemplate");
            Optional<String> desc = optionalString(obj, "description");
            Optional<String> media = optionalString(obj, "mediaType");
            return new Resources.ResourceTemplate(name, uriTemplate, desc, media);
        }

        private static JsonValue resourceTemplatesToJson(List<Resources.ResourceTemplate> list) {
            JsonArrayBuilder ab = Json.createArrayBuilder();
            list.forEach(t -> ab.add(resourceTemplateToJson(t)));
            return ab.build();
        }

        private static List<Resources.ResourceTemplate> resourceTemplatesFromJson(jakarta.json.JsonArray arr) {
            List<Resources.ResourceTemplate> list = new ArrayList<>();
            arr.forEach(v -> list.add(resourceTemplateFromJson(v.asJsonObject())));
            return list;
        }

        private static JsonObject resourceContentsToJson(Resources.ResourceContents c) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("mediaType", c.mediaType());
            switch (c) {
                case Resources.ResourceContents.TextResourceContents t -> {
                    b.add("type", "text");
                    b.add("text", t.text());
                }
                case Resources.ResourceContents.BlobResourceContents bl -> {
                    b.add("type", "blob");
                    b.add("data", Base64.getEncoder().encodeToString(bl.data()));
                }
            }
            return b.build();
        }

        private static Resources.ResourceContents resourceContentsFromJson(JsonObject obj) {
            String mediaType = obj.getString("mediaType");
            String type = obj.getString("type");
            return switch (type) {
                case "text" -> new Resources.ResourceContents.TextResourceContents(obj.getString("text"), mediaType);
                case "blob" -> new Resources.ResourceContents.BlobResourceContents(Base64.getDecoder().decode(obj.getString("data")), mediaType);
                default -> throw new IllegalArgumentException("unknown contents type: " + type);
            };
        }

        private static JsonObject toolInputSchemaToJson(Tools.ToolInputSchema s) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("properties", primitiveSchemaMapToJson(s.properties()));
            s.required().ifPresent(r -> b.add("required", listToJson(r).build()));
            return b.build();
        }

        private static Tools.ToolInputSchema toolInputSchemaFromJson(JsonObject obj) {
            Map<String, PrimitiveSchemaDefinition> props = primitiveSchemaMapFromJson(obj.getJsonObject("properties"));
            Optional<List<String>> required = obj.containsKey("required") ? Optional.of(listStringFromJson(obj.getJsonArray("required"))) : Optional.empty();
            return new Tools.ToolInputSchema(props, required);
        }

        private static JsonObject toolOutputSchemaToJson(Tools.ToolOutputSchema s) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("properties", primitiveSchemaMapToJson(s.properties()));
            s.required().ifPresent(r -> b.add("required", listToJson(r).build()));
            return b.build();
        }

        private static Tools.ToolOutputSchema toolOutputSchemaFromJson(JsonObject obj) {
            Map<String, PrimitiveSchemaDefinition> props = primitiveSchemaMapFromJson(obj.getJsonObject("properties"));
            Optional<List<String>> required = obj.containsKey("required") ? Optional.of(listStringFromJson(obj.getJsonArray("required"))) : Optional.empty();
            return new Tools.ToolOutputSchema(props, required);
        }

        private static JsonObject toolToJson(Tools.Tool t) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("name", t.name());
            b.add("inputSchema", toolInputSchemaToJson(t.inputSchema()));
            t.outputSchema().ifPresent(o -> b.add("outputSchema", toolOutputSchemaToJson(o)));
            t.description().ifPresent(d -> b.add("description", contentListToJson(d)));
            t.hints().ifPresent(h -> b.add("hints", mapToJson(h)));
            return b.build();
        }

        private static Tools.Tool toolFromJson(JsonObject obj) {
            String name = obj.getString("name");
            Tools.ToolInputSchema in = toolInputSchemaFromJson(obj.getJsonObject("inputSchema"));
            Optional<Tools.ToolOutputSchema> out = optional(obj, "outputSchema").map(v -> toolOutputSchemaFromJson(v.asJsonObject()));
            Optional<List<ContentBlock>> desc = obj.containsKey("description") ? Optional.of(contentListFromJson(obj.getJsonArray("description"))) : Optional.empty();
            Optional<Map<String, Object>> hints = optionalMap(obj, "hints");
            return new Tools.Tool(name, in, out, desc, hints);
        }

        private static JsonValue toolsToJson(List<Tools.Tool> list) {
            JsonArrayBuilder ab = Json.createArrayBuilder();
            list.forEach(t -> ab.add(toolToJson(t)));
            return ab.build();
        }

        private static List<Tools.Tool> toolsFromJson(jakarta.json.JsonArray arr) {
            List<Tools.Tool> list = new ArrayList<>();
            arr.forEach(v -> list.add(toolFromJson(v.asJsonObject())));
            return list;
        }

        private static JsonObject promptArgumentToJson(Prompts.PromptArgument a) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("name", a.name());
            a.description().ifPresent(d -> b.add("description", d));
            return b.build();
        }

        private static Prompts.PromptArgument promptArgumentFromJson(JsonObject obj) {
            String name = obj.getString("name");
            Optional<String> desc = optionalString(obj, "description");
            return new Prompts.PromptArgument(name, desc);
        }

        private static JsonObject promptToJson(Prompts.Prompt p) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("name", p.name());
            p.description().ifPresent(d -> b.add("description", d));
            JsonArrayBuilder args = Json.createArrayBuilder();
            p.arguments().forEach(a -> args.add(promptArgumentToJson(a)));
            b.add("arguments", args);
            return b.build();
        }

        private static Prompts.Prompt promptFromJson(JsonObject obj) {
            String name = obj.getString("name");
            Optional<String> desc = optionalString(obj, "description");
            List<Prompts.PromptArgument> args = new ArrayList<>();
            obj.getJsonArray("arguments").forEach(v -> args.add(promptArgumentFromJson(v.asJsonObject())));
            return new Prompts.Prompt(name, desc, args);
        }

        private static JsonValue promptsToJson(List<Prompts.Prompt> list) {
            JsonArrayBuilder ab = Json.createArrayBuilder();
            list.forEach(p -> ab.add(promptToJson(p)));
            return ab.build();
        }

        private static List<Prompts.Prompt> promptsFromJson(jakarta.json.JsonArray arr) {
            List<Prompts.Prompt> list = new ArrayList<>();
            arr.forEach(v -> list.add(promptFromJson(v.asJsonObject())));
            return list;
        }

        private static JsonValue messagesToJson(List<Sampling.SamplingMessage> list) {
            JsonArrayBuilder ab = Json.createArrayBuilder();
            list.forEach(m -> ab.add(messageToJson(m)));
            return ab.build();
        }

        private static List<Sampling.SamplingMessage> messagesFromJson(jakarta.json.JsonArray arr) {
            List<Sampling.SamplingMessage> list = new ArrayList<>();
            arr.forEach(v -> list.add(messageFromJson(v.asJsonObject())));
            return list;
        }

        private static JsonObject messageToJson(Sampling.SamplingMessage m) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("role", m.role().name());
            b.add("content", contentListToJson(m.content()));
            return b.build();
        }

        private static Sampling.SamplingMessage messageFromJson(JsonObject obj) {
            Sampling.Role role = Sampling.Role.valueOf(obj.getString("role"));
            List<ContentBlock> content = contentListFromJson(obj.getJsonArray("content"));
            return new Sampling.ContentMessage(role, content);
        }

        private static JsonObject modelHintToJson(Sampling.ModelHint hint) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            if (hint instanceof Sampling.NamedModel(String value)) {
                b.add("type", "named");
                b.add("value", value);
            } else if (hint instanceof Sampling.ProviderModel(String value)) {
                b.add("type", "provider");
                b.add("value", value);
            }
            return b.build();
        }

        private static Sampling.ModelHint modelHintFromJson(JsonObject obj) {
            String type = obj.getString("type");
            String value = obj.getString("value");
            return switch (type) {
                case "named" -> new Sampling.NamedModel(value);
                case "provider" -> new Sampling.ProviderModel(value);
                default -> throw new IllegalArgumentException("unknown model hint type: " + type);
            };
        }

        private static JsonObject modelPrefsToJson(Sampling.ModelPreferences p) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            p.hints().ifPresent(h -> {
                JsonArrayBuilder ab = Json.createArrayBuilder();
                h.forEach(m -> ab.add(modelHintToJson(m)));
                b.add("hints", ab);
            });
            p.temperature().ifPresent(t -> b.add("temperature", t));
            p.maxTokens().ifPresent(t -> b.add("maxTokens", t));
            p.costPriority().ifPresent(c -> b.add("costPriority", c));
            return b.build();
        }

        private static Sampling.ModelPreferences modelPrefsFromJson(JsonObject obj) {
            Optional<List<Sampling.ModelHint>> hints = obj.containsKey("hints") ? Optional.of(modelHintsFromJson(obj.getJsonArray("hints"))) : Optional.empty();
            Optional<Double> temperature = optionalNumber(obj, "temperature");
            Optional<Integer> maxTokens = obj.containsKey("maxTokens") ? Optional.of(obj.getInt("maxTokens")) : Optional.empty();
            Optional<Double> costPriority = optionalNumber(obj, "costPriority");
            return new Sampling.ModelPreferences(hints, temperature, maxTokens, costPriority);
        }

        private static List<Sampling.ModelHint> modelHintsFromJson(jakarta.json.JsonArray arr) {
            List<Sampling.ModelHint> list = new ArrayList<>();
            arr.forEach(v -> list.add(modelHintFromJson(v.asJsonObject())));
            return list;
        }

        private static JsonObject completionRefToJson(Completion.CompletionReference ref) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            if (ref instanceof Completion.PromptReference(String name)) {
                b.add("type", "prompt");
                b.add("name", name);
            } else if (ref instanceof Completion.ResourceTemplateReference(String name)) {
                b.add("type", "resourceTemplate");
                b.add("name", name);
            }
            return b.build();
        }

        private static Completion.CompletionReference completionRefFromJson(JsonObject obj) {
            String type = obj.getString("type");
            String name = obj.getString("name");
            return switch (type) {
                case "prompt" -> new Completion.PromptReference(name);
                case "resourceTemplate" -> new Completion.ResourceTemplateReference(name);
                default -> throw new IllegalArgumentException("unknown completion reference type: " + type);
            };
        }

        private static JsonObject completionArgToJson(Completion.CompletionArgument a) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("name", a.name());
            a.value().ifPresent(v -> b.add("value", v));
            return b.build();
        }

        private static Completion.CompletionArgument completionArgFromJson(JsonObject obj) {
            String name = obj.getString("name");
            Optional<String> value = optionalString(obj, "value");
            return new Completion.CompletionArgument(name, value);
        }

        private static JsonObject completionDataToJson(Completion.CompletionData c) {
            JsonArrayBuilder ab = Json.createArrayBuilder();
            c.suggestions().forEach(ab::add);
            return Json.createObjectBuilder().add("suggestions", ab).build();
        }

        private static Completion.CompletionData completionDataFromJson(JsonObject obj) {
            List<String> list = listStringFromJson(obj.getJsonArray("suggestions"));
            return new Completion.CompletionData(list);
        }

        private static JsonValue rootsToJson(List<Roots.Root> roots) {
            JsonArrayBuilder ab = Json.createArrayBuilder();
            roots.forEach(r -> ab.add(rootToJson(r)));
            return ab.build();
        }

        private static List<Roots.Root> rootsFromJson(jakarta.json.JsonArray arr) {
            List<Roots.Root> list = new ArrayList<>();
            arr.forEach(v -> list.add(rootFromJson(v.asJsonObject())));
            return list;
        }

        private static JsonObject rootToJson(Roots.Root r) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("uri", r.uri());
            r.name().ifPresent(n -> b.add("name", n));
            return b.build();
        }

        private static Roots.Root rootFromJson(JsonObject obj) {
            String uri = obj.getString("uri");
            Optional<String> name = optionalString(obj, "name");
            return new Roots.Root(uri, name);
        }

        private static JsonObject elicitationSchemaToJson(Elicitation.ElicitationSchema s) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            b.add("properties", primitiveSchemaMapToJson(s.properties()));
            s.required().ifPresent(r -> b.add("required", listToJson(r).build()));
            return b.build();
        }

        private static Elicitation.ElicitationSchema elicitationSchemaFromJson(JsonObject obj) {
            Map<String, PrimitiveSchemaDefinition> props = primitiveSchemaMapFromJson(obj.getJsonObject("properties"));
            Optional<List<String>> required = obj.containsKey("required") ? Optional.of(listStringFromJson(obj.getJsonArray("required"))) : Optional.empty();
            return new Elicitation.ElicitationSchema(props, required);
        }

        private static JsonObject primitiveSchemaMapToJson(Map<String, PrimitiveSchemaDefinition> map) {
            JsonObjectBuilder b = Json.createObjectBuilder();
            map.forEach((k, v) -> b.add(k, v.toJson()));
            return b.build();
        }

        private static Map<String, PrimitiveSchemaDefinition> primitiveSchemaMapFromJson(JsonObject obj) {
            Map<String, PrimitiveSchemaDefinition> map = new LinkedHashMap<>();
            obj.forEach((k, v) -> map.put(k, primitiveSchemaFromJson(v.asJsonObject())));
            return map;
        }

        private static PrimitiveSchemaDefinition primitiveSchemaFromJson(JsonObject obj) {
            String type = obj.getString("type", "");
            if (obj.containsKey("enum")) {
                List<String> values = listStringFromJson(obj.getJsonArray("enum"));
                Optional<List<String>> names = obj.containsKey("enumNames") ? Optional.of(listStringFromJson(obj.getJsonArray("enumNames"))) : Optional.empty();
                Optional<String> title = optionalString(obj, "title");
                Optional<String> desc = optionalString(obj, "description");
                return new EnumSchema(title, desc, values, names);
            }
            return switch (type) {
                case "string" -> {
                    Optional<String> title = optionalString(obj, "title");
                    Optional<String> desc = optionalString(obj, "description");
                    Optional<String> format = optionalString(obj, "format");
                    Optional<Integer> minLength = obj.containsKey("minLength") ? Optional.of(obj.getInt("minLength")) : Optional.empty();
                    Optional<Integer> maxLength = obj.containsKey("maxLength") ? Optional.of(obj.getInt("maxLength")) : Optional.empty();
                    yield new StringSchema(title, desc, format, minLength, maxLength);
                }
                case "number" -> {
                    Optional<String> title = optionalString(obj, "title");
                    Optional<String> desc = optionalString(obj, "description");
                    Optional<Double> minimum = optionalNumber(obj, "minimum");
                    Optional<Double> maximum = optionalNumber(obj, "maximum");
                    yield new NumberSchema(title, desc, minimum, maximum);
                }
                case "boolean" -> {
                    Optional<String> title = optionalString(obj, "title");
                    Optional<String> desc = optionalString(obj, "description");
                    yield new BooleanSchema(title, desc);
                }
                default -> throw new IllegalArgumentException("unknown schema type: " + type);
            };
        }

        private static List<String> listStringFromJson(jakarta.json.JsonArray arr) {
            List<String> list = new ArrayList<>();
            arr.forEach(v -> list.add(((JsonString) v).getString()));
            return list;
        }
    }
}
