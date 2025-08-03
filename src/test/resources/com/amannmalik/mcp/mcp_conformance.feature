Feature: MCP protocol conformance

  # - [Base Protocol](specification/2025-06-18/basic.mdx)
  # - [Server Features](specification/2025-06-18/server.mdx)
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
      | operation                     | parameter  | expected_error_code |
      | read_invalid_uri              | bad://uri  | -32002              |
      | list_resources_invalid_cursor | notacursor | -32602              |
      | call_unknown_tool             | nope       | -32602              |
      | cancel_tool_call              | slow_tool  | -32603              |
    And a cancellation log message is received
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  # - [Tools](specification/2025-06-18/server/tools.mdx)
  Scenario Outline: MCP tools specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation                | parameter  | expected_result |
      | list_tools_schema        |            | test_tool       |
      | list_tools_output_schema |            | test_tool       |
      | call_tool_structured     | test_tool  | ok              |
      | call_tool_error          | error_tool | fail            |
    And testing error conditions
      | operation                 | parameter  | expected_error_code |
      | call_unknown_tool         | nope       | -32602              |
      | list_tools_invalid_cursor | notacursor | -32602              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  # - [Prompts](specification/2025-06-18/server/prompts.mdx)
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
      | operation                   | parameter   | expected_error_code |
      | get_prompt_invalid          | nope        | -32602              |
      | get_prompt_missing_arg      | test_prompt | -32602              |
      | list_prompts_invalid_cursor | notacursor  | -32602              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  # - [Resources](specification/2025-06-18/server/resources.mdx)
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

  # - [Annotations](specification/2025-06-18/server/annotations.mdx)
  Scenario Outline: MCP annotations specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation                  | parameter | expected_result |
      | list_resources_annotations | example   | 0.5             |
      | list_tools_annotations     | test_tool | true            |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  # - [Logging](specification/2025-06-18/server/utilities/logging.mdx)
  Scenario Outline: MCP logging specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation     | parameter | expected_result |
      | set_log_level | warning   | success         |
    And testing error conditions
      | operation             | parameter | expected_error_code |
      | set_log_level_invalid | verbose   | -32602              |
      | set_log_level_missing |           | -32602              |
      | set_log_level_extra   | warning   | -32602              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  # - [Completion](specification/2025-06-18/server/utilities/completion.mdx)
  Scenario Outline: MCP completion specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation          | parameter | expected_result |
      | request_completion |           | test_completion |
    And testing error conditions
      | operation                      | parameter | expected_error_code |
      | request_completion_invalid     |           | -32602              |
      | request_completion_missing_arg |           | -32602              |
      | request_completion_missing_ref |           | -32602              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  # - [Elicitation](specification/2025-06-18/client/elicitation.mdx)
  Scenario Outline: MCP elicitation specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation        | parameter | expected_result |
      | call_tool_elicit | echo_tool | ping            |
    And testing error conditions
      | operation                | parameter | expected_error_code |
      | call_tool_elicit_cancel  | echo_tool | -32602              |
      | call_tool_elicit_decline | echo_tool | -32602              |
      | call_tool_elicit_invalid | echo_tool | -32602              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  # - [Sampling](specification/2025-06-18/client/sampling.mdx)
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

  # - [Ping](specification/2025-06-18/basic/utilities/ping.mdx)
  Scenario Outline: MCP ping error handling specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing error conditions
      | operation    | parameter | expected_error_code |
      | ping_invalid | oops      | -32602              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  # - [Progress](specification/2025-06-18/basic/utilities/progress.mdx)
  Scenario Outline: MCP progress specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When requesting resource list with progress tracking
    Then progress updates are received
    And progress completes to 1.0
    And progress message is provided
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  # - [Pagination](specification/2025-06-18/server/utilities/pagination.mdx)
  Scenario Outline: MCP pagination specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When listing tools with pagination
    Then pagination covers all tools
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  # - [Roots](specification/2025-06-18/client/roots.mdx)
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

  # - [Authorization](specification/2025-06-18/basic/authorization.mdx)
  Scenario Outline: MCP authorization specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing core functionality
      | operation                     | parameter | expected_result                                                                     |
      | unauthorized_request          |           | Bearer resource_metadata="https://example.com/.well-known/oauth-protected-resource" |
      | resource_metadata_auth_server |           | https://auth.example.com                                                            |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | http      |

  # - [Subscriptions](specification/2025-06-18/server/subscriptions.mdx)
  # - [Notifications](specification/2025-06-18/server/notifications.mdx)
  Scenario Outline: MCP notification and subscription specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing subscription capabilities
      | capability_type | feature     | expected_support |
      | resources       | subscribe   | true             |
      | resources       | listChanged | true             |
      | prompts         | listChanged | true             |
      | tools           | listChanged | true             |
      | roots           | listChanged | true             |
    And testing notification behaviors
      | notification_type                    | trigger_action       | expected_notification |
      | notifications/resources/list_changed | modify_resource_list | received              |
      | notifications/resources/updated      | update_subscribed    | received              |
      | notifications/prompts/list_changed   | modify_prompt_list   | received              |
      | notifications/tools/list_changed     | modify_tool_list     | received              |
      | notifications/roots/list_changed     | modify_root_list     | received              |
    And testing subscription lifecycle
      | operation                   | parameter      | expected_result |
      | subscribe_resource          | test://example | success         |
      | receive_update_notification | test://example | success         |
      | unsubscribe_resource        | test://example | success         |
      | no_further_notifications    | test://example | success         |
    And testing notification error conditions
      | operation                    | parameter      | expected_error_code |
      | subscribe_invalid_resource   | bad://uri      | -32002              |
      | unsubscribe_nonexistent      | fake://uri     | -32602              |
      | subscribe_duplicate_resource | test://example | -32602              |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  # - [Notifications](specification/2025-06-18/server/notifications.mdx)
  Scenario Outline: MCP notification timing and delivery conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing notification delivery patterns
      | pattern_type           | setup_action          | expected_behavior     |
      | immediate_notification | subscribe_then_update | notification_received |
      | batch_notifications    | multiple_updates      | single_notification   |
      | unsubscribe_cleanup    | unsubscribe_all       | notifications_stopped |
      | capability_negotiation | check_listChanged     | notifications_enabled |
    And testing notification content validation
      | notification_type                    | required_field | validation_result |
      | notifications/resources/updated      | uri            | present           |
      | notifications/resources/list_changed | method         | correct           |
      | notifications/prompts/list_changed   | method         | correct           |
      | notifications/tools/list_changed     | method         | correct           |
      | notifications/roots/list_changed     | method         | correct           |
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |

  # - [Authorization](specification/2025-06-18/basic/authorization.mdx)
  Scenario Outline: MCP authorization metadata specification conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When fetching authorization metadata
    Then authorization metadata uses server base URL
    And authorization servers are advertised
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | http      |

