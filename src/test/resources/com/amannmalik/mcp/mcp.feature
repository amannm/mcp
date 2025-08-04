Feature: Comprehensive MCP Protocol Specification Compliance
  
  # This feature file provides comprehensive testing scenarios for the Model Context Protocol (MCP)
  # implementation against the 2025-06-18 specification revision. Each scenario is grounded in 
  # realistic use cases and includes references to specific sections of the MCP specification.
  #
  # Architecture Reference: specification/2025-06-18/architecture/index.mdx
  # Base Protocol Reference: specification/2025-06-18/basic/index.mdx

  Background:
    # Lifecycle Reference: specification/2025-06-18/basic/lifecycle.mdx#initialization
    Given a clean MCP environment
    And protocol version "2025-06-18" is supported

  @lifecycle @critical
  Scenario: Complete Protocol Lifecycle with Capability Negotiation
    # Reference: specification/2025-06-18/basic/lifecycle.mdx#initialization
    # Tests the complete initialization → operation → shutdown lifecycle
    Given an MCP server with comprehensive capabilities:
      | capability      | feature        | enabled |
      | resources       | subscribe      | true    |
      | resources       | listChanged    | true    |
      | tools           | listChanged    | true    |
      | prompts         | listChanged    | true    |
      | logging         |                | true    |
      | completions     |                | true    |
    And an MCP client with capabilities:
      | capability      | feature        | enabled |
      | sampling        |                | true    |
      | roots           | listChanged    | true    |
      | elicitation     |                | true    |
    When the client initiates connection with protocol version "2025-06-18"
    Then the server responds with supported capabilities
    And capability negotiation completes successfully
    And the client sends "initialized" notification
    Then the connection enters operation phase
    When the client requests shutdown
    Then the connection terminates gracefully
    And all resources are properly cleaned up

  @authorization @security @http
  Scenario: OAuth 2.1 Authorization Flow with Dynamic Client Registration
    # Reference: specification/2025-06-18/basic/authorization.mdx#authorization-flow
    # Tests complete OAuth 2.1 flow including dynamic client registration and resource indicators
    Given an MCP server at "https://mcp.example.com/api" requiring authorization
    And an authorization server at "https://auth.example.com"
    And dynamic client registration is supported
    When the client makes an unauthorized request
    Then the server responds with "401 Unauthorized"
    And includes "WWW-Authenticate" header with resource metadata URL
    When the client fetches protected resource metadata
    Then the metadata contains authorization server URLs
    When the client performs dynamic client registration
    Then a client ID and credentials are obtained
    When the client initiates OAuth 2.1 authorization code flow
    And uses PKCE code challenge method "S256"
    And includes resource parameter "https://mcp.example.com/api"
    And user grants consent through authorization server
    Then authorization code is received at callback
    When the client exchanges code for access token
    And includes PKCE code verifier
    And includes resource parameter "https://mcp.example.com/api"
    Then access token is received with correct audience
    When the client makes MCP requests with Bearer token
    Then requests are successfully authorized
    And token audience validation passes

  @security @token-validation
  Scenario: Token Audience Validation and Passthrough Prevention
    # Reference: specification/2025-06-18/basic/security_best_practices.mdx#token-passthrough
    # Tests security measures against token passthrough and audience validation
    Given an MCP server configured for token validation
    And the server's canonical URI is "https://mcp.example.com"
    When a client presents a token with wrong audience "https://other.example.com"
    Then the server rejects the token with "401 Unauthorized"
    And logs security violation with level "WARNING"
    When a client presents a token without audience claim
    Then the server rejects the token with "401 Unauthorized"
    When a client presents a properly scoped token for "https://mcp.example.com"
    Then the server accepts the token
    And validates token signature and expiration
    And does not pass token to downstream services

  @resources @server-features
  Scenario: Advanced Resource Management with Templates and Subscriptions
    # Reference: specification/2025-06-18/server/resources.mdx
    # Tests complex resource scenarios including templates, subscriptions, and annotations
    Given an MCP server with file system resources
    And resource templates are configured:
      | template                    | description                   | mime_type        |
      | file:///{path}             | Access project files         | text/plain       |
      | git://commits/{branch}     | Git commit history           | application/json |
      | db://tables/{schema}/{table} | Database table contents    | application/json |
    When the client lists resource templates
    Then all templates are returned with proper schemas
    When the client expands template "file:///src/main.rs"
    Then the expanded resource is accessible
    And has proper MIME type "text/x-rust"
    When the client subscribes to resource updates for "file:///src/main.rs"
    Then subscription is confirmed
    When the resource content changes externally
    Then "notifications/resources/updated" is sent to subscriber
    And notification includes URI and updated title
    When the client reads the updated resource
    Then new content is returned
    And proper annotations are included:
      | annotation    | value              |
      | audience      | ["user","assistant"] |
      | priority      | 0.8                |
      | lastModified  | recent timestamp   |

  @tools @model-control @elicitation
  Scenario: Complex Tool Execution with Elicitation and Structured Output
    # Reference: specification/2025-06-18/server/tools.mdx
    # Reference: specification/2025-06-18/client/elicitation.mdx
    # Tests advanced tool scenarios with user interaction and structured responses
    Given an MCP server with tools:
      | name           | description              | requires_confirmation |
      | file_analyzer  | Analyze file content     | false                |
      | data_processor | Process data with schema | true                 |
      | api_caller     | Call external API        | true                 |
    And tool "data_processor" has input schema requiring:
      | field     | type   | required | description        |
      | data_type | string | true     | Type of data       |
      | format    | string | true     | Output format      |
      | options   | object | false    | Processing options |
    And tool "data_processor" has output schema:
      | field      | type   | required | description      |
      | processed  | object | true     | Processed data   |
      | metadata   | object | true     | Processing info  |
      | timestamp  | string | true     | Processing time  |
    When the client calls tool "data_processor" with incomplete arguments:
      | field     | value |
      | data_type | json  |
    Then the server detects missing required argument "format"
    And initiates elicitation request for missing parameters
    When the client's elicitation provider prompts user
    And user provides:
      | field   | value |
      | format  | csv   |
      | options | {"headers": true} |
    Then elicitation completes with action "ACCEPT"
    When the server retries tool execution with complete arguments
    Then tool execution succeeds
    And returns structured output conforming to output schema
    And includes both structured content and text representation

  @sampling @client-features
  Scenario: Server-Initiated LLM Sampling with Model Preferences
    # Reference: specification/2025-06-18/client/sampling.mdx
    # Tests complex sampling scenarios with model selection and user approval
    Given an MCP client with sampling capability
    And model preferences are configured:
      | preference       | value | description                    |
      | costPriority     | 0.3   | Cost is less important        |
      | speedPriority    | 0.8   | Speed is very important       |
      | intelligencePriority | 0.5 | Moderate capability needs   |
    And model hints are configured:
      | hint           | preference_order |
      | claude-3-sonnet | 1               |
      | claude         | 2               |
    When the server requests LLM sampling with message:
      """
      {
        "role": "user",
        "content": {
          "type": "text", 
          "text": "Analyze this code for security vulnerabilities"
        }
      }
      """
    And includes model preferences and hints
    Then the client presents sampling request to user for approval
    When user approves the sampling request
    Then the client selects appropriate model based on preferences
    And sends request to LLM with system prompt
    When LLM responds with analysis
    Then the client presents response to user for review
    When user approves the response
    Then the response is returned to the server
    And includes metadata about selected model and stop reason

  @prompts @user-control
  Scenario: Interactive Prompt Templates with Complex Arguments
    # Reference: specification/2025-06-18/server/prompts.mdx
    # Tests prompt system with templates, arguments, and user interaction
    Given an MCP server with prompt templates:
      | name              | description                | arguments |
      | code_review       | Review code for issues     | file_path, focus_areas |
      | documentation_gen | Generate documentation     | code_snippet, style    |
      | security_audit    | Security audit template    | scope, depth_level     |
    And prompt "code_review" has arguments:
      | name        | type   | required | description               |
      | file_path   | string | true     | Path to file for review   |
      | focus_areas | array  | false    | Specific areas to focus   |
    When the client lists available prompts
    Then all prompts are returned with argument schemas
    When the client requests prompt "code_review" with arguments:
      | argument    | value                           |
      | file_path   | src/security/auth_handler.rs   |
      | focus_areas | ["input_validation", "crypto"] |
    Then the server returns instantiated prompt messages:
      | role      | content_type | content_preview                |
      | system    | text         | You are a security expert...   |
      | user      | text         | Review this file for security: |
    And prompt includes actual file content in context
    And focuses on specified areas

  @roots @filesystem-boundaries
  Scenario: Filesystem Root Management with Security Boundaries
    # Reference: specification/2025-06-18/client/roots.mdx
    # Tests root management and security boundary enforcement
    Given an MCP client with root management capability
    And configured roots:
      | uri                           | name              | permissions |
      | file:///home/user/project1    | Main Project      | read,write  |
      | file:///home/user/project2    | Secondary Project | read        |
      | file:///shared/resources      | Shared Resources  | read        |
    When the server requests root list
    Then the client returns available roots with proper URIs
    And each root includes human-readable names
    When the server attempts to access "file:///home/user/project1/src/main.rs"
    Then access is granted as path is within allowed root
    When the server attempts to access "file:///etc/passwd"
    Then access is denied as path is outside allowed roots
    And security violation is logged
    When root configuration changes (new project added)
    Then "notifications/roots/list_changed" is sent to server
    When server refreshes root list
    Then updated roots are returned

  @progress @cancellation @utilities
  Scenario: Request Progress Tracking and Cancellation
    # Reference: specification/2025-06-18/basic/utilities/progress.mdx
    # Reference: specification/2025-06-18/basic/utilities/cancellation.mdx
    # Tests progress tracking and request cancellation functionality
    Given an MCP server with long-running operations
    When the client initiates a large resource listing operation
    Then progress token is assigned to the request
    And initial progress notification is sent:
      | field    | value                          |
      | token    | progress_token_123             |
      | progress | 0.0                           |
      | total    | 1000                          |
    And the operation proceeds
    Then progress notifications are sent periodically:
      | progress | total | message                    |
      | 0.2      | 1000  | Processing directory 1/5   |
      | 0.4      | 1000  | Processing directory 2/5   |
      | 0.6      | 1000  | Processing directory 3/5   |
    When the client decides to cancel the operation
    And sends cancellation notification with reason "user_requested"
    Then the server stops the operation
    And sends final progress notification:
      | progress | total | message   |
      | 0.6      | 1000  | Cancelled |
    And releases the progress token

  @logging @utilities
  Scenario: Structured Logging with Level Control and Rate Limiting
    # Reference: specification/2025-06-18/server/utilities/logging.mdx
    # Tests logging system with different levels and rate limiting
    Given an MCP server with logging capability
    And default log level is "INFO"
    When the client sets log level to "DEBUG"
    Then server confirms level change
    When server operations generate log messages:
      | level   | logger      | message                         |
      | DEBUG   | resource    | Loading resource metadata       |
      | INFO    | connection  | Client connected successfully   |
      | WARNING | validation  | Invalid parameter provided      |
      | ERROR   | tool        | Tool execution failed           |
    Then all messages are sent to client as they meet threshold
    When the client sets log level to "WARNING"
    And server generates DEBUG and INFO messages
    Then only WARNING and ERROR messages are sent
    When server generates excessive log messages rapidly
    Then rate limiting kicks in after configured threshold
    And some messages are dropped to prevent flooding

  @completion @utilities
  Scenario: Argument Completion for Complex Schemas
    # Reference: specification/2025-06-18/server/utilities/completion.mdx
    # Tests autocompletion for resource templates and tool arguments
    Given an MCP server with completion capability
    And resource template "file:///{path}" is available
    And file system contains:
      | path                    | type      |
      | src/main.rs            | file      |
      | src/lib.rs             | file      |
      | src/utils/             | directory |
      | src/utils/helper.rs    | file      |
      | tests/                 | directory |
    When the client requests completion for "file:///sr"
    Then completion suggestions include:
      | value | label                | type      |
      | src   | src/ (directory)     | directory |
    When the client requests completion for "file:///src/"
    Then completion suggestions include:
      | value    | label            | type      |
      | main.rs  | main.rs (file)   | file      |
      | lib.rs   | lib.rs (file)    | file      |
      | utils    | utils/ (dir)     | directory |
    And suggestions are properly ranked by relevance

  @error-handling @edge-cases
  Scenario: Comprehensive Error Handling and Edge Cases
    # Reference: specification/2025-06-18/basic/index.mdx#messages
    # Tests error handling across different protocol layers
    Given an MCP server and client in operation phase
    When the client sends malformed JSON
    Then server responds with "Parse Error" (-32700)
    When the client sends invalid JSON-RPC structure
    Then server responds with "Invalid Request" (-32600)
    When the client calls non-existent method "nonexistent/method"
    Then server responds with "Method not found" (-32601)
    When the client calls valid method with invalid parameters
    Then server responds with "Invalid params" (-32602)
    When server encounters internal error during tool execution
    Then server responds with "Internal error" (-32603)
    When the client sends request before initialization
    Then server responds with appropriate lifecycle error
    When network connection is interrupted during request
    Then both sides handle disconnection gracefully
    And pending requests are properly cleaned up

  @transport @stdio @http
  Scenario: Multi-Transport Protocol Consistency
    # Reference: specification/2025-06-18/basic/transports.mdx
    # Tests that protocol works consistently across different transports
    Given MCP implementation supports both stdio and HTTP transports
    When testing identical operations across transports:
      | operation        | stdio_result | http_result |
      | initialization   | success      | success     |
      | capability_nego  | identical    | identical   |
      | resource_list    | identical    | identical   |
      | tool_execution   | identical    | identical   |
      | progress_track   | supported    | supported   |
      | notifications    | supported    | supported   |
    Then results are functionally equivalent
    But HTTP transport includes additional features:
      | feature              | stdio | http |
      | authorization        | no    | yes  |
      | resource_metadata    | no    | yes  |
      | session_management   | no    | yes  |

  @security @session-management
  Scenario: Session Security and Hijacking Prevention
    # Reference: specification/2025-06-18/basic/security_best_practices.mdx#session-hijacking
    # Tests prevention of session hijacking attacks
    Given an MCP HTTP server with session management
    And multiple server instances sharing session storage
    When a client connects and receives session ID
    Then session ID is securely generated and non-predictable
    And session is bound to user-specific information
    When an attacker tries to use guessed session ID
    Then server rejects requests due to user binding mismatch
    When legitimate user makes request with valid session
    Then request includes proper authorization token validation
    And session binding is verified on each request
    When server processes requests with session context
    Then session data includes user ID and not just session ID
    And prevents cross-user impersonation attacks

  @integration @real-world
  Scenario: Multi-Server Integration with Resource Sharing
    # Reference: specification/2025-06-18/architecture/index.mdx
    # Tests realistic scenario with multiple MCP servers
    Given a host application managing multiple MCP servers:
      | server_name      | capabilities           | focus_area        |
      | file_server      | resources, tools       | filesystem access |
      | git_server       | resources, prompts     | version control   |
      | api_server       | tools, sampling        | external APIs     |
    And each server maintains security boundaries
    When the host aggregates resources from all servers
    Then resources are properly isolated by server
    And cross-server access is controlled by host
    When a git operation requires file system access
    Then host coordinates between git_server and file_server
    But servers cannot directly access each other
    When external API requires user consent
    Then host presents unified consent interface
    And manages permissions across all servers
    And maintains audit trail for all operations

  @performance @scalability
  Scenario: Large-Scale Resource Handling with Pagination
    # Reference: specification/2025-06-18/server/utilities/pagination.mdx
    # Tests handling of large datasets with proper pagination
    Given an MCP server with 10,000+ resources
    When the client requests resource list without pagination
    Then server returns first page with reasonable page size
    And includes nextCursor for continued pagination
    When the client uses cursor to fetch subsequent pages
    Then each page contains expected number of items
    And resources are returned in consistent order
    When the client reaches the final page
    Then nextCursor is null or omitted
    And total resource count is accurate
    When concurrent clients paginate the same dataset
    Then each client receives consistent pagination results
    And cursor tokens remain valid across reasonable time window

  @versioning @compatibility
  Scenario: Protocol Version Negotiation and Backward Compatibility
    # Reference: specification/2025-06-18/basic/lifecycle.mdx#version-negotiation
    # Tests protocol version handling and compatibility
    Given an MCP server supporting versions ["2025-06-18", "2024-11-05"]
    When a client requests initialization with version "2025-06-18"
    Then server responds with same version "2025-06-18"
    When a client requests initialization with version "2024-11-05"
    Then server responds with "2024-11-05"
    And operates in compatibility mode
    When a client requests unsupported version "2024-01-01"
    Then server responds with supported version from its list
    When client doesn't support server's fallback version
    Then client disconnects gracefully
    And logs version mismatch for debugging