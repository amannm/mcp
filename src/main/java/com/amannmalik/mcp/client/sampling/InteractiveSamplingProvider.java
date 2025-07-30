package com.amannmalik.mcp.client.sampling;

import com.amannmalik.mcp.prompts.Role;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public final class InteractiveSamplingProvider implements SamplingProvider {

    private final BufferedReader reader;
    private final boolean autoApprove;

    public InteractiveSamplingProvider() {
        this(false);
    }

    public InteractiveSamplingProvider(boolean autoApprove) {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.autoApprove = autoApprove;
    }

    @Override
    public CreateMessageResponse createMessage(CreateMessageRequest request, long timeoutMillis) throws InterruptedException {
        if (autoApprove) {
            return generateResponse(request);
        }

        try {
            System.err.println("\n=== MCP Sampling Request ===");
            System.err.println("System prompt: " + (request.systemPrompt() != null ? request.systemPrompt() : "(none)"));
            System.err.println("Max tokens: " + request.maxTokens());

            if (request.modelPreferences() != null) {
                var prefs = request.modelPreferences();
                System.err.println("Model preferences:");
                if (prefs.hints() != null && !prefs.hints().isEmpty()) {
                    System.err.print("  Hints: ");
                    prefs.hints().forEach(hint -> System.err.print(hint.name() + " "));
                    System.err.println();
                }
                if (prefs.costPriority() != null) {
                    System.err.println("  Cost priority: " + prefs.costPriority());
                }
                if (prefs.speedPriority() != null) {
                    System.err.println("  Speed priority: " + prefs.speedPriority());
                }
                if (prefs.intelligencePriority() != null) {
                    System.err.println("  Intelligence priority: " + prefs.intelligencePriority());
                }
            }

            System.err.println("\nMessages:");
            List<SamplingMessage> messages = request.messages();
            for (int i = 0; i < messages.size(); i++) {
                SamplingMessage msg = messages.get(i);
                System.err.println("  " + (i + 1) + ". [" + msg.role() + "] " + formatContent(msg.content()));
            }

            System.err.print("\nApprove this sampling request? [y/N/edit]: ");
            String response = reader.readLine();

            if (response == null || response.trim().isEmpty() || response.toLowerCase().startsWith("n")) {
                throw new InterruptedException("User rejected sampling request");
            }

            if (response.toLowerCase().startsWith("e")) {
                System.err.println("Edit mode not implemented in this version. Proceeding with original request.");
            }

            System.err.println("Generating response...");
            CreateMessageResponse result = generateResponse(request);

            System.err.println("\n=== Generated Response ===");
            System.err.println("Role: " + result.role());
            System.err.println("Content: " + formatContent(result.content()));
            System.err.println("Model: " + (result.model() != null ? result.model() : "(unknown)"));
            System.err.println("Stop reason: " + (result.stopReason() != null ? result.stopReason() : "(unknown)"));

            System.err.print("\nApprove this response? [Y/n]: ");
            String approveResponse = reader.readLine();

            if (approveResponse != null && approveResponse.toLowerCase().startsWith("n")) {
                throw new InterruptedException("User rejected response");
            }

            System.err.println("Response approved and sent.\n");
            return result;

        } catch (IOException e) {
            throw new InterruptedException("IO error during user interaction: " + e.getMessage());
        }
    }

    private String formatContent(MessageContent content) {
        return switch (content) {
            case MessageContent.Text text -> text.text();
            case MessageContent.Image image -> "[Image: " + image.mimeType() + ", " + image.data().length + " bytes]";
            case MessageContent.Audio audio -> "[Audio: " + audio.mimeType() + ", " + audio.data().length + " bytes]";
        };
    }

    private CreateMessageResponse generateResponse(CreateMessageRequest request) {
        String responseText = generateSimpleResponse(request);

        return new CreateMessageResponse(
                Role.ASSISTANT,
                new MessageContent.Text(responseText, null, null),
                "simulated-llm",
                "endTurn",
                null
        );
    }

    private String generateSimpleResponse(CreateMessageRequest request) {
        // Extract the last user message for context
        List<SamplingMessage> messages = request.messages();
        String lastUserMessage = "";

        for (int i = messages.size() - 1; i >= 0; i--) {
            SamplingMessage msg = messages.get(i);
            if (msg.role() == Role.USER && msg.content() instanceof MessageContent.Text text) {
                lastUserMessage = text.text();
                break;
            }
        }

        // Generate a simple contextual response
        if (lastUserMessage.toLowerCase().contains("hello") || lastUserMessage.toLowerCase().contains("hi")) {
            return "Hello! How can I assist you today?";
        } else if (lastUserMessage.toLowerCase().contains("help")) {
            return "I'm here to help! Please let me know what you need assistance with.";
        } else if (lastUserMessage.toLowerCase().contains("weather")) {
            return "I don't have access to real-time weather data, but I'd be happy to help you find weather information or discuss weather-related topics.";
        } else if (lastUserMessage.toLowerCase().contains("time")) {
            return "I don't have access to the current time, but I can help you with time-related calculations or questions.";
        } else {
            return "I understand you're asking about: \"" +
                    (lastUserMessage.length() > 50 ? lastUserMessage.substring(0, 50) + "..." : lastUserMessage) +
                    "\". I'm a simulated assistant and would be happy to help with information or guidance on this topic.";
        }
    }
}