@utilities
Feature: MCP Protocol Utilities
  # Verifies conformance to specification/2025-06-18/basic/utilities/cancellation.mdx
  # and specification/2025-06-18/basic/utilities/ping.mdx
  # and specification/2025-06-18/basic/utilities/progress.mdx
  # and specification/2025-06-18/server/utilities/pagination.mdx
  As an MCP participant (client or server)
  I want to use protocol utilities for cancellation, ping, progress, and pagination
  So that I can manage long-running operations, verify connectivity, and handle large datasets efficiently

  Background:
    Given a clean MCP environment
    And an established MCP connection

  @cancellation @notifications
  Scenario: Request cancellation notification
    # Tests specification/2025-06-18/basic/utilities/cancellation.mdx:14-30 (Cancellation flow)
    Given I have sent a request with ID "request-123"
    And the request is still in progress
    When I send a cancellation notification:
      | field     | value                       |
      | requestId | request-123                 |
      | reason    | User requested cancellation |
    Then the notification should be properly formatted
    And the receiver should stop processing the request
    And no response should be sent for the cancelled request

  @cancellation @behavior
  Scenario: Cancellation behavior requirements
    # Tests specification/2025-06-18/basic/utilities/cancellation.mdx:32-48 (Behavior requirements)
    Given I have active requests and completed requests
    When I test cancellation scenarios:
      | scenario                   | request_state | should_cancel | expected_behavior   |
      | cancel in-progress request | in_progress   | true          | stop processing     |
      | cancel completed request   | completed     | false         | ignore notification |
      | cancel unknown request     | unknown       | false         | ignore notification |
      | cancel initialize request  | in_progress   | false         | must not cancel     |
    Then each scenario should behave according to specification requirements

  @cancellation @race-conditions
  Scenario: Cancellation race condition handling
    # Tests specification/2025-06-18/basic/utilities/cancellation.mdx:49-69 (Timing considerations)
    Given I have sent a request that may complete quickly
    When I send a cancellation notification that arrives after completion
    Then both parties should handle the race condition gracefully
    And the cancellation notification should be ignored
    And any response that arrives should be ignored by the cancellation sender

  @cancellation @error-handling
  Scenario: Invalid cancellation notification handling
    # Tests specification/2025-06-18/basic/utilities/cancellation.mdx:76-85 (Error handling)
    Given I am receiving cancellation notifications
    When I receive invalid cancellation notifications:
      | invalid_type           | description                 |
      | unknown_request_id     | Request ID not found        |
      | malformed_notification | Invalid notification format |
      | already_completed      | Request already completed   |
    Then I should ignore all invalid notifications
    And maintain the fire-and-forget nature of notifications

  @ping @connectivity
  Scenario: Basic ping request and response
    # Tests specification/2025-06-18/basic/utilities/ping.mdx:14-39 (Message format and behavior)
    Given I want to verify connection health
    When I send a ping request with ID "ping-123"
    Then the receiver should respond promptly with an empty result
    And the response should have the same ID "ping-123"
    And the response format should be valid JSON-RPC

  @ping @health-monitoring
  Scenario: Connection health monitoring with ping
    # Tests specification/2025-06-18/basic/utilities/ping.mdx:46-55 (Usage patterns)
    Given I have an established MCP connection for utilities
    When I implement periodic ping monitoring
    Then I should be able to detect connection health
    And configure appropriate ping frequency
    And handle ping timeouts appropriately

  @ping @timeout-handling
  Scenario: Ping timeout and connection failure detection
    # Tests specification/2025-06-18/basic/utilities/ping.mdx:41-44, 64-68 (Timeout handling)
    Given I have sent a ping request
    When no response is received within the timeout period
    Then I may consider the connection stale
    And I may terminate the connection
    And I may attempt reconnection procedures
    And I should log ping failures for diagnostics

  @ping @bidirectional
  Scenario: Bidirectional ping support
    # Tests specification/2025-06-18/basic/utilities/ping.mdx:9-15 (Either party can ping)
    Given I have an established MCP connection for utilities
    When both client and server send ping requests:
      | sender | receiver | ping_id     |
      | client | server   | client-ping |
      | server | client   | server-ping |
    Then both should respond appropriately
    And ping functionality should work in both directions

  @ping @validation
  Scenario: Ping request with invalid parameters
    # Tests specification/2025-06-18/basic/utilities/ping.mdx:17-27 (Message format)
    Given I have an established MCP connection for utilities
    And I want to verify connection health
    When I send a ping request with parameters:
      | field | value |
      | extra | data  |
    Then the error message should be "Invalid params"
    And the error code should be -32602

  @ping @validation
  Scenario: Ping request with empty parameters
    # Tests specification/2025-06-18/basic/utilities/ping.mdx:19-27 (Message format)
    Given I have an established MCP connection for utilities
    And I want to verify connection health
    When I send a ping request with empty parameters
    Then the error message should be "Invalid params"
    And the error code should be -32602

  @ping @meta-parameter
  Scenario: Ping request with reserved meta parameter
    # Tests specification/2025-06-18/basic/utilities/ping.mdx:17-27 (Message format)
    Given I have an established MCP connection for utilities
    And I want to verify connection health
    When I send a ping request with parameters:
      | field | value |
      | _meta | {}    |
    Then the receiver should respond promptly with an empty result

  @ping @meta-validation
  Scenario: Ping request with invalid meta parameter
    # Tests specification/2025-06-18/basic/utilities/ping.mdx:17-27 (Message format)
    Given I have an established MCP connection for utilities
    And I want to verify connection health
    When I send a ping request with parameters:
      | field | value   |
      | _meta | invalid |
    Then the error message should be "Invalid params"
    And the error code should be -32602

  @progress @token-tracking
  Scenario: Progress tracking with progress tokens
    # Tests specification/2025-06-18/basic/utilities/progress.mdx:14-33 (Progress flow)
    Given I want to track progress for a long-running operation
    When I send a request with progress token "progress-abc123"
    Then the receiver may send progress notifications
    And each notification should reference the token "progress-abc123"
    And progress notifications should include current progress value

  @progress @notification-format
  Scenario: Progress notification message format
    # Tests specification/2025-06-18/basic/utilities/progress.mdx:35-53 (Notification format)
    Given I am receiving progress notifications for token "abc123"
    When I receive progress notifications with different data:
      | progress | total | message             | valid |
      | 25       | 100   | Processing files... | true  |
      | 50.5     |       |                     | true  |
      | 75       | 100   | Nearly done         | true  |
      | 100      | 100   | Operation complete  | true  |
    Then all valid notifications should be properly formatted
    And progress values should increase with each notification
    And floating point values should be supported
    And total value should be optional
    And message field should be optional

  @progress @behavior-requirements
  Scenario: Progress notification behavior requirements
    # Tests specification/2025-06-18/basic/utilities/progress.mdx:60-71 (Behavior requirements)
    Given I have requests with and without progress tokens
    When I test progress notification scenarios:
      | scenario                     | has_token | should_notify | token_validity |
      | request with valid token     | true      | optional      | active         |
      | request without token        | false     | false         | none           |
      | token from completed request | true      | false         | inactive       |
      | unknown token                | true      | false         | invalid        |
    Then behavior should match specification requirements
    And notifications should only reference valid active tokens

  @progress @token-uniqueness
  Scenario: Progress token uniqueness enforcement
    # Tests specification/2025-06-18/basic/utilities/progress.mdx:16-18 (Unique tokens)
    Given I have active requests with progress tokens:
      | request_id | progress_token |
      | req-1      | dup-token      |
      | req-2      | dup-token      |
    When I validate progress token uniqueness
    Then the system should reject duplicate progress tokens

  @progress @token-types
  Scenario: Progress token type requirements
    # Tests specification/2025-06-18/basic/utilities/progress.mdx:14-22 (Token type)
    Given I have requests with progress tokens of different types:
      | token    | valid |
      | "abc123" | true  |
      | 42       | true  |
      | 1.5      | false |
      | true     | false |
    When I validate progress token types
    Then only valid progress token types should be accepted

  @progress @token-location
  Scenario: Progress token placement in request metadata
    # Tests specification/2025-06-18/basic/utilities/progress.mdx:14-22 (Token placement)
    Given I have a request with progress token outside metadata
    When I attempt to send the request with misplaced progress token
    Then I should receive an invalid metadata error for progress token

  @progress @rate-limiting
  Scenario: Progress notification rate limiting
    # Tests specification/2025-06-18/basic/utilities/progress.mdx:89-93 (Implementation notes)
    Given I am sending progress notifications
    When I implement progress tracking for operations
    Then I should implement rate limiting to prevent flooding
    And track active progress tokens appropriately
    And stop notifications after operation completion

  @pagination @cursor-based
  Scenario: Cursor-based pagination flow
    # Tests specification/2025-06-18/server/utilities/pagination.mdx:26-41 (Response format)
    Given the server has a large dataset to return
    When I request a paginated list operation
    Then the response should include the current page of results
    And include a nextCursor if more results exist
    And the cursor should be an opaque string token

  @pagination @continuation
  Scenario: Pagination continuation with cursor
    # Tests specification/2025-06-18/server/utilities/pagination.mdx:43-56 (Request format)
    Given I have received a response with nextCursor "eyJwYWdlIjogM30="
    When I send a continuation request with that cursor
    Then I should receive the next page of results
    And the server should handle the cursor appropriately
    And may provide another nextCursor for further pages

  @pagination @final-page
  Scenario: Pagination final page without nextCursor
    # Tests specification/2025-06-18/server/utilities/pagination.mdx:27-31 (nextCursor omission)
    Given the server has no further results after the current page
    When I request a paginated list operation
    Then the response should include the current page of results
    And the response should not include a nextCursor field

  @pagination @supported-operations
  Scenario: Operations supporting pagination
    # Tests specification/2025-06-18/server/utilities/pagination.mdx:72-80 (Supported operations)
    Given the server supports pagination
    When I test pagination for different operations:
      | operation                | supports_pagination |
      | resources/list           | true                |
      | resources/templates/list | true                |
      | prompts/list             | true                |
      | tools/list               | true                |
    Then all specified operations should support pagination
    And pagination should work consistently across operations

  @pagination @client-behavior
  Scenario: Client pagination behavior guidelines
    # Tests specification/2025-06-18/server/utilities/pagination.mdx:88-96 (Implementation guidelines)
    Given I am a client handling paginated responses
    When I implement pagination support
    Then I should treat missing nextCursor as end of results
    And support both paginated and non-paginated flows
    And treat cursors as opaque tokens
    And not make assumptions about cursor format
    And not attempt to parse or modify cursors
    And not persist cursors across sessions

  @pagination @server-behavior
  Scenario: Server pagination behavior guidelines
    # Tests specification/2025-06-18/server/utilities/pagination.mdx:83-87 (Server guidelines)
    Given I am a server implementing pagination
    When I provide paginated responses
    Then I should provide stable cursors
    And handle invalid cursors gracefully
    And determine appropriate page sizes
    And maintain cursor validity for active sessions

  @pagination @error-handling
  Scenario: Pagination error handling
    # Tests specification/2025-06-18/server/utilities/pagination.mdx:98-100 (Error handling)
    Given I am handling pagination requests
    When I receive requests with invalid cursors:
      | cursor_type      | error_code | error_message  |
      | expired_cursor   | -32602     | Invalid params |
      | malformed_cursor | -32602     | Invalid params |
      | unknown_cursor   | -32602     | Invalid params |
    Then I should return appropriate error responses for utilities
    And use JSON-RPC error code -32602 for invalid parameters

  @integration @utility-combinations
  Scenario: Combined utility feature usage
    # Tests integration between different utility features
    Given I have operations that use multiple utilities
    When I combine pagination with progress tracking:
      | page | progress_token | expected_notifications |
      | 1    | page-1-token   | progress updates       |
      | 2    | page-2-token   | progress updates       |
      | 3    | page-3-token   | progress updates       |
    Then each utility should function independently
    And utilities should not interfere with each other
    And cancellation should work for paginated operations with progress

  @integration @utility-lifecycle
  Scenario: Utility feature lifecycle management
    # Tests proper cleanup and state management for utilities
    Given I have active operations using utilities
    When operations complete or are cancelled:
      | operation_state | progress_tokens | cursors | cancellation |
      | completed       | cleanup         | valid   | n/a          |
      | cancelled       | cleanup         | invalid | processed    |
      | failed          | cleanup         | invalid | optional     |
    Then all utility state should be managed appropriately
    And resources should be cleaned up properly
    And no dangling references should remain

  @error-handling @utility-errors
  Scenario: Comprehensive utility error handling
    # Tests error handling across all utility features
    Given I am testing error scenarios for utilities
    When I encounter errors in utility operations:
      | utility      | error_scenario          | expected_behavior    |
      | cancellation | malformed notification  | ignore gracefully    |
      | ping         | timeout exceeded        | connection failure   |
      | progress     | invalid token reference | ignore notification  |
      | pagination   | cursor expired          | invalid params error |
    Then each utility should handle errors according to specification
    And error handling should be consistent across utilities
    And system stability should be maintained during error conditions
