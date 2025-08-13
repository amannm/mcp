@protocol
Feature: MCP Protocol Lifecycle
  Ensure MCP implementations correctly handle protocol initialization,
  capability negotiation, and JSON-RPC messaging for secure, interoperable connections

  Background:
    Given a clean MCP environment
    And JSON-RPC transport is available

  @initialization @smoke
  Scenario: Successful protocol initialization
    Given a client requesting protocol version "2025-06-18"
    And client declares capabilities "sampling, roots, elicitation"
    When initialize request is sent
    Then server responds with compatible version
    And server declares its capabilities
    And server provides implementation information
    When initialized notification is sent
    Then connection becomes operational
    And message exchange is possible

  @version-negotiation
  Scenario: Protocol version negotiation with supported version
    Given server supports versions "2024-11-05, 2025-06-18"
    When client requests version "2025-06-18"
    Then server accepts the requested version

  @version-negotiation
  Scenario: Protocol version negotiation with unsupported version
    Given server supports versions "2024-11-05, 2025-06-18"
    When client requests unsupported version "1.0.0"
    Then server responds with its latest supported version
    And client decides whether to continue

  @capability-negotiation
  Scenario Outline: Server capability discovery
    Given server declares capabilities "<server_capabilities>"
    When initialization completes
    Then server features "<available_features>" are available
    And server features "<unavailable_features>" are unavailable

    Examples:
      | server_capabilities             | available_features              | unavailable_features    |
      | resources,tools                 | resources,tools                 | prompts,logging         |
      | prompts                         | prompts                         | resources,tools,logging |
      | resources,tools,prompts,logging | resources,tools,prompts,logging | none                    |
      | none                            | none                            | resources,tools,prompts,logging |

  @jsonrpc @message-format
  Scenario: JSON-RPC request format compliance
    Given an operational MCP connection
    When request is sent with id "test-123"
    Then request contains required JSON-RPC 2.0 fields
    And request id is not null and unique
    When server responds
    Then response id matches request id "test-123"
    And response contains either result or error

  @jsonrpc @message-format  
  Scenario: JSON-RPC notification format compliance
    Given an operational MCP connection
    When notification is sent
    Then notification has no id field
    And notification contains method and optional params

  @jsonrpc @error-handling
  Scenario Outline: JSON-RPC standard error responses
    Given an operational MCP connection
    When "<error_condition>" occurs
    Then server returns "<error_message>" with code <error_code>

    Examples:
      | error_condition          | error_message         | error_code |
      | malformed request        | Parse error           | -32700     |
      | invalid method request   | Method not found      | -32601     |
      | invalid parameters       | Invalid params        | -32602     |
      | server internal error    | Internal error        | -32603     |

  @lifecycle @shutdown
  Scenario: Graceful stdio connection shutdown
    Given an operational MCP connection with "stdio" transport
    When client closes input stream
    And waits for graceful server exit
    Then connection terminates cleanly

  @lifecycle @shutdown
  Scenario: Graceful HTTP connection shutdown
    Given an operational MCP connection with "http" transport
    When client closes HTTP connection
    Then connection terminates cleanly

  @meta-fields
  Scenario: Client _meta field preservation
    Given an operational MCP connection
    When request contains _meta field
    Then server preserves _meta data unchanged

  @meta-fields
  Scenario: Server _meta field handling
    Given an operational MCP connection
    When server sends response with reserved "_meta_reserved" prefix
    Then client handles MCP-reserved fields correctly
    When server sends response with custom "_custom" prefix
    Then client treats as implementation-specific data

  @timeouts @cancellation
  Scenario: Request timeout and cancellation
    Given an operational MCP connection with "5" second timeout
    When request exceeds timeout duration
    Then client sends cancellation notification
    And client stops waiting for response

  @timeouts @progress
  Scenario: Progress-based timeout extension
    Given an operational MCP connection
    When request sends progress notifications
    Then timeout may be extended
    But maximum timeout is enforced

  @initialization-errors
  Scenario: Unsupported protocol version error
    Given client requests protocol version "2025-06-18"
    When server supports only incompatible versions
    Then server returns protocol version error
    And error includes server's supported versions

  @initialization-errors
  Scenario: Premature request handling
    Given connection is initialized but not ready
    When client sends non-ping request
    Then server may reject or queue the request

  @security
  Scenario: Implementation information disclosure
    Given an operational MCP connection
    When server provides implementation info
    Then sensitive information is not exposed
    And version information is appropriate

  @security @sampling
  Scenario: Sampling request authorization
    Given an operational MCP connection with sampling capability
    When server requests LLM sampling
    Then client requires explicit user approval
    And client controls prompt visibility