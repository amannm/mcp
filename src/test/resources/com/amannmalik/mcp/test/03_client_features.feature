@client-features
Feature: MCP Client Features
  # Verifies conformance to specification/2025-06-18/client/elicitation.mdx
  # and specification/2025-06-18/client/roots.mdx
  # and specification/2025-06-18/client/sampling.mdx
  As an MCP client application
  I want to provide elicitation, roots, and sampling capabilities to servers
  So that servers can request user input, access filesystem boundaries, and leverage AI models

  Background:
    Given a clean MCP environment
    And I have established an MCP connection
    And I have declared client capabilities

  @elicitation @capability
  Scenario: Elicitation capability declaration
    # Tests specification/2025-06-18/client/elicitation.mdx:44-55 (Capability declaration)
    Given I want to support elicitation requests
    When I declare my capabilities during initialization
    Then I should include the "elicitation" capability
    And the server should recognize my elicitation support

  @elicitation @request-handling
  Scenario: Simple text elicitation request
    # Tests specification/2025-06-18/client/elicitation.mdx:63-100 (Simple text request flow)
    Given I have declared elicitation capability
    When I receive an elicitation/create request for simple text input:
      | message                            | schema_type | required_field |
      | Please provide your GitHub username | object      | name           |
    Then I should present the request to the user
    And I should validate the user's response against the schema
    And I should return the response with action "accept"

  @elicitation @structured-data
  Scenario: Structured data elicitation request
    # Tests specification/2025-06-18/client/elicitation.mdx:102-152 (Structured data request)
    Given I have declared elicitation capability
    When I receive an elicitation/create request for structured data:
      | field_name | field_type | required | description        |
      | name       | string     | true     | Your full name     |
      | email      | string     | true     | Your email address |
      | age        | number     | false    | Your age           |
    Then I should generate appropriate input forms
    And I should validate each field against its schema
    And I should return valid structured data

  @elicitation @response-actions
  Scenario: Elicitation response action handling
    # Tests specification/2025-06-18/client/elicitation.mdx:284-322 (Response action model)
    Given I have declared elicitation capability
    When I test elicitation responses with the following user actions:
      | user_action      | expected_action |
      | submit_data      | accept          |
      | click_decline    | decline         |
      | close_dialog     | cancel          |
      | press_escape     | cancel          |
      | explicit_reject  | decline         |
    Then I should return the correct action for each user interaction
    And I should include content data only for "accept" actions

  @elicitation @schema-validation
  Scenario: Elicitation schema type support
    # Tests specification/2025-06-18/client/elicitation.mdx:222-282 (Supported schema types)
    Given I have declared elicitation capability
    When I receive elicitation requests with different schema types:
      | schema_type | format     | validation_rules      |
      | string      | email      | format validation     |
      | string      | uri        | URI format            |
      | number      | none       | min/max constraints   |
      | integer     | none       | integer validation    |
      | boolean     | none       | true/false values     |
      | string      | enum       | allowed values only   |
    Then I should support all specified primitive types
    And I should validate input according to schema constraints
    And I should reject nested objects and arrays

  @elicitation @security
  Scenario: Elicitation security controls
    # Tests specification/2025-06-18/client/elicitation.mdx:30-42 (Security warnings)
    # Tests specification/2025-06-18/client/elicitation.mdx:323-332 (Security considerations)
    Given I have declared elicitation capability
    When I receive elicitation requests
    Then I should provide clear indication of which server is requesting information
    And I should allow users to review and modify responses before sending
    And I should provide clear decline and cancel options
    And I should implement rate limiting for elicitation requests

  @roots @capability
  Scenario: Roots capability declaration
    # Tests specification/2025-06-18/client/roots.mdx:27-43 (Capability declaration)
    Given I want to support filesystem roots
    When I declare my capabilities during initialization
    Then I should include the "roots" capability
    And I should indicate whether I support "listChanged" notifications

  @roots @listing
  Scenario: Roots listing request
    # Tests specification/2025-06-18/client/roots.mdx:47-76 (Listing roots)
    Given I have declared roots capability
    And I have configured the following roots:
      | uri                                  | name              |
      | file:///home/user/projects/myproject | My Project        |
      | file:///home/user/repos/frontend     | Frontend Repository |
    When I receive a roots/list request
    Then I should return all configured roots
    And each root should have a valid file:// URI
    And each root should include an optional human-readable name

  @roots @change-notifications
  Scenario: Roots list change notifications
    # Tests specification/2025-06-18/client/roots.mdx:78-104 (Root list changes)
    Given I have declared roots capability with listChanged support
    And I have an established connection
    When my root configuration changes
    Then I should send a notifications/roots/list_changed notification
    And the server should be able to request an updated roots list

  @roots @security
  Scenario: Roots security boundaries
    # Tests specification/2025-06-18/client/roots.mdx:165-193 (Security and implementation guidelines)
    Given I have declared roots capability
    When I configure roots for server access
    Then I should only expose roots with appropriate permissions
    And I should validate all root URIs to prevent path traversal
    And I should implement proper access controls
    And I should prompt users for consent before exposing roots

  @roots @error-handling
  Scenario: Roots error responses
    # Tests specification/2025-06-18/client/roots.mdx:142-163 (Error handling)
    Given I do not support roots capability
    When I receive a roots/list request
    Then I should return error code -32601 (Method not found)
    And the error message should indicate "Roots not supported"

  @sampling @capability
  Scenario: Sampling capability declaration
    # Tests specification/2025-06-18/client/sampling.mdx:38-49 (Capability declaration)
    Given I want to support LLM sampling requests
    When I declare my capabilities during initialization
    Then I should include the "sampling" capability
    And the server should recognize my sampling support

  @sampling @message-creation
  Scenario: Basic sampling message creation
    # Tests specification/2025-06-18/client/sampling.mdx:53-105 (Creating messages)
    Given I have declared sampling capability
    When I receive a sampling/createMessage request:
      | message_content                | system_prompt              | max_tokens |
      | What is the capital of France? | You are a helpful assistant | 100        |
    Then I should present the request for user approval
    And I should forward the approved request to the language model
    And I should return the model's response with metadata

  @sampling @content-types
  Scenario: Sampling content type support
    # Tests specification/2025-06-18/client/sampling.mdx:136-168 (Data types - content)
    Given I have declared sampling capability
    When I receive sampling requests with different content types:
      | content_type | data_format        | mime_type   |
      | text         | plain_text         | none        |
      | image        | base64_encoded     | image/jpeg  |
      | audio        | base64_encoded     | audio/wav   |
    Then I should support all specified content types
    And I should handle base64 encoding for binary data
    And I should validate mime types for media content

  @sampling @model-preferences
  Scenario: Model preference handling
    # Tests specification/2025-06-18/client/sampling.mdx:170-216 (Model preferences)
    Given I have declared sampling capability
    When I receive sampling requests with model preferences:
      | hint_model      | cost_priority | speed_priority | intelligence_priority |
      | claude-3-sonnet | 0.3           | 0.8            | 0.5                   |
      | claude          | 0.7           | 0.2            | 0.9                   |
    Then I should process hints as substrings for model matching
    And I should consider priority values for model selection
    And I should map hints to equivalent models from available providers
    And I should make final model selection based on combined preferences

  @sampling @human-in-loop
  Scenario: Sampling human-in-the-loop controls
    # Tests specification/2025-06-18/client/sampling.mdx:25-36 (Human-in-the-loop requirements)
    # Tests specification/2025-06-18/client/sampling.mdx:118-133 (Message flow with user review)
    Given I have declared sampling capability
    When I receive a sampling request
    Then I should present the request for user approval
    And I should allow users to view and edit prompts before sending
    And I should present generated responses for review before delivery
    And I should provide options to deny or modify the request

  @sampling @error-handling
  Scenario: Sampling error responses
    # Tests specification/2025-06-18/client/sampling.mdx:217-232 (Error handling)
    Given I have declared sampling capability
    When sampling requests are rejected or fail:
      | failure_reason           | expected_error                    |
      | user_rejection           | User rejected sampling request    |
      | model_unavailable        | Requested model not available     |
      | rate_limit_exceeded      | Rate limit exceeded               |
      | invalid_content_type     | Unsupported content type          |
    Then I should return appropriate error responses
    And error messages should be clear and actionable

  @sampling @security
  Scenario: Sampling security controls
    # Tests specification/2025-06-18/client/sampling.mdx:234-241 (Security considerations)
    Given I have declared sampling capability
    When processing sampling requests
    Then I should implement user approval controls
    And I should validate all message content
    And I should implement rate limiting
    And I should handle sensitive data appropriately
    And I should respect model preference hints while maintaining security

  @integration @multi-capability
  Scenario: Multiple client capabilities integration
    # Tests integration between client features
    Given I have declared multiple client capabilities:
      | capability  | features           |
      | roots       | listChanged: true  |
      | sampling    | {}                 |
      | elicitation | {}                 |
    When I receive requests that combine these capabilities
    Then each capability should function independently
    And capabilities should not interfere with each other
    And I should maintain consistent user interaction patterns

  @integration @capability-discovery
  Scenario: Client capability negotiation
    # Tests specification/2025-06-18/basic/lifecycle.mdx:146-171 (Capability negotiation)
    Given I want to test capability negotiation with different configurations:
      | declared_capabilities    | server_expectations      | negotiation_result |
      | roots,sampling          | roots                    | success            |
      | elicitation             | elicitation,sampling     | partial            |
      | none                    | roots                    | degraded           |
      | roots,sampling,elicitation | none                  | unused             |
    When I complete capability negotiation for each configuration
    Then the agreed capabilities should match the intersection of client and server support
    And both parties should respect the negotiated capability boundaries

  @security @capability-boundaries
  Scenario: Client capability boundary enforcement
    # Tests that servers respect declared client capabilities
    Given I have declared only specific client capabilities:
      | declared_capability | undeclared_capability |
      | roots              | sampling              |
      | sampling           | elicitation           |
      | elicitation        | roots                 |
    When the server attempts to use undeclared capabilities
    Then I should reject requests for undeclared capabilities
    And I should return appropriate "Method not found" errors
    And I should maintain security boundaries for unsupported features

  @error-handling @graceful-degradation
  Scenario: Client feature graceful degradation
    # Tests client behavior when features become unavailable
    Given I have declared client capabilities
    When client features become temporarily unavailable:
      | feature_type | unavailability_reason    | expected_behavior        |
      | roots        | filesystem_disconnected  | error with retry option  |
      | sampling     | model_service_down       | clear error message      |
      | elicitation  | ui_component_failure     | fallback interaction     |
    Then I should handle each unavailability gracefully
    And I should provide clear error messages to servers
    And I should attempt recovery when possible