# MCP Java Schema Implementation Plan

## Overview
Establish complete MCP (Model Context Protocol) 2025-06-18 schema in Java with high-density, type-safe, modern implementation. Focus on sealed types, composition, and stateless design following latest Java 24 practices.

## Architecture Principles
- **Sealed interfaces** for protocol message hierarchies 
- **Records** for immutable data carriers
- **Composition** over inheritance patterns
- **Optional<T>** boundaries to eliminate nulls
- **Static methods** for transformations/validation
- **Flattened package structure** (`com.amannmalik.mcp.schema`)
- **Jakarta JSON-P** for serialization (Eclipse Parsson)

---

## Phase 1: Core Protocol Foundation
*Parallelizable: 4 agents can work simultaneously on each component*

### 1A: JSON-RPC Base Types
**File: `JsonRpcTypes.java`**
```java
// Sealed hierarchy for all JSON-RPC messages
public sealed interface JsonRpcMessage 
    permits JsonRpcRequest, JsonRpcNotification, JsonRpcResponse, JsonRpcError;

// RequestId as sealed type (string | number)
public sealed interface RequestId permits StringRequestId, NumberRequestId;
```
**Dependencies**: None  
**Agent Focus**: Core JSON-RPC 2.0 message structure, request/response/notification/error types

### 1B: Base Protocol Interfaces  
**File: `BaseProtocol.java`**
```java
// Request/Result/Notification base interfaces with _meta support
public sealed interface Request permits /* all request types */;
public sealed interface Result permits /* all result types */;
public sealed interface Notification permits /* all notification types */;
```
**Dependencies**: JsonRpcTypes  
**Agent Focus**: Base interfaces, metadata handling, progress token support

### 1C: Primitive Schema Support
**File: `SchemaTypes.java`**
```java
// Sealed schema definition hierarchy for primitive types
public sealed interface PrimitiveSchemaDefinition 
    permits StringSchema, NumberSchema, BooleanSchema, EnumSchema;
```
**Dependencies**: None  
**Agent Focus**: JSON Schema primitive type definitions for tool inputs/elicitation

### 1D: Content Block System
**File: `ContentBlocks.java`**
```java
// Sealed content type hierarchy
public sealed interface ContentBlock 
    permits TextContent, ImageContent, AudioContent, ResourceLink, EmbeddedResource;
```
**Dependencies**: None  
**Agent Focus**: Text/image/audio content, resource embedding, annotations

---

## Phase 2: Protocol Capabilities & Lifecycle  
*Parallelizable: 3 agents working on capabilities, initialization, and ping/progress*

### 2A: Capabilities & Implementation Info
**File: `Capabilities.java`**
```java
// Client/Server capability records
public record ClientCapabilities(Optional<RootsCapability> roots, 
                                Optional<SamplingCapability> sampling, ...) {}
public record ServerCapabilities(Optional<PromptsCapability> prompts,
                                Optional<ResourcesCapability> resources, ...) {}
```
**Dependencies**: BaseProtocol  
**Agent Focus**: Capability negotiation, experimental capability handling

### 2B: Initialization Protocol
**File: `Initialization.java`**
```java
// Initialize request/response/notification
public record InitializeRequest(...) implements Request;
public record InitializeResult(...) implements Result; 
public record InitializedNotification() implements Notification;
```
**Dependencies**: Capabilities, BaseProtocol  
**Agent Focus**: Connection establishment, protocol version negotiation

### 2C: Base Operations (Ping, Progress, Cancellation)
**File: `BaseOperations.java`** 
```java
// Ping, progress tracking, cancellation support
public record PingRequest() implements Request;
public record ProgressNotification(...) implements Notification;
public record CancelledNotification(...) implements Notification;
```
**Dependencies**: BaseProtocol  
**Agent Focus**: Connection health, long-running operation support

---

## Phase 3: Core Feature Domains
*Parallelizable: 4 agents on Resources, Tools, Prompts, Sampling*

### 3A: Resources System
**File: `Resources.java`**
```java
// Resource operations: list, read, subscribe, templates
public record ListResourcesRequest(Optional<Cursor> cursor) implements Request;
public record Resource(String name, String uri, Optional<String> description, ...) {}
public sealed interface ResourceContents permits TextResourceContents, BlobResourceContents;
```
**Dependencies**: BaseProtocol, ContentBlocks  
**Agent Focus**: Resource discovery, URI templates, subscription model, MIME types

### 3B: Tools System  
**File: `Tools.java`**
```java
// Tool operations: list, call with input/output schemas
public record ListToolsRequest(Optional<Cursor> cursor) implements Request;
public record Tool(String name, ToolInputSchema inputSchema, 
                  Optional<ToolOutputSchema> outputSchema, ...) {}
public record CallToolRequest(String name, Map<String, Object> arguments) implements Request;
```
**Dependencies**: BaseProtocol, SchemaTypes, ContentBlocks  
**Agent Focus**: Tool discovery, JSON Schema validation, structured I/O, annotations/hints

### 3C: Prompts System
**File: `Prompts.java`**
```java
// Prompt operations: list, get with templating
public record ListPromptsRequest(Optional<Cursor> cursor) implements Request;
public record Prompt(String name, Optional<String> description, 
                    List<PromptArgument> arguments) {}
public record GetPromptRequest(String name, Map<String, String> arguments) implements Request;
```
**Dependencies**: BaseProtocol, ContentBlocks  
**Agent Focus**: Template prompts, argument handling, message construction

### 3D: Sampling System
**File: `Sampling.java`**
```java
// LLM sampling requests from server to client
public record CreateMessageRequest(List<SamplingMessage> messages, 
                                  Optional<ModelPreferences> modelPreferences, ...) implements Request;
public record ModelPreferences(Optional<List<ModelHint>> hints, 
                              Optional<Double> costPriority, ...) {}
```
**Dependencies**: BaseProtocol, ContentBlocks  
**Agent Focus**: LLM interaction, model selection, temperature/tokens/stops

---

## Phase 4: Extended Features
*Parallelizable: 4 agents on Roots, Logging, Completion, Elicitation*

### 4A: Roots System
**File: `Roots.java`**
```java
// Filesystem boundary definitions
public record ListRootsRequest() implements Request;
public record Root(String uri, Optional<String> name) {}
public record RootsListChangedNotification() implements Notification;
```
**Dependencies**: BaseProtocol  
**Agent Focus**: File system access boundaries, URI validation

### 4B: Logging System
**File: `Logging.java`**
```java
// Structured logging with RFC-5424 severity levels  
public enum LoggingLevel { DEBUG, INFO, NOTICE, WARNING, ERROR, CRITICAL, ALERT, EMERGENCY }
public record SetLevelRequest(LoggingLevel level) implements Request;
public record LoggingMessageNotification(LoggingLevel level, Object data, ...) implements Notification;
```
**Dependencies**: BaseProtocol  
**Agent Focus**: Log level management, structured message passing

### 4C: Completion System
**File: `Completion.java`**
```java
// Argument autocompletion for prompts/resources
public record CompleteRequest(CompletionReference ref, CompletionArgument argument, ...) implements Request;
public sealed interface CompletionReference permits PromptReference, ResourceTemplateReference;
public record CompleteResult(CompletionData completion) implements Result;
```
**Dependencies**: BaseProtocol  
**Agent Focus**: Autocompletion logic, reference resolution, value suggestions

### 4D: Elicitation System
**File: `Elicitation.java`**
```java
// Server-initiated user input requests
public record ElicitRequest(String message, ElicitationSchema requestedSchema) implements Request;
public record ElicitationSchema(Map<String, PrimitiveSchemaDefinition> properties, 
                               Optional<List<String>> required) {}
public record ElicitResult(ElicitAction action, Optional<Map<String, Object>> content) implements Result;
```
**Dependencies**: BaseProtocol, SchemaTypes  
**Agent Focus**: Form generation, schema validation, user interaction patterns

---

## Phase 5: JSON Serialization Layer
*Parallelizable: 3 agents on Serialization, Validation, Protocol Utils*

### 5A: JSON Codec System
**File: `JsonCodec.java`**
```java
// Jakarta JSON-P based serialization
public static final class McpJsonCodec {
    public static JsonObject toJson(JsonRpcMessage message) { ... }
    public static <T extends JsonRpcMessage> T fromJson(JsonObject json, Class<T> messageType) { ... }
    
    // Type-safe serialization methods
    public static JsonObject requestToJson(Request request) { ... }
    public static JsonObject resultToJson(Result result) { ... }
    public static JsonObject notificationToJson(Notification notification) { ... }
}
```
**Dependencies**: All schema types  
**Agent Focus**: Bi-directional JSON transformation, type safety, error handling

### 5B: Schema Validation
**File: `SchemaValidator.java`**
```java
// JSON Schema validation for tool inputs/outputs and elicitation
public static final class McpSchemaValidator {
    public static ValidationResult validateToolInput(Tool tool, Map<String, Object> arguments) { ... }
    public static ValidationResult validateElicitationData(ElicitationSchema schema, Map<String, Object> data) { ... }
}
```
**Dependencies**: SchemaTypes, Tools, Elicitation  
**Agent Focus**: Runtime validation, clear error messages, type coercion

### 5C: Protocol Utilities  
**File: `ProtocolUtils.java`**
```java
// Protocol constants, helpers, factory methods
public static final class McpProtocol {
    public static final String PROTOCOL_VERSION = "2025-06-18";
    public static final String JSONRPC_VERSION = "2.0";
    
    // Factory methods for common message construction
    public static JsonRpcError createError(RequestId id, int code, String message) { ... }
    public static InitializeRequest createInitializeRequest(ClientCapabilities capabilities, ...) { ... }
}
```
**Dependencies**: All types  
**Agent Focus**: Constants, factories, common operations, version management

---

## Phase 6: Integration & Testing Infrastructure
*Parallelizable: 2 agents on Testing and Integration*

### 6A: Comprehensive Test Suite
**File: `src/test/java/com/amannmalik/mcp/schema/`**
```java
// JSON round-trip tests for all message types
// Schema validation tests  
// Protocol compliance verification
// Performance benchmarks with JMH
```
**Dependencies**: All implementation phases  
**Agent Focus**: Message serialization correctness, spec compliance, performance validation

### 6B: Integration Layer
**File: `McpSchemaRegistry.java`**
```java
// Central registry for schema operations
public static final class McpSchemaRegistry {
    public static void registerMessageHandler(String method, MessageHandler handler) { ... }
    public static Optional<Result> handleRequest(Request request) { ... }
    public static void processNotification(Notification notification) { ... }
}
```
**Dependencies**: All schema types, JsonCodec  
**Agent Focus**: Message dispatch, handler registration, protocol state management

---

## Implementation Guidelines

### Code Density Optimization
- **Sealed interfaces** with exhaustive pattern matching
- **Record types** with validation in compact constructors
- **Static factory methods** for complex object construction
- **Method references** (`::`) for transformation pipelines

### Type Safety Requirements
- **No raw types** or unchecked casts
- **Optional<T>** for all nullable protocol fields  
- **Sealed hierarchies** prevent unexpected subtypes
- **Immutable data structures** throughout

### JSON Integration Patterns
```java
// Preferred serialization pattern
public static JsonObject toJson(SomeRecord record) {
    return Json.createObjectBuilder()
        .add("field1", record.field1())
        .add("field2", record.field2().orElse(JsonValue.NULL))
        .build();
}

// Preferred deserialization pattern  
public static SomeRecord fromJson(JsonObject json) {
    return new SomeRecord(
        json.getString("field1"),
        Optional.ofNullable(json.getString("field2", null))
    );
}
```

### Performance Considerations
- **Lazy evaluation** for expensive operations
- **Cached JSON parsers** for repeated schema validation
- **Minimal object allocation** in hot paths
- **JMH benchmarks** for critical serialization paths

---

## Delivery Milestones

### Milestone 1 (Phases 1-2): Protocol Foundation
**Deliverable**: Core JSON-RPC infrastructure, initialization, capabilities
**Validation**: Can establish MCP connections and negotiate capabilities

### Milestone 2 (Phase 3): Core Features  
**Deliverable**: Resources, Tools, Prompts, Sampling systems
**Validation**: Full MCP feature set operational with JSON round-trips

### Milestone 3 (Phases 4-5): Complete Protocol
**Deliverable**: Extended features + robust JSON serialization
**Validation**: Spec-compliant MCP implementation with comprehensive validation

### Milestone 4 (Phase 6): Production Ready
**Deliverable**: Full test coverage, integration layer, performance optimization
**Validation**: Production-grade MCP schema library ready for integration

---

## Parallelization Strategy

**4-Agent Teams Per Phase**: Each agent owns 1-2 related files, coordinating through shared interfaces
**Cross-Phase Dependencies**: Later phases can begin interface definition while earlier phases complete implementation
**Integration Points**: JsonCodec and ProtocolUtils require coordination but can be developed iteratively
**Testing Strategy**: Each agent writes tests for their components, final agent handles integration testing

**Estimated Timeline**: 4-6 weeks with 4 parallel agents, 2-3 weeks with 8+ agents