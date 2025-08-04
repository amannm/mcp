package com.amannmalik.mcp;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.*;
import java.util.stream.Collectors;

public class McpFeatureSteps {
    private Main server;
    private Main client;
    private Set<String> serverCapabilities;
    private Set<String> clientCapabilities;
    private String negotiatedVersion;
    private boolean initialized;
    private boolean connectionActive;
    private boolean shutdownRequested;

    @Before
    public void setupTestEnvironment() {
        server = new Main();
        client = new Main();
        serverCapabilities = new HashSet<>();
        clientCapabilities = new HashSet<>();
        negotiatedVersion = "";
        initialized = false;
        connectionActive = false;
        shutdownRequested = false;
    }

    @After
    public void cleanupTestEnvironment() {
        serverCapabilities.clear();
        clientCapabilities.clear();
        negotiatedVersion = "";
        initialized = false;
        connectionActive = false;
        shutdownRequested = false;
    }

    // ========================================
    // BACKGROUND AND SETUP STEPS
    // ========================================

    @Given("a clean MCP environment")
    public void aCleanMcpEnvironment() {
        if (connectionActive || initialized || !serverCapabilities.isEmpty() || !clientCapabilities.isEmpty())
            throw new IllegalStateException("environment not clean");
    }

    @Given("protocol version {string} is supported")
    public void protocolVersionIsSupported(String version) {
        negotiatedVersion = version;
    }

    // ========================================
    // LIFECYCLE MANAGEMENT STEPS
    // ========================================

    @Given("an MCP server with comprehensive capabilities:")
    public void anMcpServerWithComprehensiveCapabilities(DataTable capabilitiesTable) {
        capabilitiesTable.asMaps().stream()
                .filter(row -> Boolean.parseBoolean(row.getOrDefault("enabled", "false")))
                .map(row -> row.get("capability"))
                .forEach(serverCapabilities::add);
    }

    @Given("an MCP client with capabilities:")
    public void anMcpClientWithCapabilities(DataTable capabilitiesTable) {
        capabilitiesTable.asMaps().stream()
                .filter(row -> Boolean.parseBoolean(row.getOrDefault("enabled", "false")))
                .map(row -> row.get("capability"))
                .forEach(clientCapabilities::add);
    }

    @When("the client initiates connection with protocol version {string}")
    public void theClientInitiatesConnectionWithProtocolVersion(String version) {
        if (!version.equals(negotiatedVersion)) throw new IllegalStateException("unsupported version");
    }

    @Then("the server responds with supported capabilities")
    public void theServerRespondsWithSupportedCapabilities() {
        if (serverCapabilities.isEmpty()) throw new IllegalStateException("no server capabilities");
    }

    @Then("capability negotiation completes successfully")
    public void capabilityNegotiationCompletesSuccessfully() {
        if (serverCapabilities.isEmpty() || clientCapabilities.isEmpty())
            throw new IllegalStateException("capabilities missing");
    }

    @Then("the client sends {string} notification")
    public void theClientSendsNotification(String notificationType) {
        if (notificationType.equals("initialized")) initialized = true;
    }

    @Then("the connection enters operation phase")
    public void theConnectionEntersOperationPhase() {
        if (!initialized) throw new IllegalStateException("not initialized");
        connectionActive = true;
    }

    @When("the client requests shutdown")
    public void theClientRequestsShutdown() {
        shutdownRequested = true;
    }

    @Then("the connection terminates gracefully")
    public void theConnectionTerminatesGracefully() {
        if (!shutdownRequested) throw new IllegalStateException("shutdown not requested");
        connectionActive = false;
    }

    @Then("all resources are properly cleaned up")
    public void allResourcesAreProperlyCleanedUp() {
        if (connectionActive) throw new IllegalStateException("connection still active");
    }

    // ========================================
    // AUTHORIZATION AND SECURITY STEPS
    // ========================================

    @Given("an MCP server at {string} requiring authorization")
    public void anMcpServerAtRequiringAuthorization(String serverUrl) {
        // TODO: Set up HTTP MCP server with OAuth 2.1 authorization requirement
        // Configure server to reject unauthorized requests with 401 + WWW-Authenticate header
    }

    @Given("an authorization server at {string}")
    public void anAuthorizationServerAt(String authServerUrl) {
        // TODO: Set up OAuth 2.1 authorization server for testing
        // This server handles dynamic client registration and token issuance
    }

    @Given("dynamic client registration is supported")
    public void dynamicClientRegistrationIsSupported() {
        // TODO: Enable dynamic client registration on authorization server
        // This allows clients to register without pre-configured credentials
    }

    @When("the client makes an unauthorized request")
    public void theClientMakesAnUnauthorizedRequest() {
        // TODO: Send MCP request without authorization header
        // This should trigger the authorization flow
    }

    @Then("the server responds with {string}")
    public void theServerRespondsWith(String expectedStatus) {
        // TODO: Verify HTTP status code matches expected value
        // For unauthorized requests, expect 401 status
    }

    @Then("includes {string} header with resource metadata URL")
    public void includesHeaderWithResourceMetadataUrl(String headerName) {
        // TODO: Verify WWW-Authenticate header contains metadata URL
        // This URL provides information about the authorization server
    }

    @When("the client fetches protected resource metadata")
    public void theClientFetchesProtectedResourceMetadata() {
        // TODO: Retrieve OAuth metadata from resource metadata URL
        // This metadata should contain authorization server endpoints and supported flows
    }

    @Then("the metadata contains authorization server URLs")
    public void theMetadataContainsAuthorizationServerUrls() {
        // TODO: Verify metadata contains required OAuth 2.1 endpoints
        // Check for authorization_endpoint, token_endpoint, registration_endpoint
    }

    @When("the client performs dynamic client registration")
    public void theClientPerformsDynamicClientRegistration() {
        // TODO: Execute RFC 7591 dynamic client registration
        // Send client metadata to registration endpoint
    }

    @Then("a client ID and credentials are obtained")
    public void aClientIdAndCredentialsAreObtained() {
        // TODO: Verify successful client registration response
        // Should receive client_id and potentially client_secret
    }

    @When("the client initiates OAuth 2.1 authorization code flow")
    public void theClientInitiatesOauth21AuthorizationCodeFlow() {
        // TODO: Start OAuth 2.1 authorization code flow
        // Redirect user to authorization endpoint with required parameters
    }

    @When("uses PKCE code challenge method {string}")
    public void usesPkceCodeChallengeMethod(String method) {
        // TODO: Generate PKCE code challenge using specified method (S256/plain)
        // Store code verifier for later token exchange
    }

    @When("includes resource parameter {string}")
    public void includesResourceParameter(String resource) {
        // TODO: Add resource parameter to authorization request
        // This binds the token to specific resource server
    }

    @When("user grants consent through authorization server")
    public void userGrantsConsentThroughAuthorizationServer() {
        // TODO: Simulate user consent flow
        // Authorization server redirects back with authorization code
    }

    @Then("authorization code is received at callback")
    public void authorizationCodeIsReceivedAtCallback() {
        // TODO: Verify authorization code in callback URL
        // Extract code parameter from redirect URI
    }

    @When("the client exchanges code for access token")
    public void theClientExchangesCodeForAccessToken() {
        // TODO: Exchange authorization code for access token
        // Send POST request to token endpoint with code and verifier
    }

    @When("includes PKCE code verifier")
    public void includesPkceCodeVerifier() {
        // TODO: Include PKCE code verifier in token exchange
        // Must match the challenge sent in authorization request
    }

    @Then("access token is received with correct audience")
    public void accessTokenIsReceivedWithCorrectAudience() {
        // TODO: Verify token response contains valid access token
        // Check that audience claim matches resource server URI
    }

    @When("the client makes MCP requests with Bearer token")
    public void theClientMakesMcpRequestsWithBearerToken() {
        // TODO: Send MCP requests with Authorization: Bearer header
        // Include access token in all subsequent requests
    }

    @Then("requests are successfully authorized")
    public void requestsAreSuccessfullyAuthorized() {
        // TODO: Verify requests are accepted with valid token
        // Should receive successful responses instead of 401
    }

    @Then("token audience validation passes")
    public void tokenAudienceValidationPasses() {
        // TODO: Verify server validates token audience correctly
        // Token audience must match server's canonical URI
    }

    // ========================================
    // TOKEN VALIDATION AND SECURITY STEPS
    // ========================================

    @Given("an MCP server configured for token validation")
    public void anMcpServerConfiguredForTokenValidation() {
        // TODO: Set up server with JWT token validation
        // Configure token validation middleware with proper key verification
    }

    @Given("the server's canonical URI is {string}")
    public void theServersCanonicalUriIs(String canonicalUri) {
        // TODO: Configure server's canonical URI for audience validation
        // This URI must match the audience claim in valid tokens
    }

    @When("a client presents a token with wrong audience {string}")
    public void aClientPresentsATokenWithWrongAudience(String wrongAudience) {
        // TODO: Send request with token containing incorrect audience claim
        // This should trigger audience validation failure
    }

    @When("logs security violation with level {string}")
    public void logsSecurityViolationWithLevel(String logLevel) {
        // TODO: Verify security violation is logged at specified level
        // Check that audit logs capture the invalid token attempt
    }

    @When("a client presents a token without audience claim")
    public void aClientPresentsATokenWithoutAudienceClaim() {
        // TODO: Send request with token missing audience claim
        // This should fail audience validation
    }

    @When("a client presents a properly scoped token for {string}")
    public void aClientPresentsAProperlycopedTokenFor(String audience) {
        // TODO: Send request with valid token containing correct audience
        // Token should pass all validation checks
    }

    @Then("the server accepts the token")
    public void theServerAcceptsTheToken() {
        // TODO: Verify server accepts valid token
        // Request should proceed to normal processing
    }

    @Then("validates token signature and expiration")
    public void validatesTokenSignatureAndExpiration() {
        // TODO: Verify server checks token cryptographic signature
        // Also verify expiration time validation
    }

    @Then("does not pass token to downstream services")
    public void doesNotPassTokenToDownstreamServices() {
        // TODO: Verify token is not leaked to downstream services
        // Token should be stripped from internal requests
    }

    // ========================================
    // RESOURCE MANAGEMENT STEPS
    // ========================================

    @Given("an MCP server with file system resources")
    public void anMcpServerWithFileSystemResources() {
        // TODO: Set up server with file system resource provider
        // This enables access to files and directories as MCP resources
    }

    @Given("resource templates are configured:")
    public void resourceTemplatesAreConfigured(DataTable templatesTable) {
        // TODO: Configure URI templates for dynamic resource access
        // Parse template definitions and register with resource manager
    }

    @When("the client lists resource templates")
    public void theClientListsResourceTemplates() {
        // TODO: Send resources/templates/list request
        // This should return all registered resource templates
    }

    @Then("all templates are returned with proper schemas")
    public void allTemplatesAreReturnedWithProperSchemas() {
        // TODO: Verify response contains expected templates with valid schemas
        // Check that each template has proper URI pattern and parameter definitions
    }

    @When("the client expands template {string}")
    public void theClientExpandsTemplate(String templateUri) {
        // TODO: Expand resource template with specific parameters
        // Replace template variables with actual values to create concrete resource URI
    }

    @Then("the expanded resource is accessible")
    public void theExpandedResourceIsAccessible() {
        // TODO: Verify expanded resource can be successfully accessed
        // Resource should return content without errors
    }

    @Then("has proper MIME type {string}")
    public void hasProperMimeType(String expectedMimeType) {
        // TODO: Verify resource returns correct MIME type
        // Check Content-Type matches expected type for resource format
    }

    @When("the client subscribes to resource updates for {string}")
    public void theClientSubscribesToResourceUpdatesFor(String resourceUri) {
        // TODO: Subscribe to resource change notifications
        // Send resources/subscribe request for specified URI
    }

    @Then("subscription is confirmed")
    public void subscriptionIsConfirmed() {
        // TODO: Verify subscription was successfully established
        // Should receive confirmation response from server
    }

    @When("the resource content changes externally")
    public void theResourceContentChangesExternally() {
        // TODO: Simulate external change to subscribed resource
        // Modify resource outside of MCP to trigger notification
    }

    @Then("{string} is sent to subscriber")
    public void notificationIsSentToSubscriber(String notificationType) {
        // TODO: Verify notification is sent to subscribed client
        // Check that proper notification message is received
    }

    @Then("notification includes URI and updated title")
    public void notificationIncludesUriAndUpdatedTitle() {
        // TODO: Verify notification contains resource URI and updated metadata
        // Check notification payload includes required fields
    }

    @When("the client reads the updated resource")
    public void theClientReadsTheUpdatedResource() {
        // TODO: Read resource after receiving update notification
        // Fetch current resource content to verify changes
    }

    @Then("new content is returned")
    public void newContentIsReturned() {
        // TODO: Verify resource content reflects the changes
        // Compare with previous content to confirm update
    }

    @Then("proper annotations are included:")
    public void properAnnotationsAreIncluded(DataTable annotationsTable) {
        // TODO: Verify resource includes expected annotations
        // Parse annotations table and validate each annotation is present
    }

    // ========================================
    // TOOL EXECUTION STEPS
    // ========================================

    @Given("an MCP server with tools:")
    public void anMcpServerWithTools(DataTable toolsTable) {
        // TODO: Register tools with specified properties
        // Parse tool definitions and set up mock tool implementations
    }

    @Given("tool {string} has input schema requiring:")
    public void toolHasInputSchemaRequiring(String toolName, DataTable schemaTable) {
        // TODO: Define input schema for specified tool
        // Parse schema requirements and attach to tool definition
    }

    @When("the client calls tool {string} with incomplete arguments:")
    public void theClientCallsToolWithIncompleteArguments(String toolName, DataTable argumentsTable) {
        // TODO: Execute tool call with missing required parameters
        // This should trigger elicitation flow for missing arguments
    }

    @Given("tool {string} has output schema:")
    public void toolHasOutputSchema(String toolName, DataTable schemaTable) {
        // TODO: Define output schema for specified tool
        // Parse schema definitions and attach to tool's output specification
    }

    @Then("the server detects missing required argument {string}")
    public void theServerDetectsMissingRequiredArgument(String argumentName) {
        // TODO: Verify server validates required arguments
        // Should detect when required parameters are not provided
    }

    @Then("initiates elicitation request for missing parameters")
    public void initiatesElicitationRequestForMissingParameters() {
        // TODO: Verify server requests missing parameters through elicitation
        // Should send elicitation request to client for user input
    }

    @When("the client's elicitation provider prompts user")
    public void theClientsElicitationProviderPromptsUser() {
        // TODO: Simulate client presenting elicitation prompt to user
        // Client should display UI for user to provide missing information
    }

    @When("user provides:")
    public void userProvides(DataTable userInputTable) {
        // TODO: Simulate user providing requested information
        // Parse user input table and prepare elicitation response
    }

    @Then("elicitation completes with action {string}")
    public void elicitationCompletesWithAction(String action) {
        // TODO: Verify elicitation completes with specified action (ACCEPT/REJECT)
        // Should receive elicitation response with user's decision
    }

    @When("the server retries tool execution with complete arguments")
    public void theServerRetriesToolExecutionWithCompleteArguments() {
        // TODO: Execute tool with complete argument set after elicitation
        // Server should retry original tool call with elicited parameters
    }

    @Then("tool execution succeeds")
    public void toolExecutionSucceeds() {
        // TODO: Verify tool executes successfully
        // Should receive success response from tool execution
    }

    @Then("returns structured output conforming to output schema")
    public void returnsStructuredOutputConformingToOutputSchema() {
        // TODO: Verify tool output matches defined schema
        // Validate response structure against tool's output schema
    }

    @Then("includes both structured content and text representation")
    public void includesBothStructuredContentAndTextRepresentation() {
        // TODO: Verify tool response includes both formats
        // Should have structured data and human-readable text
    }

    // ========================================
    // SAMPLING AND MODEL INTERACTION STEPS
    // ========================================

    @Given("an MCP client with sampling capability")
    public void anMcpClientWithSamplingCapability() {
        // TODO: Configure client to support server-initiated LLM sampling
        // This allows servers to request LLM interactions through the client
    }

    @Given("model preferences are configured:")
    public void modelPreferencesAreConfigured(DataTable preferencesTable) {
        // TODO: Set up model selection preferences for sampling
        // Parse preference weights for cost, speed, intelligence priorities
    }

    @Given("model hints are configured:")
    public void modelHintsAreConfigured(DataTable hintsTable) {
        // TODO: Configure model hints for sampling requests
        // Parse hint preferences and model priority ordering
    }

    @When("the server requests LLM sampling with message:")
    public void theServerRequestsLlmSamplingWithMessage(String messageJson) {
        // TODO: Send sampling request with specified message content
        // Parse JSON message and create sampling request
    }

    @When("includes model preferences and hints")
    public void includesModelPreferencesAndHints() {
        // TODO: Include model preferences and hints in sampling request
        // Add configured preferences to the sampling request
    }

    @Then("the client presents sampling request to user for approval")
    public void theClientPresentsSamplingRequestToUserForApproval() {
        // TODO: Present sampling request to user via client UI
        // User should be able to review and approve/deny the request
    }

    @When("user approves the sampling request")
    public void userApprovesTheSamplingRequest() {
        // TODO: Simulate user approving the sampling request
        // User grants permission to proceed with LLM interaction
    }

    @Then("the client selects appropriate model based on preferences")
    public void theClientSelectsAppropriateModelBasedOnPreferences() {
        // TODO: Model selection logic based on configured preferences
        // Consider cost, speed, intelligence priorities and hints
    }

    @Then("sends request to LLM with system prompt")
    public void sendsRequestToLlmWithSystemPrompt() {
        // TODO: Send request to selected LLM with proper system prompt
        // Include system context and user message in request
    }

    @When("LLM responds with analysis")
    public void llmRespondsWithAnalysis() {
        // TODO: Simulate LLM response with analysis content
        // Generate realistic analysis response for testing
    }

    @Then("the client presents response to user for review")
    public void theClientPresentsResponseToUserForReview() {
        // TODO: Present LLM response to user for review and approval
        // User should be able to review response before sending to server
    }

    @When("user approves the response")
    public void userApprovesTheResponse() {
        // TODO: Simulate user approving the LLM response
        // User grants permission to send response back to server
    }

    @Then("the response is returned to the server")
    public void theResponseIsReturnedToTheServer() {
        // TODO: Send approved LLM response back to requesting server
        // Complete the sampling request with LLM output
    }

    @Then("includes metadata about selected model and stop reason")
    public void includesMetadataAboutSelectedModelAndStopReason() {
        // TODO: Include sampling metadata in response
        // Add model name, stop reason, and other relevant metadata
    }

    // ========================================
    // UTILITY AND ERROR HANDLING STEPS
    // ========================================

    @When("server operations generate log messages:")
    public void serverOperationsGenerateLogMessages(DataTable messagesTable) {
        // TODO: Generate log messages at various levels
        // Test that logging system properly filters and delivers messages
    }

    @When("the client requests completion for {string}")
    public void theClientRequestsCompletionFor(String prefix) {
        // TODO: Request argument completion for given prefix
        // This tests autocompletion for resource URIs and tool arguments
    }

    // ========================================
    // PROMPTS STEPS
    // ========================================

    @Given("an MCP server with prompt templates:")
    public void anMcpServerWithPromptTemplates(DataTable templatesTable) {
        // TODO: Configure server with prompt templates
        // Parse template definitions with names, descriptions, and arguments
    }

    @Given("prompt {string} has arguments:")
    public void promptHasArguments(String promptName, DataTable argumentsTable) {
        // TODO: Define arguments schema for specified prompt
        // Parse argument definitions including types and requirements
    }

    @When("the client lists available prompts")
    public void theClientListsAvailablePrompts() {
        // TODO: Send prompts/list request to server
        // Should return all configured prompt templates
    }

    @Then("all prompts are returned with argument schemas")
    public void allPromptsAreReturnedWithArgumentSchemas() {
        // TODO: Verify prompt list response includes argument schemas
        // Each prompt should have complete argument definitions
    }

    @When("the client requests prompt {string} with arguments:")
    public void theClientRequestsPromptWithArguments(String promptName, DataTable argumentsTable) {
        // TODO: Send prompts/get request with specified arguments
        // Parse arguments table and include in prompt request
    }

    @Then("the server returns instantiated prompt messages:")
    public void theServerReturnsInstantiatedPromptMessages(DataTable messagesTable) {
        // TODO: Verify prompt response contains properly instantiated messages
        // Check message roles, content types, and content preview
    }

    @Then("prompt includes actual file content in context")
    public void promptIncludesActualFileContentInContext() {
        // TODO: Verify prompt includes referenced file content
        // File content should be embedded in prompt context
    }

    @Then("focuses on specified areas")
    public void focusesOnSpecifiedAreas() {
        // TODO: Verify prompt focuses on requested areas
        // Check that focus areas are properly incorporated
    }

    // ========================================
    // ROOTS AND FILESYSTEM STEPS
    // ========================================

    @Given("an MCP client with root management capability")
    public void anMcpClientWithRootManagementCapability() {
        // TODO: Configure client with root management capability
        // Enable roots capability for filesystem boundary management
    }

    @Given("configured roots:")
    public void configuredRoots(DataTable rootsTable) {
        // TODO: Configure filesystem roots with URIs, names, and permissions
        // Parse roots table and set up filesystem boundaries
    }

    @When("the server requests root list")
    public void theServerRequestsRootList() {
        // TODO: Send roots/list request to client
        // Request available filesystem roots
    }

    @Then("the client returns available roots with proper URIs")
    public void theClientReturnsAvailableRootsWithProperUris() {
        // TODO: Verify roots response contains proper URI formats
        // All roots should have valid file:// URIs
    }

    @Then("each root includes human-readable names")
    public void eachRootIncludesHumanReadableNames() {
        // TODO: Verify each root has descriptive name
        // Names should be user-friendly for root identification
    }

    @When("the server attempts to access {string}")
    public void theServerAttemptsToAccess(String filePath) {
        // TODO: Attempt to access specified file path
        // Test filesystem access within root boundaries
    }

    @Then("access is granted as path is within allowed root")
    public void accessIsGrantedAsPathIsWithinAllowedRoot() {
        // TODO: Verify access is granted for paths within roots
        // Should allow access to files under configured roots
    }

    @Then("access is denied as path is outside allowed roots")
    public void accessIsDeniedAsPathIsOutsideAllowedRoots() {
        // TODO: Verify access is denied for paths outside roots
        // Should reject access to files outside configured boundaries
    }

    @Then("security violation is logged")
    public void securityViolationIsLogged() {
        // TODO: Verify security violation is properly logged
        // Should log attempted access outside boundaries
    }

    @When("root configuration changes \\(new project added)")
    public void rootConfigurationChangesNewProjectAdded() {
        // TODO: Simulate root configuration change
        // Add new root to test dynamic configuration updates
    }

    @When("server refreshes root list")
    public void serverRefreshesRootList() {
        // TODO: Request updated root list after configuration change
        // Server should get updated list with new roots
    }

    @Then("updated roots are returned")
    public void updatedRootsAreReturned() {
        // TODO: Verify root list includes newly added roots
        // Response should reflect current root configuration
    }

    // ========================================
    // PROGRESS AND CANCELLATION STEPS
    // ========================================

    @Given("an MCP server with long-running operations")
    public void anMcpServerWithLongRunningOperations() {
        // TODO: Configure server with operations that support progress tracking
        // Set up operations that can report progress over time
    }

    @When("the client initiates a large resource listing operation")
    public void theClientInitiatesALargeResourceListingOperation() {
        // TODO: Start operation that will take significant time
        // This should trigger progress tracking mechanism
    }

    @Then("progress token is assigned to the request")
    public void progressTokenIsAssignedToTheRequest() {
        // TODO: Verify progress token is created for the operation
        // Token should be unique identifier for tracking progress
    }

    @Then("initial progress notification is sent:")
    public void initialProgressNotificationIsSent(DataTable progressTable) {
        // TODO: Verify initial progress notification with proper fields
        // Should include token, progress value, and total count
    }

    @Then("the operation proceeds")
    public void theOperationProceeds() {
        // TODO: Allow operation to continue and generate progress updates
        // Operation should proceed while sending periodic updates
    }

    @Then("progress notifications are sent periodically:")
    public void progressNotificationsAreSentPeriodically(DataTable progressUpdatesTable) {
        // TODO: Verify periodic progress notifications are sent
        // Each update should show advancing progress with descriptive messages
    }

    @When("the client decides to cancel the operation")
    public void theClientDecidesToCancelTheOperation() {
        // TODO: Initiate cancellation of the long-running operation
        // Client should send cancellation request
    }

    @When("sends cancellation notification with reason {string}")
    public void sendsCancellationNotificationWithReason(String reason) {
        // TODO: Send cancelled notification with specified reason
        // Include cancellation reason in notification
    }

    @Then("the server stops the operation")
    public void theServerStopsTheOperation() {
        // TODO: Verify server stops the operation upon cancellation
        // Operation should halt and clean up resources
    }

    @Then("sends final progress notification:")
    public void sendsFinalProgressNotification(DataTable finalProgressTable) {
        // TODO: Verify final progress notification indicates cancellation
        // Should show final progress state and cancellation message
    }

    @Then("releases the progress token")
    public void releasesTheProgressToken() {
        // TODO: Verify progress token is released after operation completion
        // Token should no longer be valid for further updates
    }

    // ========================================
    // LOGGING STEPS
    // ========================================

    @Given("an MCP server with logging capability")
    public void anMcpServerWithLoggingCapability() {
        // TODO: Configure server with logging capability enabled
        // Set up logging system for message delivery to client
    }

    @Given("default log level is {string}")
    public void defaultLogLevelIs(String logLevel) {
        // TODO: Set default logging level for the server
        // Configure initial threshold for log message delivery
    }

    @When("the client sets log level to {string}")
    public void theClientSetsLogLevelTo(String logLevel) {
        // TODO: Send logging/setLevel request to change log level
        // Update server's logging threshold dynamically
    }

    @Then("server confirms level change")
    public void serverConfirmsLevelChange() {
        // TODO: Verify server acknowledges log level change
        // Should receive confirmation of new log level setting
    }

    @Then("all messages are sent to client as they meet threshold")
    public void allMessagesAreSentToClientAsTheyMeetThreshold() {
        // TODO: Verify all log messages at or above threshold are delivered
        // Messages should be filtered by current log level
    }

    @Then("only WARNING and ERROR messages are sent")
    public void onlyWarningAndErrorMessagesAreSent() {
        // TODO: Verify only high-priority messages are sent
        // DEBUG and INFO messages should be filtered out
    }

    @When("server generates excessive log messages rapidly")
    public void serverGeneratesExcessiveLogMessagesRapidly() {
        // TODO: Generate high volume of log messages to test rate limiting
        // Simulate logging flood to trigger rate limiting mechanism
    }

    @Then("rate limiting kicks in after configured threshold")
    public void rateLimitingKicksInAfterConfiguredThreshold() {
        // TODO: Verify rate limiting activates when threshold is exceeded
        // System should detect and respond to excessive logging
    }

    @Then("some messages are dropped to prevent flooding")
    public void someMessagesAreDroppedToPreventFlooding() {
        // TODO: Verify some messages are dropped during rate limiting
        // Should prevent overwhelming the client with too many messages
    }

    // ========================================
    // COMPLETION STEPS
    // ========================================

    @Given("an MCP server with completion capability")
    public void anMcpServerWithCompletionCapability() {
        // TODO: Configure server with completion capability
        // Enable autocompletion for resource URIs and tool arguments
    }

    @Given("resource template {string} is available")
    public void resourceTemplateIsAvailable(String template) {
        // TODO: Configure specified resource template for completion
        // Template should support parameter completion
    }

    @Given("file system contains:")
    public void fileSystemContains(DataTable filesTable) {
        // TODO: Set up test filesystem with specified files and directories
        // Create filesystem structure for completion testing
    }

    @Then("completion suggestions include:")
    public void completionSuggestionsInclude(DataTable suggestionsTable) {
        // TODO: Verify completion response includes expected suggestions
        // Each suggestion should have value, label, and type
    }

    @Then("suggestions are properly ranked by relevance")
    public void suggestionsAreProperlyRankedByRelevance() {
        // TODO: Verify suggestions are ordered by relevance
        // Most relevant matches should appear first
    }

    // ========================================
    // ERROR HANDLING STEPS
    // ========================================

    @Given("an MCP server and client in operation phase")
    public void anMcpServerAndClientInOperationPhase() {
        // TODO: Set up server and client in operational state
        // Both should be past initialization and ready for requests
    }

    @When("the client sends malformed JSON")
    public void theClientSendsMalformedJson() {
        // TODO: Send invalid JSON to test error handling
        // This should trigger JSON parse error response
    }

    @Then("server responds with {string} \\({int})")
    public void serverRespondsWithError(String errorType, int errorCode) {
        // TODO: Verify proper JSON-RPC error response
        // Check that error code and message match expected values
    }

    @When("the client sends invalid JSON-RPC structure")
    public void theClientSendsInvalidJsonRpcStructure() {
        // TODO: Send JSON with invalid JSON-RPC structure
        // Missing required fields or incorrect format
    }

    @When("the client calls non-existent method {string}")
    public void theClientCallsNonExistentMethod(String methodName) {
        // TODO: Call method that doesn't exist on server
        // Should trigger method not found error
    }

    @When("the client calls valid method with invalid parameters")
    public void theClientCallsValidMethodWithInvalidParameters() {
        // TODO: Call valid method with wrong parameter format
        // Should trigger invalid parameters error
    }

    @When("server encounters internal error during tool execution")
    public void serverEncountersInternalErrorDuringToolExecution() {
        // TODO: Simulate internal server error during operation
        // Should trigger internal error response
    }

    @When("the client sends request before initialization")
    public void theClientSendsRequestBeforeInitialization() {
        // TODO: Send operation request before completing handshake
        // Should trigger lifecycle error
    }

    @Then("server responds with appropriate lifecycle error")
    public void serverRespondsWithAppropriateLifecycleError() {
        // TODO: Verify server rejects premature requests
        // Should indicate initialization is required
    }

    @When("network connection is interrupted during request")
    public void networkConnectionIsInterruptedDuringRequest() {
        // TODO: Simulate network disconnection during operation
        // Test graceful handling of connection loss
    }

    @Then("both sides handle disconnection gracefully")
    public void bothSidesHandleDisconnectionGracefully() {
        // TODO: Verify both client and server handle disconnection properly
        // Should clean up resources and handle errors gracefully
    }

    @Then("pending requests are properly cleaned up")
    public void pendingRequestsAreProperlyCleanedUp() {
        // TODO: Verify pending requests are cancelled or completed
        // No requests should be left in undefined state
    }

    // ========================================
    // TRANSPORT LAYER STEPS
    // ========================================

    @Given("MCP implementation supports both stdio and HTTP transports")
    public void mcpImplementationSupportsBothStdioAndHttpTransports() {
        // TODO: Configure MCP implementation to support both transport types
        // Set up both stdio and HTTP transport capabilities
    }

    @When("testing identical operations across transports:")
    public void testingIdenticalOperationsAcrossTransports(DataTable operationsTable) {
        // TODO: Execute same operations on both stdio and HTTP transports
        // Compare results to ensure functional equivalence
    }

    @Then("results are functionally equivalent")
    public void resultsAreFunctionallyEquivalent() {
        // TODO: Verify identical behavior across transport types
        // Core protocol behavior should be the same regardless of transport
    }

    @Then("HTTP transport includes additional features:")
    public void httpTransportIncludesAdditionalFeatures(DataTable featuresTable) {
        // TODO: Verify HTTP-specific features are available
        // HTTP transport may have additional capabilities not available in stdio
    }

    // ========================================
    // SESSION MANAGEMENT STEPS
    // ========================================

    @Given("an MCP HTTP server with session management")
    public void anMcpHttpServerWithSessionManagement() {
        // TODO: Set up HTTP server with session management capability
        // Configure session storage and security mechanisms
    }

    @Given("multiple server instances sharing session storage")
    public void multipleServerInstancesSharingSessionStorage() {
        // TODO: Configure multiple server instances with shared session store
        // Test session consistency across server instances
    }

    @When("a client connects and receives session ID")
    public void aClientConnectsAndReceivesSessionId() {
        // TODO: Establish client connection and obtain session identifier
        // Session ID should be securely generated
    }

    @Then("session ID is securely generated and non-predictable")
    public void sessionIdIsSecurelyGeneratedAndNonPredictable() {
        // TODO: Verify session ID meets security requirements
        // Should be cryptographically random and non-guessable
    }

    @Then("session is bound to user-specific information")
    public void sessionIsBoundToUserSpecificInformation() {
        // TODO: Verify session includes user binding information
        // Session should be associated with specific user context
    }

    @When("an attacker tries to use guessed session ID")
    public void anAttackerTriesToUseGuessedSessionId() {
        // TODO: Simulate session hijacking attempt with guessed ID
        // Test security against session ID guessing attacks
    }

    @Then("server rejects requests due to user binding mismatch")
    public void serverRejectsRequestsDueToUserBindingMismatch() {
        // TODO: Verify server rejects requests with invalid user binding
        // Session should be validated against user context
    }

    @When("legitimate user makes request with valid session")
    public void legitimateUserMakesRequestWithValidSession() {
        // TODO: Make request with properly authenticated session
        // Should demonstrate correct session validation
    }

    @Then("request includes proper authorization token validation")
    public void requestIncludesProperAuthorizationTokenValidation() {
        // TODO: Verify request includes valid authorization token
        // Session should be backed by proper token validation
    }

    @Then("session binding is verified on each request")
    public void sessionBindingIsVerifiedOnEachRequest() {
        // TODO: Verify session validation occurs on every request
        // Each request should validate session binding
    }

    @When("server processes requests with session context")
    public void serverProcessesRequestsWithSessionContext() {
        // TODO: Process requests using session context information
        // Server should use session data for request processing
    }

    @Then("session data includes user ID and not just session ID")
    public void sessionDataIncludesUserIdAndNotJustSessionId() {
        // TODO: Verify session data includes user identification
        // Session should contain user context, not just session token
    }

    @Then("prevents cross-user impersonation attacks")
    public void preventsCrossUserImpersonationAttacks() {
        // TODO: Verify protection against user impersonation
        // Session system should prevent cross-user access
    }

    // ========================================
    // INTEGRATION & MULTI-SERVER STEPS
    // ========================================

    @Given("a host application managing multiple MCP servers:")
    public void aHostApplicationManagingMultipleMcpServers(DataTable serversTable) {
        // TODO: Set up host application with multiple MCP server connections
        // Configure servers with different capabilities and focus areas
    }

    @Given("each server maintains security boundaries")
    public void eachServerMaintainsSecurityBoundaries() {
        // TODO: Configure security isolation between servers
        // Each server should have its own security context
    }

    @When("the host aggregates resources from all servers")
    public void theHostAggregatesResourcesFromAllServers() {
        // TODO: Collect resources from all connected servers
        // Host should aggregate while maintaining server isolation
    }

    @Then("resources are properly isolated by server")
    public void resourcesAreProperlyIsolatedByServer() {
        // TODO: Verify resources maintain server-specific isolation
        // Resources should be tagged with their originating server
    }

    @Then("cross-server access is controlled by host")
    public void crossServerAccessIsControlledByHost() {
        // TODO: Verify host controls access between servers
        // Direct server-to-server access should be prevented
    }

    @When("a git operation requires file system access")
    public void aGitOperationRequiresFileSystemAccess() {
        // TODO: Simulate operation requiring multiple server capabilities
        // Git server needs file system access from file server
    }

    @Then("host coordinates between git_server and file_server")
    public void hostCoordinatesBetweenGitServerAndFileServer() {
        // TODO: Verify host facilitates inter-server coordination
        // Host should manage communication between servers
    }

    @Then("servers cannot directly access each other")
    public void serversCannotDirectlyAccessEachOther() {
        // TODO: Verify servers cannot bypass host for direct communication
        // All inter-server communication should go through host
    }

    @When("external API requires user consent")
    public void externalApiRequiresUserConsent() {
        // TODO: Simulate operation requiring user consent
        // API server operation needs user permission
    }

    @Then("host presents unified consent interface")
    public void hostPresentsUnifiedConsentInterface() {
        // TODO: Verify host provides consistent consent UI
        // User should see unified interface regardless of server
    }

    @Then("manages permissions across all servers")
    public void managesPermissionsAcrossAllServers() {
        // TODO: Verify host manages permissions consistently
        // Permission model should be unified across servers
    }

    @Then("maintains audit trail for all operations")
    public void maintainsAuditTrailForAllOperations() {
        // TODO: Verify comprehensive audit logging
        // All operations should be logged for security auditing
    }

    // ========================================
    // PERFORMANCE & SCALABILITY STEPS
    // ========================================

    @Given("an MCP server with 10,000+ resources")
    public void anMcpServerWith10000PlusResources() {
        // TODO: Set up server with large dataset for performance testing
        // Create server with significant resource count
    }

    @When("the client requests resource list without pagination")
    public void theClientRequestsResourceListWithoutPagination() {
        // TODO: Request resource list without specifying pagination parameters
        // Server should automatically paginate large result sets
    }

    @Then("server returns first page with reasonable page size")
    public void serverReturnsFirstPageWithReasonablePageSize() {
        // TODO: Verify server paginates results automatically
        // Page size should be reasonable for performance
    }

    @Then("includes nextCursor for continued pagination")
    public void includesNextCursorForContinuedPagination() {
        // TODO: Verify response includes pagination cursor
        // Cursor should allow fetching subsequent pages
    }

    @When("the client uses cursor to fetch subsequent pages")
    public void theClientUsesCursorToFetchSubsequentPages() {
        // TODO: Use pagination cursor to fetch next pages
        // Demonstrate cursor-based pagination workflow
    }

    @Then("each page contains expected number of items")
    public void eachPageContainsExpectedNumberOfItems() {
        // TODO: Verify consistent page size across pages
        // All pages should have similar item counts
    }

    @Then("resources are returned in consistent order")
    public void resourcesAreReturnedInConsistentOrder() {
        // TODO: Verify stable ordering across pagination
        // Resource order should be consistent between requests
    }

    @When("the client reaches the final page")
    public void theClientReachesTheFinalPage() {
        // TODO: Fetch pages until reaching the end of results
        // Demonstrate complete pagination workflow
    }

    @Then("nextCursor is null or omitted")
    public void nextCursorIsNullOrOmitted() {
        // TODO: Verify final page indicates no more results
        // Final page should not include next cursor
    }

    @Then("total resource count is accurate")
    public void totalResourceCountIsAccurate() {
        // TODO: Verify total count matches actual resource count
        // Count should be consistent with actual data
    }

    @When("concurrent clients paginate the same dataset")
    public void concurrentClientsPaginateTheSameDataset() {
        // TODO: Test concurrent pagination from multiple clients
        // Multiple clients should get consistent results
    }

    @Then("each client receives consistent pagination results")
    public void eachClientReceivesConsistentPaginationResults() {
        // TODO: Verify consistent results across concurrent clients
        // All clients should see the same data ordering
    }

    @Then("cursor tokens remain valid across reasonable time window")
    public void cursorTokensRemainValidAcrossReasonableTimeWindow() {
        // TODO: Verify cursor tokens have reasonable validity period
        // Cursors should remain valid for a practical time window
    }

    // ========================================
    // VERSION COMPATIBILITY STEPS
    // ========================================

    @Given("an MCP server supporting versions [{string}]")
    public void anMcpServerSupportingVersions(String versionList) {
        // TODO: Configure server with multiple protocol version support
        // Parse version list and set up version compatibility
    }

    @When("a client requests initialization with version {string}")
    public void aClientRequestsInitializationWithVersion(String requestedVersion) {
        // TODO: Send initialization request with specific version
        // Test version negotiation with requested version
    }

    @Then("server responds with same version {string}")
    public void serverRespondsWithSameVersion(String expectedVersion) {
        // TODO: Verify server accepts requested version
        // Server should confirm the negotiated version
    }

    @Then("operates in compatibility mode")
    public void operatesInCompatibilityMode() {
        // TODO: Verify server operates with older protocol version
        // Server should provide compatibility features
    }

    @When("a client requests unsupported version {string}")
    public void aClientRequestsUnsupportedVersion(String unsupportedVersion) {
        // TODO: Request initialization with unsupported version
        // Test fallback behavior for unsupported versions
    }

    @Then("server responds with supported version from its list")
    public void serverRespondsWithSupportedVersionFromItsList() {
        // TODO: Verify server offers alternative supported version
        // Server should suggest compatible version from its list
    }

    @When("client doesn't support server's fallback version")
    public void clientDoesntSupportServersFallbackVersion() {
        // TODO: Simulate client rejecting server's fallback version
        // Test version negotiation failure scenario
    }

    @Then("client disconnects gracefully")
    public void clientDisconnectsGracefully() {
        // TODO: Verify client handles version mismatch gracefully
        // Client should close connection cleanly
    }

    @Then("logs version mismatch for debugging")
    public void logsVersionMismatchForDebugging() {
        // TODO: Verify version mismatch is logged appropriately
        // Should log version negotiation failure for debugging
    }

    // ========================================
    // HELPER METHODS FOR DATA TABLE PARSING
    // ========================================

    /** Parse capabilities table into structured format. */
    private List<Capability> parseCapabilitiesTable(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new Capability(
                        Objects.requireNonNull(row.get("capability")),
                        row.getOrDefault("feature", ""),
                        Boolean.parseBoolean(row.getOrDefault("enabled", "false"))))
                .toList();
    }

    /** Parse resource templates table into template definitions. */
    private List<ResourceTemplateRow> parseResourceTemplates(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new ResourceTemplateRow(
                        Objects.requireNonNull(row.get("template")),
                        Objects.requireNonNull(row.get("description")),
                        Objects.requireNonNull(row.get("mime_type"))))
                .toList();
    }

    /** Parse tool definitions table. */
    private List<ToolDefinition> parseToolDefinitions(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new ToolDefinition(
                        Objects.requireNonNull(row.get("name")),
                        Objects.requireNonNull(row.get("description")),
                        Boolean.parseBoolean(row.getOrDefault("requires_confirmation", "false"))))
                .toList();
    }

    /** Parse input schema table for tool parameters. */
    private List<InputField> parseInputSchema(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new InputField(
                        Objects.requireNonNull(row.get("field")),
                        Objects.requireNonNull(row.get("type")),
                        Boolean.parseBoolean(row.getOrDefault("required", "false")),
                        Objects.requireNonNull(row.get("description"))))
                .toList();
    }

    /** Parse arguments table for tool calls. */
    private Map<String, String> parseArguments(DataTable table) {
        return table.asMaps().stream()
                .collect(Collectors.toMap(
                        row -> Objects.requireNonNull(row.get("field")),
                        row -> Objects.requireNonNull(row.get("value"))));
    }

    /** Parse model preferences table. */
    private List<ModelPreference> parseModelPreferences(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new ModelPreference(
                        Objects.requireNonNull(row.get("preference")),
                        Objects.requireNonNull(row.get("value")),
                        Objects.requireNonNull(row.get("description"))))
                .toList();
    }

    /** Parse log messages table for testing. */
    private List<LogMessage> parseLogMessages(DataTable table) {
        return table.asMaps().stream()
                .map(row -> new LogMessage(
                        Objects.requireNonNull(row.get("level")),
                        Objects.requireNonNull(row.get("logger")),
                        Objects.requireNonNull(row.get("message"))))
                .toList();
    }

    private record Capability(String capability, String feature, boolean enabled) {}

    private record ResourceTemplateRow(String template, String description, String mimeType) {}

    private record ToolDefinition(String name, String description, boolean requiresConfirmation) {}

    private record InputField(String field, String type, boolean required, String description) {}

    private record ModelPreference(String preference, String value, String description) {}

    private record LogMessage(String level, String logger, String message) {}

    @Then("the server rejects the token with {string}")
    public void theServerRejectsTheTokenWith(String arg0) {

    }

    @Then("{string} is sent to server")
    public void isSentToServer(String arg0) {

    }

    @Given("an MCP server supporting versions [{string}, {string}]")
    public void anMCPServerSupportingVersions(String arg0, String arg1) {
    }

    @Then("server responds with {string}")
    public void serverRespondsWith(String arg0) {
    }

    @And("server generates DEBUG and INFO messages")
    public void serverGeneratesDEBUGAndINFOMessages() {
    }
}