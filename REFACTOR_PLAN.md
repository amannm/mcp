# MCP Codebase Refactoring Plan

## Current State Analysis

The codebase has 21 packages with 100+ classes, showing over-segmentation that contradicts the "flat organization" philosophy. Many packages contain only 2-4 classes, creating unnecessary depth.

## Refactoring Objectives

1. **Flatten package hierarchy** - Reduce from 21 packages to ~8 core domains
2. **Strengthen core abstractions** - Centralize common patterns in `core/`
3. **Eliminate micro-packages** - Merge packages with <5 classes
4. **Reduce provider complexity** - Abstract common provider patterns
5. **Consolidate transport layer** - Simplify transport implementations

## Proposed Structure

```
com.amannmalik.mcp/
├── core/           # Core abstractions, codecs, providers
├── protocol/       # JSON-RPC, lifecycle, wire methods
├── capabilities/   # Tools, prompts, resources, sampling, completion
├── transport/      # All transport implementations
├── security/       # Auth, validation, privacy, consent
├── management/     # Roots, host, configuration
├── utilities/      # Ping, logging, progress, pagination
└── [root classes]  # Main, McpClient, McpServer, Locator
```

## Phase 1: Core Consolidation

### Merge into `core/`
- `content/ContentBlock` → `core/ContentBlock`
- `validation/ValidationUtil` → `core/ValidationUtil`
- Abstract provider pattern from all `*Provider` classes into `core/AbstractProvider`
- Create `core/McpEntity` base type for all MCP data structures

### Eliminate `annotations/`
- Move `Annotations` to `core/` or merge into existing classes
- Avoid annotation-based patterns per preferences

## Phase 2: Protocol Unification

### Merge into `protocol/`
- `jsonrpc/*` → `protocol/jsonrpc/`
- `lifecycle/*` → `protocol/lifecycle/`
- `wire/*` → `protocol/wire/`
- Create `protocol/MessageHandler` abstraction

## Phase 3: Capabilities Consolidation

### Merge into `capabilities/`
- `tools/*` → `capabilities/tools/`
- `prompts/*` → `capabilities/prompts/`
- `resources/*` → `capabilities/resources/`
- `sampling/*` → `capabilities/sampling/`
- `completion/*` → `capabilities/completion/`
- `elicitation/*` → `capabilities/elicitation/`

Create shared abstractions:
- `capabilities/CapabilityProvider<T>`
- `capabilities/CapabilityRequest<T>`
- `capabilities/CapabilityResult<T>`

## Phase 4: Transport Simplification

### Consolidate transport implementations
- Merge `cli/TransportType` into `transport/`
- Create `transport/TransportFactory`
- Abstract common transport patterns
- Reduce servlet complexity

## Phase 5: Security & Management

### Merge into `security/`
- `auth/*` → `security/auth/`
- `host/PrivacyBoundaryEnforcer` → `security/`
- `host/ConsentManager` → `security/`

### Merge into `management/`
- `roots/*` → `management/roots/`
- `host/HostProcess` → `management/`
- `config/*` → `management/config/`

### Merge into `utilities/`
- `ping/*` → `utilities/ping/`
- `logging/*` → `utilities/logging/`
- `util/*` → `utilities/`

## Implementation Strategy

### Step 1: Create Core Abstractions
1. Define `core/McpEntity` sealed interface
2. Create `core/AbstractProvider<Request, Result>` 
3. Move validation and codec logic to core

### Step 2: Package Migrations (Bottom-Up)
1. Start with leaf packages (annotations, content, validation)
2. Move to capability packages
3. Restructure protocol layer
4. Consolidate transport and utilities

### Step 3: Interface Simplification
1. Replace multiple provider interfaces with generic `Provider<T>`
2. Unify request/result patterns
3. Eliminate duplicate notification patterns

### Step 4: Dependency Cleanup
1. Remove any reflection usage
2. Eliminate null-returning methods
3. Replace Optional with sealed types where appropriate
4. Ensure immutable data structures

## Expected Benefits

1. **Reduced cognitive load** - 8 packages vs 21
2. **Improved discoverability** - Related functionality co-located
3. **Stronger type safety** - Common abstractions prevent misuse
4. **Better testability** - Fewer integration points
5. **Simplified dependencies** - Clear separation of concerns

## Success Metrics

- Package count reduced by 60%
- Class count maintained or reduced
- Zero unchecked casts
- No reflection usage
- All data structures immutable
- Build time improved

## Risk Mitigation

1. **Gradual migration** - One package at a time
2. **Maintain tests** - Ensure all tests pass after each phase
3. **Interface stability** - Keep public APIs stable during refactoring
4. **Performance validation** - Benchmark before/after each phase