# MCP Codebase Refactoring Plan

## Executive Summary

Simplify the MCP Java implementation by eliminating repetitive patterns and consolidating abstractions while maintaining specification compliance. Primary focus: reduce boilerplate through better generic abstractions.

## Current Issues

- **15+ codec classes** with identical serialization patterns
- **Scattered validation** logic across domain models
- **Over-engineered configuration** system with unnecessary complexity
- **Repetitive provider patterns** across domains
- **Complex JSON-RPC processing** with excessive abstraction

## Refactoring Phases

### Phase 1: Codec Infrastructure (HIGH IMPACT)

**Problem**: Massive boilerplate across codec classes
**Solution**: Generic serialization infrastructure

#### Changes:
1. **Create `JsonCodec<T>` interface**
   ```java
   public interface JsonCodec<T> {
       JsonObject toJson(T entity);
       T fromJson(JsonObject json);
   }
   ```

2. **Create `AbstractEntityCodec<T>`**
   - Common validation patterns
   - Error handling standardization
   - Field extraction utilities

3. **Eliminate 12+ codec classes**:
   - `AnnotationsCodec` → generic approach
   - `CompletionCodec` → generic approach
   - `ContentCodec` → generic approach
   - `ElicitCodec` → generic approach
   - `LifecycleCodec` → generic approach
   - `LoggingCodec` → generic approach
   - `PingCodec` → generic approach
   - `PromptCodec` → generic approach
   - `ResourcesCodec` → generic approach
   - `RootsCodec` → generic approach
   - `SamplingCodec` → generic approach
   - `ToolCodec` → generic approach

4. **Consolidate pagination handling**
   - Move `PaginationCodec` logic into `AbstractEntityCodec`
   - Remove `PaginationJson` duplication

**Files affected**: ~35 files
**Lines reduced**: ~2000+ lines
**Testing impact**: Minimal (same interfaces)

### Phase 2: JSON-RPC Simplification (MEDIUM IMPACT)

**Problem**: Over-engineered request processing
**Solution**: Streamlined handler registration and processing

#### Changes:
1. **Simplify `JsonRpcRequestProcessor`**
   - Remove unnecessary abstraction layers
   - Direct handler mapping
   - Eliminate wrapper classes

2. **Consolidate progress tracking**
   - Merge `ProgressManager` and `CancellationTracker`
   - Single progress/cancellation interface

3. **Streamline handler registration**
   - Remove `RpcHandlerRegistry` complexity
   - Direct method mapping

**Files affected**: ~8 files
**Lines reduced**: ~500 lines

### Phase 3: Configuration Simplification (MEDIUM IMPACT)

**Problem**: Over-engineered configuration hierarchy
**Solution**: Flatten to essential functionality

#### Changes:
1. **Merge configuration classes**
   - Combine `McpConfiguration` and `McpConfigurationDocs`
   - Remove `McpConfigurationParser` complexity

2. **Remove unnecessary features**
   - Configuration watching (if unused)
   - Complex reloading mechanisms
   - Excessive validation layers

3. **Simplify config loading**
   - Single configuration entry point
   - Basic validation only

**Files affected**: ~5 files  
**Lines reduced**: ~300 lines

### Phase 4: Validation Consolidation (MEDIUM IMPACT)

**Problem**: Scattered validation logic
**Solution**: Centralized validation patterns

#### Changes:
1. **Create `ValidationUtil` utility**
   - Common validation patterns
   - Standardized error messages
   - URI/schema validation consolidation

2. **Remove `InvalidParams` wrapper**
   - Direct exception throwing
   - Simplified error handling

3. **Consolidate validators**
   - Merge similar validator classes
   - Single validation entry point per domain

**Files affected**: ~8 files
**Lines reduced**: ~200 lines

### Phase 5: Provider Pattern Unification (LOW IMPACT)

**Problem**: Repetitive provider interfaces
**Solution**: Common provider base patterns

#### Changes:
1. **Create `Provider<T>` base interface**
   - Common list/subscribe methods
   - Standardized change notifications

2. **Consolidate change listeners**
   - Single `ChangeListener<T>` interface
   - Remove domain-specific listener classes

**Files affected**: ~15 files
**Lines reduced**: ~400 lines

## Implementation Strategy

### Approach
- **Incremental refactoring**: Maintain working system throughout
- **Backward compatibility**: Keep existing APIs during transition
- **Test-driven**: Ensure no functionality regression

### Order of Operations
1. **Phase 1** → Largest impact, lowest risk
2. **Phases 2-4** → Parallel implementation possible
3. **Phase 5** → Final cleanup

### Risk Mitigation
- **Feature flags** for new codec infrastructure
- **Comprehensive testing** before removing old code
- **Gradual migration** of domain packages

## Success Metrics

### Quantitative
- **Lines of code**: Reduce by ~3400 lines (25%)
- **File count**: Reduce by ~20 files (15%)
- **Cyclomatic complexity**: Reduce by 30%

### Qualitative
- **Developer productivity**: Faster feature implementation
- **Maintainability**: Fewer places to make changes
- **Code consistency**: Unified patterns across domains

## Post-Refactoring Architecture

### Core Abstractions
- `JsonCodec<T>` - Unified serialization
- `Provider<T>` - Unified provider interface
- `ValidationUtil` - Centralized validation
- Streamlined JSON-RPC processing

### Package Structure (Post)
```
com.amannmalik.mcp/
├── core/           # JsonCodec, Provider base classes
├── auth/           # Authentication (unchanged)
├── cli/            # CLI commands (unchanged) 
├── jsonrpc/        # Simplified RPC processing
├── lifecycle/      # Protocol lifecycle (unchanged)
├── transport/      # Transport layer (unchanged)
├── domains/        # All domain packages (prompts, resources, tools, etc.)
│   ├── prompts/    # Simplified, codec-free
│   ├── resources/  # Simplified, codec-free
│   └── tools/      # Simplified, codec-free
├── util/           # Consolidated utilities
└── validation/     # Centralized validation
```

## Expected Benefits

1. **Reduced cognitive load** - Single serialization pattern to understand
2. **Faster development** - Less boilerplate for new features  
3. **Easier maintenance** - Fewer files to update for changes
4. **Better consistency** - Unified error handling and validation
5. **Specification compliance** - Maintained throughout refactoring

## Timeline Estimate

- **Phase 1**: 2-3 weeks (codec infrastructure)
- **Phase 2**: 1-2 weeks (JSON-RPC simplification)
- **Phase 3**: 1 week (configuration)
- **Phase 4**: 1 week (validation)
- **Phase 5**: 1 week (provider patterns)

**Total**: 6-8 weeks for complete refactoring

## Next Steps

1. **Review and approve** this refactoring plan
2. **Create feature branch** for codec infrastructure work
3. **Implement `JsonCodec<T>` interface** and `AbstractEntityCodec<T>`
4. **Migrate one domain package** as proof of concept
5. **Iterate based on feedback** before full migration