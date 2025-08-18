@server
Feature: MCP Server Features
  # Verifies conformance to specification/2025-06-18/server/tools.mdx
  # and specification/2025-06-18/server/resources.mdx
  # and specification/2025-06-18/server/prompts.mdx
  # and specification/2025-06-18/server/utilities/logging.mdx
  # and specification/2025-06-18/server/utilities/completion.mdx
  As an MCP client application
  I want to interact with server-exposed features and capabilities
  So that I can access tools, resources, prompts, logging, and completion services

  Background:
    Given a clean MCP environment
    And an established MCP connection with server capabilities

  @tools @capabilities
  Scenario: Server tools capability declaration
    # Tests specification/2025-06-18/server/tools.mdx:40-53 (Tools capability)
    Given the server supports tools functionality
    When I check server capabilities during initialization
    Then the server should declare the "tools" capability
    And the capability should include "listChanged" configuration

  @tools @listing
  Scenario: Tools discovery and listing
    # Tests specification/2025-06-18/server/tools.mdx:57-102 (Listing tools)
    Given the server has tools capability enabled
    When I send a "tools/list" request
    Then I should receive a list of available tools
    And each tool should have required fields:
      | field        | type   | required |
      | name         | string | true     |
      | description  | string | true     |
      | inputSchema  | object | true     |
      | title        | string | false    |
      | outputSchema | object | false    |

  @tools @pagination
  Scenario: Tools listing with pagination
    # Tests specification/2025-06-18/server/tools.mdx:60 (Pagination support)
    # Tests specification/2025-06-18/server/utilities/pagination.mdx (Pagination flow)
    Given the server has multiple tools available
    When I send a "tools/list" request with pagination parameters
    Then I should receive paginated tool results
    And the response should include appropriate cursor information

  @tools @invocation
  Scenario: Tool invocation with valid parameters
    # Tests specification/2025-06-18/server/tools.mdx:104-140 (Calling tools)
    Given the server has a tool named "echo_tool"
    And the tool expects required parameter "msg"
    When I call the tool with valid arguments:
      | parameter | value |
      | msg       | hello |
    Then the tool should execute successfully
    And I should receive tool result content
    And the result should have "isError" field set to false

  @tools @error-handling
  Scenario: Tool invocation error handling
    # Tests specification/2025-06-18/server/tools.mdx:381-425 (Error handling)
    Given the server has tools available
    When I test tool error scenarios:
      | scenario                    | error_type     | expected_code |
      | invalid tool name           | Protocol error | -32602        |
      | missing required parameters | Protocol error | -32602        |
      | invalid parameter types     | Protocol error | -32602        |
      | tool execution failure      | Tool error     | none          |
    Then I should receive appropriate error responses for each scenario

  @tools @notifications
  Scenario: Tool list change notifications
    # Tests specification/2025-06-18/server/tools.mdx:142-152 (List changed notification)
    Given the server has tools capability with "listChanged" enabled
    And I have subscribed to tool updates
    When the server's tool list changes
    Then I should receive a "notifications/tools/list_changed" notification

  @tools @content-types
  Scenario: Tool result content types
    # Tests specification/2025-06-18/server/tools.mdx:201-296 (Content types)
    Given the server has tools that return different content types
    When I invoke tools with various output formats
    Then I should receive valid result content in supported formats:
      | content_type  | field_name | encoding |
      | text          | text       | plain    |
      | image         | data       | base64   |
      | audio         | data       | base64   |
      | resource_link | uri        | none     |
      | resource      | resource   | none     |

  @resources @capabilities
  Scenario: Server resources capability declaration
    # Tests specification/2025-06-18/server/resources.mdx:32-83 (Resources capability)
    Given the server supports resources functionality
    When I check server capabilities during initialization
    Then the server should declare the "resources" capability
    And the capability may include optional features:
      | feature     | description                   |
      | subscribe   | Resource change subscriptions |
      | listChanged | List change notifications     |

  @resources @listing
  Scenario: Resources discovery and listing
    # Tests specification/2025-06-18/server/resources.mdx:85-124 (Listing resources)
    Given the server has resources capability enabled
    When I send a "resources/list" request
    Then I should receive a list of available resources
    And each resource should have required fields:
      | field       | type   | required |
      | uri         | string | true     |
      | name        | string | true     |
      | title       | string | false    |
      | description | string | false    |
      | mimeType    | string | false    |

  @resources @reading
  Scenario: Resource content reading
    # Tests specification/2025-06-18/server/resources.mdx:126-161 (Reading resources)
    Given the server has a resource with URI "file:///project/src/main.rs"
    When I send a "resources/read" request for that URI
    Then I should receive the resource contents
    And the content should match the resource metadata
    And the content should be in valid format (text or blob)

  @resources @templates
  Scenario: Resource templates discovery
    # Tests specification/2025-06-18/server/resources.mdx:163-197 (Resource templates)
    Given the server supports resource templates
    When I send a "resources/templates/list" request
    Then I should receive a list of available resource templates
    And each template should have required fields:
      | field       | type   | required |
      | uriTemplate | string | true     |
      | name        | string | true     |
      | title       | string | false    |
      | description | string | false    |
      | mimeType    | string | false    |

  @resources @subscriptions
  Scenario: Resource change subscriptions
    # Tests specification/2025-06-18/server/resources.mdx:211-240 (Subscriptions)
    Given the server has resources capability with "subscribe" enabled
    And there is a resource I want to monitor
    When I send a "resources/subscribe" request for the resource URI
    Then I should receive subscription confirmation
    And when the resource changes, I should receive "notifications/resources/updated"

  @resources @subscriptions
  Scenario: Resource unsubscription
    # Tests specification/2025-06-18/server/resources.mdx:211-240 (Subscriptions)
    # Tests specification/2025-06-18/schema.ts:473-481 (resources/unsubscribe)
    Given the server has resources capability with "subscribe" enabled
    And there is a resource I want to monitor
    And I send a "resources/subscribe" request for the resource URI
    When I send a "resources/unsubscribe" request for the resource URI
    Then I should receive unsubscription confirmation
    And a subsequent "resources/unsubscribe" request should result in error

  @resources @notifications
  Scenario: Resource list change notifications
    # Tests specification/2025-06-18/server/resources.mdx:199-209 (List changed notification)
    Given the server has resources capability with "listChanged" enabled
    When the server's resource list changes
    Then I should receive a "notifications/resources/list_changed" notification

  @resources @annotations
  Scenario: Resource annotations handling
    # Tests specification/2025-06-18/server/resources.mdx:308-337 (Annotations)
    Given the server has resources with annotations
    When I retrieve resource information
    Then the annotations should include valid metadata:
      | annotation   | type      | description                     |
      | audience     | array     | Intended audience specification |
      | priority     | number    | Importance from 0.0 to 1.0      |
      | lastModified | timestamp | ISO 8601 modification time      |

  @resources @uri-schemes
  Scenario: Standard URI scheme support
    # Tests specification/2025-06-18/server/resources.mdx:338-372 (Common URI schemes)
    Given the server exposes resources with different URI schemes
    When I request resources using standard schemes
    Then the server should handle these URI schemes appropriately:
      | scheme | description                   |
      | https  | Web-accessible resources      |
      | file   | Filesystem-like resources     |
      | git    | Git version control resources |

  @resources @error-handling
  Scenario: Resource error handling
    # Tests specification/2025-06-18/server/resources.mdx:374-395 (Error handling)
    Given the server has resources capability
    When I test resource error scenarios:
      | scenario             | error_code | error_message      |
      | nonexistent resource | -32002     | Resource not found |
      | invalid URI format   | -32602     | Invalid params     |
    Then I should receive appropriate JSON-RPC error responses

  @prompts @capabilities
  Scenario: Server prompts capability declaration
    # Tests specification/2025-06-18/server/prompts.mdx:30-46 (Prompts capability)
    Given the server supports prompts functionality
    When I check server capabilities during initialization
    Then the server should declare the "prompts" capability
    And the capability should include "listChanged" configuration

  @prompts @listing
  Scenario: Prompts discovery and listing
    # Tests specification/2025-06-18/server/prompts.mdx:48-92 (Listing prompts)
    Given the server has prompts capability enabled
    When I send a "prompts/list" request
    Then I should receive a list of available prompts
    And each prompt should have required fields:
      | field       | type   | required |
      | name        | string | true     |
      | title       | string | false    |
      | description | string | false    |
      | arguments   | array  | false    |

  @prompts @retrieval
  Scenario: Prompt retrieval with arguments
    # Tests specification/2025-06-18/server/prompts.mdx:94-134 (Getting prompts)
    Given the server has a prompt named "code_review"
    And the prompt accepts argument "code"
    When I send a "prompts/get" request with arguments:
      | argument | value                            |
      | code     | def hello():\n    print('world') |
    Then I should receive the prompt content
    And the response should contain message array
    And each message should have role and content fields

  @prompts @content-types
  Scenario: Prompt message content types
    # Tests specification/2025-06-18/server/prompts.mdx:183-263 (Content types)
    Given the server has prompts with various content types
    When I retrieve prompts with different message content
    Then I should receive valid content in supported formats:
      | content_type | required_fields      |
      | text         | type, text           |
      | image        | type, data, mimeType |
      | audio        | type, data, mimeType |
      | resource     | type, resource       |

  @prompts @notifications
  Scenario: Prompt list change notifications
    # Tests specification/2025-06-18/server/prompts.mdx:135-146 (List changed notification)
    Given the server has prompts capability with "listChanged" enabled
    When the server's prompt list changes
    Then I should receive a "notifications/prompts/list_changed" notification

  @prompts @error-handling
  Scenario: Prompt error handling
    # Tests specification/2025-06-18/server/prompts.mdx:265-272 (Error handling)
    Given the server has prompts capability
    When I test prompt error scenarios:
      | scenario                   | error_code | error_message  |
      | invalid prompt name        | -32602     | Invalid params |
      | missing required arguments | -32602     | Invalid params |
      | server internal error      | -32603     | Internal error |
    Then I should receive appropriate JSON-RPC error responses

  @logging @capabilities
  Scenario: Server logging capability declaration
    # Tests specification/2025-06-18/server/utilities/logging.mdx:20-29 (Logging capability)
    Given the server supports logging functionality
    When I check server capabilities during initialization
    Then the server should declare the "logging" capability

  @logging @level-setting
  Scenario: Log level configuration
    # Tests specification/2025-06-18/server/utilities/logging.mdx:49-64 (Setting log level)
    Given the server has logging capability enabled
    When I send a "logging/setLevel" request with level "info"
    And the server generates log messages at levels:
      | level |
      | debug |
      | info  |
      | error |
    Then the server should accept the log level configuration
    And only messages at "info" level and above should be sent

  @logging @message-notifications
  Scenario: Log message notifications
    # Tests specification/2025-06-18/server/utilities/logging.mdx:66-86 (Log notifications)
    Given the server has logging capability enabled
    And I have set an appropriate log level
    When the server generates log messages
    Then I should receive "notifications/message" notifications
    And each notification should include:
      | field  | type   | required | description            |
      | level  | string | true     | Severity level         |
      | logger | string | false    | Optional logger name   |
      | data   | object | true     | JSON-serializable data |

  @logging @severity-levels
  Scenario: Log severity level handling
    # Tests specification/2025-06-18/server/utilities/logging.mdx:31-45 (Log levels)
    Given the server has logging capability enabled
    When I configure different log levels
    Then the server should respect the severity hierarchy:
      | level     | numeric_value | should_include_above |
      | debug     | 7             | true                 |
      | info      | 6             | true                 |
      | notice    | 5             | true                 |
      | warning   | 4             | true                 |
      | error     | 3             | true                 |
      | critical  | 2             | true                 |
      | alert     | 1             | true                 |
      | emergency | 0             | true                 |

  @logging @error-handling
  Scenario: Logging error handling
    # Tests specification/2025-06-18/server/utilities/logging.mdx:110-116 (Error handling)
    Given the server has logging capability
    When I test logging error scenarios:
      | scenario            | error_code | error_message  |
      | invalid log level   | -32602     | Invalid params |
    Then I should receive appropriate JSON-RPC error responses

  @completion @capabilities
  Scenario: Server completion capability declaration
    # Tests specification/2025-06-18/server/utilities/completion.mdx:26-36 (Completion capability)
    Given the server supports completion functionality
    When I check server capabilities during initialization
    Then the server should declare the "completions" capability

  @completion @prompt-arguments
  Scenario: Prompt argument completion
    # Tests specification/2025-06-18/server/utilities/completion.mdx:40-79 (Completion requests)
    Given the server has completion capability enabled
    And there is a prompt "code_review" with argument "language"
    When I send a "completion/complete" request for prompt argument completion:
      | ref_type   | ref_name    | argument_name | argument_value |
      | ref/prompt | code_review | language      | py             |
    Then I should receive completion suggestions
    And the response should include matching values like "python"

  @completion @resource-templates
  Scenario: Resource template argument completion
    # Tests specification/2025-06-18/server/utilities/completion.mdx:124-131 (Reference types)
    Given the server has completion capability enabled
    And there is a resource template with URI template "file:///path"
    When I send a "completion/complete" request for resource template argument:
      | ref_type     | ref_uri        | argument_name | argument_value |
      | ref/resource | file:///{path} | path          | src/           |
    Then I should receive completion suggestions for available paths

  @completion @contextual
  Scenario: Contextual completion with multiple arguments
    # Tests specification/2025-06-18/server/utilities/completion.mdx:81-122 (Context arguments)
    Given the server has completion capability enabled
    And there is a prompt with multiple arguments
    When I request completion with context from previous arguments:
      | argument  | value  |
      | language  | python |
      | framework | fla    |
    Then the server should provide contextually relevant suggestions
    And the suggestions should be filtered based on previous context

  @completion @result-limits
  Scenario: Completion result pagination and limits
    # Tests specification/2025-06-18/server/utilities/completion.mdx:133-140 (Completion results)
    Given the server has completion capability enabled
    When I request completions that have many matches
    Then the response should respect the maximum of 100 items
    And include metadata about total matches and whether more exist:
      | field   | type    | description                   |
      | values  | array   | Maximum 100 completion values |
      | total   | number  | Optional total available      |
      | hasMore | boolean | Whether more results exist    |

  @completion @error-handling
  Scenario: Completion error handling
    # Tests specification/2025-06-18/server/utilities/completion.mdx:175-183 (Error handling)
    Given the server has completion capability
    When I test completion error scenarios:
      | scenario                   | error_code | error_message    |
      | capability not supported   | -32601     | Method not found |
      | invalid prompt name        | -32602     | Invalid params   |
      | missing required arguments | -32602     | Invalid params   |
      | server internal error      | -32603     | Internal error   |
    Then I should receive appropriate JSON-RPC error responses

  @security @input-validation
  Scenario: Server input validation and sanitization
    # Tests specification/2025-06-18/server/tools.mdx:427-442 (Security considerations)
    # Tests specification/2025-06-18/server/resources.mdx:397-402 (Security considerations)
    Given the server has security controls enabled
    When I send requests with potentially malicious input
    Then the server should validate and sanitize all inputs
    And reject requests with invalid or dangerous parameters
    And implement appropriate rate limiting

  @security @access-control
  Scenario: Resource access control validation
    # Tests specification/2025-06-18/server/resources.mdx:400-401 (Access controls)
    Given the server has access controls configured
    When I attempt to access restricted resources
    Then the server should enforce proper access controls
    And deny access to unauthorized resources
    And provide appropriate error messages

  @security @sensitive-data
  Scenario: Sensitive information protection
    # Tests specification/2025-06-18/server/utilities/logging.mdx:133-144 (Security)
    Given the server handles sensitive information
    When the server generates logs, tool results, or other outputs
    Then sensitive information should not be exposed:
      | sensitive_type          | should_be_filtered |
      | credentials             | true               |
      | secrets                 | true               |
      | personal information    | true               |
      | internal system details | true               |

  @integration @multi-capability
  Scenario: Combined server feature interaction
    # Tests integration between multiple server capabilities
    Given the server supports multiple capabilities
    When I use tools that reference resources and prompts
    Then the features should work together seamlessly
    And completion should work for all supported argument types
    And logging should capture activities across all features
