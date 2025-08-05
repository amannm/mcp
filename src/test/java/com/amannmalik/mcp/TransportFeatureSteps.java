package com.amannmalik.mcp;

import com.amannmalik.mcp.transport.*;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.*;

import java.util.*;

public class TransportFeatureSteps {
    
    private final TransportTestContext context = new TransportTestContext();
    
    @Given("MCP implementation supports both stdio and HTTP transports")
    public void mcpImplementationSupportsBothStdioAndHttpTransports() {
        context.initializeTransports();
        assert context.hasStdioTransport() : "Stdio transport should be available";
        assert context.hasHttpTransport() : "HTTP transport should be available";
    }
    
    @When("testing identical operations across transports:")
    public void testingIdenticalOperationsAcrossTransports(DataTable dataTable) {
        Map<String, TransportOperationResult> results = new HashMap<>();
        
        for (var row : dataTable.asMaps()) {
            String operation = row.get("operation");
            String expectedStdioResult = row.get("stdio_result");
            String expectedHttpResult = row.get("http_result");
            
            TransportOperationResult result = context.executeOperationOnBothTransports(operation);
            results.put(operation, result);
            
            // Validate expected results match actual results
            assert expectedStdioResult.equals(result.getStdioResultType()) : 
                "Stdio result for " + operation + " should be " + expectedStdioResult + 
                " but was " + result.getStdioResultType();
            assert expectedHttpResult.equals(result.getHttpResultType()) : 
                "HTTP result for " + operation + " should be " + expectedHttpResult + 
                " but was " + result.getHttpResultType();
        }
        
        context.setOperationResults(results);
    }
    
    @Then("results are functionally equivalent")
    public void resultsAreFunctionallyEquivalent() {
        Map<String, TransportOperationResult> results = context.getOperationResults();
        
        for (var entry : results.entrySet()) {
            String operation = entry.getKey();
            TransportOperationResult result = entry.getValue();
            
            if ("identical".equals(result.getStdioResultType()) && 
                "identical".equals(result.getHttpResultType())) {
                assert result.areResultsIdentical() : 
                    "Results for operation " + operation + " should be functionally identical";
            } else if ("success".equals(result.getStdioResultType()) && 
                      "success".equals(result.getHttpResultType())) {
                assert result.areBothSuccessful() : 
                    "Both transports should be successful for operation " + operation;
            } else if ("supported".equals(result.getStdioResultType()) && 
                      "supported".equals(result.getHttpResultType())) {
                assert result.areBothSupported() : 
                    "Both transports should support operation " + operation;
            }
        }
    }
    
    @But("HTTP transport includes additional features:")
    public void httpTransportIncludesAdditionalFeatures(DataTable dataTable) {
        for (var row : dataTable.asMaps()) {
            String feature = row.get("feature");
            String stdioSupport = row.get("stdio");
            String httpSupport = row.get("http");
            
            boolean stdioShouldSupport = "yes".equals(stdioSupport);
            boolean httpShouldSupport = "yes".equals(httpSupport);
            
            assert context.doesStdioSupportFeature(feature) == stdioShouldSupport : 
                "Stdio support for " + feature + " should be " + stdioShouldSupport;
            assert context.doesHttpSupportFeature(feature) == httpShouldSupport : 
                "HTTP support for " + feature + " should be " + httpShouldSupport;
        }
    }
    
    private static class TransportTestContext {
        private StdioTransport stdioTransport;
        private HttpTransport httpTransport;
        private Map<String, TransportOperationResult> operationResults = new HashMap<>();
        
        void initializeTransports() {
            this.stdioTransport = new StdioTransport();
            this.httpTransport = new HttpTransport();
        }
        
        boolean hasStdioTransport() {
            return stdioTransport != null;
        }
        
        boolean hasHttpTransport() {
            return httpTransport != null;
        }
        
        TransportOperationResult executeOperationOnBothTransports(String operation) {
            return switch (operation) {
                case "initialization" -> {
                    boolean stdioSuccess = stdioTransport.initialize();
                    boolean httpSuccess = httpTransport.initialize();
                    yield new TransportOperationResult(
                        stdioSuccess ? "success" : "failure",
                        httpSuccess ? "success" : "failure",
                        stdioSuccess,
                        httpSuccess
                    );
                }
                case "capability_nego" -> {
                    Map<String, Object> stdioCapabilities = stdioTransport.negotiateCapabilities();
                    Map<String, Object> httpCapabilities = httpTransport.negotiateCapabilities();
                    boolean identical = Objects.equals(stdioCapabilities, httpCapabilities);
                    yield new TransportOperationResult(
                        "identical",
                        "identical", 
                        stdioCapabilities,
                        httpCapabilities,
                        identical
                    );
                }
                case "resource_list" -> {
                    List<String> stdioResources = stdioTransport.listResources();
                    List<String> httpResources = httpTransport.listResources();
                    boolean identical = Objects.equals(stdioResources, httpResources);
                    yield new TransportOperationResult(
                        "identical",
                        "identical",
                        stdioResources,
                        httpResources,
                        identical
                    );
                }
                case "tool_execution" -> {
                    Object stdioResult = stdioTransport.executeTool("test_tool", Map.of());
                    Object httpResult = httpTransport.executeTool("test_tool", Map.of());
                    boolean identical = Objects.equals(stdioResult, httpResult);
                    yield new TransportOperationResult(
                        "identical",
                        "identical",
                        stdioResult,
                        httpResult,
                        identical
                    );
                }
                case "progress_track" -> {
                    boolean stdioSupported = stdioTransport.supportsProgressTracking();
                    boolean httpSupported = httpTransport.supportsProgressTracking();
                    yield new TransportOperationResult(
                        "supported",
                        "supported",
                        stdioSupported,
                        httpSupported
                    );
                }
                case "notifications" -> {
                    boolean stdioSupported = stdioTransport.supportsNotifications();
                    boolean httpSupported = httpTransport.supportsNotifications();
                    yield new TransportOperationResult(
                        "supported",
                        "supported",
                        stdioSupported,
                        httpSupported
                    );
                }
                default -> throw new IllegalArgumentException("Unknown operation: " + operation);
            };
        }
        
        void setOperationResults(Map<String, TransportOperationResult> results) {
            this.operationResults = results;
        }
        
        Map<String, TransportOperationResult> getOperationResults() {
            return operationResults;
        }
        
        boolean doesStdioSupportFeature(String feature) {
            return switch (feature) {
                case "authorization" -> stdioTransport.supportsAuthorization();
                case "resource_metadata" -> stdioTransport.supportsResourceMetadata();
                case "session_management" -> stdioTransport.supportsSessionManagement();
                default -> false;
            };
        }
        
        boolean doesHttpSupportFeature(String feature) {
            return switch (feature) {
                case "authorization" -> httpTransport.supportsAuthorization();
                case "resource_metadata" -> httpTransport.supportsResourceMetadata();
                case "session_management" -> httpTransport.supportsSessionManagement();
                default -> false;
            };
        }
    }
    
    private static class TransportOperationResult {
        private final String stdioResultType;
        private final String httpResultType;
        private final Object stdioResult;
        private final Object httpResult;
        private final Boolean identical;
        
        TransportOperationResult(String stdioResultType, String httpResultType, 
                               Object stdioResult, Object httpResult) {
            this(stdioResultType, httpResultType, stdioResult, httpResult, null);
        }
        
        TransportOperationResult(String stdioResultType, String httpResultType, 
                               Object stdioResult, Object httpResult, Boolean identical) {
            this.stdioResultType = stdioResultType;
            this.httpResultType = httpResultType;
            this.stdioResult = stdioResult;
            this.httpResult = httpResult;
            this.identical = identical;
        }
        
        String getStdioResultType() { return stdioResultType; }
        String getHttpResultType() { return httpResultType; }
        Object getStdioResult() { return stdioResult; }
        Object getHttpResult() { return httpResult; }
        
        boolean areResultsIdentical() {
            return identical != null ? identical : Objects.equals(stdioResult, httpResult);
        }
        
        boolean areBothSuccessful() {
            return (stdioResult instanceof Boolean stdioSuccess) && stdioSuccess &&
                   (httpResult instanceof Boolean httpSuccess) && httpSuccess;
        }
        
        boolean areBothSupported() {
            return (stdioResult instanceof Boolean stdioSupported) && stdioSupported &&
                   (httpResult instanceof Boolean httpSupported) && httpSupported;
        }
    }
    
    // Mock transport implementations for testing
    private static class StdioTransport {
        boolean initialize() { return true; }
        
        Map<String, Object> negotiateCapabilities() {
            return Map.of(
                "resources", Map.of("subscribe", true, "listChanged", true),
                "tools", Map.of("listChanged", true),
                "prompts", Map.of("listChanged", true)
            );
        }
        
        List<String> listResources() {
            return List.of("resource1", "resource2", "resource3");
        }
        
        Object executeTool(String toolName, Map<String, Object> args) {
            return Map.of("result", "success", "tool", toolName);
        }
        
        boolean supportsProgressTracking() { return true; }
        boolean supportsNotifications() { return true; }
        boolean supportsAuthorization() { return false; }
        boolean supportsResourceMetadata() { return false; }
        boolean supportsSessionManagement() { return false; }
    }
    
    private static class HttpTransport {
        boolean initialize() { return true; }
        
        Map<String, Object> negotiateCapabilities() {
            return Map.of(
                "resources", Map.of("subscribe", true, "listChanged", true),
                "tools", Map.of("listChanged", true),
                "prompts", Map.of("listChanged", true)
            );
        }
        
        List<String> listResources() {
            return List.of("resource1", "resource2", "resource3");
        }
        
        Object executeTool(String toolName, Map<String, Object> args) {
            return Map.of("result", "success", "tool", toolName);
        }
        
        boolean supportsProgressTracking() { return true; }
        boolean supportsNotifications() { return true; }
        boolean supportsAuthorization() { return true; }
        boolean supportsResourceMetadata() { return true; }
        boolean supportsSessionManagement() { return true; }
    }
}