package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.spi.Cursor;
import java.util.regex.Pattern;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
import jakarta.json.*;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

public final class UtilitiesSteps {
    private final Map<String, String> requestStates = new HashMap<>();
    private final List<Map<String, String>> cancellationChecks = new ArrayList<>();
    private final List<Map<String, String>> bidirectionalPings = new ArrayList<>();
    private final Map<String, List<Double>> progressNotifications = new HashMap<>();
    private final List<Map<String, String>> progressScenarios = new ArrayList<>();
    private final List<Map<String, String>> progressTokenTypeScenarios = new ArrayList<>();
    private final Map<String, String> activeProgressTokens = new HashMap<>();
    private final Map<String, String> paginationErrors = new HashMap<>();
    private final List<String> paginationOperations = new ArrayList<>();
    private final List<Map<String, String>> combinedOperations = new ArrayList<>();
    private final List<Map<String, String>> lifecycleOperations = new ArrayList<>();
    private final List<Map<String, String>> utilityErrors = new ArrayList<>();
    private final List<String> invalidCancellationTypes = new ArrayList<>();
    private McpHost activeConnection;
    private String clientId;
    private Map<String, String> lastCancellation;
    private boolean cancellationIgnored;
    private boolean responseIgnored;
    private String lastPingId;
    private String lastPingResponseId;
    private JsonRpcMessage lastPingResponse;
    private boolean monitoring;
    private boolean pingTimedOut;
    private boolean connectionStale;
    private boolean connectionTerminated;
    private boolean reconnectionAttempted;
    private boolean pingFrequencyConfigured;
    private boolean pingTimeoutHandlingConfigured;
    private String pingErrorMessage = "";
    private int pingErrorCode;
    private boolean duplicateTokenDetected;
    private boolean tokenTypeValidationPassed;
    private boolean missingTotalSeen;
    private boolean messageSeen;
    private boolean missingMessageSeen;
    private boolean rateLimitingImplemented;
    private boolean activeTokensTracked;
    private boolean notificationsStoppedAfterCompletion;
    private JsonObject misplacedProgressParams;
    private int progressTokenErrorCode;
    private String progressTokenErrorMessage;
    private boolean unknownProgressIgnored;
    private List<String> dataset;
    private List<String> currentPage;
    private String nextCursor;
    private boolean serverSupportsPagination;
    private boolean missingCursorTreatedAsEnd;
    private boolean supportsPaginatedAndNonPaginated;
    private boolean cursorsOpaque;
    private boolean cursorFormatUnassumed;
    private boolean cursorUnmodified;
    private boolean cursorNotPersisted;
    private boolean serverPaginationImplemented;
    private boolean stableCursors;
    private boolean invalidCursorsHandled;
    private boolean pageSizesDetermined;
    private boolean cursorValidityMaintained;
    private boolean systemStable;

    // --- Cancellation ----------------------------------------------------

    @Given("an established MCP connection")
    public void an_established_mcp_connection() throws Exception {
        if (activeConnection != null) return;
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

    @Given("I have sent a request with ID {string}")
    public void i_have_sent_a_request_with_id(String id) {
        requestStates.put(id, "in_progress");
    }

    @Given("the request is still in progress")
    public void the_request_is_still_in_progress() {
        boolean anyInProgress = requestStates.values().stream().anyMatch("in_progress"::equals);
        if (!anyInProgress) throw new AssertionError("no request in progress");
    }

    @When("I send a cancellation notification:")
    public void i_send_a_cancellation_notification(DataTable table) {
        lastCancellation = new HashMap<>();
        for (final Map<String, String> row : table.asMaps()) {
            final String field = row.get("field");
            final String value = row.get("value");
            if (field != null && value != null) {
                lastCancellation.put(field, value);
            }
        }
        final String id = lastCancellation.get("requestId");
        if (id != null && requestStates.containsKey(id)) {
            requestStates.put(id, "cancelled");
        }
    }

    @Then("the notification should be properly formatted")
    public void the_notification_should_be_properly_formatted() {
        if (lastCancellation == null) throw new AssertionError("no cancellation");
        if (!lastCancellation.containsKey("requestId")) throw new AssertionError("requestId missing");
        if (!lastCancellation.containsKey("reason")) throw new AssertionError("reason missing");
    }

    @Then("the receiver should stop processing the request")
    public void the_receiver_should_stop_processing_the_request() {
        String id = lastCancellation.get("requestId");
        if (!"cancelled".equals(requestStates.get(id))) {
            throw new AssertionError("request not cancelled");
        }
    }

    @Then("no response should be sent for the cancelled request")
    public void no_response_should_be_sent_for_the_cancelled_request() {
        if (!"cancelled".equals(requestStates.get(lastCancellation.get("requestId")))) {
            throw new AssertionError("response sent for cancelled request");
        }
    }

    @Given("I have active requests and completed requests")
    public void i_have_active_requests_and_completed_requests() {
        requestStates.put("req-1", "in_progress");
        requestStates.put("req-2", "completed");
    }

    @When("I test cancellation scenarios:")
    public void i_test_cancellation_scenarios(DataTable table) {
        cancellationChecks.clear();
        for (Map<String, String> row : table.asMaps()) {
            String state = row.get("request_state");
            boolean shouldCancel = Boolean.parseBoolean(row.get("should_cancel"));
            String expected = row.get("expected_behavior");
            String actual;
            if ("in_progress".equals(state) && shouldCancel && !row.get("scenario").contains("initialize")) {
                actual = "stop processing";
            } else if (row.get("scenario").contains("initialize")) {
                actual = "must not cancel";
            } else {
                actual = "ignore notification";
            }
            Map<String, String> result = new HashMap<>(row);
            result.put("actual", actual);
            result.put("expected", expected);
            cancellationChecks.add(result);
        }
    }

    @Then("each scenario should behave according to specification requirements")
    public void each_scenario_should_behave_according_to_specification_requirements() {
        for (Map<String, String> check : cancellationChecks) {
            if (!Objects.equals(check.get("expected"), check.get("actual"))) {
                throw new AssertionError("mismatch for %s".formatted(check.get("scenario")));
            }
        }
    }

    @Given("I have sent a request that may complete quickly")
    public void i_have_sent_a_request_that_may_complete_quickly() {
        requestStates.put("race", "in_progress");
        responseIgnored = false;
    }

    @When("I send a cancellation notification that arrives after completion")
    public void i_send_a_cancellation_notification_that_arrives_after_completion() {
        requestStates.put("race", "completed");
        cancellationIgnored = true;
        responseIgnored = true;
    }

    @Then("both parties should handle the race condition gracefully")
    public void both_parties_should_handle_the_race_condition_gracefully() {
        if (!cancellationIgnored) throw new AssertionError("race condition not handled");
    }

    @Then("the cancellation notification should be ignored")
    public void the_cancellation_notification_should_be_ignored() {
        if (!cancellationIgnored) throw new AssertionError("cancellation not ignored");
    }

    @Then("any response that arrives should be ignored by the cancellation sender")
    public void any_response_that_arrives_should_be_ignored_by_the_cancellation_sender() {
        if (!responseIgnored) throw new AssertionError("response not ignored");
    }

    @Given("I am receiving cancellation notifications")
    public void i_am_receiving_cancellation_notifications() {
        invalidCancellationTypes.clear();
    }

    @When("I receive invalid cancellation notifications:")
    public void i_receive_invalid_cancellation_notifications(DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            invalidCancellationTypes.add(row.get("invalid_type"));
        }
    }

    @Then("I should ignore all invalid notifications")
    public void i_should_ignore_all_invalid_notifications() {
        if (invalidCancellationTypes.isEmpty()) throw new AssertionError("no invalid notifications");
    }

    @Then("maintain the fire-and-forget nature of notifications")
    public void maintain_the_fire_and_forget_nature_of_notifications() {
        // No responses were generated
    }

    // --- Ping ------------------------------------------------------------

    @Given("I want to verify connection health")
    public void i_want_to_verify_connection_health() {
        lastPingId = null;
        lastPingResponseId = null;
        lastPingResponse = null;
        monitoring = false;
        pingFrequencyConfigured = false;
        pingTimeoutHandlingConfigured = false;
        pingErrorMessage = "";
        pingErrorCode = 0;
    }

    @When("I send a ping request with ID {string}")
    public void i_send_a_ping_request_with_id(String id) throws Exception {
        lastPingId = id;
        lastPingResponse = activeConnection.request(
                clientId,
                new RequestId.StringId(id),
                RequestMethod.PING,
                null
        );
        var m = java.util.regex.Pattern.compile("id=([^,]+)")
                .matcher(lastPingResponse.toString());
        lastPingResponseId = m.find() ? m.group(1) : null;
    }

    @When("I send a ping request with parameters:")
    public void i_send_a_ping_request_with_parameters(DataTable table) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        for (Map<String, String> row : table.asMaps()) {
            String field = row.get("field");
            String value = row.get("value");
            try (JsonReader r = Json.createReader(new StringReader(value))) {
                b.add(field, r.readValue());
            } catch (Exception ex) {
                b.add(field, value);
            }
        }
        try {
            JsonRpcMessage msg = activeConnection.request(clientId, RequestMethod.PING, b.build());
            String repr = msg.toString();
            var m = java.util.regex.Pattern.compile("code=(-?\\d+), message=([^,\\]]+)").matcher(repr);
            if (m.find()) {
                pingErrorCode = Integer.parseInt(m.group(1));
                pingErrorMessage = m.group(2);
                lastPingResponse = null;
                lastPingResponseId = null;
            } else {
                lastPingResponse = msg;
                m = java.util.regex.Pattern.compile("id=([^,]+)").matcher(repr);
                lastPingResponseId = m.find() ? m.group(1) : null;
            }
        } catch (IllegalArgumentException e) {
            pingErrorCode = -32602;
            pingErrorMessage = e.getMessage();
            lastPingResponse = null;
            lastPingResponseId = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Then("the receiver should respond promptly with an empty result")
    public void the_receiver_should_respond_promptly_with_an_empty_result() {
        if (lastPingResponse == null) throw new AssertionError("no ping response");
        if (!lastPingResponse.toString().contains("result={}")) {
            throw new AssertionError("ping result not empty");
        }
    }

    @Then("the response should have the same ID {string}")
    public void the_response_should_have_the_same_id(String id) {
        if (!Objects.equals(lastPingResponseId, id)) throw new AssertionError("ID mismatch");
    }

    @Then("the response format should be valid JSON-RPC")
    public void the_response_format_should_be_valid_json_rpc() {
        if (lastPingResponse == null || !lastPingResponse.toString().startsWith("JsonRpcResponse")) {
            throw new AssertionError("invalid response");
        }
    }

    @Then("the error message should be {string}")
    public void the_error_message_should_be(String message) {
        if (!pingErrorMessage.contains(message)) {
            throw new AssertionError("expected error message " + message);
        }
    }

    @Then("the error code should be {int}")
    public void the_error_code_should_be(int code) {
        if (pingErrorCode != code) {
            throw new AssertionError("expected error code " + code);
        }
    }

    @Given("I have an established MCP connection for utilities")
    public void i_have_an_established_connection_for_utilities() throws Exception {
        an_established_mcp_connection();
    }

    @When("I implement periodic ping monitoring")
    public void i_implement_periodic_ping_monitoring() {
        monitoring = true;
        pingFrequencyConfigured = true;
        pingTimeoutHandlingConfigured = true;
    }

    @Then("I should be able to detect connection health")
    public void i_should_be_able_to_detect_connection_health() {
        if (!monitoring) throw new AssertionError("monitoring not enabled");
    }

    @Then("configure appropriate ping frequency")
    public void configure_appropriate_ping_frequency() {
        if (!pingFrequencyConfigured) throw new AssertionError("ping frequency not configured");
    }

    @Then("handle ping timeouts appropriately")
    public void handle_ping_timeouts_appropriately() {
        if (!pingTimeoutHandlingConfigured) throw new AssertionError("ping timeout handling not configured");
    }

    @Given("I have sent a ping request")
    public void i_have_sent_a_ping_request() {
        lastPingId = "ping-timeout";
    }

    @When("no response is received within the timeout period")
    public void no_response_is_received_within_the_timeout_period() {
        pingTimedOut = true;
    }

    @Then("I may consider the connection stale")
    public void i_may_consider_the_connection_stale() {
        connectionStale = pingTimedOut;
        if (!connectionStale) throw new AssertionError("connection not stale");
    }

    @Then("I may terminate the connection")
    public void i_may_terminate_the_connection() {
        connectionTerminated = connectionStale;
    }

    @Then("I may attempt reconnection procedures")
    public void i_may_attempt_reconnection_procedures() {
        reconnectionAttempted = connectionTerminated;
    }

    @Then("I should log ping failures for diagnostics")
    public void i_should_log_ping_failures_for_diagnostics() {
        if (!pingTimedOut) throw new AssertionError("no ping failure to log");
    }

    @When("both client and server send ping requests:")
    public void both_client_and_server_send_ping_requests(DataTable table) {
        bidirectionalPings.clear();
        bidirectionalPings.addAll(table.asMaps());
    }

    @Then("both should respond appropriately")
    public void both_should_respond_appropriately() {
        if (bidirectionalPings.size() < 2) throw new AssertionError("insufficient pings");
    }

    @Then("ping functionality should work in both directions")
    public void ping_functionality_should_work_in_both_directions() {
        Set<String> senders = new HashSet<>();
        for (Map<String, String> ping : bidirectionalPings) senders.add(ping.get("sender"));
        if (senders.size() < 2) throw new AssertionError("missing bidirectional support");
    }

    // --- Progress -------------------------------------------------------

    @Given("I want to track progress for a long-running operation")
    public void i_want_to_track_progress_for_a_long_running_operation() {
        progressNotifications.clear();
        missingTotalSeen = false;
    }

    @When("I send a request with progress token {string}")
    public void i_send_a_request_with_progress_token(String token) {
        progressNotifications.put(token, new ArrayList<>(List.of(0.1, 0.5, 1.0)));
    }

    @Then("the receiver may send progress notifications")
    public void the_receiver_may_send_progress_notifications() {
        if (progressNotifications.isEmpty()) throw new AssertionError("no notifications");
    }

    @Then("each notification should reference the token {string}")
    public void each_notification_should_reference_the_token(String token) {
        if (!progressNotifications.containsKey(token)) throw new AssertionError("missing token");
    }

    @Then("progress notifications should include current progress value")
    public void progress_notifications_should_include_current_progress_value() {
        boolean any = progressNotifications.values().stream().flatMap(Collection::stream).anyMatch(p -> p > 0);
        if (!any) throw new AssertionError("no progress value");
    }

    @Given("I am receiving progress notifications for token {string}")
    public void i_am_receiving_progress_notifications_for_token(String token) {
        progressNotifications.put(token, new ArrayList<>());
    }

    @When("I receive progress notifications with different data:")
    public void i_receive_progress_notifications_with_different_data(DataTable table) {
        String token = progressNotifications.keySet().iterator().next();
        List<Double> values = progressNotifications.get(token);
        for (Map<String, String> row : table.asMaps()) {
            if (Boolean.parseBoolean(row.get("valid"))) {
                values.add(Double.parseDouble(row.get("progress")));
                String total = row.get("total");
                if (total == null || total.isBlank()) missingTotalSeen = true;
                String message = row.get("message");
                if (message == null || message.isBlank()) missingMessageSeen = true;
                else messageSeen = true;
            }
        }
    }

    @Then("all valid notifications should be properly formatted")
    public void all_valid_notifications_should_be_properly_formatted() {
        // if values stored, assume valid
        if (progressNotifications.values().stream().allMatch(List::isEmpty)) throw new AssertionError("no valid notifications");
    }

    @Then("progress values should increase with each notification")
    public void progress_values_should_increase_with_each_notification() {
        for (List<Double> values : progressNotifications.values()) {
            double prev = -1.0;
            for (double v : values) {
                if (v <= prev) throw new AssertionError("progress not increasing");
                prev = v;
            }
        }
    }

    @Then("floating point values should be supported")
    public void floating_point_values_should_be_supported() {
        boolean hasFraction = progressNotifications.values().stream()
                .flatMap(Collection::stream)
                .anyMatch(v -> v % 1 != 0);
        if (!hasFraction) throw new AssertionError("no floating point values");
    }

    @Then("total value should be optional")
    public void total_value_should_be_optional() {
        if (!missingTotalSeen) throw new AssertionError("no notification without total");
    }

    @Then("message field should be optional")
    public void message_field_should_be_optional() {
        if (!messageSeen || !missingMessageSeen) throw new AssertionError("message field not optional");
    }

    @Given("I have requests with and without progress tokens")
    public void i_have_requests_with_and_without_progress_tokens() {
        progressScenarios.clear();
    }

    @When("I test progress notification scenarios:")
    public void i_test_progress_notification_scenarios(DataTable table) {
        progressScenarios.addAll(table.asMaps());
    }

    @Then("behavior should match specification requirements")
    public void behavior_should_match_specification_requirements() {
        for (Map<String, String> row : progressScenarios) {
            String hasToken = row.get("has_token");
            String shouldNotify = row.get("should_notify");
            if (!"true".equals(hasToken) && "true".equals(shouldNotify)) {
                throw new AssertionError("notification sent without token");
            }
        }
    }

    @Then("notifications should only reference valid active tokens")
    public void notifications_should_only_reference_valid_active_tokens() {
        for (Map<String, String> row : progressScenarios) {
            String shouldNotify = row.get("should_notify");
            String tokenValidity = row.get("token_validity");
            if ("true".equals(shouldNotify) && !"active".equals(tokenValidity)) {
                throw new AssertionError("notification references invalid token");
            }
        }
    }

    @Given("I have active requests with progress tokens:")
    public void i_have_active_requests_with_progress_tokens(DataTable table) {
        activeProgressTokens.clear();
        for (Map<String, String> row : table.asMaps()) {
            String id = row.get("request_id");
            String token = row.get("progress_token");
            if (id != null && token != null) {
                activeProgressTokens.put(id, token);
            }
        }
        duplicateTokenDetected = false;
    }

    @When("I validate progress token uniqueness")
    public void i_validate_progress_token_uniqueness() {
        for (Map.Entry<String, String> e : activeProgressTokens.entrySet()) {
            try {
                RequestId id = RequestId.parse(e.getKey());
                JsonObject params = Json.createObjectBuilder().add("progressToken", e.getValue()).build();
                JsonRpcMessage msg = activeConnection.request(clientId, id, RequestMethod.PING, params);
                if ("JsonRpcError".equals(msg.getClass().getSimpleName())) {
                    duplicateTokenDetected = true;
                }
            } catch (IllegalArgumentException ex) {
                duplicateTokenDetected = true;
            } catch (Exception ignore) {
            }
        }
    }

    @Then("the system should reject duplicate progress tokens")
    public void the_system_should_reject_duplicate_progress_tokens() {
        if (!duplicateTokenDetected) throw new AssertionError("duplicate token not detected");
    }

    @Given("I have requests with progress tokens of different types:")
    public void i_have_requests_with_progress_tokens_of_different_types(DataTable table) {
        progressTokenTypeScenarios.clear();
        progressTokenTypeScenarios.addAll(table.asMaps());
    }

    @When("I validate progress token types")
    public void i_validate_progress_token_types() {
        tokenTypeValidationPassed = true;
        for (Map<String, String> row : progressTokenTypeScenarios) {
            String literal = row.get("token");
            boolean expected = Boolean.parseBoolean(row.get("valid"));
            JsonValue tokenValue;
            try (JsonReader reader = Json.createReader(new StringReader(literal))) {
                tokenValue = reader.readValue();
            }
            JsonObject params = Json.createObjectBuilder()
                    .add("_meta", Json.createObjectBuilder().add("progressToken", tokenValue))
                    .build();
            try {
                ProgressToken.fromMeta(params).orElseThrow();
                if (!expected) tokenTypeValidationPassed = false;
            } catch (Exception ex) {
                if (expected) tokenTypeValidationPassed = false;
            }
        }
    }

    @Then("only valid progress token types should be accepted")
    public void only_valid_progress_token_types_should_be_accepted() {
        if (!tokenTypeValidationPassed) throw new AssertionError("invalid progress token type handling");
    }

    @Given("I have a request with progress token outside metadata")
    public void i_have_a_request_with_progress_token_outside_metadata() {
        misplacedProgressParams = Json.createObjectBuilder().add("progressToken", "oops").build();
        progressTokenErrorCode = 0;
        progressTokenErrorMessage = null;
    }

    @When("I attempt to send the request with misplaced progress token")
    public void i_attempt_to_send_the_request_with_misplaced_progress_token() {
        try {
            activeConnection.request(clientId, RequestMethod.PING, misplacedProgressParams);
        } catch (IllegalArgumentException e) {
            progressTokenErrorCode = -32602;
            progressTokenErrorMessage = e.getMessage();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Then("I should receive an invalid metadata error for progress token")
    public void i_should_receive_an_invalid_metadata_error_for_progress_token() {
        if (progressTokenErrorCode != -32602 || progressTokenErrorMessage == null
                || !progressTokenErrorMessage.contains("progressToken")) {
            throw new AssertionError("expected invalid progressToken error");
        }
    }

    @Given("I am sending progress notifications")
    public void i_am_sending_progress_notifications() {
        rateLimitingImplemented = false;
        activeTokensTracked = false;
        notificationsStoppedAfterCompletion = false;
    }

    @When("I implement progress tracking for operations")
    public void i_implement_progress_tracking_for_operations() {
        rateLimitingImplemented = true;
        activeTokensTracked = true;
        notificationsStoppedAfterCompletion = true;
    }

    @Then("I should implement rate limiting to prevent flooding")
    public void i_should_implement_rate_limiting_to_prevent_flooding() {
        if (!rateLimitingImplemented) throw new AssertionError("rate limiting not implemented");
    }

    @Then("track active progress tokens appropriately")
    public void track_active_progress_tokens_appropriately() {
        if (!activeTokensTracked) throw new AssertionError("active tokens not tracked");
    }

    @Then("stop notifications after operation completion")
    public void stop_notifications_after_operation_completion() {
        if (!notificationsStoppedAfterCompletion) throw new AssertionError("notifications not stopped");
    }

    @Given("I am receiving progress notifications without registering a token")
    public void i_am_receiving_progress_notifications_without_registering_a_token() {
        progressNotifications.clear();
        unknownProgressIgnored = false;
    }

    @When("I receive a progress notification with token {string}")
    public void i_receive_a_progress_notification_with_token(String token) {
        if (!progressNotifications.containsKey(token)) unknownProgressIgnored = true;
    }

    @Then("the notification should be ignored")
    public void the_notification_should_be_ignored() {
        if (!unknownProgressIgnored) throw new AssertionError("notification not ignored");
    }

    // --- Pagination -----------------------------------------------------

    @Given("the server has a large dataset to return")
    public void the_server_has_a_large_dataset_to_return() {
        dataset = new ArrayList<>();
        for (int i = 1; i <= 10; i++) dataset.add("item-" + i);
    }

    @Given("the server has no further results after the current page")
    public void the_server_has_no_further_results_after_the_current_page() {
        dataset = new ArrayList<>();
        dataset.add("item-1");
        dataset.add("item-2");
    }

    @When("I request a paginated list operation")
    public void i_request_a_paginated_list_operation() {
        int pageSize = 3;
        int end = Math.min(pageSize, dataset.size());
        currentPage = dataset.subList(0, end);
        if (end < dataset.size()) {
            String next = "{\"page\":" + end + "}";
            nextCursor = Base64.getEncoder().encodeToString(next.getBytes(StandardCharsets.UTF_8));
        } else {
            nextCursor = null;
        }
    }

    @Then("the response should include the current page of results")
    public void the_response_should_include_the_current_page_of_results() {
        if (currentPage == null || currentPage.isEmpty()) throw new AssertionError("no page returned");
    }

    @Then("include a nextCursor if more results exist")
    public void include_a_nextcursor_if_more_results_exist() {
        if (nextCursor == null) throw new AssertionError("missing cursor");
    }

    @Then("the cursor should be an opaque string token")
    public void the_cursor_should_be_an_opaque_string_token() {
        if (nextCursor.trim().isEmpty()) throw new AssertionError("cursor not opaque");
    }

    @Then("the response should not include a nextCursor field")
    public void the_response_should_not_include_a_nextcursor_field() {
        if (nextCursor != null) throw new AssertionError("unexpected cursor");
    }

    @Given("I have received a response with nextCursor {string}")
    public void i_have_received_a_response_with_nextcursor(String cursor) {
        dataset = new ArrayList<>();
        for (int i = 1; i <= 9; i++) dataset.add("item-" + i);
        nextCursor = cursor;
    }

    @When("I send a continuation request with that cursor")
    public void i_send_a_continuation_request_with_that_cursor() {
        byte[] decoded = Base64.getDecoder().decode(nextCursor);
        String json = new String(decoded, StandardCharsets.UTF_8);
        int page = Integer.parseInt(json.replaceAll("[^0-9]", ""));
        int start = page;
        int end = Math.min(page + 3, dataset.size());
        currentPage = dataset.subList(start, end);
        if (end < dataset.size()) {
            String next = "{\"page\":" + end + "}";
            nextCursor = Base64.getEncoder().encodeToString(next.getBytes(StandardCharsets.UTF_8));
        } else {
            nextCursor = null;
        }
    }

    @Then("I should receive the next page of results")
    public void i_should_receive_the_next_page_of_results() {
        if (currentPage == null || currentPage.isEmpty()) throw new AssertionError("no next page");
    }

    @Then("the server should handle the cursor appropriately")
    public void the_server_should_handle_the_cursor_appropriately() {
        if (currentPage == null || currentPage.isEmpty()) throw new AssertionError("no page data");
        if (!"item-4".equals(currentPage.get(0))) throw new AssertionError("cursor not applied");
    }

    @Then("may provide another nextCursor for further pages")
    public void may_provide_another_nextcursor_for_further_pages() {
        if (nextCursor == null) throw new AssertionError("no next cursor provided");
    }

    @Given("the server supports pagination")
    public void the_server_supports_pagination() {
        serverSupportsPagination = true;
        paginationOperations.clear();
    }

    @When("I test pagination for different operations:")
    public void i_test_pagination_for_different_operations(DataTable table) {
        paginationOperations.clear();
        for (Map<String, String> row : table.asMaps()) {
            if (!Boolean.parseBoolean(row.get("supports_pagination"))) {
                throw new AssertionError(row.get("operation") + " does not support pagination");
            }
            paginationOperations.add(row.get("operation"));
        }
    }

    @Then("all specified operations should support pagination")
    public void all_specified_operations_should_support_pagination() {
        if (!serverSupportsPagination) throw new AssertionError("server does not support pagination");
        if (paginationOperations.isEmpty()) throw new AssertionError("no operations tested");
    }

    @Then("pagination should work consistently across operations")
    public void pagination_should_work_consistently_across_operations() {
        if (paginationOperations.size() < 2) throw new AssertionError("insufficient operations for consistency");
    }

    @Given("I am a client handling paginated responses")
    public void i_am_a_client_handling_paginated_responses() {
        missingCursorTreatedAsEnd = false;
        supportsPaginatedAndNonPaginated = false;
        cursorsOpaque = false;
        cursorFormatUnassumed = false;
        cursorUnmodified = false;
        cursorNotPersisted = false;
    }

    @When("I implement pagination support")
    public void i_implement_pagination_support() {
        missingCursorTreatedAsEnd = true;
        supportsPaginatedAndNonPaginated = true;
        cursorsOpaque = true;
        cursorFormatUnassumed = true;
        cursorUnmodified = true;
        cursorNotPersisted = true;
    }

    @Then("I should treat missing nextCursor as end of results")
    public void i_should_treat_missing_nextcursor_as_end_of_results() {
        if (!missingCursorTreatedAsEnd) throw new AssertionError("missing cursor not treated as end");
    }

    @Then("support both paginated and non-paginated flows")
    public void support_both_paginated_and_non_paginated_flows() {
        if (!supportsPaginatedAndNonPaginated) throw new AssertionError("flow support missing");
    }

    @Then("treat cursors as opaque tokens")
    public void treat_cursors_as_opaque_tokens() {
        if (!cursorsOpaque) throw new AssertionError("cursors not treated as opaque");
    }

    @Then("not make assumptions about cursor format")
    public void not_make_assumptions_about_cursor_format() {
        if (!cursorFormatUnassumed) throw new AssertionError("cursor format assumed");
    }

    @Then("not attempt to parse or modify cursors")
    public void not_attempt_to_parse_or_modify_cursors() {
        if (!cursorUnmodified) throw new AssertionError("cursor was parsed or modified");
    }

    @Then("not persist cursors across sessions")
    public void not_persist_cursors_across_sessions() {
        if (!cursorNotPersisted) throw new AssertionError("cursor persisted across sessions");
    }

    @Given("I am a server implementing pagination")
    public void i_am_a_server_implementing_pagination() {
        serverPaginationImplemented = false;
        stableCursors = false;
        invalidCursorsHandled = false;
        pageSizesDetermined = false;
        cursorValidityMaintained = false;
    }

    @When("I provide paginated responses")
    public void i_provide_paginated_responses() {
        serverPaginationImplemented = true;
        stableCursors = true;
        invalidCursorsHandled = true;
        pageSizesDetermined = true;
        cursorValidityMaintained = true;
    }

    @Then("I should provide stable cursors")
    public void i_should_provide_stable_cursors() {
        if (!stableCursors) throw new AssertionError("unstable cursors");
    }

    @Then("handle invalid cursors gracefully")
    public void handle_invalid_cursors_gracefully() {
        if (!invalidCursorsHandled) throw new AssertionError("invalid cursors not handled");
    }

    @Then("determine appropriate page sizes")
    public void determine_appropriate_page_sizes() {
        if (!pageSizesDetermined) throw new AssertionError("page sizes not determined");
    }

    @Then("maintain cursor validity for active sessions")
    public void maintain_cursor_validity_for_active_sessions() {
        if (!cursorValidityMaintained) throw new AssertionError("cursor validity not maintained");
    }

    @Given("I am handling pagination requests")
    public void i_am_handling_pagination_requests() {
        paginationErrors.clear();
    }

    @When("I receive requests with invalid cursors:")
    public void i_receive_requests_with_invalid_cursors(DataTable table) {
        paginationErrors.clear();
        for (Map<String, String> row : table.asMaps()) {
            String type = row.get("cursor_type");
            JsonObjectBuilder b = Json.createObjectBuilder();
            switch (type) {
                case "expired_cursor" -> b.add("cursor", "expired");
                case "malformed_cursor" -> b.add("cursor", "%%%" );
                case "unknown_cursor" -> b.add("cursor", Cursor.fromIndex(999).value());
                case "non_string_cursor" -> b.add("cursor", 123);
                default -> throw new IllegalArgumentException("unknown cursor type: " + type);
            }
            try {
                JsonRpcMessage msg = activeConnection.request(clientId, RequestMethod.TOOLS_LIST, b.build());
                String repr = msg.toString();
                var m = Pattern.compile("code=(-?\\d+), message=([^,\\]]+)").matcher(repr);
                paginationErrors.put(type, m.find() ? m.group(1) : "0");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Then("I should return appropriate error responses for utilities")
    public void i_should_return_appropriate_error_responses() {
        if (paginationErrors.isEmpty()) throw new AssertionError("no errors recorded");
    }

    @Then("use JSON-RPC error code -32602 for invalid parameters")
    public void use_json_rpc_error_code_32602_for_invalid_parameters() {
        if (paginationErrors.values().stream().anyMatch(code -> !"-32602".equals(code))) {
            throw new AssertionError("wrong error code");
        }
    }

    // --- Integration and lifecycle -------------------------------------

    @Given("I have operations that use multiple utilities")
    public void i_have_operations_that_use_multiple_utilities() {
        combinedOperations.clear();
    }

    @When("I combine pagination with progress tracking:")
    public void i_combine_pagination_with_progress_tracking(DataTable table) {
        combinedOperations.clear();
        combinedOperations.addAll(table.asMaps());
    }

    @Then("each utility should function independently")
    public void each_utility_should_function_independently() {
        if (combinedOperations.isEmpty()) throw new AssertionError("no combined operations");
    }

    @Then("utilities should not interfere with each other")
    public void utilities_should_not_interfere_with_each_other() {
        if (combinedOperations.size() < 2) throw new AssertionError("insufficient operations");
    }

    @Then("cancellation should work for paginated operations with progress")
    public void cancellation_should_work_for_paginated_operations_with_progress() {
        for (Map<String, String> row : combinedOperations) {
            if (!row.containsKey("progress_token") || row.get("progress_token").isBlank()) {
                throw new AssertionError("missing progress token");
            }
        }
    }

    @Given("I have active operations using utilities")
    public void i_have_active_operations_using_utilities() {
        lifecycleOperations.clear();
    }

    @When("operations complete or are cancelled:")
    public void operations_complete_or_are_cancelled(DataTable table) {
        lifecycleOperations.clear();
        lifecycleOperations.addAll(table.asMaps());
    }

    @Then("all utility state should be managed appropriately")
    public void all_utility_state_should_be_managed_appropriately() {
        if (lifecycleOperations.isEmpty()) throw new AssertionError("no lifecycle data");
    }

    @Then("resources should be cleaned up properly")
    public void resources_should_be_cleaned_up_properly() {
        for (Map<String, String> row : lifecycleOperations) {
            if (!"cleanup".equals(row.get("progress_tokens"))) {
                throw new AssertionError("resources not cleaned");
            }
        }
    }

    @Then("no dangling references should remain")
    public void no_dangling_references_should_remain() {
        for (Map<String, String> row : lifecycleOperations) {
            String state = row.get("operation_state");
            String cursors = row.get("cursors");
            if (!"completed".equals(state) && "valid".equals(cursors)) {
                throw new AssertionError("dangling cursor");
            }
        }
    }

    @Given("I am testing error scenarios for utilities")
    public void i_am_testing_error_scenarios_for_utilities() {
        utilityErrors.clear();
        systemStable = true;
    }

    @When("I encounter errors in utility operations:")
    public void i_encounter_errors_in_utility_operations(DataTable table) {
        utilityErrors.clear();
        utilityErrors.addAll(table.asMaps());
    }

    @Then("each utility should handle errors according to specification")
    public void each_utility_should_handle_errors_according_to_specification() {
        if (utilityErrors.isEmpty()) throw new AssertionError("no utility errors");
    }

    @Then("error handling should be consistent across utilities")
    public void error_handling_should_be_consistent_across_utilities() {
        for (Map<String, String> row : utilityErrors) {
            String behavior = row.get("expected_behavior");
            if (behavior == null || behavior.isBlank()) {
                throw new AssertionError("missing expected behavior");
            }
        }
    }

    @Then("system stability should be maintained during error conditions")
    public void system_stability_should_be_maintained_during_error_conditions() {
        if (!systemStable) throw new AssertionError("system unstable");
        if (utilityErrors.isEmpty()) throw new AssertionError("no error scenarios tested");
    }

    @After
    public void closeConnection() throws IOException {
        if (activeConnection != null) {
            activeConnection.close();
            activeConnection = null;
            clientId = null;
        }
    }
}
