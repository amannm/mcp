Feature: MCP protocol conformance

  Scenario Outline: MCP Complete Protocol Conformance
    Given a running MCP server using <transport> transport
    Then capabilities should be advertised and ping succeeds
    When testing all comprehensive operations
      | spec_section  | operation                      | parameter      | expected_result                                                                     | expected_error_code | transport_filter |
      # Base Protocol & Resources - specification/2025-06-18/server/resources.mdx
      | resources     | list_resources                 |                | test://example                                                                      |                     | all              |
      | resources     | read_resource                  | test://example | hello                                                                               |                     | all              |
      | resources     | resource_metadata              | example        | text/plain                                                                          |                     | all              |
      | resources     | list_resources_annotations     | example        | 0.5                                                                                 |                     | all              |
      | resources     | list_templates                 |                | 1                                                                                   |                     | all              |
      | resources     | subscribe_resource             | test://example | success                                                                             |                     | all              |
      | resources     | unsubscribe_resource           | test://example | success                                                                             |                     | all              |
      | resources     | read_invalid_uri               | bad://uri      |                                                                                     | -32002              | all              |
      | resources     | list_resources_invalid_cursor  | notacursor     |                                                                                     | -32602              | all              |
      | resources     | subscribe_invalid_resource     | bad://uri      |                                                                                     | -32002              | all              |
      | resources     | unsubscribe_nonexistent        | fake://uri     |                                                                                     | -32602              | all              |
      | resources     | subscribe_duplicate_resource   | test://example |                                                                                     | -32602              | all              |
      # Tools - specification/2025-06-18/server/tools.mdx
      | tools         | list_tools                     |                | test_tool                                                                           |                     | all              |
      | tools         | list_tools_schema              |                | test_tool                                                                           |                     | all              |
      | tools         | list_tools_output_schema       |                | test_tool                                                                           |                     | all              |
      | tools         | list_tools_annotations         | test_tool      | true                                                                                |                     | all              |
      | tools         | call_tool                      | test_tool      | ok                                                                                  |                     | all              |
      | tools         | call_tool_structured           | test_tool      | ok                                                                                  |                     | all              |
      | tools         | call_tool_error                | error_tool     | fail                                                                                |                     | all              |
      | tools         | call_tool_elicit               | echo_tool      | ping                                                                                |                     | all              |
      | tools         | call_unknown_tool              | nope           |                                                                                     | -32602              | all              |
      | tools         | list_tools_invalid_cursor      | notacursor     |                                                                                     | -32602              | all              |
      | tools         | cancel_tool_call               | slow_tool      |                                                                                     | -32603              | all              |
      | tools         | call_tool_elicit_cancel        | echo_tool      |                                                                                     | -32602              | all              |
      | tools         | call_tool_elicit_decline       | echo_tool      |                                                                                     | -32602              | all              |
      | tools         | call_tool_elicit_invalid       | echo_tool      |                                                                                     | -32602              | all              |
      # Prompts - specification/2025-06-18/server/prompts.mdx
      | prompts       | list_prompts                   |                | 1                                                                                   |                     | all              |
      | prompts       | list_prompt_name               |                | test_prompt                                                                         |                     | all              |
      | prompts       | list_prompt_arg_required       |                | true                                                                                |                     | all              |
      | prompts       | get_prompt                     | test_prompt    | hello                                                                               |                     | all              |
      | prompts       | get_prompt_text                | test_prompt    | hello                                                                               |                     | all              |
      | prompts       | get_prompt_role                | test_prompt    | user                                                                                |                     | all              |
      | prompts       | get_prompt_invalid             | nope           |                                                                                     | -32602              | all              |
      | prompts       | get_prompt_missing_arg         | test_prompt    |                                                                                     | -32602              | all              |
      | prompts       | list_prompts_invalid_cursor    | notacursor     |                                                                                     | -32602              | all              |
      # Completion - specification/2025-06-18/server/utilities/completion.mdx
      | completion    | request_completion             |                | test_completion                                                                     |                     | all              |
      | completion    | request_completion_invalid     |                |                                                                                     | -32602              | all              |
      | completion    | request_completion_missing_arg |                |                                                                                     | -32602              | all              |
      | completion    | request_completion_missing_ref |                |                                                                                     | -32602              | all              |
      # Logging - specification/2025-06-18/server/utilities/logging.mdx
      | logging       | set_log_level                  | debug          | success                                                                             |                     | all              |
      | logging       | set_log_level                  | warning        | success                                                                             |                     | all              |
      | logging       | set_log_level_invalid          | verbose        |                                                                                     | -32602              | all              |
      | logging       | set_log_level_missing          |                |                                                                                     | -32602              | all              |
      | logging       | set_log_level_extra            | warning        |                                                                                     | -32602              | all              |
      # Sampling - specification/2025-06-18/client/sampling.mdx
      | sampling      | request_sampling               |                | ok                                                                                  |                     | all              |
      | sampling      | request_sampling_reject        |                |                                                                                     | -32603              | all              |
      # Ping - specification/2025-06-18/basic/utilities/ping.mdx
      | ping          | ping_invalid                   | oops           |                                                                                     | -32602              | all              |
      # Roots - specification/2025-06-18/client/roots.mdx
      | roots         | roots_listed                   |                | 1                                                                                   |                     | all              |
      | roots         | roots_invalid                  |                |                                                                                     | -32601              | all              |
      # Authorization - specification/2025-06-18/basic/authorization.mdx
      | authorization | unauthorized_request           |                | Bearer resource_metadata="https://example.com/.well-known/oauth-protected-resource" |                     | http             |
      | authorization | resource_metadata_auth_server  |                | https://auth.example.com                                                            |                     | http             |
    And a cancellation log message is received
    When testing subscription capabilities
      | capability_type | feature     | expected_support |
      | resources       | subscribe   | true             |
      | resources       | listChanged | true             |
      | prompts         | listChanged | true             |
      | tools           | listChanged | true             |
      | roots           | listChanged | true             |
    And testing comprehensive notification behaviors
      | notification_type                    | trigger_action         | expected_notification | operation             | parameter      | expected_result | expected_error_code |
      | notifications/resources/list_changed | modify_resource_list   | received              | list_resources        |                | test://example  |                     |
      | notifications/resources/updated      | update_subscribed      | received              | subscribe_resource    | test://example | success         |                     |
      | notifications/resources/updated      | update_subscribed      | received              | read_resource         | test://example | hello           |                     |
      | notifications/prompts/list_changed   | modify_prompt_list     | received              | list_prompts          |                | 1               |                     |
      | notifications/tools/list_changed     | modify_tool_list       | received              | list_tools            |                | test_tool       |                     |
      | notifications/roots/list_changed     | modify_root_list       | received              | roots_listed          |                | 1               |                     |
      | notifications/resources/updated      | immediate_notification | received              | subscribe_then_update | test://example | success         |                     |
      | notifications/resources/updated      | batch_notifications    | single_notification   | multiple_updates      | test://example | success         |                     |
      | notifications/resources/updated      | unsubscribe_cleanup    | notifications_stopped | unsubscribe_all       | test://example | success         |                     |
      | notifications/resources/updated      | capability_negotiation | notifications_enabled | check_listChanged     |                | success         |                     |
    And testing comprehensive notification validation
      | notification_type                    | required_field | validation_result | operation      | parameter      | expected_result |
      | notifications/resources/updated      | uri            | present           | read_resource  | test://example | hello           |
      | notifications/resources/list_changed | method         | correct           | list_resources |                | test://example  |
      | notifications/prompts/list_changed   | method         | correct           | list_prompts   |                | 1               |
      | notifications/tools/list_changed     | method         | correct           | list_tools     |                | test_tool       |
      | notifications/roots/list_changed     | method         | correct           | roots_listed   |                | 1               |
    When listing tools with pagination
    Then pagination covers all tools
    When requesting resource list with progress tracking for stdio transport
    Then progress updates are received for stdio transport
    And progress completes to 1.0 for stdio transport
    And progress message is provided for stdio transport
    When fetching authorization metadata for http transport
    Then authorization metadata uses server base URL for http transport
    And authorization servers are advertised for http transport
    When the client disconnects
    Then the server terminates cleanly

    Examples:
      | transport |
      | stdio     |
      | http      |