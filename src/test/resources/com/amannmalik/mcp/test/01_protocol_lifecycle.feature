@protocol
Feature: MCP Protocol Lifecycle and Capabilities
  As an MCP implementation
  I want to correctly handle protocol initialization, capability negotiation, and JSON-RPC messaging
  So that clients and servers can establish secure, interoperable connections

  Background:
    Given a clean MCP environment
    And valid JSON-RPC transport is available

  @initialization @smoke
  Scenario: Successful protocol initialization
    Given a client with protocol version "2025-06-18"
    And client capabilities include sampling, roots, elicitation
    When client sends initialize request
    Then server responds with compatible protocol version
    And server declares supported capabilities
    And server provides implementation info
    When client sends initialized notification
    Then connection enters operational state
    And both parties can exchange messages

  @version-negotiation
  Scenario: Protocol version negotiation
    Given a client with protocol version "2025-06-18"
    And a server supporting versions "2024-11-05" and "2025-06-18"
    When client requests version "2025-06-18"
    Then server accepts requested version
    When client requests unsupported version "1.0.0"
    Then server responds with latest supported version
    And client should decide on compatibility

  @capability-negotiation
  Scenario Outline: Capability negotiation variations
    Given a client declaring <client_capabilities>
    And a server declaring <server_capabilities>
    When initialization completes
    Then available features are <available_features>
    And unavailable features are <unavailable_features>

    Examples:
      | client_capabilities           | server_capabilities              | available_features       | unavailable_features |
      | sampling,roots               | resources,tools,prompts          | none                     | all                  |
      | sampling                     | sampling                         | none                     | all                  |
      | roots,elicitation            | resources,tools                  | resources,tools          | sampling             |
      | sampling,roots,elicitation   | resources,tools,prompts,logging  | all                      | none                 |

  @jsonrpc @message-format
  Scenario: JSON-RPC message format compliance
    Given an operational MCP connection
    When client sends request with id "test-123"
    Then request includes jsonrpc "2.0", method, and id
    And id is not null
    And id has not been used before
    When server responds
    Then response includes matching id "test-123"
    And response has either result or error, not both
    When server sends notification
    Then notification lacks id field
    And notification includes method and optional params

  @jsonrpc @error-handling
  Scenario: JSON-RPC error responses
    Given an operational MCP connection
    When client sends malformed request
    Then server returns parse error (-32700)
    When client sends invalid method request
    Then server returns method not found (-32601)
    When client sends request with invalid params
    Then server returns invalid params (-32602)
    When server encounters internal error
    Then server returns internal error (-32603)

  @lifecycle @shutdown
  Scenario: Graceful connection shutdown
    Given an operational MCP connection with stdio transport
    When client closes input stream to server
    And waits for server to exit gracefully
    Then connection terminates cleanly
    Given an operational MCP connection with HTTP transport
    When client closes HTTP connection
    Then connection terminates cleanly

  @meta-fields
  Scenario: Reserved _meta field handling
    Given an operational MCP connection
    When client sends request with _meta field
    Then server preserves _meta without assumptions
    When server sends response with reserved _meta prefix
    Then client handles MCP-reserved keys appropriately
    When server sends _meta with custom prefix
    Then client treats as implementation-specific

  @timeouts
  Scenario: Request timeout handling
    Given an operational MCP connection with 5-second timeout
    When client sends request that takes 10 seconds
    Then client issues cancellation notification
    And client stops waiting for response
    When client sends request with progress notifications
    Then timeout may be extended based on progress
    But maximum timeout is still enforced

  @initialization-errors
  Scenario: Initialization failure scenarios
    Given a client with protocol version "2025-06-18"
    When server only supports incompatible versions
    Then server returns unsupported protocol error
    And includes supported versions in error data
    When required capabilities cannot be negotiated
    Then initialization fails with capability error
    When client sends requests before initialized notification
    Then server may reject or queue non-ping requests