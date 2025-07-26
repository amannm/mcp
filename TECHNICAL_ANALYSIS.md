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

| Change            | 2024-11-05      | 2025-03-26      | 2025-06-18           |
|-------------------|-----------------|-----------------|----------------------|
| JSON-RPC Batching | ❌ Not supported | ✅ Supported     | ❌ Removed            |
| Authorization     | ❌ Not defined   | ✅ OAuth 2.1     | ✅ Enhanced OAuth 2.1 |
| Elicitation       | ❌ Not supported | ❌ Not supported | ✅ Supported          |
| Tool Annotations  | ❌ Basic         | ✅ Comprehensive | ✅ Enhanced           |

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

### 1.3 Widespread _meta Field Addition (2025-06-18)

**Major Infrastructure Enhancement:**

2025-06-18 added the `_meta?: { [key: string]: unknown }` field to **10+ core interfaces**:

**Content Interfaces:**

- `TextContent`, `ImageContent`, `AudioContent`
- `EmbeddedResource`

**Protocol Entity Interfaces:**

- `Resource`, `ResourceTemplate`, `ResourceContents`
- `Prompt`, `Tool`, `Root`

**Impact:** This enables extensible metadata attachment across the entire protocol surface, requiring parsers to handle optional metadata on all major types.

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

### 2.3 Completion Request Enhancement (2025-06-18)

**Context Field Addition:**

2025-06-18 enhanced `CompleteRequest` with context information and interface renaming:

```typescript
export interface CompleteRequest extends Request {
    method: "completion/complete";
    params: {
        ref: PromptReference | ResourceTemplateReference;  // RENAMED from ResourceReference
        argument: {
            name: string;
            value: string;
        };
        context?: {                    // NEW in 2025-06-18
            arguments?: { [key: string]: string };
        };
    };
}
```

**Breaking Changes in 2025-06-18:**

1. **Interface Rename:** `ResourceReference` was renamed to `ResourceTemplateReference`
2. **Context Field Addition:** New optional context field for completion requests

**Documentation Inconsistency:** The 2025-06-18 specification documentation at `spec/mcp-2025-06-18/server/utilities/completion.mdx:161` still references `ResourceReference` while the schema correctly uses `ResourceTemplateReference`.

**Usage:** Enables completion requests to include previously-resolved variables for better context-aware suggestions.

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
- Added optional `MCP-Protocol-Version` header for Server Metadata Discovery

**2025-06-18:**

- Made `MCP-Protocol-Version` header mandatory for all HTTP requests
- MUST specify negotiated protocol version in subsequent HTTP requests
- Enhanced from optional (2025-03-26) to required (2025-06-18)

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
- **Breaking Change:** Enhanced ProgressNotification with message field (affects parsing)

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

### 5.3 BaseMetadata Extension Evolution

**2025-06-18 Introduced Widespread BaseMetadata Extension:**

Multiple core interfaces now extend `BaseMetadata` to provide consistent naming conventions:

```typescript
// Core interfaces extending BaseMetadata in 2025-06-18:
export interface Tool extends BaseMetadata { /* ... */
}

export interface Resource extends BaseMetadata { /* ... */
}

export interface ResourceTemplate extends BaseMetadata { /* ... */
}

export interface Prompt extends BaseMetadata { /* ... */
}

export interface PromptArgument extends BaseMetadata { /* ... */
}

export interface PromptReference extends BaseMetadata { /* ... */
}

export interface Implementation extends BaseMetadata { /* ... */
}
```

**Migration Impact:** This architectural change standardizes how display names are handled across all major protocol entities, affecting UI implementations significantly.

### 5.4 ContentBlock Union Type Introduction (2025-06-18)

**Major Content System Refactor:**

2025-06-18 introduced a unified `ContentBlock` union type to consolidate content handling:

```typescript
export type ContentBlock =
    | TextContent
    | ImageContent
    | AudioContent
    | ResourceLink      // NEW in 2025-06-18
    | EmbeddedResource;
```

**Usage Changes:**

- `PromptMessage.content`: Now uses `ContentBlock` instead of individual content types
- `CallToolResult.content`: Now uses `ContentBlock[]` for unified content handling

**Breaking Change Impact:** Existing content parsing logic must be updated to handle the new union type and `ResourceLink` content type.

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
      "required": [
        "name"
      ]
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

### 7.2 Annotations Interface Evolution

**Interface Rename and Enhancement:**

**2024-11-05:** Used `Annotated` interface

```typescript
export interface Annotated {
    annotations?: {
        audience?: Role[];
        priority?: number;
    }
}
```

**2025-03-26:** Renamed to `Annotations` interface

```typescript
export interface Annotations {
    audience?: Role[];
    priority?: number;
}
```

**2025-06-18:** Enhanced `Annotations` interface with new field

```typescript
export interface Annotations {
    audience?: Role[];
    priority?: number;
    lastModified?: string;  // NEW: ISO 8601 timestamp
}
```

**Breaking Changes:**

- **2025-03-26:** Interfaces extending `Annotated` now use `Annotations` directly, requiring code updates
- **2025-06-18:** Added optional `lastModified` field for timestamp tracking

### 7.3 Structured Tool Output (2025-06-18)

**New Capabilities:**

- Tools can return structured content via `structuredContent` field
- Enhanced result formatting with `ContentBlock[]`
- Resource links in tool results
- **Tool Output Schema Definition**: New `outputSchema` field in `Tool` interface

**Tool Interface Enhancement:**

```typescript
export interface Tool extends BaseMetadata {
    // ... existing fields
    outputSchema?: {          // NEW in 2025-06-18
        type: "object";
        properties?: { [key: string]: object };
        required?: string[];
    };
}
```

**CallToolResult Enhancement:**

```typescript
export interface CallToolResult extends Result {
    content: ContentBlock[];
    structuredContent?: { [key: string]: unknown };  // NEW structured output
    isError?: boolean;
}
```

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
- **Impact:** Previously optional initialization behaviors are now mandatory
- **Risk:** Existing implementations may fail if they relied on optional lifecycle steps
- Stricter initialization sequence enforcement

### 8.2 Security Documentation (2025-06-18)

**New Security Framework:**

- Added comprehensive `spec/mcp-2025-06-18/basic/security_best_practices.mdx`
- Enhanced authorization security considerations
- Clear trust and safety guidelines
- Mandatory security requirements for production deployments

**Critical Security References:**

- **Authorization:** `spec/mcp-2025-06-18/basic/authorization.mdx`
- **Security Best Practices:** `spec/mcp-2025-06-18/basic/security_best_practices.mdx`
- **Elicitation Security:** `spec/mcp-2025-06-18/client/elicitation.mdx` (Security Considerations section)

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

| Feature                                          | 2024-11-05        | 2025-03-26        | 2025-06-18    |
|--------------------------------------------------|-------------------|-------------------|---------------|
| Basic JSON-RPC                                   | ✅                 | ✅                 | ✅             |
| JSON-RPC Batching                                | ❌                 | ✅                 | ❌ **REMOVED** |
| OAuth 2.1 Authorization                          | ❌                 | ✅                 | ✅ Enhanced    |
| Tool Annotations                                 | Basic             | ✅                 | ✅ Enhanced    |
| Audio Content                                    | ❌                 | ✅                 | ✅             |
| Completions                                      | ❌                 | ✅                 | ✅ Enhanced    |
| Elicitation                                      | ❌                 | ❌                 | ✅             |
| Structured Tool Output                           | ❌                 | ❌                 | ✅             |
| Resource Links                                   | ❌                 | ❌                 | ✅             |
| BaseMetadata Extensions                          | ❌                 | ❌                 | ✅             |
| ContentBlock Union Type                          | ❌                 | ❌                 | ✅             |
| Widespread _meta Fields                          | ❌                 | ❌                 | ✅             |
| Tool Output Schema                               | ❌                 | ❌                 | ✅             |
| Completion Context                               | ❌                 | ❌                 | ✅             |
| ResourceTemplateReference (vs ResourceReference) | ResourceReference | ResourceReference | ✅ **RENAMED** |
| Annotations Interface (vs Annotated)             | ❌                 | ✅                 | ✅ Enhanced    |
| MCP-Protocol-Version Header                      | ❌                 | ✅ Optional        | ✅ Required    |
| Lifecycle MUST Requirements                      | ❌                 | ❌                 | ✅ Breaking    |

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

4. **Interface Renaming (2025-06-18):**
    - `ResourceReference` renamed to `ResourceTemplateReference`
    - Update completion request parsing logic for 2025-06-18
    - Note: Documentation inconsistency exists in spec files

5. **HTTP Headers:**
    - Add `MCP-Protocol-Version` header for 2025-06-18 HTTP requests
    - Maintain backward compatibility for older versions

## 10. Migration Path Recommendations

### 10.1 Server Implementation Strategy

**⚠️ Critical Implementation Notes:**

1. Due to JSON-RPC batching being added in 2025-03-26 and **removed** in 2025-06-18, sequential implementation is not optimal.
2. **Documentation Inconsistency in 2025-06-18:** The completion documentation still references `ResourceReference` while the schema uses `ResourceTemplateReference`. Always follow the schema definitions for implementation.

**Recommended Branching Strategy:**

1. **Base Implementation:** Start with 2024-11-05 core functionality
2. **Branch A - Conservative Path:** 2024-11-05 → 2025-06-18 (skip batching entirely)
3. **Branch B - Full Feature Path:** 2024-11-05 → 2025-03-26 → 2025-06-18 (implement then remove batching)

**Feature-Based Implementation:**

- **Core Features:** Implement across all versions (tools, resources, prompts)
- **Authorization:** 2025-03-26+ (evolving in 2025-06-18)
- **Batching:** 2025-03-26 only (removed in 2025-06-18)
- **Elicitation:** 2025-06-18 only
- **Structured Output:** 2025-06-18 only

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
- **Completion (with documentation inconsistency):** `spec/mcp-2025-06-18/server/utilities/completion.mdx`

## Conclusion

The MCP specification has evolved significantly from 2024-11-05 to 2025-06-18, with the most substantial changes being:

1. **Authorization system introduction and enhancement**
2. **JSON-RPC batching addition and subsequent removal**
3. **Elicitation capability for interactive workflows**
4. **Enhanced security and metadata handling**
5. **Interface renamings (ResourceReference → ResourceTemplateReference)**

**Important Implementation Note:** A documentation inconsistency exists in the 2025-06-18 specification where completion documentation references the old `ResourceReference` interface name while the schema correctly uses `ResourceTemplateReference`. Implementers should follow schema definitions rather than documentation when conflicts exist.

Implementations supporting all three versions must carefully handle feature detection, graceful degradation, and protocol-specific requirements to ensure compatibility across the entire specification evolution.