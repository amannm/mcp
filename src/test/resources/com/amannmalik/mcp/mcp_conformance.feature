Feature: MCP protocol conformance

  Scenario Outline: MCP server conformance test
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation            | parameter      | expected_result |
      | list_resources       |                | test://example  |
      | read_resource        | test://example | hello           |
      | list_templates       |                | 1               |
      | list_tools           |                | test_tool       |
      | call_tool            | test_tool      | ok              |
      | list_prompts         |                | 1               |
      | get_prompt           | test_prompt    | hello           |
      | request_completion   |                | test_completion |
      | set_log_level        | debug          | success         |
      | subscribe_resource   | test://example | success         |
      | unsubscribe_resource | test://example | success         |
    And testing error conditions
      | operation         | parameter | expected_error_code |
      | read_invalid_uri  | bad://uri | -32002              |
      | call_unknown_tool | nope      | -32602              |
    When the client disconnects
    Then the server terminates cleanly

      Examples:
        | transport |
        | stdio     |
        | http      |

  Scenario Outline: MCP tools specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation         | parameter | expected_result |
      | list_tools_schema |           | test_tool       |
      | call_tool         | test_tool | ok              |
    And testing error conditions
      | operation         | parameter | expected_error_code |
      | call_unknown_tool | nope      | -32602              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |