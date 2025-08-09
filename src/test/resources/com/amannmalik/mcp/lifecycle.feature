Feature: MCP Lifecycle Conformance
  As an MCP implementation
  I want to ensure proper lifecycle management
  So that client-server connections follow the specification

  Background:
    Given a clean MCP environment
    And protocol version "2025-06-18" is supported

  Scenario: Successful initialization handshake
    Given a client with capabilities:
      | capability   | subcapability | value |
      | roots        | listChanged   | true  |
      | sampling     |               |       |
      | elicitation  |               |       |
    When the client sends an initialize request with:
      | field           | value                    |
      | protocolVersion | 2025-06-18              |
      | clientInfo.name | TestClient               |
      | clientInfo.version | 1.0.0                |
    Then the server should respond with:
      | field           | value                    |
      | protocolVersion | 2025-06-18              |
      | serverInfo.name | TestServer               |
    And the response should include server capabilities
    And the client should send an initialized notification
    And the connection should be in operational state

  Scenario: Protocol version negotiation - matching versions
    Given a server supporting protocol version "2025-06-18"
    When the client requests protocol version "2025-06-18"
    Then the server should respond with protocol version "2025-06-18"
    And initialization should succeed

  Scenario: Protocol version negotiation - server downgrades
    Given a server supporting protocol versions:
      | version    |
      | 2024-11-05 |
      | 2025-06-18 |
    When the client requests protocol version "2026-01-01"
    Then the server should respond with protocol version "2025-06-18"
    And the client should accept the downgrade

  Scenario: Protocol version negotiation - incompatible versions
    Given a server supporting only protocol version "2024-11-05"
    When the client requests protocol version "2026-01-01"
    And the client only supports versions newer than "2025-01-01"
    Then the server should respond with protocol version "2024-11-05"
    And the client should disconnect due to version incompatibility

  Scenario: Initialize request validation
    When the client sends an initialize request missing:
      | field           |
      | protocolVersion |
    Then the server should respond with error code -32602
    And the error message should contain "Invalid params"

  Scenario: Initialize request must be first interaction
    Given an uninitialized connection
    When the client sends a tools/list request before initialize
    Then the server should respond with error code -32002
    And the error message should contain "Server not initialized"

  Scenario: Server capabilities negotiation
    Given a server with capabilities:
      | capability  | subcapability | value |
      | prompts     | listChanged   | true  |
      | resources   | subscribe     | true  |
      | resources   | listChanged   | true  |
      | tools       | listChanged   | true  |
      | logging     |               |       |
    When initialization completes successfully
    Then the negotiated server capabilities should include:
      | capability  | subcapability |
      | prompts     | listChanged   |
      | resources   | subscribe     |
      | resources   | listChanged   |
      | tools       | listChanged   |
      | logging     |               |

  Scenario: Client capabilities negotiation
    Given a client with capabilities:
      | capability   | subcapability | value |
      | roots        | listChanged   | true  |
      | sampling     |               |       |
      | elicitation  |               |       |
    When initialization completes successfully
    Then the negotiated client capabilities should include:
      | capability   | subcapability |
      | roots        | listChanged   |
      | sampling     |               |
      | elicitation  |               |

  Scenario: Initialized notification is required
    Given the server has responded to initialize request
    When the client does not send initialized notification
    And the client attempts to send a tools/list request
    Then the server should respond with error code -32002
    And the error message should contain "Client not initialized"

  Scenario: Server operations before initialized notification
    Given the server has responded to initialize request
    But the client has not sent initialized notification
    When the server sends a prompts/list request
    Then this should result in a protocol violation
    Except when the server sends:
      | allowed_method           |
      | notifications/ping       |
      | notifications/log        |

  Scenario: JSON-RPC 2.0 message format compliance
    When any message is sent during initialization
    Then the message should have field "jsonrpc" with value "2.0"
    And requests should have an "id" field
    And notifications should not have an "id" field

  Scenario: Initialize request required fields
    When the client sends an initialize request
    Then the request should contain:
      | required_field           |
      | params.protocolVersion   |
      | params.capabilities      |
      | params.clientInfo        |
    And clientInfo should contain:
      | required_field    |
      | name              |

  Scenario: Initialize response required fields
    When the server responds to initialize request
    Then the response should contain:
      | required_field           |
      | result.protocolVersion   |
      | result.capabilities      |
      | result.serverInfo        |
    And serverInfo should contain:
      | required_field    |
      | name              |

  Scenario: Graceful shutdown via stdio transport
    Given an established MCP connection over stdio
    When the client closes the input stream to server
    Then the server should exit gracefully
    And if server doesn't exit within reasonable time
    Then SIGTERM should be sent
    And if server still doesn't exit
    Then SIGKILL should be sent

  Scenario: Server-initiated shutdown via stdio
    Given an established MCP connection over stdio
    When the server closes its output stream
    And the server exits
    Then the client should detect connection termination

  Scenario: Request timeout handling
    Given an established MCP connection
    When the client sends a tools/call request
    And the server does not respond within timeout period
    Then the client should send a notifications/cancelled message
    And the client should stop waiting for response

  Scenario: Progress notification resets timeout
    Given an established MCP connection with 30-second timeout
    When the client sends a tools/call request
    And the server sends progress notifications every 20 seconds
    Then the timeout should be extended
    But should still enforce maximum timeout limit

  Scenario: Unsupported protocol version error format
    Given a server supporting only "2024-11-05"
    When the client requests unsupported version "1.0.0"
    Then the server should respond with error:
      | field        | value                            |
      | code         | -32602                           |
      | message      | Unsupported protocol version     |
      | data.supported | ["2024-11-05"]                |
      | data.requested | 1.0.0                         |

  Scenario: Only ping allowed before initialization response
    Given an uninitialized connection
    When the client sends initialize request
    But before server responds
    Then client should only send:
      | allowed_method    |
      | notifications/ping |
    And any other request should be deferred

  Scenario: Limited server operations before initialized notification
    Given the server has responded to initialize
    But client has not sent initialized notification
    Then server should only send:
      | allowed_method     |
      | notifications/ping |
      | notifications/log  |
    And should defer other requests until initialized

  Scenario: Capability respect during operation
    Given successful initialization with negotiated capabilities
    When either party attempts to use non-negotiated capability
    Then an appropriate error should be returned
    And the connection should remain stable

  Scenario: Protocol version consistency during operation
    Given successful initialization with protocol version "2025-06-18"
    When any message is sent during operation
    Then it should conform to protocol version "2025-06-18" specification
    And should not use features from other protocol versions