# language: en
@lifecycle @conformance
Feature: MCP connection lifecycle conformance
  The Model Context Protocol (MCP) defines a rigorous lifecycle for client-server connections.
  This feature explores boundaries for initialization, operation, shutdown, timeouts,
  capability negotiation, and error handling as defined in the 2025-06-18 revision.

  Background:
    Given an MCP Client and an MCP Server using a supported transport
    And the Client intends to use protocol version "2024-11-05"
    And both sides advertise capabilities during initialization

  Rule: Initialization must occur first and follow the required message order

    @init @happy_path
    Scenario Outline: Successful initialization handshake with capability negotiation
      When the Client sends an initialize request with:
        | protocolVersion | <clientProtocol> |
        | clientInfo.name | ExampleClient    |
        | capabilities    | roots,sampling,elicitation |
      Then the Server responds to initialize with:
        | protocolVersion | <serverProtocol> |
        | capabilities    | logging,prompts[listChanged],resources[subscribe,listChanged],tools[listChanged] |
        | serverInfo.name | ExampleServer    |
      And the Client sends a notifications/initialized notification
      And before receiving notifications/initialized the Server has sent only the following kinds of requests: pings and logging
      And no party uses any capability that was not negotiated

      Examples:
        | transport | clientProtocol | serverProtocol |
        | stdio     | 2024-11-05     | 2024-11-05     |
        | http      | 2024-11-05     | 2024-11-05     |

    @init @ordering @boundary
    Scenario: Client sends requests other than ping before initialize completes
      Given the Client has sent initialize but not yet received the response
      When the Client sends a tools/call request
      Then the Server should reject or ignore the request per implementation policy
      And the interaction must not transition to Operation phase until initialize completes

    @init @ordering @boundary
    Scenario: Server sends requests before receiving notifications/initialized
      Given the Server has responded to initialize but has not yet received notifications/initialized
      When the Server attempts to send a tools/list request
      Then the Client should reject or ignore the request because only pings and logging are allowed before initialized

    @init @idempotence @boundary
    Scenario: Re-initialization attempt after initialized
      Given the Client has completed initialization and sent notifications/initialized
      When the Client sends another initialize request
      Then the Server should respond with an error indicating invalid state or ignore the request

  Rule: Version negotiation must converge on a mutually supported protocol version

    @version @negotiate @happy_path
    Scenario: Server counters with its latest supported version and the Client accepts
      Given the Client supports protocol versions [2024-11-05, 2024-06-10]
      And the Server supports protocol versions [2024-06-10]
      When the Client sends initialize with protocolVersion "2024-11-05"
      Then the Server responds with protocolVersion "2024-06-10"
      And the Client accepts the version and continues initialization
      And the Client sends notifications/initialized

    @version @mismatch @disconnect @boundary
    Scenario: Client disconnects when Server responds with an unsupported version
      Given the Client supports protocol versions [2024-11-05]
      And the Server supports protocol versions [2024-06-10]
      When the Client sends initialize with protocolVersion "2024-11-05"
      Then the Server responds with protocolVersion "2024-06-10"
      And the Client does not support that version
      Then the Client disconnects the session

    @version @http @header @boundary
    Scenario: HTTP transport requires MCP-Protocol-Version header after initialization
      Given the transport is HTTP
      And initialization has successfully completed with protocolVersion "2024-11-05"
      When the Client sends any subsequent HTTP request
      Then the Client includes header "MCP-Protocol-Version: 2024-11-05"
      And the Server rejects the request if the header is missing or has a mismatched version

  Rule: Only negotiated capabilities may be used during Operation

    @capabilities @negotiated_only @boundary
    Scenario: Using an unnegotiated capability is rejected
      Given the negotiated server capabilities do not include "resources"
      When the Client sends resources/list
      Then the Server responds with an error indicating the capability is unavailable

    @capabilities @sub
    Scenario Outline: Respect sub-capabilities for listChanged and subscribe
      Given the negotiated server capabilities include resources with <subcaps>
      When the Client attempts to <action>
      Then the Server response is <outcome>

      Examples:
        | subcaps                 | action                | outcome                               |
        | subscribe,listChanged   | subscribe to resource | allowed                               |
        | listChanged             | subscribe to resource | error: subscription not supported     |
        | (none)                  | subscribe to resource | error: resources capability not avail |

    @capabilities @tools @prompts
    Scenario: listChanged notifications only when supported
      Given the negotiated server capabilities include tools with listChanged true
      When tools become available or unavailable on the Server
      Then the Server may send a tools/list_changed notification
      And if listChanged is false, the Server must not send tools/list_changed

  Rule: Operation phase respects negotiated protocol version and capabilities

    @operation @respect
    Scenario: All requests and notifications conform to the negotiated protocol version
      Given protocolVersion "2024-11-05" was negotiated
      When the Client issues a request defined in a different protocol revision
      Then the Server rejects the request as unsupported

  Rule: Shutdown is transport-specific and should be graceful

    @shutdown @stdio @graceful
    Scenario: Stdio shutdown initiated by Client
      Given the transport is stdio and the session is operational
      When the Client closes the input stream to the Server process
      Then the Server should exit on its own within a reasonable time
      When the Server does not exit within a reasonable time
      Then the Client sends SIGTERM
      And if the Server still does not exit within a reasonable time after SIGTERM
      Then the Client sends SIGKILL

    @shutdown @http @graceful
    Scenario: HTTP shutdown indicated by closing connections
      Given the transport is HTTP and the session is operational
      When the Client closes the associated HTTP connection(s)
      Then the session is considered shut down

  Rule: Timeouts must be enforced and integrated with cancellation and progress

    @timeouts @cancellation @boundary
    Scenario: Request times out and is cancelled
      Given the Client sets a per-request timeout of 2 seconds
      When the Client sends a tools/call request that does not complete within 2 seconds
      Then the Client sends a cancellations/notify for the request
      And the Client stops waiting for a response

    @timeouts @progress @max_timeout @boundary
    Scenario: Progress notifications may reset timeout clock but maximum timeout is enforced
      Given the Client sets a per-request timeout of 2 seconds and a maximum timeout of 10 seconds
      When the Server emits progress/notify every 1 second for a long-running request
      Then the Client may reset the 2 second timeout after each progress notification
      But regardless of progress, if 10 seconds elapse since the request began
      Then the Client cancels the request and stops waiting for a response

  Rule: Errors must be structured and cover version mismatch, capability failure, and timeouts

    @error @version
    Scenario: Initialization error for unsupported protocol version
      When the Server rejects initialize due to unsupported protocol version
      Then the error object returned conforms to JSON-RPC with:
        | code              | -32602 |
        | message           | Unsupported protocol version |
        | data.supported    | [2024-11-05] |
        | data.requested    | 1.0.0 |

    @error @capabilities
    Scenario: Failure to negotiate a required capability
      Given the Client requires the sampling capability
      And the Server does not advertise sampling
      When the Client receives the initialize response
      Then the Client must error and/or disconnect because a required capability is unavailable

    @error @timeouts
    Scenario: Timeout error surfaced to caller when cancellation is not honored
      Given the Client times out a request and sends cancellation
      And the Server continues processing and later sends a success response
      Then the Client ignores the late response and returns a timeout error to its caller

  Rule: Initialization messages must include implementation information

    @init @clientInfo @boundary
    Scenario: Missing clientInfo is rejected
      When the Client sends initialize without clientInfo
      Then the Server rejects the request or responds with an error indicating missing implementation information

  Rule: Pings and logging are the only cross-boundary requests prior to initialized

    @ping @preinit
    Scenario: Ping is allowed before initialization completes
      Given the Client has sent initialize and is waiting for the response
      When the Client sends a utilities/ping request
      Then the Server responds with success

    @logging @server @preinit
    Scenario: Server logging allowed before initialized
      Given the Server has responded to initialize but not yet received notifications/initialized
      When the Server sends a logging/notify message
      Then the Client accepts the notification
