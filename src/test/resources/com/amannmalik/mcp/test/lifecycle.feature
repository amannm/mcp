Feature: MCP Lifecycle Conformance
  As an MCP implementation
  I want to ensure proper lifecycle management between McpHost and McpServer
  So that client-server connections follow the 2025-06-18 specification

  Background:
    Given a clean MCP environment
    And protocol version "2025-06-18" is supported
    And both McpHost and McpServer are available

  @core @initialization
  # SPEC: lifecycle.mdx:42-125 - Complete initialization handshake sequence
  Scenario: Standard initialization handshake
    Given a McpServer with capabilities:
      | capability | subcapability | enabled |
      | prompts    | listChanged   | true    |
      | resources  | subscribe     | true    |
      | tools      | listChanged   | true    |
      | logging    |               | true    |
    And a McpHost with capabilities:
      | capability | subcapability | enabled |
      | roots      | listChanged   | true    |
      | sampling   |               | true    |
    When the McpHost sends initialize request with:
      | field              | value      |
      | protocolVersion    | 2025-06-18 |
      | clientInfo.name    | TestClient |
      | clientInfo.version | 1.0.0      |
    Then the McpServer should respond with:
      | field              | value      |
      | protocolVersion    | 2025-06-18 |
      | serverInfo.name    | mcp-java   |
      | serverInfo.version | 0.1.0      |
    And the response should include all declared server capabilities
    And the McpHost should send initialized notification
    And both parties should be in operational state

  @core @version-negotiation  
  # SPEC: lifecycle.mdx:129-137 - Version negotiation behavior
  Scenario: Protocol version negotiation with server downgrade
    Given a McpServer supporting protocol versions:
      | version    |
      | 2024-11-05 |
      | 2025-06-18 |
    And a McpHost requesting protocol version "2026-01-01"
    When initialization is performed
    Then the McpServer should respond with protocol version "2025-06-18"
    And the McpHost should accept the negotiated version
    And initialization should complete successfully

  @error-handling @version-negotiation
  # SPEC: lifecycle.mdx:136-137 - Client disconnection on version incompatibility
  Scenario: Protocol version negotiation failure
    Given a McpServer supporting only protocol version "2024-11-05"
    And a McpHost supporting only version "2025-06-18"
    When the McpHost attempts initialization
    Then the McpServer should respond with protocol version "2024-11-05"
    And the McpHost should disconnect due to version incompatibility

  @validation @initialization
  # SPEC: schema.ts:171-180,314-315 - InitializeRequest field requirements
  Scenario: Initialize request validation
    Given an uninitialized connection
    When the McpHost sends initialize request missing:
      | missing_field   |
      | protocolVersion |
      | capabilities    |
      | clientInfo      |
      | clientInfo.name |
    Then the McpServer should respond with error code -32602
    And error message should contain "Invalid params"

  @sequencing @initialization
  # SPEC: lifecycle.mdx:119-121 - Client request restrictions before initialization
  Scenario: Requests before initialization are restricted
    Given an uninitialized connection
    When the McpHost sends request:
      | method         |
      | tools/list     |
      | prompts/list   |
      | resources/list |
    Then the McpServer should respond with appropriate error
    And the connection should remain uninitialized

  @capabilities @negotiation
  # SPEC: lifecycle.mdx:146-149, schema.ts:243-287 - Capability negotiation
  Scenario: Server capability negotiation
    Given a McpServer declaring capabilities:
      | capability | subcapability | enabled |
      | prompts    | listChanged   | true    |
      | resources  | subscribe     | true    |
      | tools      | listChanged   | true    |
      | logging    |               | true    |
    When initialization completes successfully
    Then the negotiated server capabilities should exactly match declared capabilities

  @capabilities @negotiation  
  # SPEC: schema.ts:216-238 - ClientCapabilities definition
  Scenario: Client capability negotiation
    Given a McpHost declaring capabilities:
      | capability  | subcapability | enabled |
      | roots       | listChanged   | true    |
      | sampling    |               | true    |
      | elicitation |               | true    |
    When initialization completes successfully
    Then the negotiated client capabilities should exactly match declared capabilities

  @sequencing @operational
  # SPEC: lifecycle.mdx:122-125 - Server request restrictions before initialized notification
  Scenario: Server operations restricted before initialized notification
    Given the McpServer has responded to initialize request
    But the McpHost has not sent initialized notification
    Then the McpServer should only send:
      | allowed_method |
      | ping           |
      | logging        |
    And should defer other operations until initialized notification received

  @json-rpc @compliance
  # SPEC: basic/index.mdx:29-31,48-49,95 - JSON-RPC 2.0 message format requirements
  Scenario: JSON-RPC 2.0 message format compliance
    Given an active connection during any lifecycle phase
    When any message is transmitted
    Then requests must have field "jsonrpc" with value "2.0"
    And requests must have valid "id" field
    And notifications must not have "id" field
    And method names must follow specification format

  @validation @message-format
  # SPEC: schema.ts:171-202 - Initialize request/response structure
  Scenario: Initialize message structure validation
    When the McpHost sends initialize request
    Then the request must contain exactly:
      | required_field         | type   |
      | params.protocolVersion | string |
      | params.capabilities    | object |
      | params.clientInfo      | object |
      | params.clientInfo.name | string |
    When the McpServer responds to initialize request
    Then the response must contain exactly:
      | required_field         | type   |
      | result.protocolVersion | string |
      | result.capabilities    | object |
      | result.serverInfo      | object |
      | result.serverInfo.name | string |

  @operation-phase @capability-enforcement
  # SPEC: lifecycle.mdx:177-180 - Capability boundary enforcement
  Scenario: Capability boundaries respected during operations
    Given successful initialization with negotiated capabilities
    And server capabilities include "tools" but not "prompts"
    When McpHost attempts to use non-negotiated capability "prompts/list"
    Then McpServer should respond with error code -32601
    And error message should indicate "Method not found"

  @operation-phase @version-consistency
  # SPEC: lifecycle.mdx:179 - Protocol version consistency
  Scenario: Protocol version consistency throughout session
    Given successful initialization with protocol version "2025-06-18"
    When any message is exchanged during operation phase
    Then message format should conform exactly to "2025-06-18" specification

  @error-handling @malformed-messages
  # SPEC: schema.ts:98, basic/index.mdx:29-31 - Malformed message handling
  Scenario: Malformed message handling
    Given an active connection
    When malformed JSON is transmitted
    Then the receiver should respond with JSON-RPC parse error -32700
    And connection should remain stable for retry

  @transport @stdio @shutdown
  # SPEC: transports.mdx:190-197 - stdio shutdown procedure
  Scenario: Graceful shutdown via stdio transport
    Given an established connection over stdio transport
    When the McpHost closes input stream to McpServer
    Then the McpServer should detect EOF and exit gracefully
    And proper cleanup should occur

  @security @instructions
  # SPEC: schema.ts:196-201 - Optional instructions field handling
  Scenario: Server instructions handling during initialization
    Given a McpServer configured with initialization instructions
    When initialization completes successfully
    Then initialize response may include instructions field
    And instructions should not affect protocol compliance

  @utilities @ping
  # SPEC: ping.mdx - Ping request/response validation and bidirectional support
  Scenario: Ping utility during operational phase
    Given an established connection in operational state
    When either party sends ping request
    Then the receiver should respond promptly with empty response {}
    And connection should remain active

  @utilities @ping @initialization
  # SPEC: ping.mdx - Ping allowed during restricted phases
  Scenario: Ping allowed before initialization completion
    Given an uninitialized connection
    When either party sends ping request
    Then ping should be processed normally
    And should not violate initialization sequence restrictions

  @utilities @ping @timeout
  # SPEC: ping.mdx - Connection considered stale without ping response
  Scenario: Ping timeout handling for connection liveness
    Given an established connection
    When ping request is sent but no response received within timeout
    Then connection should be considered stale
    And appropriate reconnection or cleanup should occur

  @utilities @progress
  # SPEC: progress.mdx - Progress token handling for long-running requests
  Scenario: Progress notifications with token tracking
    Given an established connection
    When request is sent with _meta.progressToken "task-123"
    Then server should send progress notifications with matching token
    And progress values should be increasing
    And notifications should stop after request completion

  @utilities @progress @validation
  # SPEC: progress.mdx - Progress token uniqueness and structure
  Scenario: Progress token validation and uniqueness
    Given multiple concurrent requests with progress tokens
    When progress tokens "task-1" and "task-2" are used
    Then each progress notification should match correct token
    And tokens should remain unique across active requests
    And progress notifications should include optional total and message fields

  @utilities @cancellation
  # SPEC: cancellation.mdx - Cancel request validation and restrictions
  Scenario: Request cancellation validation
    Given an in-progress request with id "req-123"
    When notifications/cancelled is sent with requestId "req-123"
    Then server should stop processing request "req-123"
    And should free associated resources
    And should not send response for cancelled request

  @utilities @cancellation @restrictions
  # SPEC: cancellation.mdx - Initialize requests cannot be cancelled
  Scenario: Initialize request cancellation prohibition
    Given an initialize request in progress
    When notifications/cancelled is sent for initialize request
    Then cancellation should be ignored or rejected
    And initialization should continue normally

  @utilities @cancellation @timing
  # SPEC: cancellation.mdx - Handle cancellation after completion gracefully
  Scenario: Cancellation after request completion
    Given a completed request with id "req-456"
    When notifications/cancelled is sent with requestId "req-456"
    Then cancellation should be handled gracefully
    And should not cause errors or connection issues

  @transport @http @headers
  # SPEC: transports.mdx - HTTP protocol version header requirements
  Scenario: HTTP transport protocol version header
    Given an HTTP transport connection
    When any HTTP request is sent
    Then MCP-Protocol-Version header must be included
    And header value should match negotiated protocol version

  @transport @http @session
  # SPEC: transports.mdx - HTTP session management
  Scenario: HTTP session ID handling
    Given an HTTP transport with session support
    When connection is established
    Then Mcp-Session-Id header may be provided
    And session ID should be cryptographically secure
    And session ID should be non-sequential

  @transport @http @sse
  # SPEC: transports.mdx - Server-sent events bidirectional communication
  Scenario: SSE stream handling for bidirectional communication
    Given an HTTP transport using SSE
    When server needs to send requests to client
    Then SSE stream should be used for server-to-client messages
    And client responses should be sent via HTTP requests
    And stream should handle connection persistence

  @transport @http @resumable
  # SPEC: transports.mdx - SSE stream resumption after disconnection
  Scenario: Resumable SSE streams with Last-Event-ID
    Given an established SSE connection that gets interrupted
    When client reconnects with Last-Event-ID header
    Then server should resume from last processed event
    And missed events should be replayed if available
    And connection should continue normally

  @json-rpc @id-uniqueness
  # SPEC: basic/index.mdx:48-49 - Request ID uniqueness within session
  Scenario: Request ID uniqueness enforcement
    Given an active session
    When multiple requests are sent with same ID "duplicate-123"
    Then second request should be rejected as invalid
    And error should indicate duplicate ID usage

  @json-rpc @id-restrictions
  # SPEC: basic/index.mdx:48-49 - Request IDs must not be null
  Scenario: Request ID null value prohibition
    Given an active connection
    When request is sent with id field set to null
    Then request should be rejected with invalid request error -32600
    And connection should remain stable

  @json-rpc @metadata
  # SPEC: schema.ts - Metadata field structure validation
  Scenario: Message metadata field validation
    Given any message with _meta field
    Then _meta field should follow specified structure
    And reserved keys should be validated correctly
    And custom metadata should be preserved

  @json-rpc @error-codes
  # SPEC: schema.ts:98-106 - Standard JSON-RPC error code validation
  Scenario: Standard error code compliance
    Given various error conditions occur
    Then appropriate standard error codes should be returned:
      | condition          | code   | message          |
      | malformed JSON     | -32700 | Parse error      |
      | invalid request    | -32600 | Invalid Request  |
      | method not found   | -32601 | Method not found |
      | invalid parameters | -32602 | Invalid params   |
      | internal error     | -32603 | Internal error   |

  @authorization @bearer-token
  # SPEC: authorization.mdx:236-249 - Bearer token validation in Authorization header
  Scenario: HTTP authorization with Bearer token
    Given an HTTP connection with JWT authorization
    When request includes Authorization: Bearer <valid-token>
    Then token should be validated before processing request
    And token audience should match server identifier
    And request should proceed normally after validation

  @authorization @error-handling
  # SPEC: authorization.mdx:89-92,269-270 - 401 response with WWW-Authenticate header
  Scenario: Authorization failure handling
    Given an HTTP connection requiring authorization
    When request has invalid or expired token
    Then response should be HTTP 401 Unauthorized
    And WWW-Authenticate header should be included
    And header should contain protected resource metadata

  @authorization @audience-validation
  # SPEC: authorization.mdx - Token audience must be server-specific
  Scenario: JWT token audience validation
    Given a JWT token with audience "wrong-server"
    When token is presented to server expecting audience "correct-server"
    Then token should be rejected due to audience mismatch
    And 401 Unauthorized should be returned

  @security @token-restrictions
  # SPEC: security_best_practices.mdx - Token passthrough prohibition
  Scenario: Upstream token passthrough prohibition
    Given a server receiving upstream authorization token
    When making downstream requests
    Then upstream token must not be passed through
    And separate authorization should be used for downstream calls

  @security @session-security
  # SPEC: security_best_practices.mdx - Session ID cryptographic security
  Scenario: Session identifier security requirements
    Given session-based transport
    When session IDs are generated
    Then IDs should be cryptographically secure
    And IDs should be non-sequential
    And IDs should have sufficient entropy to prevent guessing

  @initialization @complex-capabilities
  # SPEC: lifecycle.mdx:146-149 - Complex capability negotiation scenarios
  Scenario: Complex multi-level capability negotiation
    Given a server with nested capabilities:
      | capability | subcapability | enabled |
      | resources  | subscribe     | true    |
      | resources  | listChanged   | true    |
      | tools      | listChanged   | false   |
    When initialization completes
    Then negotiated capabilities should exactly match server declarations
    And capability structure should be preserved

  @initialization @multiple-versions
  # SPEC: lifecycle.mdx:129-137 - Multiple version support edge cases
  Scenario: Server supporting multiple protocol versions
    Given a server supporting versions:
      | version    | status     |
      | 2024-11-05 | deprecated |
      | 2025-06-18 | current    |
    When client requests unsupported version "2023-01-01"
    Then server should respond with highest stable version "2025-06-18"
    And should not offer deprecated versions

  @initialization @instruction-processing
  # SPEC: lifecycle.mdx:104, schema.ts:196-201 - Server instruction field handling
  Scenario: Server initialization instructions processing
    Given a server with initialization instructions "Use tools carefully"
    When initialization completes successfully
    Then response should include instructions field
    And client should be able to access instructions
    And instructions should be treated as advisory only

  @error-recovery @connection-resilience
  # SPEC: lifecycle.mdx - Fresh initialization after connection failure
  Scenario: Connection recovery after transport failure
    Given an established connection that fails
    When client attempts reconnection
    Then new connection should start fresh
    And initialization should be performed again
    And previous connection state should not interfere

  @error-recovery @partial-initialization
  # SPEC: lifecycle.mdx:42-125 - Rollback from failed initialization
  Scenario: Partial initialization failure recovery
    Given initialization sequence in progress
    When initialization fails after server response but before initialized notification
    Then connection should return to uninitialized state
    And subsequent initialization attempt should start cleanly
    And no partial state should remain

  @shutdown @resource-cleanup
  # SPEC: transports.mdx:189-204 - Resource cleanup during shutdown
  Scenario: Proper resource cleanup on connection shutdown
    Given an active connection with ongoing operations
    When shutdown is initiated via transport-specific method
    Then all pending requests should be cancelled or completed
    And system resources should be properly released
    And no resource leaks should occur

  @performance @initialization-timeout
  # SPEC: lifecycle.mdx:208-212 - Request timeout with cancellation
  Scenario: Initialization request timeout handling
    Given initialization request timeout configured
    When initialize request exceeds timeout period
    Then timeout handler should issue cancellation notification
    And connection should be terminated cleanly
    And client should be able to retry initialization