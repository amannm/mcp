package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.api.model.*;
import jakarta.json.*;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

public final class InteractiveSamplingProvider implements SamplingProvider {

    private final BufferedReader reader;
    private final boolean autoApprove;

    public InteractiveSamplingProvider(boolean autoApprove) {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.autoApprove = autoApprove;
    }

    @Override
    public CreateMessageResponse createMessage(CreateMessageRequest request, long timeoutMillis) throws InterruptedException {
        if (autoApprove) {
            boolean reject = request.messages().stream()
                    .map(SamplingMessage::content)
                    .filter(ContentBlock.Text.class::isInstance)
                    .map(ContentBlock.Text.class::cast)
                    .map(ContentBlock.Text::text)
                    .anyMatch(t -> t.equalsIgnoreCase("reject"));
            if (reject) throw new InterruptedException("User rejected sampling request");
            return new CreateMessageResponse(
                    Role.ASSISTANT,
                    new ContentBlock.Text("ok", null, null),
                    "mock-model",
                    "endTurn",
                    null);
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
            String response = readLine(timeoutMillis);

            if (response == null || response.trim().isEmpty() || response.toLowerCase().startsWith("n")) {
                throw new InterruptedException("User rejected sampling request");
            }

            if (response.toLowerCase().startsWith("e")) {
                System.err.println("Edit mode not implemented in this version. Proceeding with original request.");
            }

            System.err.println("Generating response...");
            CreateMessageResponse result = generateResponse(request, timeoutMillis);

            System.err.println("\n=== Generated Response ===");
            System.err.println("Role: " + result.role());
            System.err.println("Content: " + formatContent(result.content()));
            System.err.println("Model: " + (result.model() != null ? result.model() : "(unknown)"));
            System.err.println("Stop reason: " + (result.stopReason() != null ? result.stopReason() : "(unknown)"));

            System.err.print("\nApprove this response? [Y/n]: ");
            String approveResponse = readLine(timeoutMillis);

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
            case ContentBlock.Text text -> text.text();
            case ContentBlock.Image image -> "[Image: " + image.mimeType() + ", " + image.data().length + " bytes]";
            case ContentBlock.Audio audio -> "[Audio: " + audio.mimeType() + ", " + audio.data().length + " bytes]";
            default -> "[Unknown content]";
        };
    }

    private CreateMessageResponse generateResponse(CreateMessageRequest request, long timeoutMillis) throws InterruptedException {
        try {
            var ai = openAiResponse(request, timeoutMillis);
            if (ai.isPresent()) {
                return new CreateMessageResponse(
                        Role.ASSISTANT,
                        new ContentBlock.Text(ai.get().content(), null, null),
                        ai.get().model(),
                        "endTurn",
                        null
                );
            }
        } catch (IOException ignore) {
        }

        String responseText = generateSimpleResponse(request);

        return new CreateMessageResponse(
                Role.ASSISTANT,
                new ContentBlock.Text(responseText, null, null),
                "claude-3-sonnet-simulation",
                "endTurn",
                null
        );
    }

    private Optional<AiResult> openAiResponse(CreateMessageRequest request, long timeoutMillis) throws IOException, InterruptedException {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) return Optional.empty();

        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        if (timeoutMillis > 0) clientBuilder.connectTimeout(Duration.ofMillis(timeoutMillis));
        HttpClient client = clientBuilder.build();

        JsonArrayBuilder msgs = Json.createArrayBuilder();
        if (request.systemPrompt() != null) {
            msgs.add(Json.createObjectBuilder()
                    .add("role", "system")
                    .add("content", request.systemPrompt())
                    .build());
        }
        for (SamplingMessage m : request.messages()) {
            if (m.content() instanceof ContentBlock.Text t) {
                msgs.add(Json.createObjectBuilder()
                        .add("role", m.role().name().toLowerCase())
                        .add("content", t.text())
                        .build());
            }
        }

        JsonObject body = Json.createObjectBuilder()
                .add("model", "gpt-3.5-turbo")
                .add("messages", msgs)
                .add("max_tokens", request.maxTokens())
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        if (timeoutMillis > 0) requestBuilder.timeout(Duration.ofMillis(timeoutMillis));
        HttpRequest httpRequest = requestBuilder.build();

        HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) return Optional.empty();

        JsonObject obj = Json.createReader(new StringReader(response.body())).readObject();
        var choices = obj.getJsonArray("choices");
        if (choices == null || choices.isEmpty()) return Optional.empty();
        var msg = choices.getJsonObject(0).getJsonObject("message");
        if (msg == null) return Optional.empty();
        String content = msg.getString("content", null);
        if (content == null) return Optional.empty();
        String model = obj.getString("model", "openai");
        return Optional.of(new AiResult(content.trim(), model));
    }

    private String readLine(long timeoutMillis) throws IOException, InterruptedException {
        if (timeoutMillis <= 0) return reader.readLine();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> future = executor.submit(reader::readLine);
            try {
                return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new InterruptedException("Timed out waiting for user input");
            }
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new InterruptedException(cause.toString());
        } finally {
            executor.shutdownNow();
        }
    }

    private String generateSimpleResponse(CreateMessageRequest request) {
        // Extract the last user message for context
        List<SamplingMessage> messages = request.messages();
        String lastUserMessage = "";

        for (int i = messages.size() - 1; i >= 0; i--) {
            SamplingMessage msg = messages.get(i);
            if (msg.role() == Role.USER && msg.content() instanceof ContentBlock.Text text) {
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

    private record AiResult(String content, String model) {
    }
}