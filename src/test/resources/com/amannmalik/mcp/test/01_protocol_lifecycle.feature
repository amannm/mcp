@protocol
Feature: MCP Connection Lifecycle
  As an MCP client application
  I want to establish secure, reliable connections with MCP servers
  So that I can access external context and tools for AI workflows

  Background:
    Given a clean MCP environment
    And a transport mechanism is available

  @connection @smoke
  Scenario: Successful connection establishment
    Given I want to connect using protocol version "2025-06-18"
    And I can provide the following capabilities:
      | capability  |
      | sampling    |
      | roots       |
      | elicitation |
    When I establish a connection with the server
    Then the connection should be established successfully
    And both parties should agree on capabilities
    And I should be able to exchange messages

  @connection @version-compatibility
  Scenario: Compatible version negotiation
    Given the server supports the following versions:
      | version    |
      | 2024-11-05 |
      | 2025-06-18 |
    When I request connection with version "2025-06-18"
    Then the server should accept my version request

  @connection @version-compatibility
  Scenario: Incompatible version fallback
    Given the server supports the following versions:
      | version    |
      | 2024-11-05 |
      | 2025-06-18 | 
    When I request connection with unsupported version "1.0.0"
    Then the server should offer its latest supported version
    And I should be able to decide whether to proceed

  @capabilities
  Scenario: Server capability discovery
    Given I test server capability discovery with the following configurations:
      | server_capability | available_feature | unavailable_feature |
      | resources         | resources         | prompts             |
      | resources         | resources         | logging             |
      | tools             | tools             | prompts             |
      | tools             | tools             | logging             |
      | prompts           | prompts           | resources           |
      | prompts           | prompts           | tools               |
      | prompts           | prompts           | logging             |
      | resources         | resources         | none                |
      | tools             | tools             | none                |
      | prompts           | prompts           | none                |
      | logging           | logging           | none                |
      | none              | none              | resources           |
      | none              | none              | tools               |
      | none              | none              | prompts             |
      | none              | none              | logging             |
    When I complete the connection handshake for each configuration
    Then the capability access should match the expected results

  @messaging
  Scenario: Request message format validation
    Given I have an established MCP connection
    When I send a request with identifier "test-123"
    Then the request should use proper message format
    And the request should have a unique identifier

  @messaging
  Scenario: Response message validation
    Given I have an established MCP connection
    When I send a request with identifier "test-123"
    And the server responds
    Then the response should match my request identifier
    And the response should contain valid result data

  @messaging
  Scenario: Notification communication
    Given I have an established MCP connection
    When I send a notification message
    Then the notification should use proper format
    And no response should be expected

  @error-handling
  Scenario: Server error responses
    Given I have an established MCP connection
    When I test error handling with the following scenarios:
      | error_situation              | error_type           |
      | malformed request            | Parse error          |
      | invalid method request       | Method not found     |
      | invalid parameters           | Invalid params       |
      | server internal error        | Internal error       |
    Then I should receive proper error responses for each scenario

  @connection @cleanup
  Scenario: Graceful connection termination
    Given I have an established MCP connection using "stdio" transport
    When I close the connection
    And wait for the server to shut down gracefully
    Then the connection should terminate cleanly

  @connection @cleanup
  Scenario: HTTP connection termination
    Given I have an established MCP connection using "http" transport
    When I close the HTTP connection
    Then the connection should terminate cleanly

  @messaging @metadata
  Scenario: Message metadata preservation
    Given I have an established MCP connection
    When I include metadata in my request
    Then the server should preserve my metadata unchanged

  @messaging @metadata
  Scenario: Server reserved metadata handling
    Given I have an established MCP connection
    When the server includes reserved metadata in responses
    Then I should handle MCP-reserved fields correctly

  @messaging @metadata
  Scenario: Server custom metadata handling
    Given I have an established MCP connection
    When the server includes custom metadata in responses
    Then I should treat it as implementation-specific data

  @messaging @timeouts
  Scenario: Request timeout and cancellation
    Given I have an established MCP connection with 5 second timeout
    When my request exceeds the timeout duration
    Then I should send a cancellation notification
    And stop waiting for the response

  @messaging @progress
  Scenario: Long-running request progress tracking
    Given I have an established MCP connection
    When my request sends progress notifications
    Then the timeout should be extended appropriately
    But a maximum timeout should still be enforced

  @connection @error-handling
  Scenario: Incompatible protocol version handling
    Given I request protocol version "2025-06-18"
    When the server only supports incompatible versions
    Then I should receive a clear protocol version error
    And the error should list the server's supported versions

  @connection @state-management
  Scenario: Premature request handling
    Given my connection is initialized but not fully ready
    When I send a non-ping request
    Then the server should handle the request appropriately

  @security @information-disclosure  
  Scenario: Safe implementation information sharing
    Given I have an established MCP connection
    When the server provides implementation information
    Then sensitive information should not be exposed
    And version information should be appropriate for sharing

  @security @authorization
  Scenario: LLM sampling authorization
    Given I have an established MCP connection with sampling capability
    When the server requests LLM sampling
    Then I should require explicit user approval
    And maintain control over prompt visibility