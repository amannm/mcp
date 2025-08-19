@security
Feature: MCP Security Handling
  # Verifies conformance to specification/2025-06-18/basic/authorization.mdx
  # and specification/2025-06-18/basic/security_best_practices.mdx
  As an MCP implementation
  I want to properly handle and report security violations and attacks
  So that I can protect resources, users, and maintain system integrity

  Background:
    Given a clean MCP environment
    And security controls are enabled
    And appropriate logging is configured

  @authorization @http-errors
  Scenario: Authorization header validation
    # Tests specification/2025-06-18/basic/authorization.mdx:236-250 (Token requirements)
    # Tests specification/2025-06-18/basic/authorization.mdx:89-92 (WWW-Authenticate header)
    # Tests specification/2025-06-18/basic/authorization.mdx:283-295 (Authorization error status codes)
    Given an MCP server requiring authorization
    When I test authorization scenarios:
      | auth_header             | expected_status | expected_error | www_authenticate_header |
      | missing                 | 401             | Unauthorized   | Bearer resource=https://mcp.example.com/.well-known/oauth-protected-resource |
      | empty_string            | 401             | Unauthorized   | Bearer resource=https://mcp.example.com/.well-known/oauth-protected-resource |
      | malformed_bearer        | 400             | Bad Request    | none |
      | Bearer invalid_token    | 401             | Unauthorized   | Bearer resource=https://mcp.example.com/.well-known/oauth-protected-resource |
      | Bearer expired_token    | 401             | Unauthorized   | Bearer resource=https://mcp.example.com/.well-known/oauth-protected-resource |
      | Basic username:password | 401             | Unauthorized   | Bearer resource=https://mcp.example.com/.well-known/oauth-protected-resource |
    Then I should receive appropriate HTTP error responses
    And the WWW-Authenticate header should be "Bearer resource=https://mcp.example.com/.well-known/oauth-protected-resource" for 401 responses

  @authorization @metadata-discovery
  Scenario: Authorization server metadata discovery
    # Tests specification/2025-06-18/basic/authorization.mdx:70-107 (Authorization server discovery)
    Given an MCP server requiring authorization
    When I retrieve the protected resource metadata
    Then the metadata should include authorization servers:
      | authorization_server     |
      | https://auth.example.com |
    And the authorization server metadata should include required fields:
      | field                  |
      | issuer                 |
      | authorization_endpoint |
      | token_endpoint         |

  @authorization @token-validation
  Scenario: Token audience validation errors
    # Tests specification/2025-06-18/basic/authorization.mdx:294-302 (Token audience binding)
    # Tests specification/2025-06-18/basic/authorization.mdx:364-369 (Access token privilege restriction)
    # Tests specification/2025-06-18/basic/security_best_practices.mdx:120-144 (Token passthrough)
    Given an MCP server with resource URI "https://mcp.example.com"
    When I test token audience validation with different scenarios:
      | token_scenario             | token_audience              | expected_status | expected_error |
      | wrong_audience             | https://other.example.com   | 401             | Unauthorized   |
      | missing_audience           | null                        | 401             | Unauthorized   |
      | multiple_wrong_audiences   | https://other1.com,other2   | 401             | Unauthorized   |
      | partially_correct_audience | https://mcp.example.com,bad | 200             | none           |
      | correct_audience           | https://mcp.example.com     | 200             | none           |
      | uppercase_audience         | https://MCP.EXAMPLE.COM     | 200             | none           |
    Then the server should validate token audience correctly
    And only accept tokens issued specifically for the server

  @authorization @jsonrpc-errors
  Scenario: JSON-RPC authorization error responses
    # Tests specification/2025-06-18/basic/authorization.mdx:280-287 (Error handling)
    Given an MCP server requiring authorization
    When I send JSON-RPC requests with authorization issues:
      | request_scenario         | auth_header      | expected_jsonrpc_error | error_code | error_message  |
      | missing_auth_header      | none             | Protocol error         | -32603     | Internal error |
      | invalid_token_format     | Bearer malformed | Protocol error         | -32603     | Internal error |
      | expired_access_token     | Bearer expired   | Protocol error         | -32603     | Internal error |
      | insufficient_permissions | Bearer limited   | Protocol error         | -32603     | Internal error |
    Then I should receive proper JSON-RPC error responses
    And errors should not expose sensitive implementation details

  @access-control @forbidden-errors
  Scenario: Resource access control violations
    # Tests specification/2025-06-18/basic/authorization.mdx:280-287 (403 Forbidden usage)
    Given an MCP server with resource access controls
    And a principal with limited permissions
    When I attempt to access restricted resources:
      | resource_type   | principal_role | required_role | expected_status | expected_error |
      | admin_tools     | user           | admin         | 403             | Forbidden      |
      | sensitive_data  | guest          | user          | 403             | Forbidden      |
      | system_config   | user           | system        | 403             | Forbidden      |
      | public_resource | guest          | none          | 200             | none           |
    Then access should be denied with appropriate error responses
    And the server should log access control violations

  @rate-limiting @security-violations
  Scenario: Rate limiting security violations
    # Tests specification/2025-06-18/basic/security_best_practices.mdx:218-231 (Session hijacking mitigation)
    Given an MCP server with rate limiting configured
    And rate limits of 10 requests per minute per principal
    When I test rate limiting scenarios:
      | scenario             | request_count | time_window | expected_behavior   |
      | within_limit         | 5             | 30s         | requests_allowed    |
      | at_limit             | 10            | 60s         | requests_allowed    |
      | exceeding_limit      | 15            | 60s         | rate_limit_exceeded |
      | burst_attack         | 50            | 10s         | rate_limit_exceeded |
      | different_principals | 10_each       | 60s         | requests_allowed    |
    Then rate limits should be enforced appropriately
    And violating requests should receive SecurityException errors
    And rate limiting should not affect legitimate users

  @session-security @hijacking-prevention
  Scenario: Session hijacking attack prevention
    # Tests specification/2025-06-18/basic/security_best_practices.mdx:145-231 (Session hijacking)
    # Tests specification/2025-06-18/basic/security_best_practices.mdx:221-228 (Mitigation strategies)
    Given an MCP server with session management enabled
    When I test session security scenarios:
      | attack_scenario           | attack_method     | expected_behavior        |
      | session_id_guessing       | predictable_ids   | secure_random_required   |
      | session_hijack_injection  | malicious_payload | request_validation       |
      | session_impersonation     | stolen_session_id | additional_auth_required |
      | cross_user_session_access | wrong_user_id     | user_binding_enforced    |
    Then the server should implement secure session management
    And sessions should not be used for authentication
    And session IDs should be cryptographically secure

  @transport-security @https-requirements
  Scenario: Transport security enforcement
    # Tests specification/2025-06-18/basic/authorization.mdx:315-323 (Communication security)
    Given I want to test transport security requirements
    When I attempt connections with different transport configurations:
      | connection_type     | protocol | expected_behavior   | error_type         |
      | insecure_http       | http     | connection_rejected | transport_security |
      | secure_https        | https    | connection_allowed  | none               |
      | localhost_http      | http     | connection_allowed  | none               |
      | invalid_certificate | https    | connection_rejected | certificate_error  |
    Then only secure transports should be accepted
    And appropriate transport security errors should be returned

  @transport-security @tls-defaults
  Scenario: TLS configuration defaults
    # Tests specification/2025-06-18/server/configuration.mdx:11-22 (TLS configuration parameters)
    Given an MCP server configuration
    When I inspect TLS configuration defaults
    Then the TLS configuration should be:
      | https_port | protocols         | cipher_suites                                        | require_client_auth |
      | 3443       | TLSv1.3,TLSv1.2   | TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384       | false              |

  @input-validation @injection-attacks
  Scenario: Malicious input validation and sanitization
    # Tests specification/2025-06-18/server/tools.mdx:427-442 (Security considerations)
    # Tests specification/2025-06-18/server/resources.mdx:397-402 (Security considerations)
    Given an MCP server with input validation enabled
    When I test malicious input scenarios:
      | input_type        | malicious_payload             | expected_behavior   |
      | sql_injection     | '; DROP TABLE users; --       | input_sanitized     |
      | script_injection  | <script>alert('xss')</script> | input_escaped       |
      | path_traversal    | ../../../etc/passwd           | path_validation     |
      | command_injection | ; rm -rf /                    | command_sanitized   |
      | json_injection    | {"evil": "payload"}           | json_validation     |
      | oversized_payload | 10MB_string                   | size_limit_enforced |
    Then all inputs should be validated and sanitized
    And dangerous payloads should be rejected
    And appropriate error messages should be returned

  @confused-deputy @proxy-attacks
  Scenario: Confused deputy attack prevention
    # Tests specification/2025-06-18/basic/security_best_practices.mdx:19-119 (Confused deputy problem)
    # Tests specification/2025-06-18/basic/authorization.mdx:347-354 (Confused deputy problem)
    Given an MCP proxy server with static client ID
    And a third-party authorization server
    When I test confused deputy attack scenarios:
      | attack_scenario             | attack_method           | expected_mitigation   |
      | malicious_redirect_uri      | crafted_authorization   | user_consent_required |
      | stolen_authorization_code   | code_interception       | pkce_validation       |
      | dynamic_client_registration | malicious_client        | consent_verification  |
      | cookie_based_consent_bypass | existing_consent_cookie | per_client_consent    |
    Then the proxy server should obtain user consent for each client
    And implement proper PKCE validation
    And prevent authorization code theft

  @token-security @passthrough-prevention
  Scenario: Token passthrough attack prevention
    # Tests specification/2025-06-18/basic/security_best_practices.mdx:120-144 (Token passthrough risks)
    # Tests specification/2025-06-18/basic/authorization.mdx:272-277 (Token handling)
    Given an MCP server that connects to upstream APIs
    When I test token passthrough scenarios:
      | scenario                     | token_usage              | expected_behavior   |
      | direct_token_passthrough     | client_token_to_upstream | token_rejected      |
      | unvalidated_token_forwarding | unvalidated_passthrough  | validation_required |
      | cross_service_token_reuse    | wrong_audience_token     | audience_validation |
      | proper_token_exchange        | server_issued_token      | token_accepted      |
    Then the server should never pass through client tokens
    And always validate token audience before use
    And use separate tokens for upstream API access

  @authentication-flow @pkce-violations
  Scenario: PKCE and authentication flow security
    # Tests specification/2025-06-18/basic/authorization.mdx:324-331 (Authorization code protection)
    # Tests specification/2025-06-18/basic/authorization.mdx:194-202 (Resource parameter implementation)
    Given an OAuth 2.1 authorization flow with PKCE
    When I test authentication security scenarios:
      | security_scenario          | attack_method       | expected_behavior         |
      | missing_pkce_challenge     | no_code_challenge   | request_rejected          |
      | invalid_pkce_verifier      | wrong_code_verifier | token_exchange_failed     |
      | missing_resource_parameter | no_resource_param   | audience_binding_failed   |
      | invalid_state_parameter    | state_mismatch      | csrf_protection_triggered |
      | authorization_code_replay  | reused_auth_code    | code_invalidated          |
    Then PKCE validation should be enforced
    And resource parameters should be required
    And state parameters should prevent CSRF attacks

  @redirect-security @open-redirection
  Scenario: Open redirection attack prevention
    # Tests specification/2025-06-18/basic/authorization.mdx:332-346 (Open redirection)
    Given an MCP authorization server
    When I test redirect security scenarios:
      | redirect_scenario         | redirect_uri             | expected_behavior    |
      | unregistered_redirect     | https://attacker.com     | redirect_rejected    |
      | malicious_redirect        | javascript:alert('xss')  | redirect_rejected    |
      | localhost_redirect        | http://localhost:8080    | redirect_allowed     |
      | registered_https_redirect | https://app.example.com  | redirect_allowed     |
      | subdomain_attack          | https://evil.example.com | exact_match_required |
    Then only registered redirect URIs should be accepted
    And exact URI matching should be enforced
    And malicious redirects should be prevented

  @sensitive-data @information-disclosure
  Scenario: Sensitive information protection
    # Tests specification/2025-06-18/server/utilities/logging.mdx:133-144 (Security)
    # Tests specification/2025-06-18/basic/lifecycle.mdx:99-104 (Implementation info sharing)
    Given an MCP server handling sensitive information
    When I test information disclosure scenarios:
      | scenario_type          | sensitive_data_type     | expected_behavior        |
      | error_message_exposure | internal_system_details | sanitized_error_messages |
      | token_logging          | access_tokens           | tokens_redacted          |
      | credential_exposure    | api_keys                | credentials_filtered     |
      | personal_data_leakage  | user_information        | pii_protection           |
      | stack_trace_exposure   | implementation_details  | traces_sanitized         |
    Then sensitive information should not be exposed in logs
    And error messages should be sanitized
    And implementation details should be protected

  @client-security @capability-boundaries
  Scenario: Client capability boundary enforcement
    # Tests that servers respect declared client capabilities for security
    Given I have declared only specific client capabilities
    When I test client capability security boundaries:
      | declared_capability | undeclared_capability | server_request_type | expected_behavior       |
      | roots               | sampling              | sampling_request    | method_not_found_error  |
      | sampling            | elicitation           | elicit_request      | capability_not_declared |
      | elicitation         | roots                 | roots_request       | security_boundary       |
      | none                | any                   | any_request         | all_requests_rejected   |
    Then the server should respect capability boundaries
    And not attempt to use undeclared capabilities
    And maintain security isolation between capabilities

  @error-disclosure @security-logging
  Scenario: Security error logging and monitoring
    # Tests appropriate security event logging without information disclosure
    Given an MCP server with security monitoring enabled
    When security violations occur:
      | violation_type           | log_level | should_include         | should_exclude   |
      | failed_authentication    | warning   | timestamp,ip,user_id   | tokens,passwords |
      | authorization_failure    | warning   | resource,principal     | internal_details |
      | rate_limit_exceeded      | info      | rate,window,identifier | request_content  |
      | malicious_input_detected | error     | input_type,source      | actual_payload   |
      | session_hijack_attempt   | critical  | session_id,ip_mismatch | session_data     |
    Then security events should be logged appropriately
    And sensitive information should be excluded from logs
    And log levels should reflect severity correctly

  @recovery @security-failsafe
  Scenario: Security error recovery and failsafe behavior
    # Tests graceful degradation and recovery from security errors
    Given an MCP server with security failsafe mechanisms
    When security systems encounter errors:
      | error_scenario           | system_response     | expected_behavior   |
      | auth_service_unavailable | fail_closed         | deny_all_requests   |
      | rate_limiter_failure     | conservative_limits | apply_strict_limits |
      | token_validator_error    | reject_tokens       | require_reauth      |
      | access_control_failure   | minimal_permissions | deny_by_default     |
      | session_store_corruption | invalidate_sessions | force_reauth        |
    Then the system should fail securely
    And deny access when security cannot be verified
    And provide clear error messages for legitimate users

  @compliance @security-standards
  Scenario: Security standards compliance validation
    # Tests compliance with OAuth 2.1 and security best practices
    Given an MCP implementation claiming standards compliance
    When I validate security standards compliance:
      | standard_requirement        | test_scenario      | compliance_check        |
      | oauth_2_1_pkce              | authorization_flow | pkce_enforced           |
      | resource_indicators         | token_requests     | resource_param_required |
      | secure_transport            | all_communications | https_enforced          |
      | token_audience_binding      | token_validation   | audience_verified       |
      | dynamic_client_registration | client_onboarding  | dcr_supported           |
    Then all required security standards should be implemented
    And optional security enhancements should be documented
    And compliance should be verifiable through testing

  @resource-exhaustion @dos-protection
  Scenario: Resource exhaustion attacks
    # Tests protection against denial-of-service attacks via resource exhaustion
    # Tests specification/2025-06-18/basic/security_best_practices.mdx (Resource limits)
    Given an MCP server with resource protection enabled
    When I test resource exhaustion scenarios:
      | attack_type           | attack_method                | expected_behavior        |
      | memory_exhaustion     | massive_message_flood        | memory_limit_enforced    |
      | connection_exhaustion | excessive_connections        | connection_limit_applied |
      | cpu_exhaustion        | complex_computation_requests | processing_limit_set     |
      | disk_exhaustion       | large_file_operations        | disk_quota_enforced      |
      | bandwidth_exhaustion  | high_volume_data_transfer    | bandwidth_throttling     |
    Then the server should reject excessive resource requests
    And maintain service availability for legitimate users
    And log resource exhaustion attempts appropriately

  @malformed-input @boundary-testing
  Scenario: Malformed JSON-RPC boundary testing
    # Tests handling of malformed JSON-RPC messages at protocol boundaries
    # Tests specification/2025-06-18/basic/index.mdx:33-95 (Message format validation)
    Given an MCP server with strict input validation
    When I send malformed JSON-RPC messages:
      | malformation_type        | malformed_content                    | expected_response        |
      | invalid_json             | {"jsonrpc": "2.0", "id": 1,         | parse_error_32700        |
      | missing_required_field   | {"id": 1, "method": "ping"}          | invalid_request_32600    |
      | invalid_field_type       | {"jsonrpc": 2.0, "id": 1}           | parse_error_32700        |
      | invalid_jsonrpc_version | {"jsonrpc": "1.0", "id": 1, "method": "ping"} | invalid_request_32600 |
      | oversized_id_field       | {"jsonrpc": "2.0", "id": "x" * 1000} | invalid_request_32600   |
      | null_id_field           | {"jsonrpc": "2.0", "id": null}       | invalid_request_32600    |
      | invalid_method_type     | {"jsonrpc": "2.0", "id": 1, "method": 123} | invalid_request_32600 |
      | oversized_message       | 100MB+ message payload               | message_too_large        |
    Then each malformed message should be rejected with appropriate error codes
    And the connection should remain stable after malformed input
    And error responses should not expose internal implementation details

  @transport-security @certificate-validation
  Scenario: TLS/Certificate validation errors
    # Tests TLS certificate validation and transport security error handling
    # Tests specification/2025-06-18/basic/authorization.mdx:315-323 (Communication security)
    Given an MCP client with strict TLS validation enabled
    When I test TLS certificate validation scenarios:
      | certificate_issue         | test_scenario                 | expected_behavior   |
      | expired_certificate       | past_expiration_date          | connection_rejected |
      | self_signed_certificate   | untrusted_ca_authority        | connection_rejected |
      | wrong_hostname            | certificate_hostname_mismatch | connection_rejected |
      | revoked_certificate       | certificate_in_crl            | connection_rejected |
      | weak_cipher_suite         | deprecated_encryption         | connection_rejected |
      | invalid_certificate_chain | broken_trust_path             | connection_rejected |
      | missing_certificate       | no_server_certificate         | connection_rejected |
    Then TLS handshake should fail for invalid certificates
    And appropriate SSL/TLS error messages should be provided
    And fallback to insecure connections should not occur
    And certificate validation errors should be logged securely
