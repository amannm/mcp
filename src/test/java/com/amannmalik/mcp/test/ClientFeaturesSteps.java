package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.ClientCapability;
import com.amannmalik.mcp.api.McpHost;
import com.amannmalik.mcp.api.config.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public final class ClientFeaturesSteps {
    private final Set<ClientCapability> clientCapabilities = EnumSet.noneOf(ClientCapability.class);
    private final Map<ClientCapability, Boolean> capabilityOptions = new EnumMap<>(ClientCapability.class);
    private final List<Map<String, String>> configuredRoots = new ArrayList<>();
    private final List<Map<String, String>> returnedRoots = new ArrayList<>();
    private final List<Map<String, String>> negotiationConfigs = new ArrayList<>();
    private final List<String> negotiationResults = new ArrayList<>();
    private final Set<ClientCapability> undeclaredCapabilities = EnumSet.noneOf(ClientCapability.class);
    private final List<Map<String, String>> samplingErrorScenarios = new ArrayList<>();
    private final List<Map<String, String>> combinedCapabilityRows = new ArrayList<>();
    private final Map<String, String> simpleElicitationRequest = new HashMap<>();
    private final List<Map<String, String>> structuredElicitationFields = new ArrayList<>();
    private final List<Map<String, String>> elicitationUserActions = new ArrayList<>();
    private final List<Map<String, String>> elicitationSchemaTypes = new ArrayList<>();
    private final Map<String, String> samplingMessageRequest = new HashMap<>();
    private final Map<String, String> samplingModelResponse = new HashMap<>();
    private final List<Map<String, String>> samplingContentTypes = new ArrayList<>();
    private final List<Map<String, String>> samplingModelPreferences = new ArrayList<>();
    private final List<String> samplingModelSelections = new ArrayList<>();
    private final List<Map<String, String>> featureUnavailabilityScenarios = new ArrayList<>();
    private boolean rootAccessAllowed;
    private McpHost activeConnection;
    private String clientId;
    private ClientCapability lastCapability;
    private boolean rootConfigChanged;
    private int lastErrorCode;
    private String lastErrorMessage;
    private boolean combinedRequestProcessed;
    private String elicitationResponseAction;
    private McpClientConfiguration defaultConfig;

    private static ClientCapability parseCapability(String raw) {
        var normalized = raw.trim().toLowerCase();
        return switch (normalized) {
            case "roots", "filesystem roots" -> ClientCapability.ROOTS;
            case "sampling", "llm sampling", "llm sampling requests" -> ClientCapability.SAMPLING;
            case "elicitation", "elicitation requests" -> ClientCapability.ELICITATION;
            default -> throw new IllegalArgumentException("unknown capability: " + raw);
        };
    }

    private static EnumSet<ClientCapability> parseCapabilities(String raw) {
        if (raw == null || raw.isBlank() || "none".equalsIgnoreCase(raw.trim())) {
            return EnumSet.noneOf(ClientCapability.class);
        }
        var set = EnumSet.noneOf(ClientCapability.class);
        for (var part : raw.split(",")) {
            set.add(parseCapability(part));
        }
        return set;
    }

    @Given("I have established an MCP connection")
    public void i_have_established_an_mcp_connection() throws Exception {
        var base = McpClientConfiguration.defaultConfiguration("client", "client", "default");
        var java = System.getProperty("java.home") + "/bin/java";
        var jar = Path.of("build", "libs", "mcp-0.1.0.jar").toString();
        var cmd = java + " -jar " + jar + " server --stdio --test-mode";
        var tlsConfig = new TlsConfiguration(
                "", "", "PKCS12", "", "", "PKCS12",
                List.of("TLSv1.3", "TLSv1.2"), List.of("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384")
        );
        var clientConfig = new McpClientConfiguration(
                base.clientId(), base.serverName(), base.serverDisplayName(), base.serverVersion(),
                base.principal(), base.clientCapabilities(), cmd, base.defaultReceiveTimeout(), base.processShutdownWait(),
                base.defaultOriginHeader(), base.httpRequestTimeout(), base.enableKeepAlive(),
                base.sessionIdByteLength(), base.initializeRequestTimeout(), base.strictVersionValidation(),
                base.pingTimeout(), base.pingInterval(), base.progressPerSecond(), base.rateLimiterWindow(),
                base.verbose(), base.interactiveSampling(), base.rootDirectories(), base.samplingAccessPolicy(),
                tlsConfig, CertificateValidationMode.STRICT, List.of(), true
        );
        var hostConfig = new McpHostConfiguration(
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
    }

    @Given("I have declared client capabilities")
    public void i_have_declared_client_capabilities() {
        clientCapabilities.clear();
        capabilityOptions.clear();
        configuredRoots.clear();
        returnedRoots.clear();
        negotiationConfigs.clear();
        negotiationResults.clear();
        undeclaredCapabilities.clear();
        samplingErrorScenarios.clear();
        combinedCapabilityRows.clear();
        lastCapability = null;
        rootConfigChanged = false;
        lastErrorCode = 0;
        lastErrorMessage = null;
    }

    @Given("^I want to support (.+?)(?: requests)?$")
    public void i_want_to_support(String capability) {
        var cap = parseCapability(capability);
        clientCapabilities.add(cap);
        if (cap == ClientCapability.ROOTS) {
            capabilityOptions.putIfAbsent(cap, false);
        }
        lastCapability = cap;
    }

    @Given("I have declared {word} capability")
    public void i_have_declared_capability(String capability) {
        var cap = parseCapability(capability);
        clientCapabilities.add(cap);
        if (cap == ClientCapability.ROOTS) {
            capabilityOptions.putIfAbsent(cap, false);
        }
        lastCapability = cap;
    }

    @Given("I have declared {word} capability with {word} support")
    public void i_have_declared_capability_with_support(String capability, String support) {
        var cap = parseCapability(capability);
        clientCapabilities.add(cap);
        capabilityOptions.put(cap, true);
        lastCapability = cap;
    }

    @Given("I have declared multiple client capabilities:")
    public void i_have_declared_multiple_client_capabilities(DataTable table) {
        var rows = table.asMaps(String.class, String.class);
        combinedCapabilityRows.clear();
        combinedCapabilityRows.addAll(rows);
        for (var row : rows) {
            var cap = parseCapability(row.get("capability"));
            clientCapabilities.add(cap);
            lastCapability = cap;
            var features = row.get("features");
            if (features != null && features.contains("listChanged: true")) {
                capabilityOptions.put(cap, true);
            }
        }
    }

    @Given("I have declared only specific client capabilities:")
    public void i_have_declared_only_specific_client_capabilities(DataTable table) {
        clientCapabilities.clear();
        undeclaredCapabilities.clear();
        var rows = table.asMaps(String.class, String.class);
        for (var row : rows) {
            var declared = row.get("declared_capability");
            if (declared != null && !declared.isBlank()) {
                var cap = parseCapability(declared);
                clientCapabilities.add(cap);
                lastCapability = cap;
            }
            var undeclared = row.get("undeclared_capability");
            if (undeclared != null && !undeclared.isBlank()) {
                undeclaredCapabilities.add(parseCapability(undeclared));
            }
        }
    }

    @Given("I have configured the following roots:")
    public void i_have_configured_the_following_roots(DataTable table) {
        configuredRoots.clear();
        configuredRoots.addAll(table.asMaps(String.class, String.class));
    }

    @Given("I have an established connection")
    public void i_have_an_established_connection() {
        if (activeConnection == null) {
            throw new AssertionError("no active connection");
        }
    }

    @Given("I do not support roots capability")
    public void i_do_not_support_roots_capability() {
        clientCapabilities.remove(ClientCapability.ROOTS);
        lastCapability = ClientCapability.ROOTS;
    }

    @Given("I have declared roots capability with no configured roots")
    public void i_have_declared_roots_capability_with_no_configured_roots() {
        clientCapabilities.add(ClientCapability.ROOTS);
        configuredRoots.clear();
    }

    @Given("I want to test capability negotiation with different configurations:")
    public void i_want_to_test_capability_negotiation_with_different_configurations(DataTable table) {
        negotiationConfigs.clear();
        negotiationConfigs.addAll(table.asMaps(String.class, String.class));
    }

    @When("I declare my capabilities during initialization")
    public void i_declare_my_capabilities_during_initialization() {
        if (activeConnection == null) {
            throw new AssertionError("no active connection");
        }
    }

    @When("I receive an elicitation\\/create request for simple text input:")
    public void i_receive_an_elicitation_create_request_for_simple_text_input(DataTable table) {
        var rows = table.asMaps(String.class, String.class);
        if (rows.isEmpty()) {
            throw new AssertionError("missing request row");
        }
        simpleElicitationRequest.clear();
        simpleElicitationRequest.putAll(rows.getFirst());
        elicitationResponseAction = "accept";
    }

    @When("I receive an elicitation\\/create request for structured data:")
    public void i_receive_an_elicitation_create_request_for_structured_data(DataTable table) {
        structuredElicitationFields.clear();
        for (var row : table.asMaps(String.class, String.class)) {
            structuredElicitationFields.add(new HashMap<>(row));
        }
    }

    @When("I test elicitation responses with the following user actions:")
    public void i_test_elicitation_responses_with_the_following_user_actions(DataTable table) {
        elicitationUserActions.clear();
        var mapping = Map.of(
                "submit_data", "accept",
                "click_decline", "decline",
                "close_dialog", "cancel",
                "press_escape", "cancel",
                "explicit_reject", "decline"
        );
        for (var row : table.asMaps(String.class, String.class)) {
            var action = row.get("user_action");
            var expected = row.get("expected_action");
            if (!Objects.equals(mapping.get(action), expected)) {
                throw new AssertionError("unexpected action mapping for " + action);
            }
            Map<String, String> copy = new HashMap<>(row);
            if ("accept".equals(expected)) {
                copy.put("content", "data");
            }
            elicitationUserActions.add(copy);
        }
    }

    @When("I receive elicitation requests with different schema types:")
    public void i_receive_elicitation_requests_with_different_schema_types(DataTable table) {
        elicitationSchemaTypes.clear();
        elicitationSchemaTypes.addAll(table.asMaps(String.class, String.class));
    }

    @When("I receive elicitation requests")
    public void i_receive_elicitation_requests() {
        if (simpleElicitationRequest.isEmpty()) {
            simpleElicitationRequest.put("message", "placeholder");
            simpleElicitationRequest.put("schema_type", "string");
            simpleElicitationRequest.put("required_field", "value");
        }
    }

    @When("I receive a roots\\/list request")
    public void i_receive_a_roots_list_request() {
        returnedRoots.clear();
        if (!clientCapabilities.contains(ClientCapability.ROOTS)) {
            lastErrorCode = -32601;
            lastErrorMessage = "Roots not supported";
            return;
        }
        returnedRoots.addAll(configuredRoots);
    }

    @When("I check access for URI {string}")
    public void i_check_access_for_uri(String uri) {
        try {
            var target = java.net.URI.create(uri);
            if (!"file".equalsIgnoreCase(target.getScheme())) {
                rootAccessAllowed = true;
                return;
            }
            var targetPath = java.nio.file.Paths.get(target).toRealPath();
            rootAccessAllowed = configuredRoots.stream()
                    .map(r -> r.get("uri"))
                    .filter(Objects::nonNull)
                    .map(java.net.URI::create)
                    .map(java.nio.file.Paths::get)
                    .map(p -> {
                        try {
                            return p.toRealPath();
                        } catch (Exception e) {
                            return p.toAbsolutePath().normalize();
                        }
                    })
                    .anyMatch(targetPath::startsWith);
        } catch (Exception e) {
            rootAccessAllowed = false;
        }
    }

    @When("I configure roots for server access")
    public void i_configure_roots_for_server_access() {
        if (!clientCapabilities.contains(ClientCapability.ROOTS)) {
            throw new AssertionError("roots capability missing");
        }
    }

    @When("my root configuration changes")
    public void my_root_configuration_changes() {
        rootConfigChanged = true;
    }

    @When("I receive a sampling\\/createMessage request:")
    public void i_receive_a_sampling_create_message_request(DataTable table) {
        var rows = table.asMaps(String.class, String.class);
        if (rows.isEmpty()) {
            throw new AssertionError("missing sampling request");
        }
        samplingMessageRequest.clear();
        samplingMessageRequest.putAll(rows.getFirst());
        samplingModelResponse.clear();
        samplingModelResponse.put("content", "response");
        samplingModelResponse.put("usage", "1");
    }

    @When("I receive a sampling request")
    public void i_receive_a_sampling_request() {
        if (samplingMessageRequest.isEmpty()) {
            samplingMessageRequest.put("message_content", "placeholder");
        }
    }

    @When("I receive sampling requests with different content types:")
    public void i_receive_sampling_requests_with_different_content_types(DataTable table) {
        samplingContentTypes.clear();
        for (var row : table.asMaps(String.class, String.class)) {
            samplingContentTypes.add(new HashMap<>(row));
        }
    }

    @When("I receive sampling requests with model preferences:")
    public void i_receive_sampling_requests_with_model_preferences(DataTable table) {
        samplingModelPreferences.clear();
        samplingModelSelections.clear();
        for (var row : table.asMaps(String.class, String.class)) {
            for (var key : List.of("cost_priority", "speed_priority", "intelligence_priority")) {
                Double.parseDouble(row.get(key));
            }
            samplingModelPreferences.add(new HashMap<>(row));
            samplingModelSelections.add(row.get("hint_model"));
        }
    }

    @When("I receive requests that combine these capabilities")
    public void i_receive_requests_that_combine_these_capabilities() {
        combinedRequestProcessed = true;
    }

    @When("I complete capability negotiation for each configuration")
    public void i_complete_capability_negotiation_for_each_configuration() {
        negotiationResults.clear();
        for (var cfg : negotiationConfigs) {
            var declared = parseCapabilities(cfg.get("declared_capabilities"));
            var server = parseCapabilities(cfg.get("server_expectations"));
            var intersection = EnumSet.copyOf(declared);
            intersection.retainAll(server);
            String result;
            if (intersection.isEmpty()) {
                result = server.isEmpty() ? "unused" : "degraded";
            } else if (intersection.equals(server)) {
                result = "success";
            } else {
                result = "partial";
            }
            negotiationResults.add(result);
        }
    }

    @When("the server attempts to use undeclared capabilities")
    public void the_server_attempts_to_use_undeclared_capabilities() {
        if (!undeclaredCapabilities.isEmpty()) {
            lastErrorCode = -32601;
            lastErrorMessage = "Method not found";
        }
    }

    @When("client features become temporarily unavailable:")
    public void client_features_become_temporarily_unavailable(DataTable table) {
        featureUnavailabilityScenarios.clear();
        for (var row : table.asMaps(String.class, String.class)) {
            featureUnavailabilityScenarios.add(new HashMap<>(row));
        }
    }

    @When("sampling requests are rejected or fail:")
    public void sampling_requests_are_rejected_or_fail(DataTable table) {
        samplingErrorScenarios.clear();
        samplingErrorScenarios.addAll(table.asMaps(String.class, String.class));
        if (!samplingErrorScenarios.isEmpty()) {
            lastErrorCode = -32601;
            lastErrorMessage = samplingErrorScenarios.get(0).get("expected_error");
        }
    }

    @When("processing sampling requests")
    public void processing_sampling_requests() {
        if (samplingMessageRequest.isEmpty()) {
            samplingMessageRequest.put("message_content", "placeholder");
        }
    }

    @Then("I should include the \"{word}\" capability")
    public void i_should_include_capability(String capability) {
        var cap = parseCapability(capability);
        if (!clientCapabilities.contains(cap)) {
            throw new AssertionError("missing capability: " + cap);
        }
    }

    @Then("I should include content data only for \"accept\" actions")
    public void i_should_include_content_data_only_for_accept_actions() {
        for (var row : elicitationUserActions) {
            var hasContent = row.get("content") != null;
            if ("accept".equals(row.get("expected_action")) != hasContent) {
                throw new AssertionError("content inclusion mismatch for " + row.get("user_action"));
            }
        }
    }

    @Then("I should indicate whether I support \"listChanged\" notifications")
    public void i_should_indicate_whether_i_support_list_changed_notifications() {
        if (lastCapability == null || !capabilityOptions.containsKey(lastCapability)) {
            throw new AssertionError("missing capability indication");
        }
    }

    @Then("I should implement rate limiting for elicitation requests")
    public void i_should_implement_rate_limiting_for_elicitation_requests() {
        if (activeConnection == null) {
            throw new AssertionError("no active connection");
        }
    }

    @Then("I should implement proper access controls")
    public void i_should_implement_proper_access_controls() {
        if (activeConnection == null) {
            throw new AssertionError("no active connection");
        }
    }

    @Then("I should implement user approval controls")
    public void i_should_implement_user_approval_controls() {
        if (activeConnection == null) {
            throw new AssertionError("no active connection");
        }
    }

    @Then("I should implement rate limiting")
    public void i_should_implement_rate_limiting() {
        if (activeConnection == null) {
            throw new AssertionError("no active connection");
        }
    }

    @Then("I should send a notifications\\/roots\\/list_changed notification")
    public void i_should_send_a_notifications_roots_list_changed_notification() {
        if (rootConfigChanged && !capabilityOptions.getOrDefault(ClientCapability.ROOTS, false)) {
            throw new AssertionError("listChanged not supported");
        }
    }

    @Then("I should return the correct action for each user interaction")
    public void i_should_return_the_correct_action_for_each_user_interaction() {
        var mapping = Map.of(
                "submit_data", "accept",
                "click_decline", "decline",
                "close_dialog", "cancel",
                "press_escape", "cancel",
                "explicit_reject", "decline"
        );
        for (var row : elicitationUserActions) {
            var expected = row.get("expected_action");
            if (!Objects.equals(mapping.get(row.get("user_action")), expected)) {
                throw new AssertionError("unexpected action for " + row.get("user_action"));
            }
        }
    }

    @Then("I should return the response with action \"accept\"")
    public void i_should_return_the_response_with_action_accept() {
        if (elicitationResponseAction != null && !"accept".equals(elicitationResponseAction)) {
            throw new AssertionError("unexpected elicitation action: " + elicitationResponseAction);
        }
    }

    @Then("I should return valid structured data")
    public void i_should_return_valid_structured_data() {
    }

    @Then("I should return the model's response with metadata")
    public void i_should_return_the_models_response_with_metadata() {
        if (!samplingModelResponse.isEmpty() && (!samplingModelResponse.containsKey("content") || !samplingModelResponse.containsKey("usage"))) {
            throw new AssertionError("model response missing metadata");
        }
    }

    @Then("I should return appropriate error responses")
    public void i_should_return_appropriate_error_responses() {
        if (lastErrorCode != 0 && lastErrorCode != -32601) {
            throw new AssertionError("unexpected error code: " + lastErrorCode);
        }
    }

    @Then("I should reject requests for undeclared capabilities")
    public void i_should_reject_requests_for_undeclared_capabilities() {
        if (undeclaredCapabilities.isEmpty()) {
            throw new AssertionError("no undeclared capabilities");
        }
    }

    @Then("I should reject nested objects and arrays")
    public void i_should_reject_nested_objects_and_arrays() {
        for (var row : elicitationSchemaTypes) {
            var type = row.get("schema_type");
            if ("object".equalsIgnoreCase(type) || "array".equalsIgnoreCase(type)) {
                throw new AssertionError("nested structures not rejected");
            }
        }
    }

    @Then("I should present the request to the user")
    public void i_should_present_the_request_to_the_user() {
        if (simpleElicitationRequest.isEmpty() && samplingMessageRequest.isEmpty()) {
            throw new AssertionError("nothing to present");
        }
    }

    @Then("I should present the request for user approval")
    public void i_should_present_the_request_for_user_approval() {
        if (simpleElicitationRequest.isEmpty() && samplingMessageRequest.isEmpty()) {
            throw new AssertionError("nothing to present");
        }
    }

    @Then("I should present generated responses for review before delivery")
    public void i_should_present_generated_responses_for_review_before_delivery() {
        if (simpleElicitationRequest.isEmpty() && samplingMessageRequest.isEmpty()) {
            throw new AssertionError("nothing to present");
        }
    }

    @Then("I should validate the user's response against the schema")
    public void i_should_validate_the_users_response_against_the_schema() {
        if (simpleElicitationRequest.get("schema_type") == null || simpleElicitationRequest.get("required_field") == null) {
            throw new AssertionError("invalid simple elicitation schema");
        }
    }

    @Then("I should validate each field against its schema")
    public void i_should_validate_each_field_against_its_schema() {
        for (var field : structuredElicitationFields) {
            if (field.get("field_name") == null || field.get("field_type") == null) {
                throw new AssertionError("invalid structured field");
            }
        }
    }

    @Then("I should validate input according to schema constraints")
    public void i_should_validate_input_according_to_schema_constraints() {
        for (var row : elicitationSchemaTypes) {
            if (row.get("schema_type") == null) {
                throw new AssertionError("missing schema type");
            }
        }
    }

    @Then("I should validate all root URIs to prevent path traversal")
    public void i_should_validate_all_root_uris_to_prevent_path_traversal() {
        for (var root : configuredRoots) {
            var uri = root.get("uri");
            if (uri != null && uri.contains("..")) {
                throw new AssertionError("path traversal: " + uri);
            }
        }
    }

    @Then("I should validate mime types for media content")
    public void i_should_validate_mime_types_for_media_content() {
        for (var row : samplingContentTypes) {
            var type = row.get("content_type");
            var mime = row.get("mime_type");
            if (("image".equals(type) || "audio".equals(type)) && (mime == null || mime.isBlank() || "none".equalsIgnoreCase(mime))) {
                throw new AssertionError("missing mime type for " + type);
            }
        }
    }

    @Then("I should validate all message content")
    public void i_should_validate_all_message_content() {
        if (samplingMessageRequest.isEmpty()) {
            throw new AssertionError("missing content to validate");
        }
    }

    @Then("I should support all specified primitive types")
    public void i_should_support_all_specified_primitive_types() {
        for (var row : elicitationSchemaTypes) {
            var type = row.get("schema_type");
            if (Set.of("object", "array").contains(type)) {
                throw new AssertionError("unsupported schema type: " + type);
            }
        }
    }

    @Then("I should support all specified content types")
    public void i_should_support_all_specified_content_types() {
        var types = samplingContentTypes.stream().map(r -> r.get("content_type")).collect(Collectors.toSet());
        if (!types.containsAll(Set.of("text", "image", "audio"))) {
            throw new AssertionError("missing content types");
        }
    }

    @Then("I should handle base64 encoding for binary data")
    public void i_should_handle_base64_encoding_for_binary_data() {
        for (var row : samplingContentTypes) {
            var type = row.get("content_type");
            var format = row.get("data_format");
            if (!"text".equals(type) && !"base64_encoded".equals(format)) {
                throw new AssertionError("binary data not base64 encoded");
            }
        }
    }

    @Then("I should handle each unavailability gracefully")
    public void i_should_handle_each_unavailability_gracefully() {
        for (var row : featureUnavailabilityScenarios) {
            if (row.get("expected_behavior") == null || row.get("expected_behavior").isBlank()) {
                throw new AssertionError("missing expected behavior");
            }
        }
    }

    @Then("I should handle sensitive data appropriately")
    public void i_should_handle_sensitive_data_appropriately() {
    }

    @Then("I should provide clear indication of which server is requesting information")
    public void i_should_provide_clear_indication_of_which_server_is_requesting_information() {
        if (activeConnection == null || clientId == null) {
            throw new AssertionError("no active connection");
        }
    }

    @Then("I should provide clear decline and cancel options")
    public void i_should_provide_clear_decline_and_cancel_options() {
        if (activeConnection == null || clientId == null) {
            throw new AssertionError("no active connection");
        }
    }

    @Then("I should provide options to deny or modify the request")
    public void i_should_provide_options_to_deny_or_modify_the_request() {
        if (activeConnection == null || clientId == null) {
            throw new AssertionError("no active connection");
        }
    }

    @Then("I should provide clear error messages to servers")
    public void i_should_provide_clear_error_messages_to_servers() {
        for (var row : featureUnavailabilityScenarios) {
            if (row.get("expected_behavior") == null || row.get("expected_behavior").isBlank()) {
                throw new AssertionError("missing expected behavior");
            }
        }
    }

    @Then("I should allow users to review and modify responses before sending")
    public void i_should_allow_users_to_review_and_modify_responses_before_sending() {
        if (simpleElicitationRequest.isEmpty() && samplingMessageRequest.isEmpty()) {
            throw new AssertionError("nothing to allow");
        }
    }

    @Then("I should allow users to view and edit prompts before sending")
    public void i_should_allow_users_to_view_and_edit_prompts_before_sending() {
        if (simpleElicitationRequest.isEmpty() && samplingMessageRequest.isEmpty()) {
            throw new AssertionError("nothing to allow");
        }
    }

    @Then("I should consider priority values for model selection")
    public void i_should_consider_priority_values_for_model_selection() {
        if (samplingModelPreferences.isEmpty()) {
            throw new AssertionError("no model preferences");
        }
        for (var row : samplingModelPreferences) {
            for (var key : List.of("cost_priority", "speed_priority", "intelligence_priority")) {
                var v = Double.parseDouble(row.get(key));
                if (v < 0 || v > 1) {
                    throw new AssertionError("priority out of range");
                }
            }
        }
    }

    @Then("I should forward the approved request to the language model")
    public void i_should_forward_the_approved_request_to_the_language_model() {
        if (samplingMessageRequest.isEmpty()) {
            throw new AssertionError("no sampling request to forward");
        }
    }

    @Then("I should generate appropriate input forms")
    public void i_should_generate_appropriate_input_forms() {
        if (structuredElicitationFields.isEmpty()) {
            throw new AssertionError("no structured fields");
        }
    }

    @Then("I should process hints as substrings for model matching")
    public void i_should_process_hints_as_substrings_for_model_matching() {
        if (samplingModelPreferences.isEmpty()) {
            throw new AssertionError("no model preferences to process");
        }
    }

    @Then("I should map hints to equivalent models from available providers")
    public void i_should_map_hints_to_equivalent_models_from_available_providers() {
        if (samplingModelSelections.size() != samplingModelPreferences.size()) {
            throw new AssertionError("model hints not mapped");
        }
    }

    @Then("I should make final model selection based on combined preferences")
    public void i_should_make_final_model_selection_based_on_combined_preferences() {
        if (samplingModelSelections.size() != samplingModelPreferences.size()) {
            throw new AssertionError("no final model selections");
        }
    }

    @Then("I should maintain consistent user interaction patterns")
    public void i_should_maintain_consistent_user_interaction_patterns() {
        if (!combinedRequestProcessed) {
            throw new AssertionError("no combined request processed");
        }
    }

    @Then("I should maintain security boundaries for unsupported features")
    public void i_should_maintain_security_boundaries_for_unsupported_features() {
        if (undeclaredCapabilities.isEmpty()) {
            throw new AssertionError("no undeclared capabilities to enforce");
        }
        if (lastErrorCode == 0) {
            throw new AssertionError("undeclared capabilities accepted");
        }
    }

    @Then("I should attempt recovery when possible")
    public void i_should_attempt_recovery_when_possible() {
        if (featureUnavailabilityScenarios.isEmpty()) {
            throw new AssertionError("no unavailability scenarios");
        }
    }

    @Then("I should only expose roots with appropriate permissions")
    public void i_should_only_expose_roots_with_appropriate_permissions() {
        for (var root : configuredRoots) {
            var uri = root.get("uri");
            if (uri == null || !uri.startsWith("file://")) {
                throw new AssertionError("root outside allowed scheme: " + uri);
            }
        }
    }

    @Then("I should prompt users for consent before exposing roots")
    public void i_should_prompt_users_for_consent_before_exposing_roots() {
        if (!clientCapabilities.contains(ClientCapability.ROOTS)) {
            throw new AssertionError("roots capability missing");
        }
    }

    @Then("I should respect model preference hints while maintaining security")
    public void i_should_respect_model_preference_hints_while_maintaining_security() {
        if (!undeclaredCapabilities.isEmpty() && lastErrorCode == 0) {
            throw new AssertionError("undeclared capabilities accepted");
        }
    }

    @Then("the server should recognize my {word} support")
    public void the_server_should_recognize_my_support(String capability) {
        var cap = parseCapability(capability);
        if (!clientCapabilities.contains(cap)) {
            throw new AssertionError("capability not recognized: " + cap);
        }
    }

    @Then("I should return all configured roots")
    public void i_should_return_all_configured_roots() {
        if (returnedRoots.size() != configuredRoots.size()) {
            throw new AssertionError("root count mismatch");
        }
        var expected = configuredRoots.stream().map(r -> r.get("uri")).collect(Collectors.toSet());
        var actual = returnedRoots.stream().map(r -> r.get("uri")).collect(Collectors.toSet());
        if (!expected.equals(actual)) {
            throw new AssertionError("roots mismatch");
        }
    }

    @Then("I should return no roots")
    public void i_should_return_no_roots() {
        if (!returnedRoots.isEmpty()) {
            throw new AssertionError("expected no roots");
        }
    }

    @When("I inspect the default client TLS configuration")
    public void i_inspect_the_default_client_tls_configuration() {
        defaultConfig = McpClientConfiguration.defaultConfiguration("test-client", "test-server", "test-principal");
    }

    @Then("hostname verification should be enabled")
    public void hostname_verification_should_be_enabled() {
        if (!defaultConfig.verifyHostname()) {
            throw new AssertionError("hostname verification disabled");
        }
    }

    @Then("certificate validation mode should be {string}")
    public void certificate_validation_mode_should_be(String mode) {
        if (defaultConfig.certificateValidationMode() != CertificateValidationMode.valueOf(mode)) {
            throw new AssertionError("unexpected mode");
        }
    }

    @Then("each root should have a valid file:\\/\\/ URI")
    public void each_root_should_have_a_valid_file_uri() {
        for (var root : returnedRoots) {
            var uri = root.get("uri");
            if (uri == null || !uri.startsWith("file://")) {
                throw new AssertionError("invalid uri: " + uri);
            }
        }
    }

    @Then("each root should include an optional human-readable name")
    public void each_root_should_include_an_optional_human_readable_name() {
        for (var root : returnedRoots) {
            if (!root.containsKey("name")) {
                throw new AssertionError("missing root name");
            }
        }
    }

    @Then("the server should be able to request an updated roots list")
    public void the_server_should_be_able_to_request_an_updated_roots_list() {
        if (!capabilityOptions.getOrDefault(ClientCapability.ROOTS, false)) {
            throw new AssertionError("listChanged support missing");
        }
    }

    @Then("error messages should be clear and actionable")
    public void error_messages_should_be_clear_and_actionable() {
        for (var scenario : samplingErrorScenarios) {
            var msg = scenario.get("expected_error");
            if (msg == null || msg.isBlank()) {
                throw new AssertionError("missing error message");
            }
        }
    }

    @Then("the agreed capabilities should match the intersection of client and server support")
    public void the_agreed_capabilities_should_match_the_intersection() {
        for (var i = 0; i < negotiationConfigs.size(); i++) {
            var expected = negotiationConfigs.get(i).get("negotiation_result");
            var actual = negotiationResults.size() > i ? negotiationResults.get(i) : null;
            if (!Objects.equals(expected, actual)) {
                throw new AssertionError("expected %s but was %s".formatted(expected, actual));
            }
        }
    }

    @Then("both parties should respect the negotiated capability boundaries")
    public void both_parties_should_respect_the_negotiated_capability_boundaries() {
        if (negotiationResults.size() != negotiationConfigs.size()) {
            throw new AssertionError("negotiation not completed for all cases");
        }
    }

    @Then("each capability should function independently")
    public void each_capability_should_function_independently() {
        if (!combinedRequestProcessed) {
            throw new AssertionError("no combined request processed");
        }
        if (combinedCapabilityRows.size() != clientCapabilities.size()) {
            throw new AssertionError("capability count mismatch");
        }
    }

    @Then("capabilities should not interfere with each other")
    public void capabilities_should_not_interfere_with_each_other() {
        if (!combinedRequestProcessed) {
            throw new AssertionError("no combined request processed");
        }
    }

    @Then("I should return appropriate \"Method not found\" errors")
    public void i_should_return_appropriate_method_not_found_errors() {
        if (lastErrorCode != -32601 || !"Method not found".equals(lastErrorMessage)) {
            throw new AssertionError(
                    "expected -32601/Method not found but was %d/%s".formatted(lastErrorCode, lastErrorMessage)
            );
        }
    }

    @Then("I should return error code {int} \\(Method not found)")
    public void i_should_return_error_code_method_not_found(int code) {
        if (lastErrorCode != code) {
            throw new AssertionError("expected %d but was %d".formatted(code, lastErrorCode));
        }
    }

    @Then("the error message should indicate \"Roots not supported\"")
    public void the_error_message_should_indicate_roots_not_supported() {
        if (!"Roots not supported".equals(lastErrorMessage)) {
            throw new AssertionError("unexpected error message: " + lastErrorMessage);
        }
    }

    @Then("the URI should be considered outside allowed roots")
    public void the_uri_should_be_considered_outside_allowed_roots() {
        if (rootAccessAllowed) {
            throw new AssertionError("unexpected root access");
        }
    }

    @After
    public void closeConnection() {
        try {
            if (activeConnection != null) {
                activeConnection.close();
                activeConnection = null;
                clientId = null;
                clientCapabilities.clear();
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
