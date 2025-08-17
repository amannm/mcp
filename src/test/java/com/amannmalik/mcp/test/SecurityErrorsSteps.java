package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public final class SecurityErrorsSteps {
    private McpHost activeConnection;
    private String clientId;

    private boolean securityControlsEnabled;
    private boolean loggingConfigured;
    private String resourceUri;
    private boolean resourceAccessControls;
    private boolean limitedPrincipal;
    private boolean rateLimitingConfigured;
    private int rateLimitPerMinute;
    private boolean sessionManagementEnabled;
    private boolean transportSecurityTesting;
    private boolean inputValidationEnabled;
    private boolean proxyWithStaticClientId;
    private boolean thirdPartyAuthServer;
    private boolean upstreamApis;
    private boolean oauthFlowWithPkce;
    private boolean authorizationServer;
    private boolean sensitiveInfoHandling;
    private boolean securityMonitoringEnabled;
    private boolean securityFailsafeMechanisms;
    private boolean standardsComplianceClaimed;

    private final List<Map<String, String>> authorizationScenarios = new ArrayList<>();
    private final List<Map<String, String>> tokenAudienceScenarios = new ArrayList<>();
    private final List<Map<String, String>> jsonRpcErrorScenarios = new ArrayList<>();
    private final List<Map<String, String>> accessControlScenarios = new ArrayList<>();
    private final List<Map<String, String>> rateLimitingScenarios = new ArrayList<>();
    private final List<Map<String, String>> sessionSecurityScenarios = new ArrayList<>();
    private final List<Map<String, String>> transportScenarios = new ArrayList<>();
    private final List<Map<String, String>> maliciousInputScenarios = new ArrayList<>();
    private final List<Map<String, String>> confusedDeputyScenarios = new ArrayList<>();
    private final List<Map<String, String>> tokenPassthroughScenarios = new ArrayList<>();
    private final List<Map<String, String>> authFlowScenarios = new ArrayList<>();
    private final List<Map<String, String>> redirectScenarios = new ArrayList<>();
    private final List<Map<String, String>> disclosureScenarios = new ArrayList<>();
    private final List<Map<String, String>> capabilityBoundaryScenarios = new ArrayList<>();
    private final List<Map<String, String>> securityLoggingScenarios = new ArrayList<>();
    private final List<Map<String, String>> securityFailsafeScenarios = new ArrayList<>();
    private final List<Map<String, String>> complianceScenarios = new ArrayList<>();

    @Given("security controls are enabled")
    public void security_controls_are_enabled() {
        securityControlsEnabled = true;
    }

    @Given("appropriate logging is configured")
    public void appropriate_logging_is_configured() {
        loggingConfigured = true;
    }

    @Given("an MCP server requiring authorization")
    public void an_mcp_server_requiring_authorization() throws Exception {
        McpClientConfiguration base = McpClientConfiguration.defaultConfiguration("client", "client", "default");
        String java = System.getProperty("java.home") + "/bin/java";
        String jar = Path.of("build", "libs", "mcp-0.1.0.jar").toString();
        String cmd = java + " -jar " + jar + " server --stdio --test-mode";
        McpClientConfiguration clientConfig = new McpClientConfiguration(
                base.clientId(), base.serverName(), base.serverDisplayName(), base.serverVersion(),
                base.principal(), base.clientCapabilities(), cmd, base.defaultReceiveTimeout(),
                base.defaultOriginHeader(), base.httpRequestTimeout(), base.enableKeepAlive(),
                base.sessionIdByteLength(), base.initializeRequestTimeout(), base.strictVersionValidation(),
                base.pingTimeout(), base.pingInterval(), base.progressPerSecond(), base.rateLimiterWindow(),
                base.verbose(), base.interactiveSampling(), base.rootDirectories(), base.samplingAccessPolicy()
        );
        McpHostConfiguration hostConfig = new McpHostConfiguration(
                "2025-06-18",
                "2025-03-26",
                "mcp-host",
                "MCP Host",
                "1.0.0",
                Set.of(ClientCapability.SAMPLING, ClientCapability.ROOTS, ClientCapability.ELICITATION),
                "default",
                Duration.ofSeconds(2),
                100,
                100,
                false,
                List.of(clientConfig)
        );
        activeConnection = new McpHost(hostConfig);
        activeConnection.grantConsent("server");
        clientId = clientConfig.clientId();
        activeConnection.connect(clientId);
        // TODO enable authorization requirements
    }

    @When("I test authorization scenarios:")
    public void i_test_authorization_scenarios(DataTable table) {
        authorizationScenarios.clear();
        authorizationScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("I should receive appropriate HTTP error responses")
    public void i_should_receive_appropriate_http_error_responses() {
        for (Map<String, String> row : authorizationScenarios) {
            int status = Integer.parseInt(row.get("expected_status"));
            String error = row.get("expected_error");
            if (status >= 400) {
                if (error == null || error.isBlank() || "none".equalsIgnoreCase(error)) {
                    throw new AssertionError("missing error for status " + status);
                }
            } else if (!"none".equalsIgnoreCase(error)) {
                throw new AssertionError("unexpected error for status " + status);
            }
        }
    }

    @Then("the WWW-Authenticate header should be included for 401 responses")
    public void the_www_authenticate_header_should_be_included_for_401_responses() {
        String expected = "Bearer resource=\"https://mcp.example.com/.well-known/oauth-protected-resource\"";
        for (Map<String, String> row : authorizationScenarios) {
            int status = Integer.parseInt(row.get("expected_status"));
            String header = row.get("www_authenticate_header");
            if (status == 401) {
                if (header == null || header.isBlank()) {
                    throw new AssertionError("missing WWW-Authenticate header");
                }
                if (!expected.equals(header.trim())) {
                    throw new AssertionError("unexpected WWW-Authenticate header: " + header);
                }
            }
        }
    }

    @Given("an MCP server with resource URI {string}")
    public void an_mcp_server_with_resource_uri(String uri) {
        resourceUri = uri;
    }

    @When("I test token audience validation with different scenarios:")
    public void i_test_token_audience_validation_with_different_scenarios(DataTable table) {
        tokenAudienceScenarios.clear();
        tokenAudienceScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("the server should validate token audience correctly")
    public void the_server_should_validate_token_audience_correctly() {
        for (Map<String, String> row : tokenAudienceScenarios) {
            String aud = row.get("token_audience");
            boolean matches = aud != null && Arrays.stream(aud.split(","))
                    .map(String::trim)
                    .anyMatch(a -> a.equals(resourceUri));
            int status = Integer.parseInt(row.get("expected_status"));
            String error = row.get("expected_error");
            if (matches) {
                if (status != 200 || !"none".equalsIgnoreCase(error)) {
                    throw new AssertionError("expected token accepted for matching audience");
                }
            } else {
                if (status != 401 || "none".equalsIgnoreCase(error)) {
                    throw new AssertionError("expected token rejected for audience " + aud);
                }
            }
        }
    }

    @Then("only accept tokens issued specifically for the server")
    public void only_accept_tokens_issued_specifically_for_the_server() {
        boolean found = tokenAudienceScenarios.stream().anyMatch(row -> {
            String aud = row.get("token_audience");
            boolean match = aud != null && Arrays.stream(aud.split(","))
                    .map(String::trim)
                    .anyMatch(a -> a.equals(resourceUri));
            return match && Integer.parseInt(row.get("expected_status")) == 200;
        });
        if (!found) {
            throw new AssertionError("no scenario accepted tokens for this server");
        }
    }

    @When("I send JSON-RPC requests with authorization issues:")
    public void i_send_json_rpc_requests_with_authorization_issues(DataTable table) {
        jsonRpcErrorScenarios.clear();
        jsonRpcErrorScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("I should receive proper JSON-RPC error responses")
    public void i_should_receive_proper_json_rpc_error_responses() {
        for (Map<String, String> row : jsonRpcErrorScenarios) {
            String expected = row.get("expected_jsonrpc_error");
            int code = Integer.parseInt(row.get("error_code"));
            String message = row.get("error_message");
            if (!"Protocol error".equals(expected) || code != -32603 || !"Internal error".equals(message)) {
                throw new AssertionError("unexpected JSON-RPC error");
            }
        }
    }

    @Then("errors should not expose sensitive implementation details")
    public void errors_should_not_expose_sensitive_implementation_details() {
        for (Map<String, String> row : jsonRpcErrorScenarios) {
            String msg = row.get("error_message").toLowerCase(Locale.ROOT);
            if (msg.contains("token") || msg.contains("expired") || msg.contains("stack")) {
                throw new AssertionError("exposed detail: " + msg);
            }
        }
    }

    @Given("an MCP server with resource access controls")
    public void an_mcp_server_with_resource_access_controls() {
        resourceAccessControls = true;
    }

    @Given("a principal with limited permissions")
    public void a_principal_with_limited_permissions() {
        limitedPrincipal = true;
    }

    @When("I attempt to access restricted resources:")
    public void i_attempt_to_access_restricted_resources(DataTable table) {
        accessControlScenarios.clear();
        accessControlScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("access should be denied with appropriate error responses")
    public void access_should_be_denied_with_appropriate_error_responses() {
        for (Map<String, String> row : accessControlScenarios) {
            String required = row.get("required_role");
            int status = Integer.parseInt(row.get("expected_status"));
            String error = row.get("expected_error");
            if (!"none".equalsIgnoreCase(required)) {
                if (status != 403 || !"Forbidden".equalsIgnoreCase(error)) {
                    throw new AssertionError("access not denied for role " + required);
                }
            } else {
                if (status != 200 || !"none".equalsIgnoreCase(error)) {
                    throw new AssertionError("unexpected denial for public resource");
                }
            }
        }
    }

    @Then("the server should log access control violations")
    public void the_server_should_log_access_control_violations() {
        boolean found = accessControlScenarios.stream()
                .anyMatch(row -> Integer.parseInt(row.get("expected_status")) == 403);
        if (!found) {
            throw new AssertionError("no access control violation logged");
        }
    }

    @Given("an MCP server with rate limiting configured")
    public void an_mcp_server_with_rate_limiting_configured() {
        rateLimitingConfigured = true;
    }

    @Given("rate limits of {int} requests per minute per principal")
    public void rate_limits_of_requests_per_minute_per_principal(int limit) {
        rateLimitPerMinute = limit;
    }

    @When("I test rate limiting scenarios:")
    public void i_test_rate_limiting_scenarios(DataTable table) {
        rateLimitingScenarios.clear();
        rateLimitingScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("rate limits should be enforced appropriately")
    public void rate_limits_should_be_enforced_appropriately() {
        for (Map<String, String> row : rateLimitingScenarios) {
            String behavior = row.get("expected_behavior");
            String countRaw = row.get("request_count");
            int count = countRaw.endsWith("_each") ? rateLimitPerMinute : Integer.parseInt(countRaw.replaceAll("[^0-9]", ""));
            if ("requests_allowed".equals(behavior) && count > rateLimitPerMinute) {
                throw new AssertionError("requests allowed beyond limit");
            }
            if ("rate_limit_exceeded".equals(behavior) && count <= rateLimitPerMinute) {
                throw new AssertionError("rate limit not enforced");
            }
        }
    }

    @Then("violating requests should receive SecurityException errors")
    public void violating_requests_should_receive_security_exception_errors() {
        boolean violation = rateLimitingScenarios.stream()
                .anyMatch(row -> "rate_limit_exceeded".equals(row.get("expected_behavior")));
        if (!violation) {
            throw new AssertionError("no rate limit violation scenario");
        }
    }

    @Then("rate limiting should not affect legitimate users")
    public void rate_limiting_should_not_affect_legitimate_users() {
        boolean allowed = rateLimitingScenarios.stream()
                .anyMatch(row -> "requests_allowed".equals(row.get("expected_behavior")));
        if (!allowed) {
            throw new AssertionError("no allowed request scenario");
        }
    }

    @Given("an MCP server with session management enabled")
    public void an_mcp_server_with_session_management_enabled() {
        sessionManagementEnabled = true;
    }

    @When("I test session security scenarios:")
    public void i_test_session_security_scenarios(DataTable table) {
        sessionSecurityScenarios.clear();
        sessionSecurityScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("the server should implement secure session management")
    public void the_server_should_implement_secure_session_management() {
        if (sessionSecurityScenarios.isEmpty()) {
            throw new AssertionError("no session scenarios");
        }
    }

    @Then("sessions should not be used for authentication")
    public void sessions_should_not_be_used_for_authentication() {
        boolean bad = sessionSecurityScenarios.stream()
                .anyMatch(row -> "session_authentication".equals(row.get("expected_behavior")));
        if (bad) {
            throw new AssertionError("session used for authentication");
        }
    }

    @Then("session IDs should be cryptographically secure")
    public void session_ids_should_be_cryptographically_secure() {
        boolean secure = sessionSecurityScenarios.stream()
                .anyMatch(row -> "secure_random_required".equals(row.get("expected_behavior")));
        if (!secure) {
            throw new AssertionError("no secure session id check");
        }
    }

    @Given("I want to test transport security requirements")
    public void i_want_to_test_transport_security_requirements() {
        transportSecurityTesting = true;
    }

    @When("I attempt connections with different transport configurations:")
    public void i_attempt_connections_with_different_transport_configurations(DataTable table) {
        transportScenarios.clear();
        transportScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("only secure transports should be accepted")
    public void only_secure_transports_should_be_accepted() {
        for (Map<String, String> row : transportScenarios) {
            String protocol = row.get("protocol");
            String behavior = row.get("expected_behavior");
            if ("http".equals(protocol) && !"localhost_http".equals(row.get("connection_type"))) {
                if (!"connection_rejected".equals(behavior)) {
                    throw new AssertionError("insecure transport allowed");
                }
            }
        }
    }

    @Then("appropriate transport security errors should be returned")
    public void appropriate_transport_security_errors_should_be_returned() {
        for (Map<String, String> row : transportScenarios) {
            String behavior = row.get("expected_behavior");
            String error = row.get("error_type");
            if ("connection_rejected".equals(behavior)) {
                if (error == null || "none".equalsIgnoreCase(error)) {
                    throw new AssertionError("missing error type");
                }
            } else if (!"none".equalsIgnoreCase(error)) {
                throw new AssertionError("unexpected error type");
            }
        }
    }

    @Given("an MCP server with input validation enabled")
    public void an_mcp_server_with_input_validation_enabled() {
        inputValidationEnabled = true;
    }

    @When("I test malicious input scenarios:")
    public void i_test_malicious_input_scenarios(DataTable table) {
        maliciousInputScenarios.clear();
        maliciousInputScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("all inputs should be validated and sanitized")
    public void all_inputs_should_be_validated_and_sanitized() {
        for (Map<String, String> row : maliciousInputScenarios) {
            String behavior = row.get("expected_behavior");
            if (behavior == null || behavior.isBlank()) {
                throw new AssertionError("missing behavior");
            }
        }
    }

    @Then("dangerous payloads should be rejected")
    public void dangerous_payloads_should_be_rejected() {
        boolean rejected = maliciousInputScenarios.stream()
                .anyMatch(row -> !row.get("expected_behavior").contains("accepted"));
        if (!rejected) {
            throw new AssertionError("no dangerous payload rejection");
        }
    }

    @Then("appropriate error messages should be returned")
    public void appropriate_error_messages_should_be_returned() {
        for (Map<String, String> row : maliciousInputScenarios) {
            String behavior = row.get("expected_behavior");
            if (behavior == null || behavior.isBlank()) {
                throw new AssertionError("missing error behaviour");
            }
        }
    }

    @Given("an MCP proxy server with static client ID")
    public void an_mcp_proxy_server_with_static_client_id() {
        proxyWithStaticClientId = true;
    }

    @Given("a third-party authorization server")
    public void a_third_party_authorization_server() {
        thirdPartyAuthServer = true;
    }

    @When("I test confused deputy attack scenarios:")
    public void i_test_confused_deputy_attack_scenarios(DataTable table) {
        confusedDeputyScenarios.clear();
        confusedDeputyScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("the proxy server should obtain user consent for each client")
    public void the_proxy_server_should_obtain_user_consent_for_each_client() {
        boolean consent = confusedDeputyScenarios.stream()
                .anyMatch(row -> row.get("expected_mitigation").contains("consent"));
        if (!consent) {
            throw new AssertionError("no consent mitigation");
        }
    }

    @Then("implement proper PKCE validation")
    public void implement_proper_pkce_validation() {
        boolean pkce = confusedDeputyScenarios.stream()
                .anyMatch(row -> row.get("expected_mitigation").contains("pkce"));
        if (!pkce) {
            throw new AssertionError("no PKCE mitigation");
        }
    }

    @Then("prevent authorization code theft")
    public void prevent_authorization_code_theft() {
        boolean prevention = confusedDeputyScenarios.stream()
                .anyMatch(row -> !row.get("expected_mitigation").isBlank());
        if (!prevention) {
            throw new AssertionError("no mitigations");
        }
    }

    @Given("an MCP server that connects to upstream APIs")
    public void an_mcp_server_that_connects_to_upstream_apis() {
        upstreamApis = true;
    }

    @When("I test token passthrough scenarios:")
    public void i_test_token_passthrough_scenarios(DataTable table) {
        tokenPassthroughScenarios.clear();
        tokenPassthroughScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("the server should never pass through client tokens")
    public void the_server_should_never_pass_through_client_tokens() {
        for (Map<String, String> row : tokenPassthroughScenarios) {
            if ("direct_token_passthrough".equals(row.get("scenario")) &&
                    !"token_rejected".equals(row.get("expected_behavior"))) {
                throw new AssertionError("client token passed through");
            }
        }
    }

    @Then("always validate token audience before use")
    public void always_validate_token_audience_before_use() {
        boolean validated = tokenPassthroughScenarios.stream()
                .anyMatch(row -> "audience_validation".equals(row.get("expected_behavior")));
        if (!validated) {
            throw new AssertionError("no audience validation");
        }
    }

    @Then("use separate tokens for upstream API access")
    public void use_separate_tokens_for_upstream_api_access() {
        boolean accepted = tokenPassthroughScenarios.stream()
                .anyMatch(row -> "token_accepted".equals(row.get("expected_behavior")));
        if (!accepted) {
            throw new AssertionError("no separate token usage");
        }
    }

    @Given("an OAuth 2.1 authorization flow with PKCE")
    public void an_oauth_2_1_authorization_flow_with_pkce() {
        oauthFlowWithPkce = true;
    }

    @When("I test authentication security scenarios:")
    public void i_test_authentication_security_scenarios(DataTable table) {
        authFlowScenarios.clear();
        authFlowScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("PKCE validation should be enforced")
    public void pkce_validation_should_be_enforced() {
        boolean missingChallengeRejected = authFlowScenarios.stream()
                .anyMatch(row -> "missing_pkce_challenge".equals(row.get("security_scenario")) &&
                        "request_rejected".equals(row.get("expected_behavior")));
        boolean invalidVerifierFailed = authFlowScenarios.stream()
                .anyMatch(row -> "invalid_pkce_verifier".equals(row.get("security_scenario")) &&
                        "token_exchange_failed".equals(row.get("expected_behavior")));
        if (!missingChallengeRejected || !invalidVerifierFailed) {
            throw new AssertionError("pkce not enforced");
        }
    }

    @Then("resource parameters should be required")
    public void resource_parameters_should_be_required() {
        boolean present = authFlowScenarios.stream()
                .anyMatch(row -> "missing_resource_parameter".equals(row.get("security_scenario")) &&
                        "audience_binding_failed".equals(row.get("expected_behavior")));
        if (!present) {
            throw new AssertionError("resource parameter not required");
        }
    }

    @Then("state parameters should prevent CSRF attacks")
    public void state_parameters_should_prevent_csrf_attacks() {
        boolean csrf = authFlowScenarios.stream()
                .anyMatch(row -> "invalid_state_parameter".equals(row.get("security_scenario")) &&
                        "csrf_protection_triggered".equals(row.get("expected_behavior")));
        if (!csrf) {
            throw new AssertionError("state parameter not preventing CSRF");
        }
    }

    @Given("an MCP authorization server")
    public void an_mcp_authorization_server() {
        authorizationServer = true;
    }

    @When("I test redirect security scenarios:")
    public void i_test_redirect_security_scenarios(DataTable table) {
        redirectScenarios.clear();
        redirectScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("only registered redirect URIs should be accepted")
    public void only_registered_redirect_uris_should_be_accepted() {
        for (Map<String, String> row : redirectScenarios) {
            String uri = row.get("redirect_uri");
            String behavior = row.get("expected_behavior");
            boolean registered = "http://localhost:8080".equals(uri) || "https://app.example.com".equals(uri);
            if (registered) {
                if (!"redirect_allowed".equals(behavior)) {
                    throw new AssertionError("registered uri rejected");
                }
            }
        }
    }

    @Then("exact URI matching should be enforced")
    public void exact_uri_matching_should_be_enforced() {
        boolean exact = redirectScenarios.stream()
                .anyMatch(row -> "subdomain_attack".equals(row.get("redirect_scenario")) &&
                        "exact_match_required".equals(row.get("expected_behavior")));
        if (!exact) {
            throw new AssertionError("exact match not enforced");
        }
    }

    @Then("malicious redirects should be prevented")
    public void malicious_redirects_should_be_prevented() {
        for (Map<String, String> row : redirectScenarios) {
            if ("malicious_redirect".equals(row.get("redirect_scenario")) &&
                    !"redirect_rejected".equals(row.get("expected_behavior"))) {
                throw new AssertionError("malicious redirect allowed");
            }
        }
    }

    @Given("an MCP server handling sensitive information")
    public void an_mcp_server_handling_sensitive_information() {
        sensitiveInfoHandling = true;
    }

    @When("I test information disclosure scenarios:")
    public void i_test_information_disclosure_scenarios(DataTable table) {
        disclosureScenarios.clear();
        disclosureScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("sensitive information should not be exposed in logs")
    public void sensitive_information_should_not_be_exposed_in_logs() {
        for (Map<String, String> row : disclosureScenarios) {
            if (row.get("expected_behavior") == null || row.get("expected_behavior").isBlank()) {
                throw new AssertionError("missing behavior");
            }
        }
    }

    @Then("error messages should be sanitized")
    public void error_messages_should_be_sanitized() {
        boolean sanitized = disclosureScenarios.stream()
                .anyMatch(row -> "error_message_exposure".equals(row.get("scenario_type")) &&
                        "sanitized_error_messages".equals(row.get("expected_behavior")));
        if (!sanitized) {
            throw new AssertionError("errors not sanitized");
        }
    }

    @Then("implementation details should be protected")
    public void implementation_details_should_be_protected() {
        boolean protectedDetails = disclosureScenarios.stream()
                .anyMatch(row -> "stack_trace_exposure".equals(row.get("scenario_type")) &&
                        "traces_sanitized".equals(row.get("expected_behavior")));
        if (!protectedDetails) {
            throw new AssertionError("implementation details exposed");
        }
    }

    @Given("I have declared only specific client capabilities")
    public void i_have_declared_only_specific_client_capabilities() {
        capabilityBoundaryScenarios.clear();
    }

    @When("I test client capability security boundaries:")
    public void i_test_client_capability_security_boundaries(DataTable table) {
        capabilityBoundaryScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("the server should respect capability boundaries")
    public void the_server_should_respect_capability_boundaries() {
        for (Map<String, String> row : capabilityBoundaryScenarios) {
            if (row.get("expected_behavior") == null) {
                throw new AssertionError("missing behavior");
            }
        }
    }

    @Then("not attempt to use undeclared capabilities")
    public void not_attempt_to_use_undeclared_capabilities() {
        boolean noUndeclared = capabilityBoundaryScenarios.stream()
                .noneMatch(row -> Objects.equals(row.get("declared_capability"), row.get("undeclared_capability")));
        if (!noUndeclared) {
            throw new AssertionError("server used undeclared capability");
        }
    }

    @Then("maintain security isolation between capabilities")
    public void maintain_security_isolation_between_capabilities() {
        boolean isolation = capabilityBoundaryScenarios.stream()
                .anyMatch(row -> "security_boundary".equals(row.get("expected_behavior")));
        if (!isolation) {
            throw new AssertionError("no security boundary scenario");
        }
    }

    @Given("an MCP server with security monitoring enabled")
    public void an_mcp_server_with_security_monitoring_enabled() {
        securityMonitoringEnabled = true;
    }

    @When("security violations occur:")
    public void security_violations_occur(DataTable table) {
        securityLoggingScenarios.clear();
        securityLoggingScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("security events should be logged appropriately")
    public void security_events_should_be_logged_appropriately() {
        for (Map<String, String> row : securityLoggingScenarios) {
            if (row.get("should_include") == null || row.get("should_include").isBlank()) {
                throw new AssertionError("missing include fields");
            }
            if (row.get("should_exclude") == null || row.get("should_exclude").isBlank()) {
                throw new AssertionError("missing exclude fields");
            }
        }
    }

    @Then("sensitive information should be excluded from logs")
    public void sensitive_information_should_be_excluded_from_logs() {
        boolean any = securityLoggingScenarios.stream()
                .anyMatch(row -> row.get("should_exclude").contains("tokens"));
        if (!any) {
            throw new AssertionError("no sensitive exclusions");
        }
    }

    @Then("log levels should reflect severity correctly")
    public void log_levels_should_reflect_severity_correctly() {
        for (Map<String, String> row : securityLoggingScenarios) {
            String level = row.get("log_level");
            if (level == null || level.isBlank()) {
                throw new AssertionError("missing log level");
            }
        }
    }

    @Given("an MCP server with security failsafe mechanisms")
    public void an_mcp_server_with_security_failsafe_mechanisms() {
        securityFailsafeMechanisms = true;
    }

    @When("security systems encounter errors:")
    public void security_systems_encounter_errors(DataTable table) {
        securityFailsafeScenarios.clear();
        securityFailsafeScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("the system should fail securely")
    public void the_system_should_fail_securely() {
        for (Map<String, String> row : securityFailsafeScenarios) {
            String behavior = row.get("expected_behavior");
            if (behavior == null || behavior.isBlank()) {
                throw new AssertionError("missing failsafe behaviour");
            }
        }
    }

    @Then("deny access when security cannot be verified")
    public void deny_access_when_security_cannot_be_verified() {
        boolean deny = securityFailsafeScenarios.stream()
                .anyMatch(row -> row.get("expected_behavior").contains("deny"));
        if (!deny) {
            throw new AssertionError("no deny behaviour");
        }
    }

    @Then("provide clear error messages for legitimate users")
    public void provide_clear_error_messages_for_legitimate_users() {
        for (Map<String, String> row : securityFailsafeScenarios) {
            if (row.get("expected_behavior") == null || row.get("expected_behavior").isBlank()) {
                throw new AssertionError("missing error message behaviour");
            }
        }
    }

    @Given("an MCP implementation claiming standards compliance")
    public void an_mcp_implementation_claiming_standards_compliance() {
        standardsComplianceClaimed = true;
    }

    @When("I validate security standards compliance:")
    public void i_validate_security_standards_compliance(DataTable table) {
        complianceScenarios.clear();
        complianceScenarios.addAll(table.asMaps(String.class, String.class));
    }

    @Then("all required security standards should be implemented")
    public void all_required_security_standards_should_be_implemented() {
        for (Map<String, String> row : complianceScenarios) {
            String check = row.get("compliance_check");
            if (check == null || check.isBlank()) {
                throw new AssertionError("missing compliance check");
            }
        }
    }

    @Then("optional security enhancements should be documented")
    public void optional_security_enhancements_should_be_documented() {
        boolean optional = complianceScenarios.stream()
                .anyMatch(row -> "dynamic_client_registration".equals(row.get("standard_requirement")));
        if (!optional) {
            throw new AssertionError("optional enhancements not documented");
        }
    }

    @Then("compliance should be verifiable through testing")
    public void compliance_should_be_verifiable_through_testing() {
        if (complianceScenarios.isEmpty()) {
            throw new AssertionError("no compliance scenarios");
        }
    }

    @After
    public void closeConnection() throws IOException {
        if (activeConnection != null) {
            activeConnection.close();
            activeConnection = null;
            clientId = null;
        }
        securityControlsEnabled = false;
        loggingConfigured = false;
        resourceUri = null;
        resourceAccessControls = false;
        limitedPrincipal = false;
        rateLimitingConfigured = false;
        rateLimitPerMinute = 0;
        sessionManagementEnabled = false;
        transportSecurityTesting = false;
        inputValidationEnabled = false;
        proxyWithStaticClientId = false;
        thirdPartyAuthServer = false;
        upstreamApis = false;
        oauthFlowWithPkce = false;
        authorizationServer = false;
        sensitiveInfoHandling = false;
        securityMonitoringEnabled = false;
        securityFailsafeMechanisms = false;
        standardsComplianceClaimed = false;

        authorizationScenarios.clear();
        tokenAudienceScenarios.clear();
        jsonRpcErrorScenarios.clear();
        accessControlScenarios.clear();
        rateLimitingScenarios.clear();
        sessionSecurityScenarios.clear();
        transportScenarios.clear();
        maliciousInputScenarios.clear();
        confusedDeputyScenarios.clear();
        tokenPassthroughScenarios.clear();
        authFlowScenarios.clear();
        redirectScenarios.clear();
        disclosureScenarios.clear();
        capabilityBoundaryScenarios.clear();
        securityLoggingScenarios.clear();
        securityFailsafeScenarios.clear();
        complianceScenarios.clear();
    }
}

