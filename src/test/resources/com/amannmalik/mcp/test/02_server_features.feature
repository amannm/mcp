@server-features
Feature: MCP Server Features - Resources, Tools, and Prompts
  As an MCP server implementation
  I want to expose resources, tools, and prompts correctly
  So that clients can access context, execute functions, and use templates

  Background:
    Given an operational MCP connection
    And server has declared appropriate capabilities

  @resources @smoke
  Scenario: Basic resource operations
    Given server has resources capability
    When client requests resources/list
    Then server returns paginated resource list
    And each resource has uri, name, optional title/description/mimeType
    When client requests resources/read with valid URI
    Then server returns resource contents
    And contents match expected format (text/blob)

  @resources @pagination
  Scenario: Resource list pagination
    Given server has 100 resources
    When client requests resources/list with limit 10
    Then server returns 10 resources and nextCursor
    When client requests with returned cursor
    Then server returns next 10 resources
    When client reaches end of list
    Then server returns resources without nextCursor

  @resources @templates
  Scenario: Resource templates with URI patterns
    Given server supports resource templates
    When client requests resources/templates/list
    Then server returns available templates
    And templates include uriTemplate with placeholders
    When client requests resource with template URI
    Then server processes template variables
    And returns appropriate resource content

  @resources @subscriptions
  Scenario: Resource change subscriptions
    Given server supports resource subscriptions
    When client subscribes to specific resource URI
    Then server confirms subscription
    When subscribed resource changes
    Then server sends resources/updated notification
    And notification includes URI and optional title
    When client unsubscribes from resource
    Then server stops sending update notifications

  @resources @list-changes
  Scenario: Resource list change notifications
    Given server supports listChanged capability
    When server's resource list changes
    Then server sends resources/list_changed notification
    And client can re-request resources/list
    When client receives notification
    Then client should refresh its resource cache

  @resources @content-types
  Scenario Outline: Resource content type handling
    Given server has resource with <content_type>
    When client reads the resource
    Then response contains <expected_fields>
    And content follows <format_rules>

    Examples:
      | content_type | expected_fields    | format_rules              |
      | text         | uri,name,text      | UTF-8 encoded string      |
      | binary       | uri,name,blob      | base64 encoded data       |
      | mixed        | uri,name,text,blob | both text and blob fields |

  @resources @annotations
  Scenario: Resource annotations for context hints
    Given server has resources with annotations
    When client lists resources
    Then annotations include audience, priority, lastModified
    And audience contains "user" and/or "assistant"
    And priority is between 0.0 and 1.0
    And lastModified follows ISO 8601 format
    When client filters by audience
    Then only matching resources are considered

  @tools @smoke
  Scenario: Basic tool operations
    Given server has tools capability
    When client requests tools/list
    Then server returns available tools
    And each tool has name, description, inputSchema
    When client calls tool with valid arguments
    Then server executes tool and returns result
    And result contains content array
    And isError indicates success/failure

  @tools @schemas
  Scenario: Tool input and output schema validation
    Given server has tool with input/output schemas
    When client calls tool with valid input
    Then server validates against input schema
    And returns result matching output schema
    When client calls tool with invalid input
    Then server returns validation error
    And error indicates schema violation

  @tools @content-types
  Scenario Outline: Tool result content types
    Given server has tool returning <content_type>
    When client calls the tool
    Then result contains <expected_content>
    And follows <content_format>

    Examples:
      | content_type      | expected_content               | content_format           |
      | text              | type:text, text field          | plain text content       |
      | image             | type:image, data, mimeType     | base64 encoded image     |
      | audio             | type:audio, data, mimeType     | base64 encoded audio     |
      | resource_link     | type:resource_link, uri, name  | URI to existing resource |
      | embedded_resource | type:resource, resource object | full resource content    |
      | structured        | structuredContent JSON object  | schema-validated JSON    |

  @tools @annotations
  Scenario: Tool result annotations
    Given server has tool returning annotated content
    When client calls the tool
    Then result content includes annotations
    And annotations specify audience, priority
    And client uses annotations for context decisions

  @tools @errors
  Scenario: Tool execution error handling
    Given server has tool that can fail
    When tool execution encounters business logic error
    Then server returns result with isError:true
    And content describes the error condition
    When tool encounters protocol error
    Then server returns JSON-RPC error response
    And error includes appropriate error code

  @tools @list-changes
  Scenario: Tool list change notifications
    Given server supports tool listChanged capability
    When server's tool list changes
    Then server sends tools/list_changed notification
    And client can re-request tools/list
    When new tools become available
    Then notification alerts client to refresh

  @prompts @smoke
  Scenario: Basic prompt operations
    Given server has prompts capability
    When client requests prompts/list
    Then server returns available prompt templates
    And each prompt has name, description
    When client requests specific prompt
    Then server returns prompt with messages
    And messages follow conversation format

  @prompts @arguments
  Scenario: Prompt template arguments
    Given server has parameterized prompt template
    When client lists prompts
    Then prompt definition includes argument schema
    When client gets prompt with valid arguments
    Then server processes template variables
    And returns instantiated prompt messages
    When client provides invalid arguments
    Then server returns argument validation error

  @prompts @content-types
  Scenario: Prompt message content types
    Given server has prompt with mixed content
    When client gets the prompt
    Then messages contain text, image, or audio content
    And each content block has appropriate type/fields
    And messages follow role-based conversation structure

  @prompts @list-changes
  Scenario: Prompt list change notifications
    Given server supports prompt listChanged capability
    When server's prompt list changes
    Then server sends prompts/list_changed notification
    And client can re-request prompts/list
    When prompt templates are updated
    Then notification alerts client to changes

  @security @server
  Scenario: Server-side security controls
    Given server implements access controls
    When client requests protected resource
    Then server validates permissions
    And returns appropriate authorization error if denied
    When client calls restricted tool
    Then server enforces tool access policies
    And sanitizes tool outputs for safety

  @uri-schemes
  Scenario Outline: Standard URI scheme handling
    Given server uses <uri_scheme> for resources
    When client accesses resource with scheme
    Then server handles <expected_behavior>
    And follows <scheme_rules>

    Examples:
      | uri_scheme | expected_behavior        | scheme_rules                   |
      | file://    | filesystem-like access   | may not map to physical files  |
      | https://   | web resource reference   | client can fetch independently |
      | git://     | version control resource | git repository integration     |
      | custom://  | implementation-specific  | follows RFC3986 compliance     |