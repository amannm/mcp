Feature: MCP Lifecycle Conformance
  As an MCP implementation
  I want to ensure proper lifecycle management between McpHost and McpServer
  So that client-server connections follow the 2025-06-18 specification rigorously

  Background:
    Given a clean MCP environment
    And protocol version "2025-06-18" is supported
    And both McpHost and McpServer are available

  @core @initialization
  Scenario: Successful initialization handshake with complete capability exchange
    Given a McpServer with capabilities:
      | capability  | subcapability | enabled |
      | prompts     | listChanged   | true    |
      | resources   | subscribe     | true    |
      | resources   | listChanged   | true    |
      | tools       | listChanged   | true    |
      | logging     |               | true    |
      | completions |               | true    |
    And a McpHost with capabilities:
      | capability  | subcapability | enabled |
      | roots       | listChanged   | true    |
      | sampling    |               | true    |
      | elicitation |               | true    |
    When the McpHost initiates connection to McpServer
    And sends initialize request with:
      | field              | value           |
      | protocolVersion    | 2025-06-18      |
      | clientInfo.name    | TestClient      |
      | clientInfo.title   | Test Client App |
      | clientInfo.version | 1.0.0           |
    Then the McpServer should respond within 5 seconds with:
      | field              | value              |
      | protocolVersion    | 2025-06-18         |
      | serverInfo.name    | mcp-java           |
      | serverInfo.title   | MCP Java Reference |
      | serverInfo.version | 0.1.0              |
    And the response should include all negotiated server capabilities
    And the McpHost should send initialized notification
    And both parties should be in operational state
    And no protocol violations should be recorded

#  @core @version-negotiation
#  Scenario: Protocol version negotiation with matching versions
#    Given a McpServer supporting protocol version "2025-06-18"
#    And a McpHost requesting protocol version "2025-06-18"
#    When initialization is performed
#    Then both parties should agree on protocol version "2025-06-18"
#    And initialization should complete successfully
#
#  @core @version-negotiation
#  Scenario: Protocol version negotiation with server downgrade
#    Given a McpServer supporting protocol versions:
#      | version    |
#      | 2024-11-05 |
#      | 2025-06-18 |
#    And a McpHost requesting protocol version "2026-01-01"
#    When initialization is performed
#    Then the McpServer should respond with protocol version "2025-06-18"
#    And the McpHost should accept the downgrade
#    And initialization should complete successfully
#
#  @error-handling @version-negotiation
#  Scenario: Protocol version negotiation failure with incompatible versions
#    Given a McpServer supporting only protocol version "2024-11-05"
#    And a McpHost supporting only versions "2025-06-18" and newer
#    When the McpHost attempts initialization with version "2025-06-18"
#    Then the McpServer should respond with protocol version "2024-11-05"
#    And the McpHost should disconnect due to version incompatibility
#    And no further communication should occur
#
#  @error-handling @initialization
#  Scenario: Initialize request validation failures
#    Given an uninitialized McpHost and McpServer connection
#    When the McpHost sends initialize request missing required field:
#      | missing_field    |
#      | protocolVersion  |
#      | capabilities     |
#      | clientInfo       |
#      | clientInfo.name  |
#    Then the McpServer should respond with error code -32602
#    And error message should contain "Invalid params"
#    And the connection should remain uninitialized
#
  @sequencing @error-handling
  Scenario: Requests before initialization must fail
    Given an uninitialized connection between McpHost and McpServer
    When the McpHost sends request:
      | method         |
      | tools/list     |
      | prompts/list   |
      | resources/list |
    Then the McpServer should respond with error code -32002
    And error message should contain "Server not initialized"
    And the connection should remain uninitialized
#
#  @capabilities @negotiation
#  Scenario: Complete server capability negotiation
#    Given a McpServer declaring capabilities:
#      | capability  | subcapability | enabled |
#      | prompts     | listChanged   | true    |
#      | resources   | subscribe     | true    |
#      | resources   | listChanged   | true    |
#      | tools       | listChanged   | true    |
#      | logging     |               | true    |
#      | completions |               | true    |
#      | experimental| customFeature | true    |
#    When initialization completes successfully
#    Then the negotiated server capabilities should exactly match:
#      | capability  | subcapability |
#      | prompts     | listChanged   |
#      | resources   | subscribe     |
#      | resources   | listChanged   |
#      | tools       | listChanged   |
#      | logging     |               |
#      | completions |               |
#      | experimental| customFeature |
#
#  @capabilities @negotiation
#  Scenario: Complete client capability negotiation
#    Given a McpHost declaring capabilities:
#      | capability  | subcapability | enabled |
#      | roots       | listChanged   | true    |
#      | sampling    |               | true    |
#      | elicitation |               | true    |
#      | experimental| customFeature | true    |
#    When initialization completes successfully
#    Then the negotiated client capabilities should exactly match:
#      | capability  | subcapability |
#      | roots       | listChanged   |
#      | sampling    |               |
#      | elicitation |               |
#      | experimental| customFeature |

#  @sequencing @error-handling
#  Scenario: Server operations forbidden before initialized notification
#    Given the McpServer has responded to initialize request
#    But the McpHost has not sent initialized notification
#    When the McpServer attempts to send:
#      | method          |
#      | prompts/list    |
#      | resources/list  |
#      | tools/call      |
#      | sampling/create |
#    Then these requests should be rejected or deferred
#    And a protocol violation should be recorded
#
#  @sequencing @allowed-operations
#  Scenario: Allowed server operations before initialized notification
#    Given the McpServer has responded to initialize request
#    But the McpHost has not sent initialized notification
#    When the McpServer sends:
#      | method               |
#      | notifications/ping   |
#      | notifications/log    |
#    Then these should be accepted without protocol violation
#    And the connection should remain stable
#
#  @json-rpc @compliance
#  Scenario: JSON-RPC 2.0 message format strict compliance
#    Given an active connection during any lifecycle phase
#    When any message is transmitted
#    Then the message must have field "jsonrpc" with exact value "2.0"
#    And requests must have a valid "id" field
#    And notifications must not have an "id" field
#    And method names must follow specification format
#
#  @validation @initialization
#  Scenario: Initialize request complete field validation
#    When the McpHost sends an initialize request
#    Then the request must contain exactly:
#      | required_field              | type   |
#      | params.protocolVersion      | string |
#      | params.capabilities         | object |
#      | params.clientInfo           | object |
#      | params.clientInfo.name      | string |
#    And params.clientInfo may optionally contain:
#      | optional_field              | type   |
#      | params.clientInfo.title     | string |
#      | params.clientInfo.version   | string |
#
#  @validation @initialization
#  Scenario: Initialize response complete field validation
#    When the McpServer responds to initialize request
#    Then the response must contain exactly:
#      | required_field              | type   |
#      | result.protocolVersion      | string |
#      | result.capabilities         | object |
#      | result.serverInfo           | object |
#      | result.serverInfo.name      | string |
#    And result may optionally contain:
#      | optional_field              | type   |
#      | result.serverInfo.title     | string |
#      | result.serverInfo.version   | string |
#      | result.instructions         | string |
#
#  @transport @stdio @shutdown
#  Scenario: Graceful shutdown via stdio transport
#    Given an established McpHost-McpServer connection over stdio transport
#    And normal operations are proceeding
#    When the McpHost initiates shutdown by closing input stream to McpServer
#    Then the McpServer should detect EOF within 2 seconds
#    And the McpServer should exit gracefully within 5 seconds
#    And if McpServer doesn't exit within 10 seconds, SIGTERM should be effective
#    And if still unresponsive after 15 seconds, SIGKILL should terminate it
#
#  @transport @stdio @shutdown
#  Scenario: Server-initiated shutdown via stdio transport
#    Given an established McpHost-McpServer connection over stdio transport
#    When the McpServer closes its output stream and exits
#    Then the McpHost should detect connection termination within 2 seconds
#    And should handle the disconnection gracefully
#    And should not attempt to send further messages
#
#  @transport @http @shutdown
#  Scenario: HTTP transport connection termination
#    Given an established McpHost-McpServer connection over HTTP transport
#    When either party closes the HTTP connection
#    Then the other party should detect disconnection within 30 seconds
#    And should handle the disconnection gracefully
#    And should stop attempting further HTTP requests
#
#  @timeouts @cancellation
#  Scenario: Request timeout with cancellation notification
#    Given an established McpHost-McpServer connection
#    And request timeout is configured for 10 seconds
#    When the McpHost sends a tools/call request
#    And the McpServer does not respond within timeout period
#    Then the McpHost should send notifications/cancelled message
#    And should include the original request id
#    And should stop waiting for response
#    And should log the timeout event
#
#  @timeouts @progress
#  Scenario: Progress notifications extend timeout within limits
#    Given an established McpHost-McpServer connection
#    And request timeout is configured for 30 seconds
#    And maximum timeout is 120 seconds
#    When the McpHost sends a long-running tools/call request
#    And the McpServer sends progress notifications every 20 seconds
#    Then timeout should be extended with each progress notification
#    But should never exceed maximum timeout of 120 seconds
#    And request should eventually complete or timeout
#
#  @error-handling @version-negotiation
#  Scenario: Detailed unsupported protocol version error response
#    Given a McpServer supporting only versions "2024-11-05" and "2024-06-20"
#    When the McpHost requests unsupported version "1.0.0"
#    Then the McpServer should respond with error containing exactly:
#      | field                | value                              |
#      | code                | -32602                             |
#      | message             | Unsupported protocol version       |
#      | data.supported      | ["2024-11-05", "2024-06-20"]      |
#      | data.requested      | 1.0.0                              |
#
#  @sequencing @initialization
#  Scenario: Client operations restricted before initialization response
#    Given an uninitialized connection
#    When the McpHost sends initialize request
#    But before McpServer responds
#    Then McpHost should only be allowed to send:
#      | allowed_method       |
#      | notifications/ping   |
#    And any other requests should be queued or rejected
#    And should be processed after initialization completes
#
#  @sequencing @initialization
#  Scenario: Server operations restricted before initialized notification
#    Given the McpServer has responded to initialize request
#    But McpHost has not yet sent initialized notification
#    Then McpServer should only send:
#      | allowed_method       |
#      | notifications/ping   |
#      | notifications/log    |
#    And should defer any other operations until initialized notification received
#
#  @operation-phase @capability-respect
#  Scenario: Capability boundaries respected during operations
#    Given successful initialization with specific negotiated capabilities
#    And server capabilities include "tools" but not "prompts"
#    And client capabilities include "sampling" but not "roots"
#    When McpHost attempts to use non-negotiated server capability "prompts/list"
#    Then McpServer should respond with error code -32601
#    And error message should indicate "Method not found"
#    And connection should remain stable for valid operations
#
#  @operation-phase @version-consistency
#  Scenario: Protocol version consistency throughout session
#    Given successful initialization with protocol version "2025-06-18"
#    When any message is exchanged during operation phase
#    Then message format should conform exactly to "2025-06-18" specification
#    And should not use deprecated features from older versions
#    And should not use preview features from newer versions
#
#  @transport @http @authorization
#  Scenario: HTTP transport with JWT authorization lifecycle
#    Given a McpServer configured with JWT authorization
#    And expected audience "test-client"
#    And a valid JWT token for McpHost
#    When McpHost establishes HTTP connection with Authorization header
#    And performs initialize sequence
#    Then authorization should be validated before initialization
#    And initialization should proceed normally after auth success
#    And subsequent requests should maintain authorization context
#
#  @transport @http @authorization @error-handling
#  Scenario: HTTP transport authorization failure handling
#    Given a McpServer configured with JWT authorization
#    And expected audience "test-client"
#    When McpHost attempts connection with invalid JWT token
#    Then HTTP request should return 401 Unauthorized
#    And initialization should not proceed
#    And no MCP messages should be processed
#
#  @interactive @cli-integration
#  Scenario: McpHost interactive mode lifecycle integration
#    Given a McpHost started in interactive CLI mode
#    And connected to test McpServer
#    When user issues "clients" command
#    Then should display active client connections
#    When user issues "context" command
#    Then should display aggregated server context
#    And all operations should respect initialized connection state
#
#  @stress-testing @multiple-connections
#  Scenario: Multiple concurrent client connections lifecycle
#    Given a McpServer capable of handling multiple connections
#    When 5 McpHost instances connect simultaneously
#    And each performs initialization sequence
#    Then all initializations should complete successfully within 30 seconds
#    And each connection should maintain independent state
#    And capabilities should be negotiated per-connection
#    And shutdown of one connection should not affect others
#
#  @error-recovery @connection-resilience
#  Scenario: Connection recovery after initialization failure
#    Given a McpHost that experienced initialization failure
#    When McpHost attempts reconnection with corrected parameters
#    Then new connection should be established cleanly
#    And initialization should proceed normally
#    And previous failed connection state should not interfere
#
#  @edge-cases @malformed-messages
#  Scenario: Malformed initialize request handling
#    Given an uninitialized connection
#    When McpHost sends malformed JSON in initialize request
#    Then McpServer should respond with JSON-RPC parse error -32700
#    And connection should remain available for retry
#    And should not crash or become unresponsive
#
#  @edge-cases @concurrent-initialization
#  Scenario: Concurrent initialization attempts prevention
#    Given an uninitialized connection
#    When McpHost sends multiple initialize requests simultaneously
#    Then McpServer should process only the first initialize request
#    And should respond with error -32600 "Invalid Request" to subsequent attempts
#    And should complete initialization normally with first request
#
#  @security @instruction-handling
#  Scenario: Server instructions handling during initialization
#    Given a McpServer configured with custom instructions
#    When initialization completes successfully
#    Then initialize response should include instructions field
#    And McpHost should be able to access and process instructions
#    And instructions should not affect protocol compliance
#
#  @performance @initialization-timing
#  Scenario: Initialization performance requirements
#    Given optimal network conditions
#    When McpHost initiates connection to McpServer
#    Then initialize request should be sent within 100ms of connection
#    And McpServer should respond within 1 second
#    And initialized notification should be sent within 100ms of response
#    And total initialization should complete within 2 seconds
#
#  @cleanup @resource-management
#  Scenario: Proper resource cleanup on lifecycle completion
#    Given an established McpHost-McpServer connection
#    When connection goes through complete lifecycle including shutdown
#    Then all file handles should be closed
#    And all threads should be terminated
#    And no memory leaks should be detected
#    And system resources should be fully released