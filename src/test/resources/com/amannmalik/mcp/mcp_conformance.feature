Feature: MCP protocol conformance

  Scenario: Basic server interaction
    Given a running MCP server and connected client
    Then the server capabilities should be advertised
    When the client pings the server
    Then the ping succeeds
    When the client lists resources
    Then one resource uri should be "test://example"
    When the client reads "test://example"
    Then the resource text should be "hello"
    When the client lists resource templates
    Then one resource template is returned
    When the client lists tools
    Then one tool named "test_tool" is returned
    When the client calls "test_tool"
    Then the response text should be "ok"
    When the client lists prompts
    Then one prompt is returned
    When the client gets prompt "test_prompt"
    Then the first message text should be "hello"
    When the client requests a completion
    Then the completion value should be "test_completion"
    When the client sets the log level to "debug"
    Then the call succeeds
    When the client reads an invalid uri
    Then an error with code -32002 is returned
    When the client calls an unknown tool
    Then an error with code -32602 is returned
    When the client disconnects
    Then the server process terminates

  Scenario: Invalid log level
    Given a running MCP server and connected client
    When the client sets the log level to "invalid"
    Then an error with code -32602 is returned

  Scenario: Unknown prompt
    Given a running MCP server and connected client
    When the client gets prompt "nope"
    Then an error with code -32602 is returned

  Scenario: Logging on invalid request
    Given a running MCP server and connected client
    When the client sets the log level to "debug"
    Then the call succeeds
    When the client sends a cancellation notification
    Then a log message with level "info" is received

  Scenario: Resource subscription lifecycle
    Given a running MCP server and connected client
    When the client subscribes to "test://example"
    Then the call succeeds
    When the client unsubscribes from "test://example"
    Then the call succeeds

  Scenario: Invalid completion request
    Given a running MCP server and connected client
    When the client requests an invalid completion
    Then an error with code -32602 is returned

  Scenario: Unknown request method
    Given a running MCP server and connected client
    When the client calls an unknown method
    Then an error with code -32601 is returned

  Scenario: Logging suppression by level
    Given a running MCP server and connected client
    When the client sets the log level to "error"
    Then the call succeeds
    When the client sends a cancellation notification
    Then no log message with level "info" is received

  Scenario: Tool rate limiting
    Given a running MCP server and connected client
    When the client rapidly calls "test_tool" 6 times
    Then an error with code -32001 is returned
