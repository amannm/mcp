@client-features
Feature: MCP Client Features - Sampling, Roots, and Elicitation
  As an MCP client implementation
  I want to support server-initiated LLM sampling, root directory management, and user elicitation
  So that servers can perform agentic behaviors and gather necessary context

  Background:
    Given an operational MCP connection
    And client has declared appropriate capabilities

  @sampling @smoke
  Scenario: Basic LLM sampling request
    Given client has sampling capability
    When server requests sampling/createMessage
    And request includes messages array with user message
    Then client processes sampling request
    And client may present request to user for approval
    When user approves sampling
    Then client forwards to LLM
    And client returns LLM response to server
    And response includes role, content, model, stopReason

  @sampling @model-preferences
  Scenario: Model preference handling
    Given client supports multiple LLMs
    When server requests sampling with model preferences
    And preferences include hints for "claude-3-sonnet"
    And preferences set intelligencePriority: 0.8, speedPriority: 0.5
    Then client evaluates available models
    And client selects best match for preferences
    When client lacks exact model from hints
    Then client maps to equivalent model capabilities
    And client honors priority weights in selection

  @sampling @content-types
  Scenario Outline: Sampling with various content types
    Given client supports <content_type> in sampling
    When server requests sampling with <input_content>
    Then client processes <content_format>
    And forwards to LLM if approved
    And returns appropriate response format

    Examples:
      | content_type | input_content        | content_format          |
      | text         | text message         | plain text string       |
      | image        | base64 image data    | image with mimeType     |
      | audio        | base64 audio data    | audio with mimeType     |
      | mixed        | text + image content | multimodal conversation |

  @sampling @human-in-loop
  Scenario: Human-in-the-loop approval workflow
    Given client requires user approval for sampling
    When server requests sampling
    Then client presents request to user
    And client shows proposed messages and model
    When user reviews and approves request
    Then client proceeds with LLM call
    When user modifies request before approval
    Then client uses modified version
    When user rejects sampling request
    Then client returns rejection error to server

  @sampling @response-review
  Scenario: LLM response review process
    Given client implements response review
    When LLM returns generated content
    Then client presents response to user
    And user can approve or modify response
    When user approves response
    Then client returns response to server
    When user modifies response
    Then client returns modified version to server

  @sampling @security
  Scenario: Sampling security controls
    Given client implements sampling security
    When server requests sampling with sensitive prompts
    Then client validates request for safety
    And client may reject unsafe requests
    When sampling involves user data
    Then client ensures user consent
    And client protects data appropriately

  @roots @smoke
  Scenario: Root directory management
    Given client has roots capability
    When server requests current roots
    Then client returns list of accessible root URIs
    And each root includes uri and optional name
    When client's root list changes
    Then client sends roots/list_changed notification
    And server can request updated root list

  @roots @access-control
  Scenario: Root directory access boundaries
    Given client has configured root directories
    When server requests access within root boundaries
    Then client allows access to permitted paths
    When server requests access outside roots
    Then client denies access appropriately
    And client returns authorization error

  @roots @notifications
  Scenario: Root list change notifications
    Given client supports roots listChanged capability
    When user adds new root directory
    Then client sends roots/list_changed notification
    When user removes root directory access
    Then client sends roots/list_changed notification
    And server should refresh its root understanding

  @elicitation @smoke
  Scenario: User information elicitation
    Given client has elicitation capability
    When server requests additional information from user
    And request specifies information type and prompt
    Then client presents elicitation UI to user
    And client collects requested information
    When user provides information
    Then client returns information to server
    When user declines to provide information
    Then client returns appropriate refusal response

  @elicitation @types
  Scenario Outline: Different elicitation types
    Given client supports <elicitation_type> elicitation
    When server requests <request_content>
    Then client presents <ui_format>
    And client validates <response_format>

    Examples:
      | elicitation_type | request_content    | ui_format           | response_format   |
      | text             | text input request | text input field    | string response   |
      | choice           | multiple choice    | selection interface | chosen option     |
      | confirmation     | yes/no decision    | confirmation dialog | boolean response  |
      | file             | file selection     | file picker dialog  | file content/path |

  @elicitation @security
  Scenario: Elicitation security and privacy
    Given client implements elicitation security
    When server requests sensitive information
    Then client warns user about information sensitivity
    And client allows user to decline safely
    When server requests too much information
    Then client may limit or refuse request
    And client protects user privacy appropriately

  @client-initiated @smoke
  Scenario: Client-initiated requests
    Given operational MCP connection
    When client needs to request server features
    Then client can list available resources/tools/prompts
    And client can read resources or call tools
    When client needs server information
    Then client can request capabilities or metadata
    And server responds with current information

  @capability-checks
  Scenario: Dynamic capability verification
    Given client has negotiated capabilities
    When server requests unsupported operation
    Then client returns capability not supported error
    When client attempts unsupported server operation
    Then server returns appropriate capability error
    When capabilities change during session
    Then both parties handle gracefully

  @concurrent-operations
  Scenario: Concurrent client feature handling
    Given client supports multiple concurrent operations
    When server initiates sampling request
    And server simultaneously requests elicitation
    Then client handles both requests appropriately
    And client may queue or present both to user
    When user responds to multiple requests
    Then client returns responses with correct request IDs

  @error-recovery @client
  Scenario: Client-side error recovery
    Given client encounters internal errors
    When sampling request fails due to LLM unavailability
    Then client returns appropriate error to server
    When elicitation UI fails to display
    Then client falls back to alternative UI or error
    When roots access fails due to permissions
    Then client updates root list and notifies server

  @integration @client-server
  Scenario: Client-server feature integration
    Given client and server both support full capabilities
    When server uses client sampling in tool execution
    Then server can request LLM calls mid-operation
    And client handles nested sampling appropriately
    When server needs user input during resource access
    Then server can request elicitation from client
    And client collects information for server use
    When server operates within client-defined roots
    Then server respects client's access boundaries
    And server adapts behavior to available roots