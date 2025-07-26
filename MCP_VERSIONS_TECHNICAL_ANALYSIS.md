# MCP Specification Technical Analysis: Version Differences

## Executive Summary

This document provides a comprehensive analysis of the three Model Context Protocol (MCP) specification versions:
- **2024-11-05** (initial specification)
- **2025-03-26** (intermediate revision)  
- **2025-06-18** (latest revision)

The analysis covers precise technical differences at the protocol, schema, and implementation levels to inform multi-version client and server implementations.

## Protocol Version Evolution

### Version Identifiers
- `2024-11-05`: `LATEST_PROTOCOL_VERSION = "2024-11-05"`
- `2025-03-26`: `LATEST_PROTOCOL_VERSION = "2025-03-26"`  
- `2025-06-18`: `LATEST_PROTOCOL_VERSION = "2025-06-18"`

### Breaking Changes Summary

| Change | 2024-11-05 | 2025-03-26 | 2025-06-18 |
|--------|-------------|-------------|-------------|
| JSON-RPC Batching | ❌ Not supported | ✅ Supported | ❌ Removed |
| Authorization | ❌ Not defined | ✅ OAuth 2.1 | ✅ Enhanced OAuth 2.1 |
| Elicitation | ❌ Not supported | ❌ Not supported | ✅ Supported |
| Tool Annotations | ❌ Basic | ✅ Comprehensive | ✅ Enhanced |

## 1. JSON-RPC Foundation Changes

### 1.1 Message Type Support

**2024-11-05:**
```typescript
export type JSONRPCMessage =
    | JSONRPCRequest
    | JSONRPCNotification  
    | JSONRPCResponse
    | JSONRPCError;
```

**2025-03-26:**
```typescript
export type JSONRPCMessage =
    | JSONRPCRequest
    | JSONRPCNotification
    | JSONRPCBatchRequest      // NEW
    | JSONRPCResponse
    | JSONRPCError
    | JSONRPCBatchResponse;    // NEW

// Added batch support
export type JSONRPCBatchRequest = (JSONRPCRequest | JSONRPCNotification)[];
export type JSONRPCBatchResponse = (JSONRPCResponse | JSONRPCError)[];
```

**2025-06-18:**
```typescript
export type JSONRPCMessage =
    | JSONRPCRequest
    | JSONRPCNotification
    | JSONRPCResponse
    | JSONRPCError;
// Batch support REMOVED
```

**Implementation Impact:** Clients supporting all versions must:
- Detect batch capability during initialization for 2025-03-26
- Handle both batched and non-batched messages for 2025-03-26
- Revert to non-batch mode for 2025-06-18

### 1.2 Metadata Field Evolution

**2024-11-05:**
```typescript
export interface Request {
    method: string;
    params?: {
        _meta?: {
            progressToken?: ProgressToken;
        };
        [key: string]: unknown;
    };
}
```

**2025-03-26:**
```typescript
// Same as 2024-11-05
```

**2025-06-18:**
```typescript
export interface Request {
    method: string;
    params?: {
        /**
         * See [specification/2025-06-18/basic/index#general-fields] for notes on _meta usage.
         */
        _meta?: {
            progressToken?: ProgressToken;
            [key: string]: unknown;  // NEW: Extensible metadata
        };
        [key: string]: unknown;
    };
}
```

**Implementation Impact:** 2025-06-18 servers can accept arbitrary metadata fields in `_meta`, requiring more flexible parsing.

## 2. Capabilities System Evolution

### 2.1 Client Capabilities

**2024-11-05 & 2025-03-26:**
```typescript
export interface ClientCapabilities {
    experimental?: { [key: string]: object };
    roots?: {
        listChanged?: boolean;
    };
    sampling?: object;
}
```

**2025-06-18:**
```typescript
export interface ClientCapabilities {
    experimental?: { [key: string]: object };
    roots?: {
        listChanged?: boolean;
    };
    sampling?: object;
    elicitation?: object;  // NEW: Elicitation support
}
```

### 2.2 Server Capabilities

**2024-11-05:**
```typescript
export interface ServerCapabilities {
    experimental?: { [key: string]: object };
    logging?: object;
    prompts?: {
        listChanged?: boolean;
    };
    resources?: {
        subscribe?: boolean;
        listChanged?: boolean;
    };
    tools?: {
        listChanged?: boolean;
    };
}
```

**2025-03-26 & 2025-06-18:**
```typescript
export interface ServerCapabilities {
    experimental?: { [key: string]: object };
    logging?: object;
    completions?: object;  // NEW: Autocompletion support
    prompts?: {
        listChanged?: boolean;
    };
    resources?: {
        subscribe?: boolean;
        listChanged?: boolean;
    };
    tools?: {
        listChanged?: boolean;
    };
}
```

## 3. Authorization Framework

### 3.1 Introduction in 2025-03-26

**New Features:**
- OAuth 2.1 authorization flow
- Dynamic client registration support
- Authorization Server Metadata (RFC8414)
- HTTP 401 Unauthorized response handling

**Protocol Requirements:**
- OPTIONAL implementation
- HTTP transports SHOULD conform
- STDIO transports SHOULD NOT use OAuth

**Key References:**
- `spec/mcp-2025-03-26/basic/authorization.mdx:31-36`

### 3.2 Enhancement in 2025-06-18

**Major Changes:**
1. **OAuth Resource Server Classification:**
   - MCP servers classified as OAuth 2.1 Resource Servers
   - MUST implement Protected Resource Metadata (RFC9728)

2. **Authorization Server Discovery:**
   ```
   MCP servers MUST implement OAuth 2.0 Protected Resource Metadata (RFC9728)
   MCP clients MUST use Protected Resource Metadata for authorization server discovery
   ```

3. **Security Enhancements:**
   - Resource Indicators required (RFC8707)
   - Enhanced security considerations
   - New security best practices document

4. **WWW-Authenticate Header:**
   ```
   MCP servers MUST use HTTP header WWW-Authenticate when returning 401 Unauthorized
   ```

**Implementation Impact:** Clients must support multiple authorization discovery methods:
- 2025-03-26: Basic OAuth 2.1 with server metadata
- 2025-06-18: Enhanced with Protected Resource Metadata and mandatory security features

## 4. Transport Layer Changes

### 4.1 HTTP Transport Evolution

**2025-03-26:**
- Introduced Streamable HTTP transport
- Replaced HTTP+SSE with more flexible approach

**2025-06-18:**
- Added mandatory `MCP-Protocol-Version` header
- MUST specify negotiated protocol version in subsequent HTTP requests

**Header Requirement:**
```
MCP-Protocol-Version: 2025-06-18
```

## 5. Content and Metadata Systems

### 5.1 Content Type Support

**2024-11-05:**
- Text and image content types

**2025-03-26:**
- Added audio content support
- Enhanced ProgressNotification with message field

**2025-06-18:**
- Added structured tool output support
- Added resource links in tool results

### 5.2 Metadata Interface Changes

**2025-06-18 Introduced BaseMetadata:**
```typescript
export interface BaseMetadata {
    /**
     * Intended for programmatic or logical use, but used as a display name 
     * in past specs or fallback (if title isn't present).
     */
    name: string;

    /**
     * Intended for UI and end-user contexts — optimized to be human-readable 
     * and easily understood, even by those unfamiliar with domain-specific terminology.
     */
    title?: string;
}

export interface Implementation extends BaseMetadata {
    version: string;
}
```

**Impact:** Clear separation between programmatic identifiers (`name`) and human-readable labels (`title`).

## 6. New Feature: Elicitation (2025-06-18)

### 6.1 Protocol Addition

**Capability Declaration:**
```json
{
  "capabilities": {
    "elicitation": {}
  }
}
```

**Request Format:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "elicitation/create",
  "params": {
    "message": "Please provide your GitHub username",
    "requestedSchema": {
      "type": "object",
      "properties": {
        "name": {
          "type": "string"
        }
      },
      "required": ["name"]
    }
  }
}
```

**Security Requirements:**
- Servers MUST NOT request sensitive information
- Applications SHOULD provide clear UI indicators
- Users MUST have decline/cancel options

## 7. Tool System Evolution

### 7.1 Tool Annotations (2025-03-26)

Added comprehensive tool behavior descriptions:
- Read-only vs destructive operations
- Safety classifications
- Operational metadata

### 7.2 Structured Tool Output (2025-06-18)

**New Capability:**
- Tools can return structured content
- Enhanced result formatting
- Resource links in tool results

**Example Enhancement:**
```json
{
  "toolResult": {
    "content": [
      {
        "type": "text",
        "text": "Operation completed"
      },
      {
        "type": "resource",
        "resource": {
          "uri": "file:///path/to/result.txt",
          "name": "Result File"
        }
      }
    ]
  }
}
```

## 8. Lifecycle and Error Handling Changes

### 8.1 Lifecycle Requirements (2025-06-18)

**Breaking Change:**
- Changed lifecycle operation requirements from **SHOULD** to **MUST**
- Stricter initialization sequence enforcement

### 8.2 Security Documentation (2025-06-18)

**New Security Framework:**
- Added `security_best_practices.mdx`
- Enhanced authorization security considerations
- Clear trust and safety guidelines

## 9. Implementation Strategy for Multi-Version Support

### 9.1 Protocol Version Detection

```typescript
// Client capability negotiation
switch (serverProtocolVersion) {
    case "2024-11-05":
        // Use basic feature set
        break;
    case "2025-03-26":
        // Enable authorization + batching
        break;
    case "2025-06-18":
        // Enable elicitation, disable batching, enhanced auth
        break;
}
```

### 9.2 Feature Capability Matrix

| Feature | 2024-11-05 | 2025-03-26 | 2025-06-18 |
|---------|-------------|-------------|-------------|
| Basic JSON-RPC | ✅ | ✅ | ✅ |
| JSON-RPC Batching | ❌ | ✅ | ❌ |
| OAuth 2.1 Authorization | ❌ | ✅ | ✅ Enhanced |
| Tool Annotations | Basic | ✅ | ✅ Enhanced |
| Audio Content | ❌ | ✅ | ✅ |
| Completions | ❌ | ✅ | ✅ |
| Elicitation | ❌ | ❌ | ✅ |
| Structured Tool Output | ❌ | ❌ | ✅ |
| Resource Links | ❌ | ❌ | ✅ |
| BaseMetadata | ❌ | ❌ | ✅ |
| MCP-Protocol-Version Header | ❌ | ❌ | ✅ Required |

### 9.3 Backward Compatibility Considerations

**Critical Implementation Points:**

1. **JSON-RPC Batching:** 
   - Must detect and handle 2025-03-26 only
   - Parse both batch and individual messages for 2025-03-26
   - Reject batch requests for other versions

2. **Authorization:**
   - 2025-03-26: Basic OAuth 2.1 + Server Metadata
   - 2025-06-18: Add Protected Resource Metadata + WWW-Authenticate headers

3. **Content Handling:**
   - Gracefully degrade unsupported content types
   - Handle title/name field variations in metadata

4. **HTTP Headers:**
   - Add `MCP-Protocol-Version` header for 2025-06-18 HTTP requests
   - Maintain backward compatibility for older versions

## 10. Migration Path Recommendations

### 10.1 Server Implementation Priority

1. **Phase 1:** Implement 2024-11-05 base functionality
2. **Phase 2:** Add 2025-03-26 features (auth, batching, tool annotations)
3. **Phase 3:** Upgrade to 2025-06-18 (remove batching, add elicitation)

### 10.2 Client Implementation Strategy

```typescript
class MCPClient {
    private protocolVersion: string;
    private capabilities: Set<string> = new Set();
    
    async initialize(serverVersion: string) {
        this.protocolVersion = serverVersion;
        
        // Set capabilities based on version
        switch (serverVersion) {
            case "2024-11-05":
                this.capabilities.add("basic");
                break;
            case "2025-03-26":
                this.capabilities.add("basic");
                this.capabilities.add("batching");
                this.capabilities.add("authorization");
                this.capabilities.add("completions");
                break;
            case "2025-06-18":
                this.capabilities.add("basic");
                this.capabilities.add("authorization_enhanced");
                this.capabilities.add("elicitation");
                this.capabilities.add("structured_output");
                break;
        }
    }
    
    sendRequest(request: any) {
        if (this.protocolVersion === "2025-06-18") {
            // Add required header for HTTP transport
            request.headers = {
                ...request.headers,
                "MCP-Protocol-Version": this.protocolVersion
            };
        }
        // ... rest of implementation
    }
}
```

## 11. Schema References

### 11.1 File Locations
- **2024-11-05:** `spec/mcp-2024-11-05/schema.ts`
- **2025-03-26:** `spec/mcp-2025-03-26/schema.ts`  
- **2025-06-18:** `spec/mcp-2025-06-18/schema.ts`

### 11.2 Key Specification Documents
- **2025-03-26:** `spec/mcp-2025-03-26/changelog.mdx`
- **2025-06-18:** `spec/mcp-2025-06-18/changelog.mdx`
- **Authorization:** `spec/mcp-2025-03-26/basic/authorization.mdx`, `spec/mcp-2025-06-18/basic/authorization.mdx`
- **Elicitation:** `spec/mcp-2025-06-18/client/elicitation.mdx`
- **Security:** `spec/mcp-2025-06-18/basic/security_best_practices.mdx`

## Conclusion

The MCP specification has evolved significantly from 2024-11-05 to 2025-06-18, with the most substantial changes being:

1. **Authorization system introduction and enhancement**
2. **JSON-RPC batching addition and subsequent removal**
3. **Elicitation capability for interactive workflows**
4. **Enhanced security and metadata handling**

Implementations supporting all three versions must carefully handle feature detection, graceful degradation, and protocol-specific requirements to ensure compatibility across the entire specification evolution.