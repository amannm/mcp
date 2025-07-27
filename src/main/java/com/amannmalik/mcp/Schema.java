package com.amannmalik.mcp;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Model Context Protocol (MCP) Schema v2025-06-18
 * Comprehensive type definitions for JSON-RPC communication between MCP clients and servers.
 * 
 * This schema defines the complete protocol including requests, responses, notifications,
 * and all content types supported by the MCP specification.
 */
public final class Schema {
    
    private Schema() { /* utility class */ }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // CORE PROTOCOL TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public static final String JSONRPC_VERSION = "2.0";
    public static final String LATEST_PROTOCOL_VERSION = "2025-06-18";
    
    public sealed interface RequestId permits RequestId.StringId, RequestId.IntegerId {
        record StringId(String value) implements RequestId {}
        record IntegerId(long value) implements RequestId {}
        
        static RequestId of(String value) { return new StringId(value); }
        static RequestId of(long value) { return new IntegerId(value); }
    }
    
    public sealed interface ProgressToken permits ProgressToken.StringToken, ProgressToken.IntegerToken {
        record StringToken(String value) implements ProgressToken {}
        record IntegerToken(long value) implements ProgressToken {}
        
        static ProgressToken of(String value) { return new StringToken(value); }
        static ProgressToken of(long value) { return new IntegerToken(value); }
    }
    
    public record Cursor(String value) {}
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // JSON-RPC MESSAGE TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public sealed interface JSONRPCMessage permits JSONRPCRequest, JSONRPCNotification, JSONRPCResponse, JSONRPCError {}
    
    public record JSONRPCRequest(
        String jsonrpc,
        RequestId id,
        String method,
        Optional<Map<String, Object>> params
    ) implements JSONRPCMessage {
        public JSONRPCRequest {
            assert JSONRPC_VERSION.equals(jsonrpc);
        }
    }
    
    public record JSONRPCNotification(
        String jsonrpc,
        String method,
        Optional<Map<String, Object>> params
    ) implements JSONRPCMessage {
        public JSONRPCNotification {
            assert JSONRPC_VERSION.equals(jsonrpc);
        }
    }
    
    public record JSONRPCResponse(
        String jsonrpc,
        RequestId id,
        Result result
    ) implements JSONRPCMessage {
        public JSONRPCResponse {
            assert JSONRPC_VERSION.equals(jsonrpc);
        }
    }
    
    public record JSONRPCError(
        String jsonrpc,
        RequestId id,
        ErrorInfo error
    ) implements JSONRPCMessage {
        public JSONRPCError {
            assert JSONRPC_VERSION.equals(jsonrpc);
        }
    }
    
    public record ErrorInfo(
        int code,
        String message,
        Optional<Object> data
    ) {}
    
    // Standard JSON-RPC error codes
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // BASE INTERFACES AND COMMON TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public record Result(Optional<Map<String, Object>> meta) {}
    
    public enum Role { USER, ASSISTANT }
    
    public enum LoggingLevel {
        DEBUG, INFO, NOTICE, WARNING, ERROR, CRITICAL, ALERT, EMERGENCY
    }
    
    public record BaseMetadata(
        String name,
        Optional<String> title
    ) {}
    
    public record Implementation(
        String name,
        Optional<String> title,
        String version
    ) {}
    
    public record Annotations(
        Optional<List<Role>> audience,
        Optional<Double> priority,
        Optional<Instant> lastModified
    ) {
        public Annotations {
            priority.ifPresent(p -> {
                if (p < 0.0 || p > 1.0) {
                    throw new IllegalArgumentException("Priority must be between 0.0 and 1.0");
                }
            });
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION PROTOCOL
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public record InitializeRequest(
        String protocolVersion,
        ClientCapabilities capabilities,
        Implementation clientInfo
    ) {}
    
    public record InitializeResult(
        String protocolVersion,
        ServerCapabilities capabilities,
        Implementation serverInfo,
        Optional<String> instructions
    ) extends Result {}
    
    public record ClientCapabilities(
        Optional<Map<String, Object>> experimental,
        Optional<RootsCapability> roots,
        Optional<Map<String, Object>> sampling,
        Optional<Map<String, Object>> elicitation
    ) {}
    
    public record RootsCapability(Optional<Boolean> listChanged) {}
    
    public record ServerCapabilities(
        Optional<Map<String, Object>> experimental,
        Optional<Map<String, Object>> logging,
        Optional<Map<String, Object>> completions,
        Optional<PromptsCapability> prompts,
        Optional<ResourcesCapability> resources,
        Optional<ToolsCapability> tools
    ) {}
    
    public record PromptsCapability(Optional<Boolean> listChanged) {}
    public record ResourcesCapability(Optional<Boolean> subscribe, Optional<Boolean> listChanged) {}
    public record ToolsCapability(Optional<Boolean> listChanged) {}
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // CONTENT TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public sealed interface ContentBlock permits TextContent, ImageContent, AudioContent, ResourceLink, EmbeddedResource {}
    
    public record TextContent(
        String text,
        Optional<Annotations> annotations,
        Optional<Map<String, Object>> meta
    ) implements ContentBlock {}
    
    public record ImageContent(
        String data,
        String mimeType,
        Optional<Annotations> annotations,
        Optional<Map<String, Object>> meta
    ) implements ContentBlock {}
    
    public record AudioContent(
        String data,
        String mimeType,
        Optional<Annotations> annotations,
        Optional<Map<String, Object>> meta
    ) implements ContentBlock {}
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // RESOURCE TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public sealed interface ResourceContents permits TextResourceContents, BlobResourceContents {}
    
    public record TextResourceContents(
        String uri,
        String text,
        Optional<String> mimeType,
        Optional<Map<String, Object>> meta
    ) implements ResourceContents {}
    
    public record BlobResourceContents(
        String uri,
        String blob,
        Optional<String> mimeType,
        Optional<Map<String, Object>> meta
    ) implements ResourceContents {}
    
    public record Resource(
        String name,
        Optional<String> title,
        String uri,
        Optional<String> description,
        Optional<String> mimeType,
        Optional<Annotations> annotations,
        Optional<Long> size,
        Optional<Map<String, Object>> meta
    ) {}
    
    public record ResourceTemplate(
        String name,
        Optional<String> title,
        String uriTemplate,
        Optional<String> description,
        Optional<String> mimeType,
        Optional<Annotations> annotations,
        Optional<Map<String, Object>> meta
    ) {}
    
    public record ResourceLink(
        String name,
        Optional<String> title,
        String uri,
        Optional<String> description,
        Optional<String> mimeType,
        Optional<Annotations> annotations,
        Optional<Long> size,
        Optional<Map<String, Object>> meta
    ) implements ContentBlock {}
    
    public record EmbeddedResource(
        ResourceContents resource,
        Optional<Annotations> annotations,
        Optional<Map<String, Object>> meta
    ) implements ContentBlock {}
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // TOOL TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public record Tool(
        String name,
        Optional<String> title,
        Optional<String> description,
        ToolInputSchema inputSchema,
        Optional<ToolOutputSchema> outputSchema,
        Optional<ToolAnnotations> annotations,
        Optional<Map<String, Object>> meta
    ) {}
    
    public record ToolInputSchema(
        Map<String, Object> properties,
        Optional<List<String>> required
    ) {}
    
    public record ToolOutputSchema(
        Map<String, Object> properties,
        Optional<List<String>> required
    ) {}
    
    public record ToolAnnotations(
        Optional<String> title,
        Optional<Boolean> readOnlyHint,
        Optional<Boolean> destructiveHint,
        Optional<Boolean> idempotentHint,
        Optional<Boolean> openWorldHint
    ) {}
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // PROMPT TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public record Prompt(
        String name,
        Optional<String> title,
        Optional<String> description,
        Optional<List<PromptArgument>> arguments,
        Optional<Map<String, Object>> meta
    ) {}
    
    public record PromptArgument(
        String name,
        Optional<String> title,
        Optional<String> description,
        Optional<Boolean> required
    ) {}
    
    public record PromptMessage(
        Role role,
        ContentBlock content
    ) {}
    
    public sealed interface PromptReference permits PromptReference.Basic {
        record Basic(String name, Optional<String> title) implements PromptReference {}
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // SAMPLING TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public record SamplingMessage(
        Role role,
        SamplingContent content
    ) {}
    
    public sealed interface SamplingContent permits TextContent, ImageContent, AudioContent {}
    
    public record ModelPreferences(
        Optional<List<ModelHint>> hints,
        Optional<Double> costPriority,
        Optional<Double> speedPriority,
        Optional<Double> intelligencePriority
    ) {
        public ModelPreferences {
            costPriority.ifPresent(this::validatePriority);
            speedPriority.ifPresent(this::validatePriority);
            intelligencePriority.ifPresent(this::validatePriority);
        }
        
        private void validatePriority(Double priority) {
            if (priority < 0.0 || priority > 1.0) {
                throw new IllegalArgumentException("Priority must be between 0.0 and 1.0");
            }
        }
    }
    
    public record ModelHint(Optional<String> name) {}
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // SCHEMA TYPES (for elicitation)
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public sealed interface PrimitiveSchemaDefinition permits StringSchema, NumberSchema, BooleanSchema, EnumSchema {}
    
    public record StringSchema(
        Optional<String> title,
        Optional<String> description,
        Optional<Integer> minLength,
        Optional<Integer> maxLength,
        Optional<StringFormat> format
    ) implements PrimitiveSchemaDefinition {}
    
    public enum StringFormat { EMAIL, URI, DATE, DATE_TIME }
    
    public record NumberSchema(
        NumberType type,
        Optional<String> title,
        Optional<String> description,
        Optional<Double> minimum,
        Optional<Double> maximum
    ) implements PrimitiveSchemaDefinition {}
    
    public enum NumberType { NUMBER, INTEGER }
    
    public record BooleanSchema(
        Optional<String> title,
        Optional<String> description,
        Optional<Boolean> defaultValue
    ) implements PrimitiveSchemaDefinition {}
    
    public record EnumSchema(
        Optional<String> title,
        Optional<String> description,
        List<String> enumValues,
        Optional<List<String>> enumNames
    ) implements PrimitiveSchemaDefinition {}
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // ROOT TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public record Root(
        String uri,
        Optional<String> name,
        Optional<Map<String, Object>> meta
    ) {}
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // REQUEST TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public sealed interface ClientRequest permits
        InitializeRequest, PingRequest, ListResourcesRequest, ListResourceTemplatesRequest,
        ReadResourceRequest, SubscribeRequest, UnsubscribeRequest, ListPromptsRequest,
        GetPromptRequest, ListToolsRequest, CallToolRequest, SetLevelRequest, CompleteRequest {}
    
    public sealed interface ServerRequest permits
        PingRequest, CreateMessageRequest, ListRootsRequest, ElicitRequest {}
    
    public record PingRequest() implements ClientRequest, ServerRequest {}
    
    public record ListResourcesRequest(Optional<Cursor> cursor) implements ClientRequest {}
    
    public record ListResourceTemplatesRequest(Optional<Cursor> cursor) implements ClientRequest {}
    
    public record ReadResourceRequest(String uri) implements ClientRequest {}
    
    public record SubscribeRequest(String uri) implements ClientRequest {}
    
    public record UnsubscribeRequest(String uri) implements ClientRequest {}
    
    public record ListPromptsRequest(Optional<Cursor> cursor) implements ClientRequest {}
    
    public record GetPromptRequest(
        String name,
        Optional<Map<String, String>> arguments
    ) implements ClientRequest {}
    
    public record ListToolsRequest(Optional<Cursor> cursor) implements ClientRequest {}
    
    public record CallToolRequest(
        String name,
        Optional<Map<String, Object>> arguments
    ) implements ClientRequest {}
    
    public record SetLevelRequest(LoggingLevel level) implements ClientRequest {}
    
    public record CompleteRequest(
        CompletionReference ref,
        CompletionArgument argument,
        Optional<CompletionContext> context
    ) implements ClientRequest {}
    
    public record CompletionArgument(String name, String value) {}
    
    public record CompletionContext(Optional<Map<String, String>> arguments) {}
    
    public sealed interface CompletionReference permits PromptReference, ResourceTemplateReference {}
    
    public record ResourceTemplateReference(String uri) implements CompletionReference {}
    
    public record CreateMessageRequest(
        List<SamplingMessage> messages,
        long maxTokens,
        Optional<ModelPreferences> modelPreferences,
        Optional<String> systemPrompt,
        Optional<IncludeContext> includeContext,
        Optional<Double> temperature,
        Optional<List<String>> stopSequences,
        Optional<Map<String, Object>> metadata
    ) implements ServerRequest {}
    
    public enum IncludeContext { NONE, THIS_SERVER, ALL_SERVERS }
    
    public record ListRootsRequest() implements ServerRequest {}
    
    public record ElicitRequest(
        String message,
        ElicitationSchema requestedSchema
    ) implements ServerRequest {}
    
    public record ElicitationSchema(
        Map<String, PrimitiveSchemaDefinition> properties,
        Optional<List<String>> required
    ) {}
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // RESULT TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public sealed interface ClientResult permits
        Result, CreateMessageResult, ListRootsResult, ElicitResult {}
    
    public sealed interface ServerResult permits
        Result, InitializeResult, ListResourcesResult, ListResourceTemplatesResult,
        ReadResourceResult, ListPromptsResult, GetPromptResult, ListToolsResult,
        CallToolResult, CompleteResult {}
    
    public record ListResourcesResult(
        List<Resource> resources,
        Optional<Cursor> nextCursor,
        Optional<Map<String, Object>> meta
    ) extends Result implements ServerResult {}
    
    public record ListResourceTemplatesResult(
        List<ResourceTemplate> resourceTemplates,
        Optional<Cursor> nextCursor,
        Optional<Map<String, Object>> meta
    ) extends Result implements ServerResult {}
    
    public record ReadResourceResult(
        List<ResourceContents> contents,
        Optional<Map<String, Object>> meta
    ) extends Result implements ServerResult {}
    
    public record ListPromptsResult(
        List<Prompt> prompts,
        Optional<Cursor> nextCursor,
        Optional<Map<String, Object>> meta
    ) extends Result implements ServerResult {}
    
    public record GetPromptResult(
        List<PromptMessage> messages,
        Optional<String> description,
        Optional<Map<String, Object>> meta
    ) extends Result implements ServerResult {}
    
    public record ListToolsResult(
        List<Tool> tools,
        Optional<Cursor> nextCursor,
        Optional<Map<String, Object>> meta
    ) extends Result implements ServerResult {}
    
    public record CallToolResult(
        List<ContentBlock> content,
        Optional<Map<String, Object>> structuredContent,
        Optional<Boolean> isError,
        Optional<Map<String, Object>> meta
    ) extends Result implements ServerResult {}
    
    public record CompleteResult(
        CompletionInfo completion,
        Optional<Map<String, Object>> meta
    ) extends Result implements ServerResult {}
    
    public record CompletionInfo(
        List<String> values,
        Optional<Integer> total,
        Optional<Boolean> hasMore
    ) {
        public CompletionInfo {
            if (values.size() > 100) {
                throw new IllegalArgumentException("Completion values must not exceed 100 items");
            }
        }
    }
    
    public record CreateMessageResult(
        Role role,
        SamplingContent content,
        String model,
        Optional<String> stopReason,
        Optional<Map<String, Object>> meta
    ) extends Result implements ClientResult {}
    
    public record ListRootsResult(
        List<Root> roots,
        Optional<Map<String, Object>> meta
    ) extends Result implements ClientResult {}
    
    public record ElicitResult(
        ElicitAction action,
        Optional<Map<String, Object>> content,
        Optional<Map<String, Object>> meta
    ) extends Result implements ClientResult {}
    
    public enum ElicitAction { ACCEPT, DECLINE, CANCEL }
    
    // ═══════════════════════════════════════════════════════════════════════════════════════
    // NOTIFICATION TYPES
    // ═══════════════════════════════════════════════════════════════════════════════════════
    
    public sealed interface ClientNotification permits
        CancelledNotification, InitializedNotification, ProgressNotification, RootsListChangedNotification {}
    
    public sealed interface ServerNotification permits
        CancelledNotification, ProgressNotification, ResourceListChangedNotification,
        ResourceUpdatedNotification, PromptListChangedNotification, ToolListChangedNotification,
        LoggingMessageNotification {}
    
    public record CancelledNotification(
        RequestId requestId,
        Optional<String> reason
    ) implements ClientNotification, ServerNotification {}
    
    public record InitializedNotification() implements ClientNotification {}
    
    public record ProgressNotification(
        ProgressToken progressToken,
        double progress,
        Optional<Double> total,
        Optional<String> message
    ) implements ClientNotification, ServerNotification {}
    
    public record RootsListChangedNotification() implements ClientNotification {}
    
    public record ResourceListChangedNotification() implements ServerNotification {}
    
    public record ResourceUpdatedNotification(String uri) implements ServerNotification {}
    
    public record PromptListChangedNotification() implements ServerNotification {}
    
    public record ToolListChangedNotification() implements ServerNotification {}
    
    public record LoggingMessageNotification(
        LoggingLevel level,
        Object data,
        Optional<String> logger
    ) implements ServerNotification {}
}