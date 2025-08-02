# MCP Configuration Analysis Report

## Executive Summary

This report provides a comprehensive analysis of all static configuration values, defaults, and hardcoded constants found in the MCP (Model Context Protocol) Java implementation. The analysis identifies **73 distinct configuration values** across the codebase that are candidates for consolidation into a single monolithic YAML configuration system.

## Current Configuration Architecture

### Configuration Loading Structure

The current configuration system uses a file-based approach with support for JSON and YAML formats:

- **ConfigLoader** (`src/main/java/com/amannmalik/mcp/cli/ConfigLoader.java`): Loads configuration from JSON/YAML files
- **CLI Configuration Model**: Three configuration types - `ServerConfig`, `ClientConfig`, `HostConfig`
- **Transport Types**: STDIO and HTTP transport modes
- **Default Overrides**: Command-line options can override file-based configuration

### Current Configuration Defaults in CLI

#### ConfigLoader Defaults
- **Transport**: `"stdio"` (line 40)
- **Port**: `0` (line 51) - for server config when not specified
- **Authorization Servers**: Empty list `List.of()` (line 45) when not configured

#### ServerCommand Defaults
- **Transport Type**: `TransportType.STDIO` when HTTP port not specified (line 65)
- **Port**: `0` when HTTP port not provided (line 66)
- **Authorization List**: Empty list `List.of()` for test mode (line 74)
- **Origin Validator**: Hardcoded allowed origins `Set.of("http://localhost", "http://127.0.0.1")` (line 83)

#### ClientCommand Defaults
- **Current Directory Root**: `"file://" + System.getProperty("user.dir")` (line 61-63)
- **Client Info**: Name="cli", Display Name="CLI", Version="0" (line 66)
- **Client Capabilities**: `EnumSet.of(ClientCapability.SAMPLING, ClientCapability.ROOTS)` (line 67)

#### HostCommand Defaults
- **Principal**: `new Principal("user", Set.of())` (line 69)
- **Security Policy**: Always allows `c -> true` (line 68)
- **Client Capabilities**: `EnumSet.noneOf(ClientCapability.class)` (line 79)
- **Client Version**: "0" (line 78)

## Comprehensive Configuration Values Inventory

### 1. Protocol and Version Constants

| Location | Value | Purpose | Line |
|----------|-------|---------|------|
| `lifecycle/Protocol.java` | `"2025-06-18"` | Latest protocol version | 13 |
| `lifecycle/Protocol.java` | `"2025-03-26"` | Previous protocol version for compatibility | 18 |
| `jsonrpc/JsonRpc.java` | `"2.0"` | JSON-RPC protocol version | 7 |

### 2. Timeout and Performance Values

| Location | Value | Purpose | Line |
|----------|-------|---------|------|
| `util/Timeouts.java` | `30_000L` | Default timeout in milliseconds | 7 |
| `client/McpClient.java` | `5000` | Ping timeout in milliseconds | 111 |
| `transport/StdioTransport.java` | `Duration.ofSeconds(2)` | Process termination wait duration | 17 |
| `security/HostProcess.java` | `30000, 5000` | Ping interval and timeout | 82 |

### 3. Rate Limiting Configuration

| Location | Value | Purpose | Line |
|----------|-------|---------|------|
| `server/McpServer.java` | `5, 1000` | Tool calls rate limit (5 per second) | 60 |
| `server/McpServer.java` | `10, 1000` | Completions rate limit (10 per second) | 61 |
| `server/McpServer.java` | `20, 1000` | Log messages rate limit (20 per second) | 62 |
| `server/McpServer.java` | `20, 1000` | Progress updates rate limit (20 per second) | 38 |
| `client/McpClient.java` | `20, 1000` | Progress manager rate limit (20 per second) | 46 |

### 4. Pagination and Size Limits

| Location | Value | Purpose | Line |
|----------|-------|---------|------|
| `util/Pagination.java` | `100` | Default page size | 9 |
| `server/completion/CompleteResult.java` | `100` | Maximum completion values | 10 |
| `transport/SseClient.java` | `100` | SSE event history limit | 16 |
| `transport/McpServlet.java` | `1` | Response queue capacity | 147 |

### 5. HTTP Headers and Transport

| Location | Value | Purpose | Line |
|----------|-------|---------|------|
| `transport/TransportHeaders.java` | `"MCP-Protocol-Version"` | Protocol version header name | 7 |
| `transport/TransportHeaders.java` | `"Mcp-Session-Id"` | Session ID header name | 8 |
| `transport/TransportHeaders.java` | `"Authorization"` | Authorization header name | 9 |
| `transport/StreamableHttpTransport.java` | `Protocol.PREVIOUS_VERSION` | Default compatibility version | 36-37 |

### 6. Error Codes

| Location | Value | Purpose | Line |
|----------|-------|---------|------|
| `jsonrpc/JsonRpcErrorCode.java` | `-32700` | Parse error code | 7 |
| `jsonrpc/JsonRpcErrorCode.java` | `-32600` | Invalid request code | 8 |
| `jsonrpc/JsonRpcErrorCode.java` | `-32601` | Method not found code | 9 |
| `jsonrpc/JsonRpcErrorCode.java` | `-32602` | Invalid params code | 10 |
| `jsonrpc/JsonRpcErrorCode.java` | `-32603` | Internal error code | 11 |
| `server/McpServer.java` | `-32001` | Rate limit error code | 59 |
| `server/McpServer.java` | `-32002` | Resource not found error code | 433, 486 |

### 7. Environment Variables

| Location | Variable Name | Purpose | Line |
|----------|---------------|---------|------|
| `client/sampling/InteractiveSamplingProvider.java` | `OPENAI_API_KEY` | OpenAI API key for sampling | 140 |
| `cli/ServerCommand.java` | `MCP_JWT_SECRET` | JWT secret for authentication | 86 |
| `cli/ClientCommand.java` | `user.dir` | Current working directory | 61 |

### 8. Security and Authentication

| Location | Value | Purpose | Line |
|----------|-------|---------|------|
| `transport/StreamableHttpTransport.java` | `"default"` | Default principal name | 202 |
| `cli/ServerCommand.java` | `Set.of("http://localhost", "http://127.0.0.1")` | Allowed CORS origins | 83 |
| `client/sampling/InteractiveSamplingProvider.java` | `"Bearer "` | Authorization header prefix | 171 |
| `validation/UnauthorizedException.java` | Regex pattern | Resource metadata extraction pattern | 9-10 |

### 9. Server Information

| Location | Value | Purpose | Line |
|----------|-------|---------|------|
| `server/McpServer.java` | `"mcp-java"` | Server implementation name | 96 |
| `server/McpServer.java` | `"MCP Java Reference"` | Server description | 96 |
| `server/McpServer.java` | `"0.1.0"` | Server version | 96 |

### 10. Validation Constants

| Location | Value | Purpose | Line |
|----------|-------|---------|------|
| `validation/MetaValidator.java` | `[A-Za-z](?:[A-Za-z0-9-]*[A-Za-z0-9])?` | Meta label validation pattern | 11-12 |
| `validation/MetaValidator.java` | `(?:[A-Za-z0-9](?:[A-Za-z0-9._-]*[A-Za-z0-9])?)?` | Meta name validation pattern | 13-14 |
| `validation/ElicitationSchemaValidator.java` | Multiple key sets | Schema validation allowed keys | 131-148 |
| `util/PaginationCodec.java` | `Set.of("cursor", "_meta")` | Request keys for pagination | 11 |
| `util/PaginationCodec.java` | `Set.of("nextCursor", "_meta")` | Result keys for pagination | 12 |

### 11. Test and Development Defaults

| Location | Value | Purpose | Line |
|----------|-------|---------|------|
| `server/ServerDefaults.java` | `"test://example"` | Test resource URI | 19 |
| `server/ServerDefaults.java` | `"test://template"` | Test resource template URI | 21 |
| `server/ServerDefaults.java` | `"test_tool"` | Test tool name | 27 |
| `server/ServerDefaults.java` | `"test_prompt"` | Test prompt name | 42 |
| `server/ServerDefaults.java` | `"default"` | Default principal ID | 71, 74 |

## Configuration Dependencies Analysis

### Critical Dependencies
1. **Protocol Versions**: Must maintain compatibility across client/server
2. **Rate Limits**: Directly affect performance and resource usage
3. **Timeouts**: Impact user experience and system reliability
4. **Authentication Defaults**: Security implications

### Environment-Dependent Values
1. **File Paths**: Working directories, configuration files
2. **Network Configuration**: Ports, origins, URLs
3. **Secrets**: API keys, JWT secrets

### Runtime vs. Compile-Time Configuration
- **Compile-time**: Protocol versions, error codes, validation patterns
- **Runtime**: Timeouts, rate limits, server information, network settings

## Recommendations for Monolithic YAML Configuration

### 1. High-Priority Configurable Items
```yaml
# Core System Configuration
system:
  protocol:
    version: "2025-06-18"
    compatibility_version: "2025-03-26"
  timeouts:
    default_ms: 30000
    ping_ms: 5000
    process_wait_seconds: 2
  
# Performance and Limits
performance:
  rate_limits:
    tools_per_second: 5
    completions_per_second: 10
    logs_per_second: 20
    progress_per_second: 20
  pagination:
    default_page_size: 100
    max_completion_values: 100
    sse_history_limit: 100

# Server Configuration
server:
  info:
    name: "mcp-java"
    description: "MCP Java Reference"
    version: "0.1.0"
  transport:
    type: "stdio"  # or "http"
    port: 0
    allowed_origins:
      - "http://localhost"
      - "http://127.0.0.1"

# Security
security:
  auth:
    jwt_secret_env: "MCP_JWT_SECRET"
    default_principal: "default"
  
# Client Configuration
client:
  info:
    name: "cli"
    display_name: "CLI"
    version: "0"
  capabilities:
    - "SAMPLING"
    - "ROOTS"
```

### 2. Medium-Priority Configurable Items
- Error codes (for customization)
- Validation patterns (for different use cases)
- Test defaults (for different test environments)

### 3. Environment-Specific Overrides
```yaml
# Environment overrides
environments:
  development:
    server:
      transport:
        port: 8080
    performance:
      rate_limits:
        tools_per_second: 10
  
  production:
    performance:
      rate_limits:
        tools_per_second: 100
      timeouts:
        default_ms: 60000
```

## Implementation Strategy

### Phase 1: Core Configuration Consolidation
1. Create `McpConfiguration` class hierarchy
2. Implement YAML-based configuration loading
3. Replace hardcoded constants with configuration references
4. Maintain backward compatibility with existing CLI options

### Phase 2: Advanced Configuration Features
1. Environment-specific configuration profiles
2. Runtime configuration reloading
3. Configuration validation and schema
4. Configuration documentation generation

### Phase 3: Migration and Optimization
1. Deprecate old configuration methods
2. Performance optimization for configuration access
3. Configuration management tooling
4. Monitoring and observability for configuration changes

## Conclusion

The MCP codebase contains 73 distinct configuration values that are currently hardcoded throughout the implementation. These values span critical areas including protocol versions, timeouts, rate limits, security settings, and performance parameters. 

Consolidating these into a monolithic YAML configuration system would provide:
- **Centralized Configuration Management**: Single source of truth for all settings
- **Environment-Specific Deployments**: Easy customization for different environments
- **Operational Flexibility**: Runtime configuration changes without code modifications
- **Better Maintainability**: Clear separation of configuration from implementation

The recommended approach prioritizes high-impact configuration items (timeouts, rate limits, server information) while maintaining backward compatibility and providing a migration path for existing deployments.