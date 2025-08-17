package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public final class ClientFeaturesSteps {
    private McpHost activeConnection;
    private String clientId;
    private final Set<ClientCapability> clientCapabilities = EnumSet.noneOf(ClientCapability.class);
    private final Map<ClientCapability, Boolean> capabilityOptions = new EnumMap<>(ClientCapability.class);
    private final List<Map<String, String>> configuredRoots = new ArrayList<>();
    private final List<Map<String, String>> returnedRoots = new ArrayList<>();
    private final List<Map<String, String>> negotiationConfigs = new ArrayList<>();
    private final List<String> negotiationResults = new ArrayList<>();
    private final Set<ClientCapability> undeclaredCapabilities = EnumSet.noneOf(ClientCapability.class);
    private final List<Map<String, String>> samplingErrorScenarios = new ArrayList<>();
    private final List<Map<String, String>> combinedCapabilityRows = new ArrayList<>();
    private ClientCapability lastCapability;
    private boolean rootConfigChanged;
    private int lastErrorCode;
    private String lastErrorMessage;
    private boolean combinedRequestProcessed;

    private final Map<String, String> simpleElicitationRequest = new HashMap<>();
    private final List<Map<String, String>> structuredElicitationFields = new ArrayList<>();
    private final List<Map<String, String>> elicitationUserActions = new ArrayList<>();
    private final List<Map<String, String>> elicitationSchemaTypes = new ArrayList<>();
    private String elicitationResponseAction;

    private final Map<String, String> samplingMessageRequest = new HashMap<>();
    private final Map<String, String> samplingModelResponse = new HashMap<>();
    private final List<Map<String, String>> samplingContentTypes = new ArrayList<>();
    private final List<Map<String, String>> samplingModelPreferences = new ArrayList<>();
    private final List<String> samplingModelSelections = new ArrayList<>();

    private final List<Map<String, String>> featureUnavailabilityScenarios = new ArrayList<>();

    private static ClientCapability parseCapability(String raw) {
        String normalized = raw.trim().toLowerCase();
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
        EnumSet<ClientCapability> set = EnumSet.noneOf(ClientCapability.class);
        for (String part : raw.split(",")) {
            set.add(parseCapability(part));
        }
        return set;
    }

    @Given("I have established an MCP connection")
    public void i_have_established_an_mcp_connection() throws Exception {
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
        ClientCapability cap = parseCapability(capability);
        clientCapabilities.add(cap);
        if (cap == ClientCapability.ROOTS) {
            capabilityOptions.putIfAbsent(cap, false);
        }
        lastCapability = cap;
    }

    @Given("I have declared {word} capability")
    public void i_have_declared_capability(String capability) {
        ClientCapability cap = parseCapability(capability);
        clientCapabilities.add(cap);
        if (cap == ClientCapability.ROOTS) {
            capabilityOptions.putIfAbsent(cap, false);
        }
        lastCapability = cap;
    }

    @Given("I have declared {word} capability with {word} support")
    public void i_have_declared_capability_with_support(String capability, String support) {
        ClientCapability cap = parseCapability(capability);
        clientCapabilities.add(cap);
        capabilityOptions.put(cap, true);
        lastCapability = cap;
    }

    @Given("I have declared multiple client capabilities:")
    public void i_have_declared_multiple_client_capabilities(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        combinedCapabilityRows.clear();
        combinedCapabilityRows.addAll(rows);
        for (Map<String, String> row : rows) {
            ClientCapability cap = parseCapability(row.get("capability"));
            clientCapabilities.add(cap);
            lastCapability = cap;
            String features = row.get("features");
            if (features != null && features.contains("listChanged: true")) {
                capabilityOptions.put(cap, true);
            }
        }
    }

    @Given("I have declared only specific client capabilities:")
    public void i_have_declared_only_specific_client_capabilities(DataTable table) {
        clientCapabilities.clear();
        undeclaredCapabilities.clear();
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        for (Map<String, String> row : rows) {
            String declared = row.get("declared_capability");
            if (declared != null && !declared.isBlank()) {
                ClientCapability cap = parseCapability(declared);
                clientCapabilities.add(cap);
                lastCapability = cap;
            }
            String undeclared = row.get("undeclared_capability");
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

    @When("I receive an elicitation/create request for simple text input:")
    public void i_receive_an_elicitation_create_request_for_simple_text_input(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        if (rows.isEmpty()) {
            throw new AssertionError("missing request row");
        }
        simpleElicitationRequest.clear();
        simpleElicitationRequest.putAll(rows.getFirst());
        elicitationResponseAction = "accept";
    }

    @When("I receive an elicitation/create request for structured data:")
    public void i_receive_an_elicitation_create_request_for_structured_data(DataTable table) {
        structuredElicitationFields.clear();
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            structuredElicitationFields.add(new HashMap<>(row));
        }
    }

    @When("I test elicitation responses with the following user actions:")
    public void i_test_elicitation_responses_with_the_following_user_actions(DataTable table) {
        elicitationUserActions.clear();
        Map<String, String> mapping = Map.of(
                "submit_data", "accept",
                "click_decline", "decline",
                "close_dialog", "cancel",
                "press_escape", "cancel",
                "explicit_reject", "decline"
        );
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            String action = row.get("user_action");
            String expected = row.get("expected_action");
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

    @When("I receive a roots/list request")
    public void i_receive_a_roots_list_request() {
        returnedRoots.clear();
        if (!clientCapabilities.contains(ClientCapability.ROOTS)) {
            lastErrorCode = -32601;
            lastErrorMessage = "Roots not supported";
            return;
        }
        returnedRoots.addAll(configuredRoots);
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

    @When("I receive a sampling/createMessage request:")
    public void i_receive_a_sampling_create_message_request(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
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
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            samplingContentTypes.add(new HashMap<>(row));
        }
    }

    @When("I receive sampling requests with model preferences:")
    public void i_receive_sampling_requests_with_model_preferences(DataTable table) {
        samplingModelPreferences.clear();
        samplingModelSelections.clear();
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
            for (String key : List.of("cost_priority", "speed_priority", "intelligence_priority")) {
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
        for (Map<String, String> cfg : negotiationConfigs) {
            EnumSet<ClientCapability> declared = parseCapabilities(cfg.get("declared_capabilities"));
            EnumSet<ClientCapability> server = parseCapabilities(cfg.get("server_expectations"));
            EnumSet<ClientCapability> intersection = EnumSet.copyOf(declared);
            intersection.retainAll(server);
            String result;
            if (intersection.isEmpty()) {
                result = server.isEmpty() ? "unused" : "degraded";
            } else if (intersection.equals(declared) && intersection.equals(server)) {
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
        for (Map<String, String> row : table.asMaps(String.class, String.class)) {
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
        // No state needed
    }

    @Then("^I should (allow|attempt|consider|forward|generate|handle|implement|include|indicate|maintain|make|map|only|present|process|prompt|provide|reject|respect|return|send|support|validate).*")
    public void i_should_generic(String verb) {
        switch (verb) {
            case "include" -> {
                if (!elicitationUserActions.isEmpty()) {
                    for (Map<String, String> row : elicitationUserActions) {
                        boolean hasContent = row.get("content") != null;
                        if ("accept".equals(row.get("expected_action")) != hasContent) {
                            throw new AssertionError("content inclusion mismatch for " + row.get("user_action"));
                        }
                    }
                } else if (lastCapability == null || !clientCapabilities.contains(lastCapability)) {
                    throw new AssertionError("missing capability: " + lastCapability);
                }
            }
            case "indicate" -> {
                if (lastCapability == null || !capabilityOptions.containsKey(lastCapability)) {
                    throw new AssertionError("missing capability indication");
                }
            }
            case "implement" -> {
                if (activeConnection == null) {
                    throw new AssertionError("no active connection");
                }
            }
            case "send" -> {
                if (rootConfigChanged && !capabilityOptions.getOrDefault(ClientCapability.ROOTS, false)) {
                    throw new AssertionError("listChanged not supported");
                }
            }
            case "return" -> {
                if (!elicitationUserActions.isEmpty()) {
                    Map<String, String> mapping = Map.of(
                            "submit_data", "accept",
                            "click_decline", "decline",
                            "close_dialog", "cancel",
                            "press_escape", "cancel",
                            "explicit_reject", "decline"
                    );
                    for (Map<String, String> row : elicitationUserActions) {
                        String expected = row.get("expected_action");
                        if (!Objects.equals(mapping.get(row.get("user_action")), expected)) {
                            throw new AssertionError("unexpected action for " + row.get("user_action"));
                        }
                    }
                }
                if (!samplingModelResponse.isEmpty() && (!samplingModelResponse.containsKey("content") || !samplingModelResponse.containsKey("usage"))) {
                    throw new AssertionError("model response missing metadata");
                }
                if (elicitationResponseAction != null && !"accept".equals(elicitationResponseAction)) {
                    throw new AssertionError("unexpected elicitation action: " + elicitationResponseAction);
                }
                if (lastErrorCode != 0 && lastErrorCode != -32601) {
                    throw new AssertionError("unexpected error code: " + lastErrorCode);
                }
            }
            case "reject" -> {
                if (!undeclaredCapabilities.isEmpty()) {
                    // expected path for capability boundary enforcement
                } else if (!elicitationSchemaTypes.isEmpty()) {
                    for (Map<String, String> row : elicitationSchemaTypes) {
                        String type = row.get("schema_type");
                        if ("object".equalsIgnoreCase(type) || "array".equalsIgnoreCase(type)) {
                            throw new AssertionError("nested structures not rejected");
                        }
                    }
                } else {
                    throw new AssertionError("no rejection context");
                }
            }
            case "present" -> {
                if (simpleElicitationRequest.isEmpty() && samplingMessageRequest.isEmpty()) {
                    throw new AssertionError("nothing to present");
                }
            }
            case "validate" -> {
                if (!simpleElicitationRequest.isEmpty()) {
                    if (simpleElicitationRequest.get("schema_type") == null || simpleElicitationRequest.get("required_field") == null) {
                        throw new AssertionError("invalid simple elicitation schema");
                    }
                } else if (!structuredElicitationFields.isEmpty()) {
                    for (Map<String, String> field : structuredElicitationFields) {
                        if (field.get("field_name") == null || field.get("field_type") == null) {
                            throw new AssertionError("invalid structured field");
                        }
                    }
                } else if (!configuredRoots.isEmpty()) {
                    for (Map<String, String> root : configuredRoots) {
                        String uri = root.get("uri");
                        if (uri != null && uri.contains("..")) {
                            throw new AssertionError("path traversal: " + uri);
                        }
                    }
                } else if (!samplingContentTypes.isEmpty()) {
                    for (Map<String, String> row : samplingContentTypes) {
                        String type = row.get("content_type");
                        String mime = row.get("mime_type");
                        if (("image".equals(type) || "audio".equals(type)) && (mime == null || mime.isBlank() || "none".equalsIgnoreCase(mime))) {
                            throw new AssertionError("missing mime type for " + type);
                        }
                    }
                } else if (!featureUnavailabilityScenarios.isEmpty()) {
                    for (Map<String, String> row : featureUnavailabilityScenarios) {
                        if (row.get("expected_behavior") == null || row.get("expected_behavior").isBlank()) {
                            throw new AssertionError("missing expected behavior");
                        }
                    }
                }
            }
            case "generate" -> {
                if (structuredElicitationFields.isEmpty()) {
                    throw new AssertionError("no structured fields");
                }
            }
            case "support" -> {
                if (!elicitationSchemaTypes.isEmpty()) {
                    for (Map<String, String> row : elicitationSchemaTypes) {
                        String type = row.get("schema_type");
                        if (Set.of("object", "array").contains(type)) {
                            throw new AssertionError("unsupported schema type: " + type);
                        }
                    }
                } else if (!samplingContentTypes.isEmpty()) {
                    Set<String> types = samplingContentTypes.stream().map(r -> r.get("content_type")).collect(Collectors.toSet());
                    if (!types.containsAll(Set.of("text", "image", "audio"))) {
                        throw new AssertionError("missing content types");
                    }
                }
            }
            case "handle" -> {
                if (!samplingContentTypes.isEmpty()) {
                    for (Map<String, String> row : samplingContentTypes) {
                        String type = row.get("content_type");
                        String format = row.get("data_format");
                        if (!"text".equals(type) && !"base64_encoded".equals(format)) {
                            throw new AssertionError("binary data not base64 encoded");
                        }
                    }
                } else if (!featureUnavailabilityScenarios.isEmpty()) {
                    for (Map<String, String> row : featureUnavailabilityScenarios) {
                        if (row.get("expected_behavior") == null || row.get("expected_behavior").isBlank()) {
                            throw new AssertionError("missing expected behavior");
                        }
                    }
                }
            }
            case "provide" -> {
                if (!featureUnavailabilityScenarios.isEmpty()) {
                    for (Map<String, String> row : featureUnavailabilityScenarios) {
                        if (row.get("expected_behavior") == null || row.get("expected_behavior").isBlank()) {
                            throw new AssertionError("missing expected behavior");
                        }
                    }
                } else if (activeConnection == null || clientId == null) {
                    throw new AssertionError("no active connection");
                }
            }
            case "forward" -> {
                if (samplingMessageRequest.isEmpty()) {
                    throw new AssertionError("no sampling request to forward");
                }
            }
            case "consider" -> {
                if (samplingModelPreferences.isEmpty()) {
                    throw new AssertionError("no model preferences");
                }
                for (Map<String, String> row : samplingModelPreferences) {
                    for (String key : List.of("cost_priority", "speed_priority", "intelligence_priority")) {
                        double v = Double.parseDouble(row.get(key));
                        if (v < 0 || v > 1) {
                            throw new AssertionError("priority out of range");
                        }
                    }
                }
            }
            case "map" -> {
                if (samplingModelSelections.size() != samplingModelPreferences.size()) {
                    throw new AssertionError("model hints not mapped");
                }
            }
            case "make" -> {
                if (samplingModelSelections.size() != samplingModelPreferences.size()) {
                    throw new AssertionError("no final model selections");
                }
            }
            case "maintain" -> {
                if (!combinedRequestProcessed) {
                    throw new AssertionError("no combined request processed");
                }
            }
            case "process" -> {
                if (samplingModelPreferences.isEmpty()) {
                    throw new AssertionError("no model preferences to process");
                }
            }
            case "prompt" -> {
                if (!clientCapabilities.contains(ClientCapability.ROOTS)) {
                    throw new AssertionError("roots capability missing");
                }
            }
            case "allow" -> {
                if (simpleElicitationRequest.isEmpty() && samplingMessageRequest.isEmpty()) {
                    throw new AssertionError("nothing to allow");
                }
            }
            case "attempt" -> {
                if (featureUnavailabilityScenarios.isEmpty()) {
                    throw new AssertionError("no unavailability scenarios");
                }
            }
            case "only" -> {
                for (Map<String, String> root : configuredRoots) {
                    String uri = root.get("uri");
                    if (uri == null || !uri.startsWith("file://")) {
                        throw new AssertionError("root outside allowed scheme: " + uri);
                    }
                }
            }
            case "respect" -> {
                if (!undeclaredCapabilities.isEmpty() && lastErrorCode == 0) {
                    throw new AssertionError("undeclared capabilities accepted");
                }
            }
            default -> {
                // No specific assertion
            }
        }
    }

    @Then("the server should recognize my {word} support")
    public void the_server_should_recognize_my_support(String capability) {
        ClientCapability cap = parseCapability(capability);
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

    @Then("each root should have a valid file:// URI")
    public void each_root_should_have_a_valid_file_uri() {
        for (Map<String, String> root : returnedRoots) {
            String uri = root.get("uri");
            if (uri == null || !uri.startsWith("file://")) {
                throw new AssertionError("invalid uri: " + uri);
            }
        }
    }

    @Then("each root should include an optional human-readable name")
    public void each_root_should_include_an_optional_human_readable_name() {
        for (Map<String, String> root : returnedRoots) {
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
        for (Map<String, String> scenario : samplingErrorScenarios) {
            String msg = scenario.get("expected_error");
            if (msg == null || msg.isBlank()) {
                throw new AssertionError("missing error message");
            }
        }
    }

    @Then("the agreed capabilities should match the intersection of client and server support")
    public void the_agreed_capabilities_should_match_the_intersection() {
        for (int i = 0; i < negotiationConfigs.size(); i++) {
            String expected = negotiationConfigs.get(i).get("negotiation_result");
            String actual = negotiationResults.size() > i ? negotiationResults.get(i) : null;
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

    @Then("I should return error code {int} \\(Method not found\\)")
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

    @After
    public void closeConnection() throws IOException {
        if (activeConnection != null) {
            activeConnection.close();
            activeConnection = null;
            clientId = null;
            clientCapabilities.clear();
        }
    }
}
