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
      | cancel_tool_call  | slow_tool | -32603              |
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
      | operation                 | parameter   | expected_result |
      | list_tools_schema         |             | test_tool       |
      | list_tools_output_schema  |             | test_tool       |
      | call_tool_structured      | test_tool   | ok              |
      | call_tool_error           | error_tool  | fail            |
    And testing error conditions
      | operation         | parameter | expected_error_code |
      | call_unknown_tool | nope      | -32602              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  Scenario Outline: MCP prompts specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation                | parameter   | expected_result |
      | list_prompt_name         |             | test_prompt     |
      | list_prompt_arg_required |             | true            |
      | get_prompt_text          | test_prompt | hello           |
      | get_prompt_role          | test_prompt | user            |
    And testing error conditions
      | operation              | parameter   | expected_error_code |
      | get_prompt_invalid     | nope        | -32602              |
      | get_prompt_missing_arg | test_prompt | -32602              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  Scenario Outline: MCP Resource metadata conforms to specification
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation         | parameter | expected_result |
      | resource_metadata | example   | text/plain      |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  Scenario Outline: MCP elicitation specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation        | parameter | expected_result |
      | call_tool_elicit | echo_tool | ping            |
    And testing error conditions
      | operation               | parameter | expected_error_code |
      | call_tool_elicit_cancel | echo_tool | -32602              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |


  Scenario Outline: MCP sampling specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation        | parameter | expected_result |
      | request_sampling |           | ok              |
    And testing error conditions
      | operation               | parameter | expected_error_code |
      | request_sampling_reject |           | -32603              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  Scenario Outline: MCP roots specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation    | parameter | expected_result |
      | roots_listed |           | 1               |
    And testing error conditions
      | operation     | parameter | expected_error_code |
      | roots_invalid |           | -32601              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

