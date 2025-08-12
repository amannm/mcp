@utilities
Feature: MCP Protocol Utilities - Progress, Cancellation, Logging, and Pagination
  As an MCP implementation
  I want to support cross-cutting utility features
  So that operations can be tracked, cancelled, logged, and paginated effectively

  Background:
    Given an operational MCP connection
    And appropriate capabilities are negotiated

  @progress @smoke
  Scenario: Basic progress tracking
    Given a long-running operation with progress support
    When operation begins
    Then server may send progress notification with progress token
    And progress notification includes current status
    When operation continues
    Then server sends updated progress notifications
    And progress shows completion percentage or status
    When operation completes
    Then final response includes operation result

  @progress @tokens
  Scenario: Progress token management
    Given multiple concurrent operations
    When each operation starts with unique progress token
    Then progress notifications include corresponding tokens
    And client can track multiple operations separately
    When operation A sends progress update
    Then client associates update with operation A only
    When operation B completes
    Then client knows operation B finished while A continues

  @progress @content-types
  Scenario Outline: Progress notification content
    Given operation supports <progress_type> progress
    When operation sends progress update
    Then notification includes <progress_fields>
    And progress content follows <format_rules>

    Examples:
      | progress_type | progress_fields              | format_rules                    |
      | percentage    | progress token, percentage   | number between 0-100            |
      | status        | progress token, status text  | human-readable status message   |
      | detailed      | progress token, current/total| current count and total count   |
      | unknown       | progress token only          | indicates work is happening     |

  @cancellation @smoke
  Scenario: Request cancellation
    Given a long-running request with id "long-op-123"
    When client decides to cancel the request
    Then client sends cancelled notification with request id
    And client stops waiting for response
    When server receives cancellation
    Then server should attempt to stop operation
    But server may still complete if too late to cancel

  @cancellation @timing
  Scenario: Cancellation timing scenarios
    Given a request that can be cancelled
    When cancellation arrives before operation starts
    Then operation should not start
    And no response should be sent
    When cancellation arrives during operation
    Then operation should stop gracefully
    And partial results may be discarded
    When cancellation arrives after completion
    Then cancellation has no effect
    And response has already been sent

  @cancellation @multiple
  Scenario: Multiple request cancellation
    Given multiple concurrent requests
    When client cancels specific request by id
    Then only that request is cancelled
    And other requests continue normally
    When client sends cancellation for unknown id
    Then server ignores unknown cancellation
    And all active requests continue

  @logging @basic
  Scenario: Server logging capability
    Given server has logging capability
    When server needs to emit log message
    Then server sends logging/message notification
    And notification includes level, logger, message
    And level is one of: error, warn, info, debug
    When client receives log message
    Then client may display or store log appropriately

  @logging @levels
  Scenario: Log level management
    Given server supports logging
    When client sets log level to "warn"
    Then server sends notifications/log/setLevel response
    And server only emits warn and error messages
    When client changes level to "debug"
    Then server emits all message levels
    When client sets level to "error"
    Then server only emits error messages

  @logging @structured
  Scenario: Structured log data
    Given server emits structured logs
    When server logs message with additional data
    Then logging notification includes data field
    And data contains structured information
    When server logs with context information
    Then client can use context for filtering/grouping
    And log data supports debugging and monitoring

  @pagination @resources
  Scenario: Resource list pagination
    Given server has 50 resources
    When client requests resources/list
    Then server may return subset with nextCursor
    When client requests with cursor
    Then server returns next page of resources
    When client reaches last page
    Then server returns resources without nextCursor

  @pagination @tools
  Scenario: Tool list pagination
    Given server has many tools
    When client requests tools/list with cursor
    Then server returns paginated tool list
    And pagination follows same pattern as resources
    When all tools have been retrieved
    Then final page lacks nextCursor

  @pagination @prompts
  Scenario: Prompt list pagination
    Given server has numerous prompt templates
    When client requests prompts/list with pagination
    Then server uses consistent pagination approach
    And client can iterate through all prompts
    Until reaching end of list

  @pagination @parameters
  Scenario: Pagination parameter handling
    Given server supports pagination
    When client requests list without cursor
    Then server returns first page
    When client provides invalid cursor
    Then server returns appropriate error
    When client requests with expired cursor
    Then server handles gracefully with error or restart

  @meta-fields @utilities
  Scenario: Meta field usage in utilities
    Given operations support _meta fields
    When progress notification includes _meta
    Then client preserves implementation-specific metadata
    When cancellation includes _meta context
    Then server uses meta for cancellation context
    When logging includes _meta tracing
    Then client can correlate logs with operations

  @timeout-integration
  Scenario: Timeout integration with progress
    Given request has 30-second timeout
    When operation sends progress every 5 seconds
    Then client may extend timeout based on progress
    But client enforces maximum timeout regardless
    When operation stops sending progress
    Then client should timeout normally
    And send cancellation notification

  @error-handling @utilities
  Scenario: Utility error scenarios
    Given utilities are in use
    When progress notification malformed
    Then client ignores malformed progress
    When cancellation references unknown request
    Then cancellation is ignored silently
    When logging level change fails
    Then server returns appropriate error
    When pagination cursor becomes invalid
    Then server returns pagination error

  @performance @utilities
  Scenario: Utility performance considerations
    Given high-frequency operations
    When many progress notifications are sent
    Then notifications should not overwhelm transport
    When many concurrent operations are cancelled
    Then cancellation processing should be efficient
    When extensive logging is enabled
    Then logging should not significantly impact performance

  @integration @utilities
  Scenario: Cross-utility integration
    Given long-running tool execution with logging
    When tool starts execution
    Then tool may send progress notifications
    And tool may emit log messages during execution
    When client cancels tool execution
    Then tool stops execution and may log cancellation
    And final tool result indicates cancellation
    When tool completes successfully
    Then final progress indicates completion
    And tool result includes execution outcome