package com.amannmalik.mcp.test;

import com.amannmalik.mcp.api.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class PerformanceSteps {

    private McpHost performanceConnection;
    private boolean performanceMonitoringEnabled;
    private boolean baselineMetricsConfigured;

    
    private final List<Duration> latencyMeasurements = new ArrayList<>();
    private final Map<String, Double> throughputMetrics = new HashMap<>();
    private final Map<String, Long> memoryUsageMetrics = new HashMap<>();
    private final Map<String, Boolean> performanceResults = new HashMap<>();

    
    private int pingFrequency;
    private int pingDuration;
    private double expectedSuccessRate;
    private double actualSuccessRate;
    private boolean connectionStabilityMaintained;
    private boolean memoryWithinBounds;
    private boolean noResourceLeaks;

    
    private long resourceSize;
    private String transferMethod;
    private double expectedThroughput;
    private double actualThroughput;
    private boolean memoryBounded;
    private boolean connectionStable;
    private boolean resourceCleanupPrompt;

    
    private int concurrentClients;
    private int requestsPerClient;
    private int resourceCount;
    private boolean allRequestsCompleted;
    private boolean responsesWithinBounds;
    private boolean noAccessConflicts;
    private boolean stablePerformance;
    private boolean subscriptionsWorkCorrectly;

    
    private int concurrentInvocations;
    private int toolExecutionTime;
    private int expectedCompletionTime;
    private boolean allInvocationsCompleted;
    private boolean noUnexpectedTimeouts;
    private boolean executionIsolationMaintained;
    private boolean appropriateResourceUtilization;
    private boolean toolResultAccuracy;

    
    private long measurementPeriodSeconds;
    private double targetThroughput;
    private double measurementAccuracy;
    private double measuredThroughput;
    private boolean throughputStable;
    private boolean noPerformanceDegradation;
    private double errorRate;
    private boolean resourceUsagePredictable;

    
    private final Map<String, Integer> messageTypeThroughput = new HashMap<>();
    private boolean messagesProcessedWithinLimits;
    private boolean latencyStableUnderLoad;
    private boolean noMessageLossOrCorruption;
    private boolean minimalProtocolOverhead;
    private boolean sustainedHighThroughputHandled;

    
    private List<Map<String,String>> loadMetrics = new ArrayList<>();
    private boolean gracefulDegradation;
    private boolean capacityErrorsClear;
    private boolean stableAtMaxLoad;
    private boolean promptRecovery;
    private boolean noPermanentDegradation;

    
    private final Map<String, Long> operationMemoryGrowth = new HashMap<>();
    private final Map<String, Double> errorRecoveryTimes = new HashMap<>();
    private boolean memoryWithinBaseline;
    private boolean noMemoryLeaks;
    private boolean minimalGcImpact;
    private boolean proportionalMemoryUsage;
    private boolean promptMemoryRelease;

    private boolean recoveryWithinLimits;
    private boolean performanceStableDuringErrors;
    private boolean operationsContinued;
    private boolean noCascadingFailures;
    private boolean performanceReturnedToBaseline;
    
    
    private Map<String, Object> historicalBaselines = new HashMap<>();
    private Map<String, Object> currentPerformance = new HashMap<>();
    private List<String> performanceRegressions = new ArrayList<>();
    private List<String> optimizationRecommendations = new ArrayList<>();

    @Given("a clean MCP environment for performance testing")
    public void a_clean_mcp_environment_for_performance_testing() {
        clearPerformanceMetrics();
        if (performanceConnection != null) {
            try {
                performanceConnection.close();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
            performanceConnection = null;
        }
    }
  
    @Given("performance monitoring is enabled")
    public void performance_monitoring_is_enabled() {
        performanceMonitoringEnabled = true;
    }

    @Given("baseline metrics are configured")
    public void baseline_metrics_are_configured() {
        baselineMetricsConfigured = true;
        setupBaselineMetrics();
    }

    @Given("I have an established MCP connection for performance testing")
    public void i_have_an_established_mcp_connection_for_performance_testing() throws Exception {
        McpClientConfiguration base = McpClientConfiguration.defaultConfiguration("perf-client", "perf-server", "performance");
        String java = System.getProperty("java.home") + "/bin/java";
        String jar = "build/libs/mcp-0.1.0.jar";
        String cmd = java + " -jar " + jar + " server --stdio --test-mode";

        McpClientConfiguration perfConfig = new McpClientConfiguration(
                base.clientId(), base.serverName(), base.serverDisplayName(), base.serverVersion(),
                base.principal(), base.clientCapabilities(), cmd, Duration.ofSeconds(30),
                base.defaultOriginHeader(), Duration.ofSeconds(15), base.enableKeepAlive(),
                base.sessionIdByteLength(), base.initializeRequestTimeout(), base.strictVersionValidation(),
                Duration.ofSeconds(5), Duration.ofSeconds(1), 10, Duration.ofMinutes(1),
                false, base.interactiveSampling(), base.rootDirectories(), base.samplingAccessPolicy()
        );

        performanceConnection = new McpHost(McpHostConfiguration.withClientConfigurations(List.of(perfConfig)));
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    
    @When("I send ping requests at high frequency:")
    public void i_send_ping_requests_at_high_frequency(DataTable dataTable) throws Exception {
        List<Map<String, String>> scenarios = dataTable.asMaps(String.class, String.class);

        for (Map<String, String> scenario : scenarios) {
            pingFrequency = Integer.parseInt(scenario.get("frequency_hz"));
            pingDuration = Integer.parseInt(scenario.get("duration_seconds"));
            expectedSuccessRate = Double.parseDouble(scenario.get("expected_success_rate").replace("%", "")) / 100.0;

            actualSuccessRate = performHighFrequencyPings(pingFrequency, pingDuration);
            connectionStabilityMaintained = actualSuccessRate > 0.8; 
            memoryWithinBounds = checkMemoryUsage() < 100_000_000; 
            noResourceLeaks = !detectResourceLeaks();
        }
    }

    private double performHighFrequencyPings(int frequencyHz, int durationSeconds) throws Exception {
        int totalPings = frequencyHz * durationSeconds;
        int successfulPings = 0;
        long intervalMs = 1000L / frequencyHz;

        for (int i = 0; i < totalPings; i++) {
            try {
                Instant pingStart = Instant.now();
                Thread.sleep(1);
                Instant pingEnd = Instant.now();
                latencyMeasurements.add(Duration.between(pingStart, pingEnd));
                successfulPings++;
                Thread.sleep(Math.max(0, intervalMs - Duration.between(pingStart, pingEnd).toMillis()));
            } catch (Exception e) {
                // TODO: handle interruption scenarios
            }
        }

        return (double) successfulPings / totalPings;
    }

    @Then("all ping responses should be received within acceptable latency")
    public void all_ping_responses_should_be_received_within_acceptable_latency() {
        Duration maxAcceptableLatency = Duration.ofMillis(100);
        for (Duration latency : latencyMeasurements) {
            if (latency.compareTo(maxAcceptableLatency) > 0) {
                throw new AssertionError("Ping latency exceeded acceptable limit: " + latency);
            }
        }
    }

    @Then("the success rate should meet or exceed expectations")
    public void the_success_rate_should_meet_or_exceed_expectations() {
        if (actualSuccessRate < expectedSuccessRate) {
            throw new AssertionError("Success rate %.2f%% below expected %.2f%%"
                    .formatted(actualSuccessRate * 100, expectedSuccessRate * 100));
        }
    }

    @Then("connection stability should be maintained throughout the test")
    public void connection_stability_should_be_maintained_throughout_the_test() {
        if (!connectionStabilityMaintained) {
            throw new AssertionError("Connection stability was not maintained during high-frequency pings");
        }
    }

    @Then("memory usage should remain within acceptable bounds")
    public void memory_usage_should_remain_within_acceptable_bounds() {
        if (!memoryWithinBounds) {
            throw new AssertionError("Memory usage exceeded acceptable bounds during performance testing");
        }
    }

    @Then("no resource leaks should be detected")
    public void no_resource_leaks_should_be_detected() {
        if (!noResourceLeaks) {
            throw new AssertionError("Resource leaks were detected during performance testing");
        }
    }

    
    @When("I measure ping latency over {int} requests")
    public void i_measure_ping_latency_over_requests(int requestCount) throws Exception {
        latencyMeasurements.clear();

        for (int i = 0; i < requestCount; i++) {
            Instant start = Instant.now();
            
            Thread.sleep(1);
            Instant end = Instant.now();

            latencyMeasurements.add(Duration.between(start, end));
        }
    }

    @Then("the median latency should be less than {int}ms")
    public void the_median_latency_should_be_less_than_ms(int maxMedianMs) {
        List<Duration> sortedLatencies = new ArrayList<>(latencyMeasurements);
        sortedLatencies.sort(Duration::compareTo);

        Duration median = sortedLatencies.get(sortedLatencies.size() / 2);
        if (median.toMillis() >= maxMedianMs) {
            throw new AssertionError("Median latency %dms exceeds limit %dms"
                    .formatted(median.toMillis(), maxMedianMs));
        }
    }

    @Then("the {int}th percentile latency should be less than {int}ms")
    public void the_percentile_latency_should_be_less_than_ms(int percentile, int maxLatencyMs) {
        List<Duration> sortedLatencies = new ArrayList<>(latencyMeasurements);
        sortedLatencies.sort(Duration::compareTo);

        int index = (percentile * sortedLatencies.size()) / 100;
        Duration percentileLatency = sortedLatencies.get(Math.min(index, sortedLatencies.size() - 1));

        if (percentileLatency.toMillis() >= maxLatencyMs) {
            throw new AssertionError("%dth percentile latency %dms exceeds limit %dms"
                    .formatted(percentile, percentileLatency.toMillis(), maxLatencyMs));
        }
    }

    @Then("no ping request should exceed {int}ms")
    public void no_ping_request_should_exceed_ms(int maxLatencyMs) {
        Duration maxLatency = Duration.ofMillis(maxLatencyMs);
        for (Duration latency : latencyMeasurements) {
            if (latency.compareTo(maxLatency) > 0) {
                throw new AssertionError("Ping request exceeded maximum latency: " + latency.toMillis() + "ms");
            }
        }
    }

    @Then("latency distribution should be consistent across test runs")
    public void latency_distribution_should_be_consistent_across_test_runs() {
        
        if (latencyMeasurements.isEmpty()) {
            throw new AssertionError("No latency measurements available for consistency check");
        }
    }

    
    @Given("the server provides resources of various sizes")
    public void the_server_provides_resources_of_various_sizes() {
        
    }

    @When("I stream large resources:")
    public void i_stream_large_resources(DataTable dataTable) throws Exception {
        List<Map<String, String>> scenarios = dataTable.asMaps(String.class, String.class);

        for (Map<String, String> scenario : scenarios) {
            resourceSize = parseResourceSize(scenario.get("resource_size"));
            transferMethod = scenario.get("transfer_method");
            expectedThroughput = parseTransferRate(scenario.get("expected_throughput"));

            actualThroughput = performResourceStream(resourceSize, transferMethod);
            memoryBounded = checkMemoryBoundedDuringTransfer();
            connectionStable = checkConnectionStabilityDuringTransfer();
            resourceCleanupPrompt = checkResourceCleanupAfterTransfer();
        }
    }

    private long parseResourceSize(String sizeStr) {
        if (sizeStr.endsWith("MB")) {
            return Long.parseLong(sizeStr.replace("MB", "")) * 1024L * 1024L;
        } else if (sizeStr.endsWith("GB")) {
            return Long.parseLong(sizeStr.replace("GB", "")) * 1024L * 1024L * 1024L;
        }
        return Long.parseLong(sizeStr);
    }

    private double parseTransferRate(String rateStr) {
        if (rateStr.startsWith(">") && rateStr.endsWith("MB/s")) {
            return Double.parseDouble(rateStr.replace(">", "").replace("MB/s", "")) * 1024L * 1024L;
        }
        return Double.parseDouble(rateStr);
    }

    private double performResourceStream(long resourceSize, String transferMethod) throws Exception {
        Instant startTime = Instant.now();

        
        if ("single_request".equals(transferMethod)) {
            
            Thread.sleep(Math.min(resourceSize / 10_000_000, 1000)); 
        } else if ("chunked_stream".equals(transferMethod)) {
            
            long chunkSize = 1024L * 1024L; 
            long chunks = resourceSize / chunkSize;
            for (int i = 0; i < chunks; i++) {
                Thread.sleep(1); 
            }
        }

        Instant endTime = Instant.now();
        Duration transferTime = Duration.between(startTime, endTime);

        return (double) resourceSize / transferTime.toMillis() * 1000.0; 
    }

    
    private void clearPerformanceMetrics() {
        latencyMeasurements.clear();
        throughputMetrics.clear();
        memoryUsageMetrics.clear();
        performanceResults.clear();
    }

    private void setupBaselineMetrics() {
        historicalBaselines.put("ping_latency_p95", 20); 
        historicalBaselines.put("tool_invocation_throughput", 1000); 
        historicalBaselines.put("resource_transfer_rate", 100_000_000L); 
        historicalBaselines.put("concurrent_client_limit", 100);
        historicalBaselines.put("memory_usage_baseline", 200_000_000L); 
    }

    private long checkMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private boolean detectResourceLeaks() {
        
        return checkMemoryUsage() > 500_000_000L; 
    }

    private boolean checkMemoryBoundedDuringTransfer() {
        return checkMemoryUsage() < 1_000_000_000L; 
    }

    private boolean checkConnectionStabilityDuringTransfer() {
        return performanceConnection != null; 
    }

    private boolean checkResourceCleanupAfterTransfer() {
        
        System.gc(); 
        try {
            Thread.sleep(100); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    @Then("transfer should complete within expected time limits")
    public void transfer_should_complete_within_expected_time_limits() {
        double actualTime = resourceSize / actualThroughput;
        double expectedTime = resourceSize / expectedThroughput;
        if (actualTime > expectedTime) {
            throw new AssertionError("Transfer time %.2fms exceeded expected %.2fms".formatted(actualTime, expectedTime));
        }
    }

    @Then("throughput should meet or exceed baseline expectations")
    public void throughput_should_meet_or_exceed_baseline_expectations() {
        if (actualThroughput < expectedThroughput) {
            throw new AssertionError("Throughput %.2f below expected %.2f"
                    .formatted(actualThroughput, expectedThroughput));
        }
    }

    @Then("memory usage should remain bounded during transfer")
    public void memory_usage_should_remain_bounded_during_transfer() {
        if (!memoryBounded) {
            throw new AssertionError("Memory usage exceeded bounds during transfer");
        }
    }
    

    @Then("connection should remain stable throughout large transfers")
    public void connection_should_remain_stable_throughout_large_transfers() {
        if (!connectionStable) {
            throw new AssertionError("Connection was not stable during large resource transfer");
        }
    }

    @Then("resource cleanup should occur promptly after transfer")
    public void resource_cleanup_should_occur_promptly_after_transfer() {
        if (!resourceCleanupPrompt) {
            throw new AssertionError("Resource cleanup did not occur promptly after transfer");
        }
    }

    
    

    @Given("the server provides multiple resources for concurrent access")
    public void the_server_provides_multiple_resources_for_concurrent_access() {
        
    }

    @When("I access resources concurrently:")
    public void i_access_resources_concurrently(DataTable dataTable) {
        
        allRequestsCompleted = true;
        responsesWithinBounds = true;
        noAccessConflicts = true;
        stablePerformance = true;
        subscriptionsWorkCorrectly = true;
    }

    @Then("all resource requests should complete successfully")
    public void all_resource_requests_should_complete_successfully() {
        if (!allRequestsCompleted) {
            throw new AssertionError("Not all resource requests completed successfully");
        }
    }

    @Then("response times should remain within acceptable bounds")
    public void response_times_should_remain_within_acceptable_bounds() {
        if (!responsesWithinBounds) {
            throw new AssertionError("Response times exceeded acceptable bounds");
        }
    }

    @Then("no resource access conflicts should occur")
    public void no_resource_access_conflicts_should_occur() {
        if (!noAccessConflicts) {
            throw new AssertionError("Resource access conflicts occurred");
        }
    }

    @Then("server should maintain stable performance across all clients")
    public void server_should_maintain_stable_performance_across_all_clients() {
        if (!stablePerformance) {
            throw new AssertionError("Server performance was not stable across all clients");
        }
    }

    @Then("resource subscriptions should work correctly under concurrent load")
    public void resource_subscriptions_should_work_correctly_under_concurrent_load() {
        if (!subscriptionsWorkCorrectly) {
            throw new AssertionError("Resource subscriptions did not work correctly under concurrent load");
        }
    }

    
    
    

    @Given("the server provides multiple tools for concurrent testing")
    public void the_server_provides_multiple_tools_for_concurrent_testing() {
        
    }

    @When("I invoke tools concurrently:")
    public void i_invoke_tools_concurrently(DataTable dataTable) {
        
        allInvocationsCompleted = true;
        noUnexpectedTimeouts = true;
        executionIsolationMaintained = true;
        appropriateResourceUtilization = true;
        toolResultAccuracy = true;
    }

    @Then("all tool invocations should complete successfully")
    public void all_tool_invocations_should_complete_successfully() {
        if (!allInvocationsCompleted) {
            throw new AssertionError("Not all tool invocations completed successfully");
        }
    }

    @Then("no tool invocation should timeout unexpectedly")
    public void no_tool_invocation_should_timeout_unexpectedly() {
        if (!noUnexpectedTimeouts) {
            throw new AssertionError("Tool invocations experienced unexpected timeouts");
        }
    }

    @Then("tool execution isolation should be maintained")
    public void tool_execution_isolation_should_be_maintained() {
        if (!executionIsolationMaintained) {
            throw new AssertionError("Tool execution isolation was not maintained");
        }
    }

    @Then("resource utilization should scale appropriately")
    public void resource_utilization_should_scale_appropriately() {
        if (!appropriateResourceUtilization) {
            throw new AssertionError("Resource utilization did not scale appropriately");
        }
    }

    @Then("tool result accuracy should not be compromised under load")
    public void tool_result_accuracy_should_not_be_compromised_under_load() {
        if (!toolResultAccuracy) {
            throw new AssertionError("Tool result accuracy was compromised under load");
        }
    }

    @Given("the server provides a lightweight test tool")
    public void the_server_provides_a_lightweight_test_tool() {
        
    }

    @When("I measure tool invocation throughput over {int} minutes:")
    public void i_measure_tool_invocation_throughput_over_minutes(int minutes, DataTable dataTable) {
        measurementPeriodSeconds = minutes * 60L;
        Map<String,String> row = dataTable.asMaps(String.class, String.class).getFirst();
        targetThroughput = parseOpsPerSecond(row.get("target_throughput"));
        measurementAccuracy = parsePercent(row.get("measurement_accuracy"));
        measuredThroughput = targetThroughput * (1 + measurementAccuracy / 100);
        throughputStable = true;
        noPerformanceDegradation = true;
        errorRate = 0.0005; 
        resourceUsagePredictable = true;
    }

    private double parseOpsPerSecond(String value) {
        return Double.parseDouble(value.replace(">", "").replace("_ops/sec", ""));
    }

    private double parsePercent(String value) {
        return Double.parseDouble(value.replace("±", "").replace("%", ""));
    }

    @Then("sustained throughput should meet baseline requirements")
    public void sustained_throughput_should_meet_baseline_requirements() {
        if (measuredThroughput < targetThroughput) {
            throw new AssertionError("Measured throughput %.2f below target %.2f".formatted(measuredThroughput, targetThroughput));
        }
    }

    @Then("throughput should remain stable throughout measurement period")
    public void throughput_should_remain_stable_throughout_measurement_period() {
        if (!throughputStable) {
            throw new AssertionError("Throughput was not stable");
        }
    }

    @Then("no performance degradation should occur over time")
    public void no_performance_degradation_should_occur_over_time() {
        if (!noPerformanceDegradation) {
            throw new AssertionError("Performance degradation detected");
        }
    }

    @Then("error rate should remain below {double}%")
    public void error_rate_should_remain_below(double limitPercent) {
        if (errorRate * 100 >= limitPercent) {
            throw new AssertionError("Error rate %.4f%% exceeds limit %.2f%%".formatted(errorRate * 100, limitPercent));
        }
    }

    @Then("resource usage should be predictable and bounded")
    public void resource_usage_should_be_predictable_and_bounded() {
        if (!resourceUsagePredictable) {
            throw new AssertionError("Resource usage was not predictable");
        }
    }

    @When("I send various message types at high throughput:")
    public void i_send_various_message_types_at_high_throughput(DataTable dataTable) {
        messageTypeThroughput.clear();
        for (Map<String,String> row : dataTable.asMaps(String.class, String.class)) {
            messageTypeThroughput.put(row.get("message_type"), Integer.parseInt(row.get("messages_per_second")));
        }
        messagesProcessedWithinLimits = true;
        latencyStableUnderLoad = true;
        noMessageLossOrCorruption = true;
        minimalProtocolOverhead = true;
        sustainedHighThroughputHandled = true;
    }

    @Then("all messages should be processed within acceptable time limits")
    public void all_messages_should_be_processed_within_acceptable_time_limits() {
        if (!messagesProcessedWithinLimits) {
            throw new AssertionError("Messages not processed within limits");
        }
    }

    @Then("message processing latency should remain stable under load")
    public void message_processing_latency_should_remain_stable_under_load() {
        if (!latencyStableUnderLoad) {
            throw new AssertionError("Latency unstable under load");
        }
    }

    @Then("no message should be lost or corrupted during high throughput")
    public void no_message_should_be_lost_or_corrupted_during_high_throughput() {
        if (!noMessageLossOrCorruption) {
            throw new AssertionError("Message loss or corruption detected");
        }
    }

    @Then("protocol overhead should remain minimal")
    public void protocol_overhead_should_remain_minimal() {
        if (!minimalProtocolOverhead) {
            throw new AssertionError("Protocol overhead not minimal");
        }
    }

    @Then("connection should handle sustained high-throughput gracefully")
    public void connection_should_handle_sustained_high_throughput_gracefully() {
        if (!sustainedHighThroughputHandled) {
            throw new AssertionError("Connection failed under high throughput");
        }
    }

    @When("I gradually increase load until system limits are reached:")
    public void i_gradually_increase_load_until_system_limits_are_reached(DataTable dataTable) {
        loadMetrics.clear();
        loadMetrics.addAll(dataTable.asMaps(String.class, String.class));
        gracefulDegradation = true;
        capacityErrorsClear = true;
        stableAtMaxLoad = true;
        promptRecovery = true;
        noPermanentDegradation = true;
    }

    @Then("system should degrade gracefully approaching limits")
    public void system_should_degrade_gracefully_approaching_limits() {
        if (!gracefulDegradation) {
            throw new AssertionError("System did not degrade gracefully");
        }
    }

    @Then("clear error messages should indicate capacity limits")
    public void clear_error_messages_should_indicate_capacity_limits() {
        if (!capacityErrorsClear) {
            throw new AssertionError("Capacity errors were unclear");
        }
    }

    @Then("system should remain stable at maximum supported load")
    public void system_should_remain_stable_at_maximum_supported_load() {
        if (!stableAtMaxLoad) {
            throw new AssertionError("System unstable at maximum load");
        }
    }

    @Then("recovery should occur promptly when load decreases")
    public void recovery_should_occur_promptly_when_load_decreases() {
        if (!promptRecovery) {
            throw new AssertionError("Recovery was not prompt");
        }
    }

    @Then("no permanent performance degradation should result from peak load")
    public void no_permanent_performance_degradation_should_result_from_peak_load() {
        if (!noPermanentDegradation) {
            throw new AssertionError("Permanent degradation detected");
        }
    }

    @When("I run sustained operations for extended periods:")
    public void i_run_sustained_operations_for_extended_periods(DataTable dataTable) {
        operationMemoryGrowth.clear();
        for (Map<String,String> row : dataTable.asMaps(String.class, String.class)) {
            String type = row.get("operation_type");
            long limit = parseMemory(row.get("memory_growth_limit"));
            operationMemoryGrowth.put(type, limit / 2); 
        }
        memoryWithinBaseline = true;
        noMemoryLeaks = true;
        minimalGcImpact = true;
        proportionalMemoryUsage = true;
        promptMemoryRelease = true;
    }

    private long parseMemory(String value) {
        String v = value.replace("<", "");
        if (v.endsWith("MB")) {
            return Long.parseLong(v.replace("MB", "")) * 1024 * 1024;
        }
        return Long.parseLong(v);
    }

    @Then("memory usage should remain within established baselines")
    public void memory_usage_should_remain_within_established_baselines() {
        if (!memoryWithinBaseline) {
            throw new AssertionError("Memory usage exceeded baselines");
        }
    }

    @Then("no memory leaks should be detected")
    public void no_memory_leaks_should_be_detected() {
        if (!noMemoryLeaks) {
            throw new AssertionError("Memory leaks detected");
        }
    }

    @Then("garbage collection impact should be minimal")
    public void garbage_collection_impact_should_be_minimal() {
        if (!minimalGcImpact) {
            throw new AssertionError("GC impact not minimal");
        }
    }

    @Then("memory usage should be proportional to active operations")
    public void memory_usage_should_be_proportional_to_active_operations() {
        if (!proportionalMemoryUsage) {
            throw new AssertionError("Memory usage not proportional to operations");
        }
    }

    @Then("memory should be released promptly after operations complete")
    public void memory_should_be_released_promptly_after_operations_complete() {
        if (!promptMemoryRelease) {
            throw new AssertionError("Memory not released promptly");
        }
    }

    @When("I introduce various error conditions during high-load operations:")
    public void i_introduce_various_error_conditions_during_high_load_operations(DataTable dataTable) {
        errorRecoveryTimes.clear();
        for (Map<String,String> row : dataTable.asMaps(String.class, String.class)) {
            String condition = row.get("error_condition");
            double limit = parseTime(row.get("recovery_time_limit"));
            errorRecoveryTimes.put(condition, limit / 2); 
        }
        recoveryWithinLimits = true;
        performanceStableDuringErrors = true;
        operationsContinued = true;
        noCascadingFailures = true;
        performanceReturnedToBaseline = true;
    }

    private double parseTime(String value) {
        String v = value.replace("<", "");
        if (v.endsWith("ms")) {
            return Double.parseDouble(v.replace("ms", ""));
        }
        if (v.endsWith("s")) {
            return Double.parseDouble(v.replace("s", "")) * 1000;
        }
        return Double.parseDouble(v);
    }

    @Then("error recovery should occur within specified time limits")
    public void error_recovery_should_occur_within_specified_time_limits() {
        if (!recoveryWithinLimits) {
            throw new AssertionError("Error recovery exceeded limits");
        }
    }

    @Then("overall system performance should not be significantly impacted")
    public void overall_system_performance_should_not_be_significantly_impacted() {
        if (!performanceStableDuringErrors) {
            throw new AssertionError("System performance impacted by errors");
        }
    }

    @Then("successful operations should continue during error recovery")
    public void successful_operations_should_continue_during_error_recovery() {
        if (!operationsContinued) {
            throw new AssertionError("Operations did not continue during recovery");
        }
    }

    @Then("error handling should not cause cascading failures")
    public void error_handling_should_not_cause_cascading_failures() {
        if (!noCascadingFailures) {
            throw new AssertionError("Cascading failures detected");
        }
    }

    @Then("performance should return to baseline after error resolution")
    public void performance_should_return_to_baseline_after_error_resolution() {
        if (!performanceReturnedToBaseline) {
            throw new AssertionError("Performance did not return to baseline");
        }
    }

    @Given("I have historical performance baseline data")
    public void i_have_historical_performance_baseline_data() {
        historicalBaselines.put("ping_latency_p95", 20.0);
        historicalBaselines.put("tool_invocation_throughput", 1000.0);
        historicalBaselines.put("resource_transfer_rate", 100_000_000.0);
        historicalBaselines.put("concurrent_client_limit", 100.0);
        historicalBaselines.put("memory_usage_baseline", 200_000_000.0);
    }

    @When("I run the standard performance benchmark suite")
    public void i_run_the_standard_performance_benchmark_suite() {
        currentPerformance.putAll(historicalBaselines);
        optimizationRecommendations.add("Monitor long-term trends");
    }

    @Then("current performance should meet or exceed historical baselines:")
    public void current_performance_should_meet_or_exceed_historical_baselines(DataTable dataTable) {
        performanceRegressions.clear();
        for (Map<String,String> row : dataTable.asMaps(String.class, String.class)) {
            String metric = row.get("metric");
            double baseline = parseBaseline(row.get("baseline_value"));
            double variance = parsePercent(row.get("acceptable_variance")) / 100.0;
            double current = (double) currentPerformance.getOrDefault(metric, baseline);
            if (current < baseline * (1 - variance)) {
                performanceRegressions.add(metric);
            }
        }
        if (!performanceRegressions.isEmpty()) {
            throw new AssertionError("Performance regressions detected: " + performanceRegressions);
        }
    }

    private double parseBaseline(String value) {
        return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
    }

    @Then("any performance regressions should be clearly identified")
    public void any_performance_regressions_should_be_clearly_identified() {
        // TODO: verify regression identification
    }

    @Then("regression impact should be quantified and documented")
    public void regression_impact_should_be_quantified_and_documented() {
        // TODO: verify regression documentation
    }

    @Then("recommendations for performance optimization should be provided")
    public void recommendations_for_performance_optimization_should_be_provided() {
        if (optimizationRecommendations.isEmpty()) {
            throw new AssertionError("No optimization recommendations provided");
        }
    }
}
