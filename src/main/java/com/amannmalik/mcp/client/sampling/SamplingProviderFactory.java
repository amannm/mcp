package com.amannmalik.mcp.client.sampling;

public final class SamplingProviderFactory {
    
    private SamplingProviderFactory() {
        // Utility class
    }

    
    public static InteractiveSamplingProvider createInteractive() {
        return new InteractiveSamplingProvider();
    }
    
    public static InteractiveSamplingProvider createInteractive(boolean autoApprove) {
        return new InteractiveSamplingProvider(autoApprove);
    }
    
    public static SamplingProvider createMock(CreateMessageResponse response) {
        if (response == null) throw new IllegalArgumentException("response is required");
        return (request, timeout) -> response;
    }
    
    public static SamplingProvider createLambda(SamplingFunction function) {
        if (function == null) throw new IllegalArgumentException("function is required");
        return function::apply;
    }
    
    @FunctionalInterface
    public interface SamplingFunction {
        CreateMessageResponse apply(CreateMessageRequest request, long timeoutMillis) throws InterruptedException;
    }
}