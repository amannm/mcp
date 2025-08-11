Feature: MCP Comprehensive Integration Scenario
  As an AI-powered development assistant 
  I need to provide comprehensive code assistance through the MCP protocol
  So that developers can efficiently manage complex software projects

  Scenario: Full-stack development workflow with comprehensive MCP protocol exercise
    Given I am an MCP host managing multiple client connections for development assistance
    And I have configured security policies with explicit user consent for all operations
    And I have registered multiple MCP servers: "filesystem", "git", "build-tools", "llm-assistant"
    
    When I initialize the MCP connection with the filesystem server
    Then the server responds with capabilities including resources, tools, prompts, and completion
    And I negotiate protocol version "2025-06-18" with transport type "STDIO"
    And I configure logging level to "INFO" with structured message notifications
    
    When I request root boundaries from the filesystem server using the roots provider
    Then I receive paginated root list containing project directories: "/workspace/myapp", "/workspace/shared-lib"
    And I grant filesystem access consent for the workspace roots
    
    When I list available resources from all registered servers with cursor-based pagination
    Then I receive resources including:
      | Server        | URI                           | Type        | Description                    |
      | filesystem    | file:///workspace/myapp/src   | directory   | Source code directory          |
      | filesystem    | file:///workspace/myapp/tests | directory   | Test directory                 |
      | git           | git://workspace/myapp/.git    | repository  | Git repository metadata        |
      | build-tools   | maven://workspace/myapp/pom   | build-file  | Maven build configuration      |
      | llm-assistant | prompt://code-review          | template    | Code review prompt template    |
    
    When I subscribe to resource updates for "file:///workspace/myapp/src" with automatic change notifications
    Then I receive a change subscription that notifies me of file modifications
    And the subscription tracks modifications using resource update notifications
    
    When I read resource content from "file:///workspace/myapp/src/Main.java" 
    Then I receive resource blocks containing the Java source code with proper content-type headers
    And the resource access is controlled by annotation-based policies verifying my principal scopes
    
    When I list available tools from all servers with pagination cursor support
    Then I receive tools including:
      | Server      | Name              | Description                           | Input Schema                    |
      | build-tools | compile-project   | Compile the Maven project            | {"type": "object"}              |
      | build-tools | run-tests         | Execute unit tests with coverage     | {"properties": {"suite": {}}}   |
      | git         | create-branch     | Create and checkout new git branch   | {"properties": {"name": {}}}    |
      | git         | commit-changes    | Commit staged changes with message   | {"properties": {"message": {}}} |
    
    When I call tool "create-branch" with arguments {"name": "feature/mcp-integration"} and progress token "branch-123"
    Then I receive progress notifications tracking the git branch creation operation
    And the tool result indicates successful branch creation with detailed output
    And the operation is rate-limited according to the server's tool access policy
    
    When I list available prompts from the llm-assistant server
    Then I receive prompts including:
      | Name         | Description                    | Arguments                           |
      | code-review  | Comprehensive code review      | ["file_path", "review_type"]        |
      | refactor     | Refactoring suggestions        | ["code_snippet", "target_style"]   |
      | test-gen     | Generate unit tests            | ["class_name", "coverage_level"]   |
    
    When I get prompt "code-review" with arguments {"file_path": "src/Main.java", "review_type": "security"}
    Then I receive a prompt instance with templated messages containing:
      | Role      | Content Template                                                       |
      | system    | You are a security-focused code reviewer analyzing Java applications  |
      | user      | Please review this code: {{file_content}} for security issues        |
      | assistant | I'll analyze this code for potential security vulnerabilities         |
    
    When the llm-assistant server requests sampling through createMessage with the code review prompt
    And I grant sampling consent with audience restrictions and prompt visibility controls
    Then I create an LLM message using the sampling provider with timeout 30000ms
    And I receive a sampling response containing the security analysis results
    And the sampling operation respects the configured access policies and user privacy controls
    
    When the filesystem server needs clarification about file permissions using the elicitation provider  
    And I invoke elicit request with timeout 15000ms asking "Should I make the config files writable?"
    Then I receive an elicitation result with the user's permission decision
    And the elicitation follows the protocol's user consent and control principles
    
    When I call tool "run-tests" with progress tracking and the operation takes significant time
    Then I receive periodic progress notifications with tokens, current/total progress, and status messages
    And I can monitor the test execution progress including sub-operations like compilation and test runs
    
    When I request completion suggestions for the string "List<String> names = new Arra" using completion provider
    Then I receive completion results with context-aware Java suggestions:
      | Completion Text    | Refs                               |
      | ArrayList<>()      | java.util.ArrayList                |
      | Arrays.asList()    | java.util.Arrays.asList            |
    
    When the filesystem detects file changes in the subscribed directory "src/"
    Then I receive resource updated notifications containing the modified file URIs and change types
    And the notifications include resource blocks with updated content
    
    When I need to cancel a long-running "compile-project" operation using the cancellation utility
    Then I send a cancelled notification with the progress token and cancellation reason
    And the server gracefully terminates the compilation process
    And I receive final progress notification indicating cancellation completion
    
    When I set logging level to "DEBUG" to troubleshoot connection issues
    Then I receive detailed logging message notifications from all servers
    And the logs include structured JSON data with logger names, timestamps, and contextual information
    And logging operations are rate-limited to prevent overwhelming the client
    
    When I unsubscribe from resource updates for "file:///workspace/myapp/src"
    Then the change subscription is properly terminated
    And I no longer receive resource update notifications for that directory
    
    When an error occurs during tool execution due to insufficient permissions
    Then I receive a proper JSON-RPC error response with specific error codes
    And the error includes detailed diagnostic information for troubleshooting
    And the error handling follows the protocol's security and privacy principles
    
    When I send a ping request to verify server connectivity with timeout 5000ms
    Then I receive a timely ping response confirming the connection is healthy
    And the round-trip time is measured for performance monitoring
    
    When I need to update server capability configurations dynamically
    Then I can modify tool access policies, resource access policies, and sampling access policies
    And the changes take effect immediately without requiring connection reset
    And all policy updates respect the user consent and control requirements
    
    When I disconnect from all MCP servers and clean up resources
    Then all active subscriptions are automatically cancelled
    And all pending operations receive proper cancellation notifications
    And transport connections are gracefully closed
    And the host unregisters all clients and clears security policies
    
    Then the complete MCP workflow has exercised:
      | Protocol Component           | Operations Tested                                                    |
      | Connection Management        | initialize, ping, disconnect, transport negotiation                |
      | Server Resource Features     | list, read, subscribe, unsubscribe, templates, updates            |
      | Server Tool Features         | list, call with args, rate limiting, access control               |
      | Server Prompt Features       | list, get with args, templating, message formatting               |
      | Server Completion Features   | complete requests, context-aware suggestions, ref encoding         |
      | Client Sampling Features     | createMessage, LLM integration, consent management                 |
      | Client Roots Features        | list boundaries, pagination, access control                        |  
      | Client Elicitation Features  | user input requests, timeout handling, consent flows               |
      | Progress Utilities           | token-based tracking, notifications, cancellation                  |
      | Logging Utilities            | level configuration, structured messages, rate limiting            |
      | Cancellation Utilities       | request cancellation, graceful termination                         |
      | Transport Layer              | STDIO transport, message serialization, protocol versioning        |
      | Security Layer               | access policies, principals, scopes, user consent                  |
      | Error Handling               | JSON-RPC errors, diagnostic info, graceful degradation             |
      | Pagination Support           | cursor-based pagination, page size configuration                   |
      | Subscription Management      | change notifications, lifecycle management                         |
    
    And all security and trust principles are upheld:
      | Principle                    | Implementation                                                       |
      | User Consent and Control     | Explicit consent for all data access and tool operations           |
      | Data Privacy                 | Access controls, no unauthorized data transmission                  |
      | Tool Safety                  | User approval for tool execution, untrusted description handling   |
      | LLM Sampling Controls        | User-controlled prompts, audience restrictions, result visibility  |