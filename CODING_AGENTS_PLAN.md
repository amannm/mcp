# MCP Java Implementation Plan
## Dense, Phased-Out, Parallelizable Schema & Serialization Implementation

### Project Overview
Implement the [Model Context Protocol (MCP) 2025-06-18 specification](spec/2025-06-18/index.mdx) in Java 24, establishing comprehensive schema definitions and JSON serialization/deserialization using Jakarta JSON-P (Eclipse Parsson). Target: production-ready, type-safe, GraalVM-native compatible implementation.

---

## Phase 1: Foundation & Core Infrastructure (2-3 agents, ~2-3 days)

### 1.1 JSON-RPC Base Protocol [Agent A]
**Package**: `com.amannmalik.mcp.protocol.jsonrpc`
- [ ] `JsonRpcMessage` - sealed interface for all messages
- [ ] `JsonRpcRequest` - request with id, method, params  
- [ ] `JsonRpcResponse` - success response with id, result
- [ ] `JsonRpcNotification` - notification without id
- [ ] `JsonRpcError` - error response with code, message, data
- [ ] `RequestId` - string/integer union type using sealed interface
- [ ] `ProgressToken` - string/integer union type
- [ ] Base serialization utilities using Jakarta JSON-P

### 1.2 Core Value Objects & Enums [Agent B] 
**Package**: `com.amannmalik.mcp.schema.core`
- [ ] `Role` - enum (user, assistant)
- [ ] `LoggingLevel` - enum (emergency, alert, critical, error, warning, notice, info, debug)
- [ ] `Cursor` - opaque pagination token (String wrapper)
- [ ] `Implementation` - name, version, optional title record
- [ ] `BaseMetadata` - name, optional title interface/record
- [ ] Annotation types and meta field structures
- [ ] Common field validation utilities

### 1.3 JSON Serialization Framework [Agent C]
**Package**: `com.amannmalik.mcp.json`
- [ ] `McpJsonReader` - Jakarta JSON-P reader with MCP-specific logic
- [ ] `McpJsonWriter` - Jakarta JSON-P writer with MCP-specific logic  
- [ ] `JsonCodec<T>` - interface for type-safe serialization
- [ ] `JsonRegistry` - codec registry and factory
- [ ] Union type handling (discriminated unions using "type" field)
- [ ] Optional field handling patterns
- [ ] Base64 encoding/decoding utilities for binary data

---

## Phase 2: Content & Schema System (2-3 agents, ~3-4 days)

### 2.1 Content Block Types [Agent A]
**Package**: `com.amannmalik.mcp.schema.content`
- [ ] `ContentBlock` - sealed interface for all content types
- [ ] `TextContent` - text, type="text", annotations, _meta
- [ ] `ImageContent` - data (base64), mimeType, type="image", annotations, _meta  
- [ ] `AudioContent` - data (base64), mimeType, type="audio", annotations, _meta
- [ ] `ResourceLink` - uri, name, title, description, mimeType, size, type="resource_link"
- [ ] `EmbeddedResource` - resource contents, type="resource"
- [ ] `Annotations` - audience, priority, lastModified record
- [ ] Content validation and MIME type handling

### 2.2 Schema Definition System [Agent B]
**Package**: `com.amannmalik.mcp.schema.definitions`
- [ ] `PrimitiveSchemaDefinition` - sealed interface for primitive schemas
- [ ] `StringSchema` - string type with format, minLength, maxLength, description, title
- [ ] `NumberSchema` - number/integer type with min/max, description, title
- [ ] `BooleanSchema` - boolean type with default, description, title  
- [ ] `EnumSchema` - string enum with values, enumNames, description, title
- [ ] JSON Schema object definitions for complex types
- [ ] Schema validation logic using Jakarta JSON-P
- [ ] Dynamic schema parsing and validation

### 2.3 Resource System [Agent C]
**Package**: `com.amannmalik.mcp.schema.resources`
- [ ] `Resource` - name, uri, title, description, mimeType, size, annotations, _meta
- [ ] `ResourceTemplate` - name, uriTemplate, title, description, mimeType, annotations, _meta
- [ ] `ResourceContents` - sealed interface for resource content types
- [ ] `TextResourceContents` - uri, text, mimeType, _meta
- [ ] `BlobResourceContents` - uri, blob (base64), mimeType, _meta  
- [ ] `ResourceReference` - type="ref/resource", uri
- [ ] URI template processing and validation
- [ ] Resource content type detection and validation

---

## Phase 3: Message Types & Protocol Operations (3-4 agents, ~4-5 days)

### 3.1 Initialization & Lifecycle [Agent A]
**Package**: `com.amannmalik.mcp.protocol.lifecycle`
- [ ] `InitializeRequest` - method="initialize", clientInfo, capabilities, protocolVersion
- [ ] `InitializeResult` - serverInfo, capabilities, protocolVersion, instructions
- [ ] `InitializedNotification` - method="notifications/initialized"
- [ ] `ClientCapabilities` - roots, sampling, elicitation, experimental capabilities
- [ ] `ServerCapabilities` - resources, prompts, tools, logging, completions, experimental
- [ ] `PingRequest` - method="ping" for connection health
- [ ] Capability negotiation logic and validation

### 3.2 Server Feature Messages [Agent B]
**Package**: `com.amannmalik.mcp.protocol.server`
- [ ] **Resources**: `ListResourcesRequest/Result`, `ReadResourceRequest/Result`, `SubscribeRequest`, `UnsubscribeRequest`
- [ ] **Resource Templates**: `ListResourceTemplatesRequest/Result`  
- [ ] **Prompts**: `ListPromptsRequest/Result`, `GetPromptRequest/Result`
- [ ] **Tools**: `ListToolsRequest/Result`, `CallToolRequest/Result`
- [ ] **Logging**: `SetLevelRequest`, `LoggingMessageNotification`
- [ ] **Completion**: `CompleteRequest/Result` for argument autocompletion
- [ ] Pagination support with cursor-based iteration
- [ ] Tool input/output schema handling

### 3.3 Client Feature Messages [Agent C]
**Package**: `com.amannmalik.mcp.protocol.client`
- [ ] **Sampling**: `CreateMessageRequest/Result` for LLM sampling
- [ ] **Roots**: `ListRootsRequest/Result` for filesystem boundaries
- [ ] **Elicitation**: `ElicitRequest/Result` for user input collection
- [ ] `SamplingMessage` - role, content for LLM interactions
- [ ] `PromptMessage` - role, content for prompt templates  
- [ ] `ModelPreferences` - costPriority, speedPriority, intelligencePriority, hints
- [ ] `ModelHint` - name for model selection hints
- [ ] `Root` - uri, name for filesystem roots

### 3.4 Notification & Progress System [Agent D]
**Package**: `com.amannmalik.mcp.protocol.notifications`
- [ ] `CancelledNotification` - requestId, reason for request cancellation
- [ ] `ProgressNotification` - progressToken, progress, total, message
- [ ] `ResourceListChangedNotification`, `ResourceUpdatedNotification`
- [ ] `PromptListChangedNotification`, `ToolListChangedNotification`
- [ ] `RootsListChangedNotification` from client to server
- [ ] Progress tracking utilities and token management
- [ ] Subscription management for resource updates

---

## Phase 4: Domain Objects & Advanced Features (2-3 agents, ~3-4 days)

### 4.1 Prompt & Tool Definitions [Agent A]
**Package**: `com.amannmalik.mcp.schema.definitions`
- [ ] `Prompt` - name, title, description, arguments array, _meta
- [ ] `PromptArgument` - name, title, description, required boolean
- [ ] `PromptReference` - type="ref/prompt", name, title
- [ ] `Tool` - name, title, description, inputSchema, outputSchema, annotations, _meta
- [ ] `ToolAnnotations` - title, readOnlyHint, idempotentHint, destructiveHint, openWorldHint
- [ ] Complex JSON Schema handling for tool parameters
- [ ] Tool invocation argument validation and processing

### 4.2 Advanced Content & Messaging [Agent B]
**Package**: `com.amannmalik.mcp.schema.advanced`
- [ ] `ElicitRequest/Result` - message, requestedSchema, action, content
- [ ] `CreateMessageRequest/Result` - messages, maxTokens, temperature, stopSequences, systemPrompt
- [ ] `CompleteRequest/Result` - ref (prompt/resource template), argument, context, completion values
- [ ] `CallToolResult` - content, isError, structuredContent for tool execution results
- [ ] Advanced completion context handling with previous arguments
- [ ] Form-based elicitation schema processing

### 4.3 Pagination & Collection Utilities [Agent C]
**Package**: `com.amannmalik.mcp.protocol.pagination`
- [ ] `PaginatedRequest` - cursor parameter interface
- [ ] `PaginatedResult` - nextCursor interface  
- [ ] `ListResult<T>` - generic paginated collection wrapper
- [ ] Cursor encoding/decoding utilities
- [ ] Collection merging and result aggregation
- [ ] Streaming pagination support for large datasets

---

## Phase 5: Integration & Message Routing (2-3 agents, ~3-4 days)

### 5.1 Message Routing & Dispatch [Agent A]
**Package**: `com.amannmalik.mcp.protocol.dispatch`
- [ ] `MessageRouter` - routes incoming JSON-RPC to appropriate handlers
- [ ] `RequestHandler<T, R>` - typed request handler interface
- [ ] `NotificationHandler<T>` - typed notification handler interface  
- [ ] `MessageDispatcher` - orchestrates request/response lifecycle
- [ ] Method name -> handler registration system
- [ ] Error handling and JSON-RPC error response generation
- [ ] Request ID tracking and response correlation

### 5.2 Protocol State Management [Agent B]
**Package**: `com.amannmalik.mcp.protocol.state`
- [ ] `ProtocolState` - tracks initialization, capabilities, subscriptions
- [ ] `CapabilityNegotiation` - client/server capability matching
- [ ] `SubscriptionManager` - resource subscription tracking
- [ ] `RequestTracker` - outstanding request management  
- [ ] `ProgressTracker` - progress notification correlation
- [ ] Connection lifecycle state machine
- [ ] Graceful shutdown and cleanup handling

### 5.3 Validation & Error Handling [Agent C]
**Package**: `com.amannmalik.mcp.validation`
- [ ] `MessageValidator` - validates incoming/outgoing messages
- [ ] `SchemaValidator` - JSON Schema validation using Jakarta JSON
- [ ] `ProtocolValidator` - protocol-level constraint validation
- [ ] `ErrorMapper` - maps Java exceptions to JSON-RPC errors
- [ ] Custom validation annotations and processors
- [ ] Detailed error reporting with field-level specificity
- [ ] Security validation for untrusted input

---

## Phase 6: Transport & Integration Layer (1-2 agents, ~2-3 days)

### 6.1 Transport Abstraction [Agent A]
**Package**: `com.amannmalik.mcp.transport`
- [ ] `Transport` - abstract transport interface for message sending/receiving
- [ ] `MessageCodec` - JSON message encoding/decoding
- [ ] `TransportHandler` - connection lifecycle management
- [ ] `ConnectionManager` - maintains transport connections
- [ ] Backpressure and flow control handling
- [ ] Connection retry and reconnection logic
- [ ] Transport-agnostic message queuing

### 6.2 HTTP Transport Implementation [Agent B]
**Package**: `com.amannmalik.mcp.transport.http`
- [ ] `HttpTransport` - HTTP-based transport using Jetty client
- [ ] `SseTransport` - Server-Sent Events for server->client notifications
- [ ] `WebSocketTransport` - WebSocket bidirectional transport
- [ ] HTTP authentication and authorization handling
- [ ] Connection pooling and keep-alive management
- [ ] CORS and security header handling
- [ ] Request/response correlation in HTTP context

---

## Phase 7: Testing & Integration (2-3 agents, ~2-3 days)

### 7.1 Unit Testing Framework [Agent A]
**Package**: `test/java/com/amannmalik/mcp`
- [ ] Comprehensive unit tests for all schema types
- [ ] JSON serialization/deserialization round-trip tests
- [ ] Message validation and error handling tests
- [ ] Mock transport implementations for testing
- [ ] Property-based testing for schema validation
- [ ] Performance benchmarks using JMH
- [ ] GraalVM native compilation tests

### 7.2 Integration Testing [Agent B]
**Package**: `test/java/com/amannmalik/mcp/integration`
- [ ] End-to-end protocol flow tests
- [ ] Client-server interaction scenarios
- [ ] Capability negotiation test suites
- [ ] Resource subscription and notification tests
- [ ] Tool execution and result handling tests
- [ ] Error propagation and recovery tests
- [ ] Multi-transport compatibility tests

### 7.3 Compliance & Validation [Agent C]
**Package**: `test/java/com/amannmalik/mcp/compliance`
- [ ] MCP specification compliance test suite
- [ ] JSON-RPC 2.0 specification compliance  
- [ ] Schema validation against official JSON Schema
- [ ] Cross-implementation compatibility tests
- [ ] Security and validation boundary tests
- [ ] Performance and memory usage benchmarks
- [ ] Documentation generation and examples

---

## Parallelization Strategy

### Concurrent Development Tracks:
1. **Track 1**: Foundation (Agents A, B, C in Phase 1)
2. **Track 2**: Content & Schema (Agents A, B, C in Phase 2)  
3. **Track 3**: Protocol Messages (Agents A, B, C, D in Phase 3)
4. **Track 4**: Domain Objects (Agents A, B, C in Phase 4)
5. **Track 5**: Integration (Agents A, B, C in Phase 5)
6. **Track 6**: Transport (Agents A, B in Phase 6)
7. **Track 7**: Testing (Agents A, B, C in Phase 7)

### Dependencies:
- Phase 2 depends on Phase 1 completion
- Phase 3 depends on Phase 1 + partial Phase 2
- Phase 4 depends on Phase 2 + partial Phase 3
- Phase 5 depends on Phase 3 + Phase 4
- Phase 6 depends on Phase 5
- Phase 7 can start after Phase 2 (incremental testing)

### Critical Path: 
Phase 1 → Phase 2 → Phase 3 → Phase 5 → Phase 6 (estimated 16-20 days)

---

## Success Criteria

### Technical Deliverables:
- [ ] **Type Safety**: All MCP schema types implemented with full Java type safety
- [ ] **Serialization**: Bidirectional JSON serialization using Jakarta JSON-P
- [ ] **Validation**: Comprehensive input validation and error handling
- [ ] **Performance**: Optimized for GraalVM native compilation
- [ ] **Compliance**: Full MCP 2025-06-18 specification compliance
- [ ] **Testing**: >95% code coverage with integration test suite

### Quality Gates:
- [ ] **Code Review**: All code reviewed following CLAUDE.md guidelines
- [ ] **Static Analysis**: No warnings from modern Java linting tools
- [ ] **Performance**: <10ms serialization/deserialization for typical messages
- [ ] **Memory**: Minimal allocation in hot paths, GC-friendly design
- [ ] **Documentation**: Complete Javadoc coverage for public APIs
- [ ] **Examples**: Working examples for all major protocol features

### Implementation Guidelines:
- **Sealed Interfaces**: Use for discriminated unions (ContentBlock, Messages, etc.)
- **Records**: Primary data structures following immutable design  
- **Optional<T>**: All nullable fields using Optional wrapper
- **Static Factory Methods**: `fromJson()`, `toJson()` on each type
- **Package Organization**: Clear separation by protocol layer and responsibility
- **Error Handling**: Specific exception types for each validation scenario
- **Resource Management**: Explicit lifecycle management for connections/subscriptions