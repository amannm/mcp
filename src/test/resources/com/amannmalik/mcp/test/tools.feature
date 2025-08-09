#Feature: MCP Tools Conformance
#  As an MCP implementation
#  I want to ensure proper tools functionality between McpHost and McpServer
#  So that tool discovery, invocation, and result handling follow the 2025-06-18 specification rigorously
#
#  Background:
#    Given a clean MCP environment
#    And protocol version "2025-06-18" is supported
#    And both McpHost and McpServer are available
#
#  @core @tools @capability-negotiation
#  Scenario: Tools capability declaration and negotiation
#    Given a McpServer with tools capability:
#      | capability | subcapability | enabled |
#      | tools      | listChanged   | true    |
#    And the server provides tools:
#      | name        | title              | description                    |
#      | get_weather | Weather Tool       | Get current weather for a city |
#      | calculator  | Basic Calculator   | Perform arithmetic operations  |
#      | file_reader | File Reader        | Read contents of a file        |
#    And a McpHost capable of invoking tools
#    When the McpHost connects to McpServer
#    And capability negotiation completes
#    Then the server should declare tools capability with listChanged support
#    And the host should acknowledge tools capability
#    And both parties should be in operational state
#
#  @core @tools @discovery
#  Scenario: Tool discovery via tools/list request
#    Given an established MCP connection with tools capability
#    And the server provides tools:
#      | name        | title              | description                    | inputSchema                                    |
#      | get_weather | Weather Tool       | Get current weather for a city | {"type":"object","properties":{"city":{"type":"string"}},"required":["city"]} |
#      | calculator  | Basic Calculator   | Perform arithmetic operations  | {"type":"object","properties":{"expression":{"type":"string"}},"required":["expression"]} |
#    When the McpHost sends tools/list request
#    Then the McpServer should respond within 2 seconds with:
#      | field          | value              |
#      | tools.length   | 2                  |
#      | tools[0].name  | get_weather        |
#      | tools[0].title | Weather Tool       |
#      | tools[1].name  | calculator         |
#      | tools[1].title | Basic Calculator   |
#    And each tool should have valid inputSchema
#    And no protocol violations should be recorded
#
#  @core @tools @pagination
#  Scenario: Tool discovery with pagination support
#    Given an established MCP connection with tools capability
#    And the server provides 25 tools numbered from "tool_001" to "tool_025"
#    When the McpHost sends tools/list request with cursor limit of 10
#    Then the McpServer should respond with exactly 10 tools
#    And the response should include nextCursor field
#    When the McpHost sends tools/list request with the returned cursor
#    Then the McpServer should respond with the next 10 tools
#    And tool names should not overlap between pages
#    And pagination should continue until all tools are retrieved
#
#  @core @tools @invocation @text-result
#  Scenario: Basic tool invocation with text result
#    Given an established MCP connection with tools capability
#    And the server provides a tool:
#      | name        | description                    | inputSchema                                    |
#      | echo_tool   | Echo the input message         | {"type":"object","properties":{"message":{"type":"string"}},"required":["message"]} |
#    And the McpHost has consent for tool "echo_tool"
#    When the McpHost calls tool "echo_tool" with arguments:
#      | field   | value        |
#      | message | Hello MCP!   |
#    Then the McpServer should respond within 5 seconds with:
#      | field                    | value                |
#      | content[0].type          | text                 |
#      | content[0].text          | Hello MCP!           |
#      | isError                  | false                |
#    And the tool result should be valid
#    And no protocol violations should be recorded
#
#  @core @tools @invocation @structured-result
#  Scenario: Tool invocation with structured result and output schema
#    Given an established MCP connection with tools capability
#    And the server provides a tool with output schema:
#      | name          | weather_data                                                           |
#      | description   | Get structured weather data                                            |
#      | inputSchema   | {"type":"object","properties":{"location":{"type":"string"}},"required":["location"]} |
#      | outputSchema  | {"type":"object","properties":{"temperature":{"type":"number"},"conditions":{"type":"string"},"humidity":{"type":"number"}},"required":["temperature","conditions","humidity"]} |
#    And the McpHost has consent for tool "weather_data"
#    When the McpHost calls tool "weather_data" with arguments:
#      | field    | value     |
#      | location | New York  |
#    Then the McpServer should respond within 5 seconds with structured content:
#      | field                       | value              |
#      | structuredContent.temperature | 22.5             |
#      | structuredContent.conditions  | Partly cloudy    |
#      | structuredContent.humidity    | 65               |
#      | isError                     | false            |
#    And the structured content should conform to the output schema
#    And the response should also include text content for backward compatibility
#    And no protocol violations should be recorded
#
#  @advanced @tools @content-types @image
#  Scenario: Tool invocation returning image content with annotations
#    Given an established MCP connection with tools capability
#    And the server provides a tool:
#      | name         | chart_generator                                |
#      | description  | Generate a chart from data                     |
#      | inputSchema  | {"type":"object","properties":{"data":{"type":"array"},"type":{"type":"string"}},"required":["data","type"]} |
#    And the McpHost has consent for tool "chart_generator"
#    When the McpHost calls tool "chart_generator" with arguments:
#      | field | value                    |
#      | data  | [1,2,3,4,5]             |
#      | type  | bar                      |
#    Then the McpServer should respond within 10 seconds with:
#      | field                    | value                    |
#      | content[0].type          | image                    |
#      | content[0].mimeType      | image/png                |
#      | isError                  | false                    |
#    And the image content should have valid base64 data
#    And the content should include annotations with audience and priority
#    And no protocol violations should be recorded
#
#  @advanced @tools @content-types @resource-link
#  Scenario: Tool invocation returning resource links
#    Given an established MCP connection with tools capability
#    And the server provides a tool:
#      | name         | file_analyzer                              |
#      | description  | Analyze file and return resource links     |
#      | inputSchema  | {"type":"object","properties":{"path":{"type":"string"}},"required":["path"]} |
#    And the McpHost has consent for tool "file_analyzer"
#    When the McpHost calls tool "file_analyzer" with arguments:
#      | field | value                |
#      | path  | /project/src/main.rs |
#    Then the McpServer should respond within 5 seconds with:
#      | field                    | value                          |
#      | content[0].type          | resource_link                  |
#      | content[0].uri           | file:///project/src/main.rs    |
#      | content[0].name          | main.rs                        |
#      | content[0].mimeType      | text/x-rust                    |
#      | isError                  | false                          |
#    And the resource link should include valid URI
#    And the resource link should have appropriate annotations
#    And no protocol violations should be recorded
#
#  @advanced @tools @content-types @embedded-resource
#  Scenario: Tool invocation returning embedded resources
#    Given an established MCP connection with tools capability
#    And the server provides a tool:
#      | name         | code_reader                            |
#      | description  | Read and return file as embedded resource |
#      | inputSchema  | {"type":"object","properties":{"file":{"type":"string"}},"required":["file"]} |
#    And the McpHost has consent for tool "code_reader"
#    When the McpHost calls tool "code_reader" with arguments:
#      | field | value     |
#      | file  | hello.py  |
#    Then the McpServer should respond within 5 seconds with:
#      | field                      | value                         |
#      | content[0].type            | resource                      |
#      | content[0].resource.uri    | file:///project/hello.py      |
#      | content[0].resource.title  | Hello Python Script           |
#      | content[0].resource.mimeType | text/x-python               |
#      | isError                    | false                         |
#    And the embedded resource should contain valid text content
#    And the resource should have proper annotations with lastModified
#    And no protocol violations should be recorded
#
#  @error-handling @tools @protocol-errors
#  Scenario: Tool invocation with unknown tool name
#    Given an established MCP connection with tools capability
#    And the server provides a tool named "valid_tool"
#    And the McpHost has consent for tools
#    When the McpHost calls tool "nonexistent_tool" with arguments:
#      | field | value |
#      | test  | value |
#    Then the McpServer should respond within 2 seconds with JSON-RPC error:
#      | field           | value                              |
#      | error.code      | -32602                             |
#      | error.message   | Unknown tool: nonexistent_tool     |
#    And no tool result should be returned
#    And the error should follow JSON-RPC 2.0 specification
#
#  @error-handling @tools @protocol-errors
#  Scenario: Tool invocation with invalid arguments
#    Given an established MCP connection with tools capability
#    And the server provides a tool:
#      | name        | strict_tool                                |
#      | description | Tool requiring specific argument format    |
#      | inputSchema | {"type":"object","properties":{"number":{"type":"number"},"text":{"type":"string"}},"required":["number","text"]} |
#    And the McpHost has consent for tool "strict_tool"
#    When the McpHost calls tool "strict_tool" with invalid arguments:
#      | field  | value      |
#      | number | not_a_number |
#    Then the McpServer should respond within 2 seconds with JSON-RPC error:
#      | field           | value                        |
#      | error.code      | -32602                       |
#      | error.message   | Invalid parameters           |
#    And the error should indicate argument validation failure
#    And no tool result should be returned
#
#  @error-handling @tools @execution-errors
#  Scenario: Tool execution error with isError flag
#    Given an established MCP connection with tools capability
#    And the server provides a tool:
#      | name        | failing_tool                           |
#      | description | Tool that simulates execution failure  |
#      | inputSchema | {"type":"object","properties":{"action":{"type":"string"}},"required":["action"]} |
#    And the McpHost has consent for tool "failing_tool"
#    When the McpHost calls tool "failing_tool" with arguments:
#      | field  | value |
#      | action | fail  |
#    Then the McpServer should respond within 5 seconds with:
#      | field                    | value                                    |
#      | content[0].type          | text                                     |
#      | content[0].text          | Tool execution failed: simulated error  |
#      | isError                  | true                                     |
#    And the response should be a valid tool result with error flag
#    And no JSON-RPC protocol error should be returned
#
#  @advanced @tools @notifications
#  Scenario: Tools list changed notification
#    Given an established MCP connection with tools capability and listChanged support
#    And the server initially provides 2 tools
#    And the McpHost has subscribed to tool list changes
#    When the server adds a new tool "dynamic_tool"
#    Then the McpServer should send tools/list_changed notification within 1 second
#    And the notification should follow JSON-RPC 2.0 format
#    When the McpHost sends tools/list request after notification
#    Then the response should include the new "dynamic_tool"
#    And the total tool count should be 3
#
#  @advanced @tools @notifications
#  Scenario: Tools list changed notification with tool removal
#    Given an established MCP connection with tools capability and listChanged support
#    And the server initially provides tools:
#      | name    |
#      | tool_1  |
#      | tool_2  |
#      | tool_3  |
#    And the McpHost has subscribed to tool list changes
#    When the server removes tool "tool_2"
#    Then the McpServer should send tools/list_changed notification within 1 second
#    When the McpHost sends tools/list request after notification
#    Then the response should contain only "tool_1" and "tool_3"
#    And "tool_2" should no longer be available
#
#  @security @tools @consent
#  Scenario: Tool invocation without user consent
#    Given an established MCP connection with tools capability
#    And the server provides a tool named "sensitive_tool"
#    And the McpHost has NOT granted consent for "sensitive_tool"
#    When the McpHost attempts to call tool "sensitive_tool" with arguments:
#      | field | value |
#      | data  | test  |
#    Then the McpHost should block the request due to lack of consent
#    And no request should be sent to the McpServer
#    And the user should be prompted for consent
#    And an audit log entry should be created
#
#  @security @tools @validation
#  Scenario: Tool input validation and sanitization
#    Given an established MCP connection with tools capability
#    And the server provides a tool with strict input validation:
#      | name        | validated_tool                         |
#      | description | Tool with comprehensive input validation |
#      | inputSchema | {"type":"object","properties":{"email":{"type":"string","format":"email"},"age":{"type":"integer","minimum":0,"maximum":150}},"required":["email","age"]} |
#    And the McpHost has consent for tool "validated_tool"
#    When the McpHost calls tool "validated_tool" with arguments:
#      | field | value              |
#      | email | invalid-email      |
#      | age   | -5                 |
#    Then the McpServer should validate inputs according to schema
#    And return JSON-RPC error for invalid format
#    And not execute the tool with invalid data
#
#  @security @tools @rate-limiting
#  Scenario: Tool invocation rate limiting
#    Given an established MCP connection with tools capability
#    And the server provides a tool with rate limiting:
#      | name        | rate_limited_tool    |
#      | description | Tool with rate limits |
#      | rateLimit   | 5 calls per minute   |
#    And the McpHost has consent for tool "rate_limited_tool"
#    When the McpHost calls tool "rate_limited_tool" 6 times within 30 seconds
#    Then the first 5 calls should succeed
#    And the 6th call should receive rate limit error:
#      | field           | value                    |
#      | error.code      | -32603                   |
#      | error.message   | Rate limit exceeded      |
#    And the server should enforce rate limiting properly
#
#  @performance @tools @timeout
#  Scenario: Tool invocation timeout handling
#    Given an established MCP connection with tools capability
#    And the server provides a tool:
#      | name        | slow_tool                          |
#      | description | Tool that takes long time to execute |
#      | inputSchema | {"type":"object","properties":{"delay":{"type":"number"}},"required":["delay"]} |
#    And the McpHost has consent for tool "slow_tool"
#    And the McpHost timeout for tool calls is set to 3 seconds
#    When the McpHost calls tool "slow_tool" with arguments:
#      | field | value |
#      | delay | 5000  |
#    Then the McpHost should timeout the request after 3 seconds
#    And return timeout error to the calling code
#    And the connection should remain stable for subsequent requests
#
#  @performance @tools @concurrent
#  Scenario: Concurrent tool invocations
#    Given an established MCP connection with tools capability
#    And the server provides tools:
#      | name     | description              |
#      | tool_1   | First concurrent tool    |
#      | tool_2   | Second concurrent tool   |
#      | tool_3   | Third concurrent tool    |
#    And the McpHost has consent for all tools
#    When the McpHost calls all 3 tools concurrently with unique request IDs
#    Then all tools should execute in parallel
#    And each should return results with correct request ID mapping
#    And the total execution time should be less than sequential execution
#    And no request/response correlation errors should occur
#
#  @edge-cases @tools @empty-list
#  Scenario: Tool discovery with empty tool list
#    Given an established MCP connection with tools capability
#    And the server provides no tools initially
#    When the McpHost sends tools/list request
#    Then the McpServer should respond within 2 seconds with:
#      | field          | value |
#      | tools.length   | 0     |
#    And the response should be valid JSON-RPC format
#    And no nextCursor should be present
#    And no protocol violations should be recorded
#
#  @edge-cases @tools @large-payload
#  Scenario: Tool invocation with large input/output payloads
#    Given an established MCP connection with tools capability
#    And the server provides a tool:
#      | name        | data_processor                     |
#      | description | Process large amounts of data      |
#      | inputSchema | {"type":"object","properties":{"data":{"type":"string"}},"required":["data"]} |
#    And the McpHost has consent for tool "data_processor"
#    When the McpHost calls tool "data_processor" with 1MB text data
#    Then the McpServer should handle the large payload correctly
#    And respond with processed data within 30 seconds
#    And no memory or performance issues should occur
#    And the connection should remain stable
#
#  @edge-cases @tools @malformed-requests
#  Scenario: Handling malformed tool call requests
#    Given an established MCP connection with tools capability
#    And the server provides a tool named "valid_tool"
#    When the McpHost sends malformed tools/call request:
#      | issue           | description              |
#      | missing_name    | No tool name specified   |
#      | invalid_json    | Malformed JSON in params |
#      | wrong_method    | tools/invalid method     |
#    Then each malformed request should receive appropriate JSON-RPC error
#    And the errors should follow standard error codes
#    And the connection should remain stable
#    And subsequent valid requests should work normally
#
#  @compliance @tools @annotation-handling
#  Scenario: Tool result annotation processing
#    Given an established MCP connection with tools capability
#    And the server provides a tool that returns content with annotations:
#      | name        | annotated_tool                     |
#      | description | Tool returning annotated content   |
#    And the McpHost has consent for tool "annotated_tool"
#    When the McpHost calls tool "annotated_tool"
#    Then the tool result should include content with annotations:
#      | field                        | value                    |
#      | content[0].annotations.audience      | ["user", "assistant"]    |
#      | content[0].annotations.priority      | 0.8                      |
#      | content[0].annotations.lastModified  | ISO 8601 format          |
#    And the McpHost should process annotations appropriately
#    And annotation format should conform to specification
#
#  @compliance @tools @backwards-compatibility
#  Scenario: Backwards compatibility for structured content
#    Given an established MCP connection with tools capability
#    And the server provides a tool that returns structured content
#    And the McpHost supports both structured and text content
#    When the McpHost calls a tool that returns structured data
#    Then the response should include both:
#      | field             | description                           |
#      | structuredContent | JSON object with structured data      |
#      | content[0]        | TextContent with serialized JSON     |
#    And the text content should be valid JSON representation
#    And legacy clients should be able to parse the text content
#    And modern clients should prefer the structured content