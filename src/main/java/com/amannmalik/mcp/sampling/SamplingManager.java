package com.amannmalik.mcp.sampling;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SamplingManager {
    private Map<String, String> preferences = Map.of();
    private List<ModelSelector.Hint> hints = List.of();
    private String request;
    private String selectedModel;
    private String response;
    private String stopReason;

    public void configure(Map<String, String> preferences, List<ModelSelector.Hint> hints) {
        this.preferences = Map.copyOf(preferences);
        this.hints = List.copyOf(hints);
    }

    public void request(String message) {
        request = Objects.requireNonNull(message);
    }

    public boolean pending() {
        return request != null && response == null;
    }

    public void approveRequest(ModelSelector selector) {
        selectedModel = Objects.requireNonNull(selector).select(hints);
    }

    public void sendToLlm(String systemPrompt) {
        Objects.requireNonNull(systemPrompt);
    }

    public void receiveResponse(String text, String stop) {
        response = Objects.requireNonNull(text);
        stopReason = Objects.requireNonNull(stop);
    }

    public String response() {
        return response;
    }

    public String selectedModel() {
        return selectedModel;
    }

    public String stopReason() {
        return stopReason;
    }

    public Map<String, String> preferences() {
        return preferences;
    }

    public List<ModelSelector.Hint> hints() {
        return hints;
    }
}
