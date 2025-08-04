# MCP Java Refactor Plan

## Overview

This refactor plan aims to simplify the MCP Java codebase while maintaining architectural integrity and adherence to the MCP specification. The plan focuses on reducing complexity, eliminating duplication, and improving maintainability through strategic consolidation and abstraction.

## Current Architecture Analysis

### Core Components
- **McpClient**: 697 lines, handles client-side protocol implementation
- **McpServer**: 620 lines, handles server-side protocol implementation
- **Main**: Simple CLI entry point with command routing
- **Locator**: Factory methods for default provider implementations

### Provider Pattern
- Unified `Provider<T>` interface with list/subscribe capabilities
- Specialized providers: `ToolProvider`, `PromptProvider`, `ResourceProvider`
- Inconsistent provider contracts (CompletionProvider, SamplingProvider, ElicitationProvider don't extend Provider)
- InMemory implementations follow similar patterns with code duplication

### Key Patterns Identified
1. **Consistent JSON-RPC handling** via `JsonRpcRequestProcessor`
2. **Uniform pagination** via `Pagination.Page<T>`
3. **Change notification** via `ChangeSupport<T>` pattern
4. **Rate limiting** consistently applied across capabilities
5. **Validation** centralized in `ValidationUtil`

## Simplification Strategy

### Phase 1: Provider Pattern Unification

#### 1.1 Create Unified Provider Hierarchy
```java
// Consolidate all provider types under common abstractions
public interface Provider<T> extends AutoCloseable {
    Pagination.Page<T> list(String cursor);
    default ChangeSubscription subscribe(ChangeListener<Change> listener) { return () -> {}; }
    default boolean supportsListChanged() { return false; }
}

public interface ExecutingProvider<T, R> extends Provider<T> {
    R execute(String name, JsonObject args) throws InterruptedException;
}
```

#### 1.2 Unify Provider Implementations
- **Merge** `CompletionProvider` → `ExecutingProvider<CompleteRequest.Ref, CompleteResult>`
- **Merge** `SamplingProvider` → `ExecutingProvider<SamplingMessage, CreateMessageResponse>`
- **Merge** `ElicitationProvider` → `ExecutingProvider<ElicitRequest, ElicitResult>`
- **Consolidate** InMemory* implementations into generic `InMemoryProvider<T>`

### Phase 2: Core Client/Server Simplification

#### 2.1 Extract Common JSON-RPC Infrastructure
```java
public class JsonRpcEndpoint implements AutoCloseable {
    protected final Transport transport;
    protected final JsonRpcRequestProcessor processor;
    protected final ProgressManager progress;
    
    // Common request/response handling
    // Common lifecycle management
    // Common error handling
}

public final class McpClient extends JsonRpcEndpoint {
    // Client-specific capabilities only
}

public final class McpServer extends JsonRpcEndpoint {
    // Server-specific capabilities only
}
```

#### 2.2 Consolidate Request/Response Handling
- Extract method registration patterns
- Unify timeout and cancellation logic
- Consolidate rate limiting across both client and server

### Phase 3: Configuration and Dependency Management

#### 3.1 Simplify Locator Pattern
```java
public final class McpComponents {
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        public Builder withResources(ResourceProvider provider) { ... }
        public Builder withTools(ToolProvider provider) { ... }
        // Fluent builder pattern
        public McpComponents build() { ... }
    }
}
```

#### 3.2 Streamline Configuration
- Reduce `McpConfiguration` record to essential fields only
- Move capability-specific config to respective modules
- Simplify default value management

### Phase 4: Transport Layer Simplification

#### 4.1 Unify Transport Abstractions
- Consolidate HTTP/SSE transport implementations
- Simplify authorization handling
- Reduce transport-specific complexity in core client/server

#### 4.2 Streamline Message Routing
- Extract common message routing patterns
- Simplify notification dispatch
- Consolidate error handling across transports

## Detailed Refactor Steps

### Step 1: Provider Consolidation (3-5 files affected)
1. Create unified `ExecutingProvider<T, R>` interface
2. Migrate CompletionProvider, SamplingProvider, ElicitationProvider
3. Create generic `InMemoryProvider<T>` implementation
4. Update Locator to use unified providers

**Files to modify:**
- `core/Provider.java`
- `completion/CompletionProvider.java`
- `sampling/SamplingProvider.java`
- `elicitation/ElicitationProvider.java`
- New: `core/ExecutingProvider.java`
- New: `core/InMemoryProvider.java`

### Step 2: Extract JsonRpcEndpoint (2 files affected)
1. Create `JsonRpcEndpoint` base class
2. Extract common fields and methods from McpClient/McpServer
3. Migrate client/server to extend JsonRpcEndpoint

**Files to modify:**
- `McpClient.java` (697 → ~400 lines)
- `McpServer.java` (620 → ~350 lines)
- New: `core/JsonRpcEndpoint.java` (~200 lines)

### Step 3: Simplify Request Handling (8-10 files affected)
1. Extract common request/response patterns
2. Unify method registration and dispatch
3. Consolidate error handling and validation

**Files to modify:**
- All request/response classes in each capability package
- `util/JsonRpcRequestProcessor.java`
- Wire protocol definitions

### Step 4: Configuration Streamlining (2-3 files affected)
1. Split McpConfiguration into capability-specific configs
2. Create McpComponents builder pattern
3. Simplify Locator implementation

**Files to modify:**
- `config/McpConfiguration.java`
- `Locator.java`
- New: `core/McpComponents.java`

## Expected Outcomes

### Complexity Reduction
- **~30% reduction** in total lines of code
- **~50% reduction** in core client/server complexity
- **Elimination** of provider pattern inconsistencies
- **Consolidation** of 15+ InMemory* classes into 3-4 generic implementations

### Maintainability Improvements
- Unified provider contracts across all capabilities
- Centralized JSON-RPC handling
- Simplified configuration management
- Reduced coupling between transport and protocol layers

### Architectural Benefits
- Cleaner separation of concerns
- More consistent error handling
- Simplified testing surface
- Better adherence to composition over inheritance

## Timeline Estimate

- **Phase 1**: 2-3 days (Provider unification)
- **Phase 2**: 3-4 days (Core simplification)  
- **Phase 3**: 1-2 days (Configuration)
- **Phase 4**: 2-3 days (Transport)
- **Total**: 8-12 days

This refactor maintains the philosophical principles of high information density, flat organization, and minimal dependencies while significantly reducing codebase complexity and improving maintainability.