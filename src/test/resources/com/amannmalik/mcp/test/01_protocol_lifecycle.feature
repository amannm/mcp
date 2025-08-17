@protocol
Feature: MCP Connection Lifecycle
  # Verifies conformance to specification/2025-06-18/basic/lifecycle.mdx
  # and specification/2025-06-18/basic/transports.mdx
  As an MCP client application
  I want to establish secure, reliable connections with MCP servers
  So that I can access external context and tools for AI workflows

  Background:
    Given a clean MCP environment
    And a transport mechanism is available

  @connection @smoke
  Scenario: Successful connection establishment
    # Tests specification/2025-06-18/basic/lifecycle.mdx:42-117 (Initialization phase)
    # Tests specification/2025-06-18/basic/lifecycle.mdx:146-171 (Capability negotiation)
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
    # Tests specification/2025-06-18/basic/lifecycle.mdx:127-144 (Version negotiation)
    Given the server supports the following versions:
      | version    |
      | 2024-11-05 |
      | 2025-06-18 |
    When I request connection with version "2025-06-18"
    Then the server should accept my version request

  @connection @version-compatibility
  Scenario: Incompatible version fallback
    # Tests specification/2025-06-18/basic/lifecycle.mdx:134-137 (Version negotiation fallback)
    Given the server supports the following versions:
      | version    |
      | 2024-11-05 |
      | 2025-06-18 | 
    When I request connection with unsupported version "1.0.0"
    Then the server should offer its latest supported version
    And I should be able to decide whether to proceed

  @capabilities
  Scenario: Server capability discovery
    # Tests specification/2025-06-18/basic/lifecycle.mdx:146-171 (Capability negotiation)
    # Tests specification/2025-06-18/basic/lifecycle.mdx:152-164 (Capability table)
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
    # Tests specification/2025-06-18/basic/index.mdx:33-52 (Request format)
    # Tests specification/2025-06-18/basic/index.mdx:48-51 (Request ID requirements)
    Given I have an established MCP connection
    When I send a request with identifier "test-123"
    Then the request should use proper message format
    And the request should have a unique identifier

  @messaging
  Scenario: Response message validation
    # Tests specification/2025-06-18/basic/index.mdx:54-79 (Response format)
    # Tests specification/2025-06-18/basic/index.mdx:72-75 (Response ID matching)
    Given I have an established MCP connection
    When I send a request with identifier "test-123"
    And the server responds
    Then the response should match my request identifier
    And the response should contain valid result data

  @messaging
  Scenario: Notification communication
    # Tests specification/2025-06-18/basic/index.mdx:81-95 (Notification format)
    # Tests specification/2025-06-18/basic/index.mdx:83,95 (No response requirement)
    Given I have an established MCP connection
    When I send a notification message
    Then the notification should use proper format
    And no response should be expected

  @error-handling
  Scenario: Server error responses
    # Tests specification/2025-06-18/basic/lifecycle.mdx:224-246 (Error handling)
    # Tests specification/2025-06-18/basic/index.mdx:64-78 (Error response format)
    Given I have an established MCP connection
    When I test error handling with the following scenarios:
      | error_situation              | error_type       | error_code |
      | malformed request            | Parse error      | -32700     |
      | invalid method request       | Method not found | -32601     |
      | invalid parameters           | Invalid params   | -32602     |
      | server internal error        | Internal error   | -32603     |
    Then I should receive proper error responses for each scenario

  @connection @cleanup
  Scenario: Graceful connection termination
    # Tests specification/2025-06-18/basic/lifecycle.mdx:183-200 (Shutdown - stdio)
    # Tests specification/2025-06-18/basic/transports.mdx:191-196 (stdio termination)
    Given I have an established MCP connection using "stdio" transport
    When I close the connection
    And wait for the server to shut down gracefully
    Then the connection should terminate cleanly

  @connection @cleanup
  Scenario: HTTP connection termination
    # Tests specification/2025-06-18/basic/lifecycle.mdx:201-205 (Shutdown - HTTP)
    # Tests specification/2025-06-18/basic/transports.mdx:202-204 (HTTP termination)
    Given I have an established MCP connection using "http" transport
    When I close the HTTP connection
    Then the connection should terminate cleanly

  @messaging @metadata
  Scenario: Message metadata preservation
    # Tests specification/2025-06-18/basic/index.mdx:125-150 (_meta field handling)
    Given I have an established MCP connection
    When I include metadata in my request
    Then the server should preserve my metadata unchanged

  @messaging @metadata
  Scenario: Server reserved metadata handling
    # Tests specification/2025-06-18/basic/index.mdx:131,142-144 (Reserved metadata keys)
    Given I have an established MCP connection
    When the server includes reserved metadata in responses
    Then I should handle MCP-reserved fields correctly

  @messaging @metadata
  Scenario: Server custom metadata handling
    # Tests specification/2025-06-18/basic/index.mdx:136-149 (Custom metadata key format)
    Given I have an established MCP connection
    When the server includes custom metadata in responses
    Then I should treat it as implementation-specific data

  @messaging @timeouts
  Scenario: Request timeout and cancellation
    # Tests specification/2025-06-18/basic/lifecycle.mdx:207-212 (Timeout implementation)
    # Tests specification/2025-06-18/basic/utilities/cancellation.mdx:15-30 (Cancellation flow)
    Given I have an established MCP connection with 5 second timeout
    When my request exceeds the timeout duration
    Then I should send a cancellation notification
    And stop waiting for the response

  @messaging @progress
  Scenario: Long-running request progress tracking
    # Tests specification/2025-06-18/basic/lifecycle.mdx:217-221 (Progress timeout reset)
    # Tests specification/2025-06-18/basic/utilities/progress.mdx:14-53 (Progress flow)
    Given I have an established MCP connection
    When my request sends progress notifications
    Then the timeout should be extended appropriately
    But a maximum timeout should still be enforced

  @connection @error-handling
  Scenario: Incompatible protocol version handling
    # Tests specification/2025-06-18/basic/lifecycle.mdx:233-246 (Version error example)
    Given I request protocol version "2025-06-18"
    When the server only supports incompatible versions
    Then I should receive a clear protocol version error
    And the error should list the server's supported versions

  @connection @state-management
  Scenario: Premature request handling
    # Tests specification/2025-06-18/basic/lifecycle.mdx:119-125 (Premature request constraints)
    Given my connection is initialized but not fully ready
    When I send a non-ping request
    Then the server should handle the request appropriately

  @security @information-disclosure  
  Scenario: Safe implementation information sharing
    # Tests specification/2025-06-18/basic/security_best_practices.mdx (General security principles)
    # Tests specification/2025-06-18/basic/lifecycle.mdx:99-104 (Implementation info sharing)
    Given I have an established MCP connection
    When the server provides implementation information
    Then sensitive information should not be exposed
    And version information should be appropriate for sharing

  @security @authorization
  Scenario: LLM sampling authorization
    # Tests specification/2025-06-18/client/sampling.mdx:28-36 (Human-in-the-loop requirements)
    # Tests specification/2025-06-18/client/sampling.mdx:164-167 (Authorization controls)
    Given I have an established MCP connection with sampling capability
    When the server requests LLM sampling
    Then I should require explicit user approval
    And maintain control over prompt visibility
