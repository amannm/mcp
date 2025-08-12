@security @errors
Feature: MCP Security and Error Handling
  As an MCP implementation
  I want to enforce security controls and handle errors gracefully
  So that systems remain secure and resilient to failures

  Background:
    Given an operational MCP connection
    And security policies are configured

  @security @user-consent
  Scenario: User consent and control requirements
    Given client requires user consent for operations
    When server requests resource access
    Then client presents request to user for approval
    And user can approve, modify, or deny access
    When server requests tool execution
    Then client shows tool name, description, and arguments
    And user explicitly authorizes tool execution
    When server requests LLM sampling
    Then client shows proposed prompt to user
    And user controls whether sampling occurs

  @security @data-privacy
  Scenario: Data privacy protection
    Given client has access to user data
    When server requests resource access
    Then client validates data exposure permissions
    And client obtains explicit user consent for data sharing
    When client shares resource data
    Then client ensures data is not transmitted elsewhere
    And client applies appropriate access controls
    When user revokes data access
    Then client immediately stops sharing that data

  @security @tool-safety
  Scenario: Tool execution safety controls
    Given server exposes potentially dangerous tools
    When client evaluates tool descriptions
    Then client treats tool annotations as untrusted
    Unless server is verified as trusted
    When user attempts to execute tool
    Then client shows clear warning about tool capabilities
    And client requires explicit user confirmation
    When tool execution could affect system
    Then client may require additional authorization

  @security @sampling-controls
  Scenario: LLM sampling security controls
    Given server requests LLM sampling
    When client receives sampling request
    Then client requires user approval for sampling
    And user can view and edit proposed prompt
    When user approves sampling
    Then client controls which model is used
    And client controls what results server can see
    When sampling involves sensitive context
    Then client may redact or filter information

  @authorization @http
  Scenario: HTTP transport authorization
    Given MCP connection over HTTP transport
    When client makes requests to server
    Then client includes MCP-Protocol-Version header
    And client may include authorization headers
    When server requires authentication
    Then server validates authorization credentials
    And server returns 401 if unauthorized
    When authorization fails
    Then connection should be terminated securely

  @authorization @custom
  Scenario: Custom authorization strategies
    Given client and server negotiate custom auth
    When custom authentication is required
    Then both parties follow negotiated strategy
    And authentication occurs during initialization
    When authentication fails
    Then initialization returns appropriate error
    And connection is terminated

  @errors @protocol
  Scenario Outline: Standard JSON-RPC error codes
    Given operational MCP connection
    When <error_condition> occurs
    Then server returns JSON-RPC error <error_code>
    And error message explains <error_description>
    And error data provides <additional_context>

    Examples:
      | error_condition           | error_code | error_description    | additional_context        |
      | malformed JSON request    | -32700     | parse error          | invalid JSON details      |
      | unknown method called     | -32601     | method not found     | attempted method name     |
      | invalid request params    | -32602     | invalid params       | parameter validation info |
      | internal server error     | -32603     | internal error       | error context if safe     |
      | resource not found        | -32002     | resource not found   | requested URI             |

  @errors @capability
  Scenario: Capability-related errors
    Given client and server with different capabilities
    When client requests unsupported server feature
    Then server returns capability not supported error
    When server requests unsupported client feature
    Then client returns capability error
    When operation requires negotiated capability
    But capability was not negotiated during init
    Then appropriate capability error is returned

  @errors @validation
  Scenario: Input validation errors
    Given server with strict input validation
    When client sends resource request with invalid URI
    Then server returns URI validation error
    When client sends tool call with invalid arguments
    Then server returns argument validation error
    When client sends malformed message structure
    Then server returns structure validation error

  @errors @timeout
  Scenario: Timeout and cancellation errors
    Given requests with configured timeouts
    When request exceeds timeout period
    Then client issues cancellation notification
    And client returns timeout error to application
    When server cannot complete before cancellation
    Then server stops operation gracefully
    And server may log cancellation reason

  @errors @resource
  Scenario: Resource-specific error handling
    Given server exposing file-based resources
    When client requests non-existent resource
    Then server returns resource not found error
    When client requests resource without permission
    Then server returns authorization error
    When resource becomes unavailable during read
    Then server returns resource unavailable error

  @errors @tool
  Scenario: Tool execution error handling
    Given server with tools that can fail
    When tool encounters business logic error
    Then server returns result with isError: true
    And error content describes failure reason
    When tool arguments fail schema validation
    Then server returns JSON-RPC parameter error
    When tool execution times out internally
    Then server returns tool timeout error

  @errors @transport
  Scenario: Transport-level error handling
    Given MCP connection over transport
    When transport connection is lost
    Then both parties detect disconnection
    And both parties clean up resources
    When transport becomes temporarily unavailable
    Then implementations may attempt reconnection
    But should respect backoff policies

  @errors @recovery
  Scenario: Error recovery strategies
    Given errors occur during operation
    When recoverable error happens
    Then client may retry operation with backoff
    When persistent errors occur
    Then client should fail fast and report clearly
    When server encounters temporary issues
    Then server may suggest retry timing
    And server preserves session state if possible

  @security @validation
  Scenario: Security-focused input validation
    Given security-conscious implementations
    When client sends resource URI
    Then server validates URI format and safety
    And server checks for path traversal attempts
    When client sends tool arguments
    Then server validates against schema strictly
    And server sanitizes inputs for safety
    When server sends content to client
    Then client validates content types and encoding
    And client protects against malicious content

  @security @logging
  Scenario: Security event logging
    Given security monitoring is enabled
    When authentication fails
    Then security event is logged with details
    When authorization is denied
    Then access denial is logged appropriately
    When suspicious patterns are detected
    Then security alerts may be generated
    But sensitive data is not logged

  @errors @edge-cases
  Scenario: Edge case error handling
    Given unusual or boundary conditions
    When extremely large payloads are sent
    Then size limits are enforced appropriately
    When rapid request bursts occur
    Then rate limiting may be applied
    When resource subscriptions become stale
    Then subscription cleanup occurs gracefully
    When concurrent operations conflict
    Then conflicts are resolved safely

  @security @compliance
  Scenario: Security compliance verification
    Given security requirements must be met
    When user data is accessed
    Then access is logged and auditable
    When encryption is required
    Then communications are properly encrypted
    When sensitive operations occur
    Then appropriate security controls are enforced
    And compliance requirements are satisfied

  @integration @security-errors
  Scenario: Integrated security and error handling
    Given complex operation involving multiple features
    When security violation occurs during operation
    Then operation is terminated immediately
    And appropriate error is returned
    When errors occur in security-sensitive context
    Then errors are handled without information leakage
    And security state remains consistent
    When recovery from security errors is needed
    Then recovery process maintains security posture