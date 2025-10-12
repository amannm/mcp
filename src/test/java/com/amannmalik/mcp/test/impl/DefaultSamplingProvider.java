package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.codec.CreateMessageRequestJsonCodec;
import com.amannmalik.mcp.spi.*;
import jakarta.json.JsonObject;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;

public final class DefaultSamplingProvider implements SamplingProvider {
    @Override
    public CreateMessageResponse createMessage(CreateMessageRequest request, Duration timeoutMillis) throws InterruptedException {
        var shouldReject = request.messages().stream()
                .map(SamplingMessage::content)
                .filter(ContentBlock.Text.class::isInstance)
                .map(ContentBlock.Text.class::cast)
                .map(ContentBlock.Text::text)
                .anyMatch("reject"::equalsIgnoreCase);
        if (shouldReject) {
            throw new InterruptedException("User rejected sampling request");
        }
        return new CreateMessageResponse(
                Role.ASSISTANT,
                new ContentBlock.Text("ok", null, null),
                "mock-model",
                "endTurn",
                null);
    }

    @Override
    public CreateMessageResponse createMessage(CreateMessageRequest request) throws InterruptedException {
        return createMessage(request, Duration.ZERO);
    }

    @Override
    public CreateMessageResponse execute(String name, JsonObject args) throws InterruptedException {
        return createMessage(new CreateMessageRequestJsonCodec().fromJson(args), Duration.ZERO);
    }

    @Override
    public Closeable onListChanged(Runnable listener) {
        return () -> {
        };
    }

    @Override
    public boolean supportsListChanged() {
        return false;
    }

    @Override
    public void close() throws IOException {

    }
}
