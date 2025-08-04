# MCP Feature Step Implementation Plan

## Overview
This document outlines the implementation strategy for completing all remaining Cucumber step definitions in `McpFeatureSteps.java` based on the comprehensive scenarios in `mcp.feature`.

## Current Status
- ✅ **Scenario 1**: Complete Protocol Lifecycle (Partially Implemented)
- ❌ **Scenarios 2-17**: Need complete implementation

## Implementation Strategy

### Phase 1: Core Infrastructure (Sequential - 2-3 days)
These scenarios establish fundamental protocol behavior and must be implemented first.

#### Agent 1: Security & Authorization Infrastructure
**Scenarios**: 2, 3, 14
**Duration**: 2-3 days
**Dependencies**: None

**Missing Step Definitions:**
```java
// Scenario 2: OAuth 2.1 Authorization Flow
@Given("an MCP server at {string} requiring authorization")
@Given("an authorization server at {string}")
@Given("dynamic client registration is supported")
@When("the client makes an unauthorized request")
@Then("the server responds with {string}")
@And("includes {string} header with resource metadata URL")
@When("the client fetches protected resource metadata")
@Then("the metadata contains authorization server URLs")
@When("the client performs dynamic client registration")
@Then("a client ID and credentials are obtained")
@When("the client initiates OAuth 2.1 authorization code flow")
@And("uses PKCE code challenge method {string}")
@And("includes resource parameter {string}")
@And("user grants consent through authorization server")
@Then("authorization code is received at callback")
@When("the client exchanges code for access token")
@And("includes PKCE code verifier")
@Then("access token is received with correct audience")
@When("the client makes MCP requests with Bearer token")
@Then("requests are successfully authorized")
@And("token audience validation passes")

// Scenario 3: Token Validation
@Given("an MCP server configured for token validation")
@And("the server's canonical URI is {string}")
@When("a client presents a token with wrong audience {string}")
@Then("the server rejects the token with {string}")
@And("logs security violation with level {string}")
@When("a client presents a token without audience claim")
@When("a client presents a properly scoped token for {string}")
@Then("the server accepts the token")
@And("validates token signature and expiration")
@And("does not pass token to downstream services")

// Scenario 14: Session Security
@Given("an MCP HTTP server with session management")
@And("multiple server instances sharing session storage")
@When("a client connects and receives session ID")
@Then("session ID is securely generated and non-predictable")
@And("session is bound to user-specific information")
@When("an attacker tries to use guessed session ID")
@Then("server rejects requests due to user binding mismatch")
@When("legitimate user makes request with valid session")
@Then("request includes proper authorization token validation")
@And("session binding is verified on each request")
@When("server processes requests with session context")
@Then("session data includes user ID and not just session ID")
@And("prevents cross-user impersonation attacks")
```

#### Agent 2: Error Handling & Transport Infrastructure  
**Scenarios**: 12, 13, 17
**Duration**: 2 days
**Dependencies**: None

**Missing Step Definitions:**
```java
// Scenario 12: Error Handling
@Given("an MCP server and client in operation phase")
@When("the client sends malformed JSON")
@Then("server responds with {string} ({int})")
@When("the client sends invalid JSON-RPC structure")
@When("the client calls non-existent method {string}")
@When("the client calls valid method with invalid parameters")
@When("server encounters internal error during tool execution")
@When("the client sends request before initialization")
@Then("server responds with appropriate lifecycle error")
@When("network connection is interrupted during request")
@Then("both sides handle disconnection gracefully")
@And("pending requests are properly cleaned up")

// Scenario 13: Multi-Transport
@Given("MCP implementation supports both stdio and HTTP transports")
@When("testing identical operations across transports:")
@Then("results are functionally equivalent")
@But("HTTP transport includes additional features:")

// Scenario 17: Version Negotiation
@Given("an MCP server supporting versions {list}")
@When("a client requests initialization with version {string}")
@Then("server responds with same version {string}")
@When("a client requests initialization with version {string}")
@Then("server responds with {string}")
@And("operates in compatibility mode")
@When("a client requests unsupported version {string}")
@Then("server responds with supported version from its list")
@When("client doesn't support server's fallback version")
@Then("client disconnects gracefully")
@And("logs version mismatch for debugging")
```

### Phase 2: Server Features (Parallel - 3-4 days)
These scenarios can be implemented in parallel as they test different server capabilities.

#### Agent 3: Resource & Tool Management
**Scenarios**: 4, 5, 7, 11
**Duration**: 3-4 days  
**Dependencies**: Phase 1 complete

**Missing Step Definitions:**
```java
// Scenario 4: Resource Management
@Given("an MCP server with file system resources")
@And("resource templates are configured:")
@When("the client lists resource templates")
@Then("all templates are returned with proper schemas")
@When("the client expands template {string}")
@Then("the expanded resource is accessible")
@And("has proper MIME type {string}")
@When("the client subscribes to resource updates for {string}")
@Then("subscription is confirmed")
@When("the resource content changes externally")
@Then("{string} is sent to subscriber")
@And("notification includes URI and updated title")
@When("the client reads the updated resource")
@Then("new content is returned")
@And("proper annotations are included:")

// Scenario 5: Tool Execution
@Given("an MCP server with tools:")
@And("tool {string} has input schema requiring:")
@And("tool {string} has output schema:")
@When("the client calls tool {string} with incomplete arguments:")
@Then("the server detects missing required argument {string}")
@And("initiates elicitation request for missing parameters")
@When("the client's elicitation provider prompts user")
@And("user provides:")
@Then("elicitation completes with action {string}")
@When("the server retries tool execution with complete arguments")
@Then("tool execution succeeds")
@And("returns structured output conforming to output schema")
@And("includes both structured content and text representation")

// Scenario 7: Prompt Templates
@Given("an MCP server with prompt templates:")
@And("prompt {string} has arguments:")
@When("the client lists available prompts")
@Then("all prompts are returned with argument schemas")
@When("the client requests prompt {string} with arguments:")
@Then("the server returns instantiated prompt messages:")
@And("prompt includes actual file content in context")
@And("focuses on specified areas")

// Scenario 11: Completion
@Given("an MCP server with completion capability")
@And("resource template {string} is available")
@And("file system contains:")
@When("the client requests completion for {string}")
@Then("completion suggestions include:")
@And("suggestions are properly ranked by relevance")
```

#### Agent 4: Client Features & Utilities
**Scenarios**: 6, 8, 9, 10
**Duration**: 3 days
**Dependencies**: Phase 1 complete

**Missing Step Definitions:**
```java  
// Scenario 6: LLM Sampling
@Given("an MCP client with sampling capability")
@And("model preferences are configured:")
@And("model hints are configured:")
@When("the server requests LLM sampling with message:")
@And("includes model preferences and hints")
@Then("the client presents sampling request to user for approval")
@When("user approves the sampling request")
@Then("the client selects appropriate model based on preferences")
@And("sends request to LLM with system prompt")
@When("LLM responds with analysis")
@Then("the client presents response to user for review")
@When("user approves the response")
@Then("the response is returned to the server")
@And("includes metadata about selected model and stop reason")

// Scenario 8: Root Management
@Given("an MCP client with root management capability")
@And("configured roots:")
@When("the server requests root list")
@Then("the client returns available roots with proper URIs")
@And("each root includes human-readable names")
@When("the server attempts to access {string}")
@Then("access is granted as path is within allowed root")
@When("the server attempts to access {string}")
@Then("access is denied as path is outside allowed roots")
@And("security violation is logged")
@When("root configuration changes (new project added)")
@Then("{string} is sent to server")
@When("server refreshes root list")
@Then("updated roots are returned")

// Scenario 9: Progress Tracking
@Given("an MCP server with long-running operations")
@When("the client initiates a large resource listing operation")
@Then("progress token is assigned to the request")
@And("initial progress notification is sent:")
@And("the operation proceeds")
@Then("progress notifications are sent periodically:")
@When("the client decides to cancel the operation")
@And("sends cancellation notification with reason {string}")
@Then("the server stops the operation")
@And("sends final progress notification:")
@And("releases the progress token")

// Scenario 10: Logging
@Given("an MCP server with logging capability")
@And("default log level is {string}")
@When("the client sets log level to {string}")
@Then("server confirms level change")
@When("server operations generate log messages:")
@Then("all messages are sent to client as they meet threshold")
@When("the client sets log level to {string}")
@And("server generates DEBUG and INFO messages")
@Then("only WARNING and ERROR messages are sent")
@When("server generates excessive log messages rapidly")
@Then("rate limiting kicks in after configured threshold")
@And("some messages are dropped to prevent flooding")
```

### Phase 3: Advanced Integration (Sequential - 2 days)
These scenarios test complex integration patterns and require previous phases.

#### Agent 5: Integration & Scalability
**Scenarios**: 15, 16
**Duration**: 2 days
**Dependencies**: Phases 1 & 2 complete

**Missing Step Definitions:**
```java
// Scenario 15: Multi-Server Integration
@Given("a host application managing multiple MCP servers:")
@And("each server maintains security boundaries")
@When("the host aggregates resources from all servers")
@Then("resources are properly isolated by server")
@And("cross-server access is controlled by host")
@When("a git operation requires file system access")
@Then("host coordinates between git_server and file_server")
@But("servers cannot directly access each other")
@When("external API requires user consent")
@Then("host presents unified consent interface")
@And("manages permissions across all servers")
@And("maintains audit trail for all operations")

// Scenario 16: Pagination
@Given("an MCP server with {int}+ resources")
@When("the client requests resource list without pagination")
@Then("server returns first page with reasonable page size")
@And("includes nextCursor for continued pagination")
@When("the client uses cursor to fetch subsequent pages")
@Then("each page contains expected number of items")
@And("resources are returned in consistent order")
@When("the client reaches the final page")
@Then("nextCursor is null or omitted")
@And("total resource count is accurate")
@When("concurrent clients paginate the same dataset")
@Then("each client receives consistent pagination results")  
@And("cursor tokens remain valid across reasonable time window")
```

## Required Supporting Classes

Each agent will need to implement supporting classes and utilities:

### Agent 1 (Security):
```java
- OAuthServer (mock implementation)
- TokenValidator
- SessionManager
- SecurityViolationLogger
```

### Agent 2 (Transport/Errors):
```java
- HttpTransport (extends Transport)
- ErrorCodeMapper
- VersionNegotiator
- ConnectionManager
```

### Agent 3 (Resources/Tools):
```java
- ResourceTemplate
- ResourceSubscriptionManager
- ToolExecutor
- ElicitationProvider
- PromptTemplateEngine
- CompletionProvider
```

### Agent 4 (Client Features):
```java
- SamplingManager
- ModelSelector
- RootSecurityManager
- ProgressTracker
- LogLevelManager
```

### Agent 5 (Integration):
```java
- MultiServerHost
- PaginationCursor
- ResourceAggregator
```

## Parallelization Strategy

- **Phase 1**: Sequential (infrastructure dependencies)
- **Phase 2**: Full parallel execution (4 agents working simultaneously)  
- **Phase 3**: Sequential (depends on all previous work)

## Testing Strategy

Each agent should:
1. Implement step definitions with proper error handling
2. Create mock implementations for external dependencies
3. Use test doubles for complex integrations
4. Follow existing patterns from Scenario 1 implementation
5. Ensure thread-safety for concurrent test execution

## Estimated Timeline

- **Phase 1**: 3 days (2 agents in parallel)
- **Phase 2**: 4 days (4 agents in parallel)  
- **Phase 3**: 2 days (1 agent)
- **Integration & Testing**: 1 day

**Total: ~10 days with 4-5 parallel agents**